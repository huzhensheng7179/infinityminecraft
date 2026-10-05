package com.himi.examplemod.event;

import java.util.UUID;

import javax.annotation.Nullable;

import com.himi.examplemod.entity.WanderingSnifferMerchant;
import com.himi.examplemod.infinityminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 流浪嗅探兽商人自然刷新器：复刻原版流浪商人的计时/消失机制，并将刷新概率改为每天 0.12%。
 *
 * <p>仅在主世界、非和平难度、且开启生物自然生成游戏规则时驱动；到点后随机选一名玩家，在其附近 24~48 格、
 * 地表高度、有足够站立空间且非「禁止流浪商人生成」生物群系处尝试生成。刷新出的商人存在 2.5 游戏日（48000 tick）后自动消失。</p>
 *
 * <p>全局同时至多存在 1 只（由最近一次刷新实体的 UUID 追踪其存活状态）。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class SnifferMerchantSpawnerHandler {

    private SnifferMerchantSpawnerHandler() {
    }

    /** 原版流浪商人的计时基准。 */
    private static final int TICK_DELAY = 1200;        // 每 1200 tick（1 分钟）推进一次
    private static final int SPAWN_DELAY = 24000;      // 每 24000 tick（1 游戏日）尝试一次

    /** 每日刷新概率：0.12%。 */
    private static final double DAILY_SPAWN_CHANCE = 0.0012D;

    /** 消失时间：2.5 游戏日（48000 tick）。 */
    private static final int DESPAWN_DELAY = 48000;

    /** 生成尝试次数与距玩家距离区间。 */
    private static final int SPAWN_ATTEMPTS = 10;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int tickDelay = TICK_DELAY;
    private static int spawnDelay = SPAWN_DELAY;

    @Nullable
    private static UUID lastSpawnedId;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        if (--tickDelay > 0) {
            return;
        }
        tickDelay = TICK_DELAY;
        spawnDelay -= TICK_DELAY;
        if (spawnDelay > 0) {
            return;
        }
        spawnDelay = SPAWN_DELAY;

        // 每天固定 0.12% 概率刷新，未命中则跳过本轮
        if (RANDOM.nextDouble() >= DAILY_SPAWN_CHANCE) {
            return;
        }
        // 已存在存活商人则不再刷新
        if (isExistingAlive(level)) {
            return;
        }
        trySpawn(level);
    }

    /** 最近一次刷新的商人是否仍存活。 */
    private static boolean isExistingAlive(ServerLevel level) {
        if (lastSpawnedId == null) {
            return false;
        }
        Entity entity = level.getEntity(lastSpawnedId);
        return entity != null && entity.isAlive();
    }

    /** 在随机玩家附近尝试生成一只流浪嗅探兽商人。 */
    private static boolean trySpawn(ServerLevel level) {
        Player player = level.getRandomPlayer();
        if (player == null) {
            return true; // 无玩家：视作已处理，避免概率无限爬升
        }
        BlockPos base = player.blockPosition();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            int distance = MIN_DISTANCE + RANDOM.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = base.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = base.getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!canSpawnAt(level, pos)) {
                continue;
            }
            WanderingSnifferMerchant merchant = infinityminecraft.WANDERING_SNIFFER_MERCHANT.get().create(level);
            if (merchant == null) {
                return false;
            }
            merchant.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, RANDOM.nextFloat() * 360.0F, 0.0F);
            merchant.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            merchant.setDespawnDelay(DESPAWN_DELAY);
            merchant.randomizeTrades();
            level.addFreshEntityWithPassengers(merchant);
            lastSpawnedId = merchant.getUUID();
            return true;
        }
        return false;
    }

    /** 生成点校验：下方为实心方块、上方 2 格无碰撞（嗅探兽高约 2 格）、非禁止流浪商人生成的生物群系。 */
    private static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.isAir() || below.getCollisionShape(level, pos.below()).isEmpty()) {
            return false;
        }
        for (int dy = 0; dy < 2; dy++) {
            BlockPos check = pos.above(dy);
            if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                return false;
            }
        }
        return !level.getBiome(pos).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS);
    }
}
