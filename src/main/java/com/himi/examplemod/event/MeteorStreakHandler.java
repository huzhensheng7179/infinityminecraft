package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 流星一条事件处理器。
 * 装备在 Curios 项链栏位时：
 * - 使用弓蓄力可持续积攒伤害：满蓄力（20 ticks）之后，每多蓄力 1 秒，
 *   箭矢伤害提升原伤害的 2%，最高提升至原伤害的 300%。
 *
 * 实现原理：
 * - 弓松开时（{@link ArrowLooseEvent}）根据蓄力 ticks 计算伤害倍率并暂存；
 * - 箭矢加入世界时（{@link EntityJoinLevelEvent}）按比例放大其 baseDamage。
 *   箭矢命中伤害 = 速度 × baseDamage，速度在满蓄力后已封顶，故放大 baseDamage 即线性放大最终伤害。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class MeteorStreakHandler {

    private static final int FULL_CHARGE_TICKS = 20;       // 弓满蓄力所需 ticks（BowItem.MAX_DRAW_DURATION）
    private static final double BONUS_PER_SECOND = 0.02;   // 满蓄力后每蓄力 1 秒 +2% 原伤害
    private static final double MAX_MULTIPLIER = 3.0;      // 伤害倍率上限：原来的 300%
    private static final int EXPIRY_TICKS = 5;             // 蓄力记录有效期（ticks），兼容多重射击

    // 玩家 UUID -> 本次射击的伤害倍率与过期 gameTime
    private static final Map<UUID, ChargeBonus> CHARGE_BONUS = new HashMap<>();

    private record ChargeBonus(double multiplier, long expireGameTime) {}

    /**
     * 弓松开事件：根据蓄力时长计算伤害倍率并暂存
     */
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        // 仅弓类型武器（排除弩：弩的 charge 恒为 1）
        if (!(event.getBow().getItem() instanceof BowItem)) return;
        // 必须装备流星一条
        if (!hasMeteorStreak(player)) return;

        long now = player.level().getGameTime();
        purgeExpired(now);

        int charge = event.getCharge();
        double multiplier = 1.0;
        // 满蓄力之后，每多蓄力 1 秒（20 ticks）提升 2% 原伤害，封顶 300%
        if (charge > FULL_CHARGE_TICKS) {
            double extraSeconds = (charge - FULL_CHARGE_TICKS) / 20.0;
            multiplier = Math.min(MAX_MULTIPLIER, 1.0 + BONUS_PER_SECOND * extraSeconds);
        }
        CHARGE_BONUS.put(player.getUUID(), new ChargeBonus(multiplier, now + EXPIRY_TICKS));
    }

    /**
     * 箭矢加入世界事件：将暂存的倍率应用到箭矢基础伤害
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;

        ChargeBonus bonus = CHARGE_BONUS.get(player.getUUID());
        if (bonus == null) return;
        // 过期则清理，避免误用于后续无关箭矢
        if (event.getLevel().getGameTime() > bonus.expireGameTime()) {
            CHARGE_BONUS.remove(player.getUUID());
            return;
        }
        if (bonus.multiplier() > 1.0) {
            arrow.setBaseDamage(arrow.getBaseDamage() * bonus.multiplier());
        }
    }

    /**
     * 清理过期的蓄力记录
     */
    private static void purgeExpired(long now) {
        Iterator<Map.Entry<UUID, ChargeBonus>> it = CHARGE_BONUS.entrySet().iterator();
        while (it.hasNext()) {
            if (now > it.next().getValue().expireGameTime()) {
                it.remove();
            }
        }
    }

    /**
     * 检查玩家是否在 Curios 项链栏位装备了流星一条
     */
    private static boolean hasMeteorStreak(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.METEOR_STREAK.get())))
                .isPresent();
    }
}
