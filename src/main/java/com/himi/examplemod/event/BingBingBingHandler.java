package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 冰冰冰护符事件处理器。
 * 装备在 Curios 护符栏位时：
 * - 当受到的伤害大于 2 点，移除周围生物的 AI（使其无法行动），并播放冰冻粒子与音效
 * - 6 秒（120 ticks）后复原生物 AI
 * - 内置冷却 25 秒（500 ticks）
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class BingBingBingHandler {

    private static final float DAMAGE_THRESHOLD = 2.0F;   // 伤害阈值：大于2点
    private static final double RADIUS = 16.0;            // 影响半径（格）
    private static final int AI_DISABLE_TICKS = 120;      // AI移除持续：6秒 = 120 ticks
    private static final int COOLDOWN_TICKS = 500;        // 冷却：25秒 = 500 ticks

    // 玩家冷却：UUID -> 冷却结束的 gameTime
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    // 被移除AI的生物：Mob -> 复原的 gameTime
    private static final Map<Mob, Long> DISABLED_MOBS = new HashMap<>();

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 仅处理大于2点的伤害
        if (event.getAmount() <= DAMAGE_THRESHOLD) return;
        // 必须装备冰冰冰护符
        if (!hasBingBingBing(player)) return;

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        // 检查冷却，未冷却完成则不触发
        Long cooldownEnd = COOLDOWNS.get(player.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // 移除周围生物的AI，并给出冰冻粒子反馈
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS));
        int frozen = 0;
        for (Mob mob : mobs) {
            // 跳过本就无AI的生物，避免误恢复其AI
            if (mob.isAlive() && !mob.isNoAi()) {
                mob.setNoAi(true);
                DISABLED_MOBS.put(mob, now + AI_DISABLE_TICKS);
                frozen++;
                // 冰冻粒子：在被冻生物身上撒一片雪花
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                        16, 0.6, mob.getBbHeight() * 0.6, 0.6, 0.0);
            }
        }

        // 音效反馈：确实冻住了生物才播放
        if (frozen > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // 设置冷却
        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 复原到时的生物AI
        if (!DISABLED_MOBS.isEmpty()) {
            Iterator<Map.Entry<Mob, Long>> it = DISABLED_MOBS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Mob, Long> entry = it.next();
                Mob mob = entry.getKey();
                // 生物已消失或死亡，直接清理记录
                if (mob.isRemoved() || !mob.isAlive()) {
                    it.remove();
                    continue;
                }
                // 到达复原时间，恢复AI
                if (mob.level().getGameTime() >= entry.getValue()) {
                    mob.setNoAi(false);
                    it.remove();
                }
            }
        }

        // 清理过期的玩家冷却记录，避免无限增长
        if (!COOLDOWNS.isEmpty()) {
            long now = event.getServer().overworld().getGameTime();
            COOLDOWNS.values().removeIf(end -> now >= end);
        }
    }

    /**
     * 检查玩家是否在 Curios 护符栏位装备了冰冰冰
     */
    private static boolean hasBingBingBing(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.BING_BING_BING.get())))
                .isPresent();
    }
}
