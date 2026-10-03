package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 流星一条事件处理器。
 * 装备在 Curios 项链栏位时：
 * - 使用弓蓄力可持续积攒伤害：满蓄力（20 ticks）之后，每多蓄力 1 秒，
 *   箭矢伤害提升原伤害的 100%，最高提升至原伤害的 5000%。
 * - 蓄力时周身持续浮现附魔台符文粒子；伤害倍率每提升 100%（+1.0 倍）在玩家身上闪烁一次橙色粒子
 *   并播放一次附魔成功音效；蓄力达到强化上限（5000%）时，持续闪烁紫色与橙色粒子；
 *   被强化的箭矢击中目标时，在命中点释放不死图腾音效与图腾粒子特效。
 *
 * 实现原理：
 * - 弓松开时（{@link ArrowLooseEvent}）根据蓄力 ticks 计算伤害倍率并暂存；
 * - 箭矢加入世界时（{@link EntityJoinLevelEvent}）按比例放大其 baseDamage，
 *   并记录为「流星箭」，供命中时播放图腾特效；
 * - 蓄力过程中的粒子由 {@link PlayerTickEvent.Post} 依据当前蓄力档位驱动。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class MeteorStreakHandler {

    private static final int FULL_CHARGE_TICKS = 20;       // 弓满蓄力所需 ticks（BowItem.MAX_DRAW_DURATION）
    private static final double BONUS_PER_SECOND = 1.00;   // 满蓄力后每蓄力 1 秒 +100% 原伤害
    private static final double MAX_MULTIPLIER = 50.0;     // 伤害倍率上限：原来的 5000%
    private static final int EXPIRY_TICKS = 5;             // 蓄力记录有效期（ticks），兼容多重射击

    // 橙色 / 紫色尘埃粒子（蓄力闪烁用）
    private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.10F), 1.4F);
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.20F, 1.0F), 1.6F);

    // 玩家 UUID -> 本次射击的伤害倍率与过期 gameTime
    private static final Map<UUID, ChargeBonus> CHARGE_BONUS = new HashMap<>();
    // 玩家 UUID -> 蓄力过程中已闪烁到的倍率档位（用于每 +100% 触发一次橙色闪烁）
    private static final Map<UUID, Integer> CHARGE_TIER = new HashMap<>();
    // 被强化的箭矢 -> 伤害倍率（命中时播放图腾特效；按实体身份索引）
    private static final Map<AbstractArrow, Double> BOOSTED_ARROWS = new HashMap<>();

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
        double multiplier = computeMultiplier(charge);
        CHARGE_BONUS.put(player.getUUID(), new ChargeBonus(multiplier, now + EXPIRY_TICKS));

        // 松开后重置蓄力档位记录，下次拉弓从头开始闪烁
        CHARGE_TIER.remove(player.getUUID());
    }

    /**
     * 箭矢加入世界事件：将暂存的倍率应用到箭矢基础伤害，并记录为流星箭
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
            // 记录该箭为流星箭，命中时播放图腾特效
            BOOSTED_ARROWS.put(arrow, bonus.multiplier());
        }
    }

    /**
     * 玩家 tick 事件：蓄力过程中驱动橙色 / 紫色粒子闪烁
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // 仅在「装备流星一条 + 正在拉弓」时处理
        boolean drawing = player.isUsingItem()
                && player.getUseItem().getItem() instanceof BowItem
                && hasMeteorStreak(player);
        if (!drawing) {
            CHARGE_TIER.remove(player.getUUID());
            return;
        }

        int charge = player.getTicksUsingItem();
        double multiplier = computeMultiplier(charge);
        int tier = (int) Math.floor(multiplier);

        // 蓄力时周身持续浮现附魔台符文粒子
        spawnEnchantAura(level, player);

        // 每跨越一个 +100% 档位，闪烁一次橙色粒子并播放附魔成功音效
        int lastTier = CHARGE_TIER.getOrDefault(player.getUUID(), 1);
        if (tier > lastTier) {
            spawnOrangeBurst(level, player);
        }
        CHARGE_TIER.put(player.getUUID(), tier);

        // 达到强化上限：持续闪烁紫色与橙色粒子
        if (multiplier >= MAX_MULTIPLIER && level.getGameTime() % 4 == 0) {
            spawnMaxFlash(level, player);
        }
    }

    /**
     * 伤害事件：被强化的流星箭击中目标时，在命中点释放不死图腾音效与粒子
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(victim.level() instanceof ServerLevel level)) return;

        // 仅对本次被强化的流星箭生效
        Double multiplier = BOOSTED_ARROWS.remove(arrow);
        if (multiplier == null) return;

        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.5;
        double z = victim.getZ();

        // 不死图腾音效
        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        // 不死图腾粒子爆发
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 100, 0.6, 0.8, 0.6, 1.0);
    }

    /**
     * 服务端 tick：清理已消失的流星箭记录，避免静态 Map 泄漏
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!BOOSTED_ARROWS.isEmpty()) {
            BOOSTED_ARROWS.keySet().removeIf(arrow -> arrow.isRemoved() || !arrow.isAlive());
        }
    }

    /**
     * 根据蓄力 ticks 计算伤害倍率（满蓄力后每 1 秒 +100%，封顶 5000%）
     */
    private static double computeMultiplier(int charge) {
        if (charge <= FULL_CHARGE_TICKS) return 1.0;
        double extraSeconds = (charge - FULL_CHARGE_TICKS) / 20.0;
        return Math.min(MAX_MULTIPLIER, 1.0 + BONUS_PER_SECOND * extraSeconds);
    }

    /**
     * 橙色粒子爆发（每 +100% 触发一次），同时播放附魔成功音效
     */
    private static void spawnOrangeBurst(ServerLevel level, Player player) {
        level.sendParticles(ORANGE_DUST,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.5, 1.0, 0.5, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 蓄力时周身浮现的附魔台符文粒子
     */
    private static void spawnEnchantAura(ServerLevel level, Player player) {
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0.8, 1.0, 0.8, 0.6);
    }

    /**
     * 达到上限时的紫色 + 橙色闪烁
     */
    private static void spawnMaxFlash(ServerLevel level, Player player) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();
        level.sendParticles(PURPLE_DUST, x, y, z, 25, 0.7, 1.2, 0.7, 0.0);
        level.sendParticles(ORANGE_DUST, x, y, z, 25, 0.7, 1.2, 0.7, 0.0);
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
