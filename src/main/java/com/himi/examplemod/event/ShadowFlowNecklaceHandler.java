package com.himi.examplemod.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.himi.examplemod.entity.ShadowClone;
import com.himi.examplemod.infinitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 「影流项链」事件处理器（装备在 Curios 项链栏位时生效）。
 *
 * <p>当佩戴者受到<strong>致命伤害</strong>（本次伤害结算后生命值将 &le; 0）时：
 * <ol>
 *   <li>把本次伤害改为 0，免于死亡；</li>
 *   <li>把佩戴者传送到以原位置为中心、半径 12 格内的一个安全位置；</li>
 *   <li>在佩戴者原来所在的位置为原点、半径 12 格内随机散布生成 6 个与之外观一致的「影流分身」作为替身（彼此保持间距，详见 {@link ShadowClone}）。</li>
 * </ol>
 * 触发后进入 60 秒（1200 ticks）冷却：冷却期间项链不再生效（致命伤害正常结算，佩戴者可能死亡），冷却结束后方可再次救命。</p>
 *
 * <p>为让其他怪物优先索敌分身：所有敌对生物（{@link Enemy}）加入世界时被注入一个最高优先级（priority 0）、
 * 以 {@link ShadowClone} 为目标的 {@link NearestAttackableTargetGoal}，因此范围内一旦出现分身，怪物会优先转向分身（而非玩家）。</p>
 *
 * <p>选择在 {@link LivingDamageEvent.Pre}（护甲/附魔等减伤结算之后）判定致命，
 * 因为此时 {@code getNewDamage()} 才是真正会扣除的生命值；该事件不可取消，
 * 故通过 {@code setNewDamage(0)} 阻止本次致死伤害。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class ShadowFlowNecklaceHandler {

    /** 触发冷却：1200 ticks（60 秒）。冷却期间项链不生效，致命伤害正常结算。 */
    private static final int COOLDOWN_TICKS = 1200;

    /** 安全位置搜索半径：12 格。 */
    private static final int SEARCH_RADIUS = 12;

    /** 替身生成数量：6 个，在原点半径 12 格内随机散布。 */
    private static final int CLONE_COUNT = 6;

    /** 替身散布半径：12 格（以原点为中心）。 */
    private static final double CLONE_SPREAD_RADIUS = 12.0D;

    /** 替身之间的最小间距：4 格，避免聚在一起。 */
    private static final double CLONE_MIN_SPACING = 4.0D;

    /** 冷却结束时间：UUID -> gameTime。 */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasNecklace(player)) {
            return;
        }
        // 仅在本次伤害会致死时触发
        if (player.getHealth() - event.getNewDamage() > 0.0F) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long cooldownEnd = COOLDOWNS.get(player.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) {
            // 冷却中（60 秒内已触发过）：项链不生效，本次致命伤害正常结算（不清零、不传送、不生成分身）
            return;
        }

        // 阻止本次致死伤害
        event.setNewDamage(0.0F);
        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);

        // 记录原始位置与朝向
        double ox = player.getX();
        double oy = player.getY();
        double oz = player.getZ();
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        // 传送到半径 12 格内的安全位置（找不到则原地保留，仍免于死亡）
        BlockPos origin = player.blockPosition();
        BlockPos safe = findSafeSpot(level, origin, level.getRandom());
        if (safe != null) {
            player.teleportTo(level, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, yRot, xRot);
            playTeleportEffect(level, safe.getX() + 0.5D, safe.getY() + 1.0D, safe.getZ() + 0.5D);
        }

        // 在原点半径 12 格内随机散布生成 6 个影流分身作为替身（彼此保持间距，不聚在一起）
        List<BlockPos> placed = new ArrayList<>();
        for (int i = 0; i < CLONE_COUNT; i++) {
            BlockPos spot = findCloneSpot(level, origin, level.getRandom(), placed);
            double cx;
            double cy;
            double cz;
            if (spot != null) {
                cx = spot.getX() + 0.5D;
                cy = spot.getY();
                cz = spot.getZ() + 0.5D;
                placed.add(spot);
            } else {
                // 极端情况（周围无可站立落点）回退到原点，保证替身一定生成
                cx = ox;
                cy = oy;
                cz = oz;
            }
            ShadowClone clone = new ShadowClone(infinitycraft.SHADOW_CLONE.get(), level);
            clone.moveTo(cx, cy, cz, yRot, xRot);
            clone.setOwnerUUID(player.getUUID());
            level.addFreshEntity(clone);
        }
        playTeleportEffect(level, ox, oy + 1.0D, oz);

        // 传送后给予短暂无敌帧，避免落地瞬间再次被同来源致死
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (COOLDOWNS.isEmpty()) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        COOLDOWNS.values().removeIf(end -> now >= end);
    }

    /**
     * 为所有敌对生物注入「优先索敌影流分身」的目标选择器。
     * 优先级 0（高于原版怪物针对玩家的目标选择器），因此范围内存在分身时会抢占并优先攻击分身。
     * {@code mustSee=true} 确保需视线可见，不会隔墙索敌；影流分身本身不是 {@link Enemy}，不会被注入。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob instanceof Enemy)) {
            return;
        }
        mob.targetSelector.addGoal(0,
                new NearestAttackableTargetGoal<>(mob, ShadowClone.class, 10, true, false, null));
    }

    /** 传送/替身特效：末影传送音效 + 传送门粒子。 */
    private static void playTeleportEffect(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 60, 0.6D, 1.0D, 0.6D, 0.3D);
    }

    /**
     * 为影流分身在原点半径 12 格内寻找一个随机、可站立且与已放置分身至少相距 4 格的落点，
     * 避免分身聚在一起。优先满足间距要求；若多次尝试仍无法满足间距，则退而接受任意可站立落点；
     * 都找不到返回 {@code null}。
     */
    private static BlockPos findCloneSpot(ServerLevel level, BlockPos origin, RandomSource random, List<BlockPos> placed) {
        int r = (int) CLONE_SPREAD_RADIUS;
        double minSqr = CLONE_MIN_SPACING * CLONE_MIN_SPACING;
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 96; attempt++) {
            int dx = random.nextIntBetweenInclusive(-r, r);
            int dy = random.nextIntBetweenInclusive(-6, 6);
            int dz = random.nextIntBetweenInclusive(-r, r);
            if (dx * dx + dy * dy + dz * dz > r * r) {
                continue;
            }
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (!isSafe(level, candidate)) {
                continue;
            }
            if (fallback == null) {
                fallback = candidate;
            }
            // 与已放置分身保持最小间距
            boolean farEnough = true;
            for (BlockPos p : placed) {
                if (candidate.distSqr(p) < minSqr) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                return candidate;
            }
        }
        return fallback;
    }

    /**
     * 在以 {@code origin} 为中心、半径 {@value #SEARCH_RADIUS} 格的球体内随机寻找一个安全落点。
     * 找不到返回 {@code null}。
     */
    private static BlockPos findSafeSpot(ServerLevel level, BlockPos origin, RandomSource random) {
        int r = SEARCH_RADIUS;
        for (int attempt = 0; attempt < 128; attempt++) {
            int dx = random.nextIntBetweenInclusive(-r, r);
            int dy = random.nextIntBetweenInclusive(-6, 6);
            int dz = random.nextIntBetweenInclusive(-r, r);
            if (dx == 0 && dy == 0 && dz == 0) {
                continue;
            }
            if (dx * dx + dy * dy + dz * dz > r * r) {
                continue;
            }
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (isSafe(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** 安全落点判定：脚下/头顶无碰撞且无流体，地面可站立，且周围不含危险方块。 */
    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockPos head = pos.above();
        BlockPos below = pos.below();
        if (level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(head)) {
            return false;
        }
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(head);
        BlockState groundState = level.getBlockState(below);
        // 脚下与头顶必须可通行（无碰撞体）且无流体（水/岩浆）
        if (!feetState.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }
        if (!feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()) {
            return false;
        }
        // 地面必须有碰撞体（可站立）
        if (groundState.getCollisionShape(level, below).isEmpty()) {
            return false;
        }
        // 排除危险方块
        return !isDangerous(feetState) && !isDangerous(headState) && !isDangerous(groundState);
    }

    private static boolean isDangerous(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.LAVA || b == Blocks.FIRE || b == Blocks.SOUL_FIRE || b == Blocks.MAGMA_BLOCK
                || b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE || b == Blocks.CACTUS
                || b == Blocks.WITHER_ROSE || b == Blocks.SWEET_BERRY_BUSH || b == Blocks.POWDER_SNOW;
    }

    /** 检查玩家是否在 Curios 项链栏位装备了「影流项链」。 */
    private static boolean hasNecklace(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.SHADOW_FLOW_NECKLACE.get())))
                .isPresent();
    }
}
