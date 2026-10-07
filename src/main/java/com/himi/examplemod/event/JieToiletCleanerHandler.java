package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * “杰”厕灵事件处理器。
 * 装备在 Curios 背饰栏位时：
 * - 被动免疫接下来受到的 3 次任何伤害（每次命中消耗 1 层）；
 * - 3 次免疫用尽后，立即损失最大生命值的 70%（可能致死）；
 * - 随后进入 60 秒（1200 ticks）冷却，冷却期间不再免疫；
 * - 冷却结束后重新恢复为 3 层免疫。
 *
 * 关键：取消 {@link LivingIncomingDamageEvent} 后，原版不会赋予无敌帧
 * （LivingEntity#hurt 在事件被取消处直接 return，早于 invulnerableTime 赋值），
 * 因此火焰/岩浆/中毒/溺水等每 tick 持续伤害、或多个生物同 tick 命中会瞬间耗尽 3 层。
 * 为此引入 IMMUNITY_GRACE_TICKS 无敌窗口：每消耗 1 层后的一段窗口内，继续免疫但不消耗层数，
 * 使“一次免疫”对应一次离散受击（至少间隔 1 秒），符合“免疫 3 次伤害”的预期。
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class JieToiletCleanerHandler {

    private static final int MAX_CHARGES = 3;             // 免疫层数
    private static final int COOLDOWN_TICKS = 60 * 20;    // 冷却：60秒 = 1200 ticks
    private static final float HEALTH_LOSS_RATIO = 0.7F;  // 用尽后损失最大生命的 70%
    private static final int IMMUNITY_GRACE_TICKS = 20;   // 每消耗 1 层后的无敌窗口：20 ticks = 1 秒

    // 剩余免疫层数：UUID -> charges（仅在消耗过部分层数时存在；归零即移除）
    private static final Map<UUID, Integer> CHARGES = new HashMap<>();
    // 冷却结束时间：UUID -> gameTime
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    // 无敌窗口结束时间：UUID -> gameTime（窗口内免疫但不消耗层数）
    private static final Map<UUID, Long> GRACE_UNTIL = new HashMap<>();

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!hasJieToiletCleaner(player)) return;

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        UUID id = player.getUUID();

        // 冷却期间不免疫
        Long cooldownEnd = COOLDOWNS.get(id);
        if (cooldownEnd != null && now < cooldownEnd) return;

        // 无敌窗口内：免疫本次伤害但不消耗层数（防止持续/同 tick 多次伤害瞬间耗尽）
        Long graceEnd = GRACE_UNTIL.get(id);
        if (graceEnd != null && now < graceEnd) {
            event.setCanceled(true);
            return;
        }

        // 无记录时视为满层（3）；已归零的记录会在用尽时移除，故此处不会 <= 0
        int charges = CHARGES.getOrDefault(id, MAX_CHARGES);
        if (charges <= 0) return;

        // 消耗 1 层并免疫本次伤害，同时开启无敌窗口
        event.setCanceled(true);
        charges--;
        GRACE_UNTIL.put(id, now + IMMUNITY_GRACE_TICKS);

        if (charges > 0) {
            CHARGES.put(id, charges);
        } else {
            // 3 次用尽：先清除充能与无敌窗口、设置冷却，再结算惩罚，避免惩罚伤害被自身免疫拦截
            CHARGES.remove(id);
            GRACE_UNTIL.remove(id);
            COOLDOWNS.put(id, now + COOLDOWN_TICKS);
            applyHealthPenalty(player);
        }
    }

    /**
     * 损失最大生命值的 70%（扁平扣除，无视护甲，附带扣血音效与受击动画）；若会致死则以绕过一切的伤害击杀。
     */
    private static void applyHealthPenalty(Player player) {
        float loss = HEALTH_LOSS_RATIO * player.getMaxHealth();
        float remaining = player.getHealth() - loss;
        if (remaining <= 0.0F) {
            player.hurt(player.damageSources().genericKill(), player.getHealth() + 1.0F);
        } else {
            player.setHealth(remaining);
            // 扁平扣血不走伤害管线，手动广播受击事件：播放扣血音效 + 红闪/倾斜受击动画
            ((ServerLevel) player.level()).broadcastDamageEvent(player, player.damageSources().generic());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        // 清理过期冷却记录，避免无限增长；过期后玩家自然恢复为满层免疫
        if (!COOLDOWNS.isEmpty()) {
            COOLDOWNS.values().removeIf(end -> now >= end);
        }
        // 清理过期无敌窗口记录
        if (!GRACE_UNTIL.isEmpty()) {
            GRACE_UNTIL.values().removeIf(end -> now >= end);
        }
    }

    /**
     * 检查玩家是否在 Curios 背饰栏位装备了“杰”厕灵。
     */
    private static boolean hasJieToiletCleaner(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.JIE_TOILET_CLEANER.get())))
                .isPresent();
    }
}
