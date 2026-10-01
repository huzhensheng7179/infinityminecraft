package com.himi.examplemod.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

/**
 * 神秘硬币：手持右键使用。
 * - 从玩家视线发射一道蓝色粒子光线；
 * - 光线触碰实体或方块时在该点产生一次不破坏方块的爆炸；
 * - 爆炸对半径内生物造成 10~100000 的随机伤害；
 * - 在通知栏（action bar）告知本次造成的伤害；
 * - 使用后进入 30 分钟冷却。
 */
public class MysteriousCoinItem extends Item {

    private static final int COOLDOWN_TICKS = 30 * 60 * 20; // 30 分钟 = 36000 ticks
    private static final double MAX_RANGE = 100.0D;          // 光线最大射程
    private static final double EXPLOSION_RADIUS = 5.0D;     // 爆炸/伤害半径
    private static final int MIN_DAMAGE = 10;
    private static final int MAX_DAMAGE = 100000;
    private static final double BEAM_STEP = 0.4D;            // 粒子光线采样步长

    // 蓝色光束粒子
    private static final DustParticleOptions BEAM_PARTICLE =
            new DustParticleOptions(new Vector3f(0.2F, 0.5F, 1.0F), 1.5F);

    public MysteriousCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        // 冷却中直接失败
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // 客户端仅播放手持动画
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(MAX_RANGE));

        // 方块射线检测
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        // 实体射线检测
        AABB entityBounds = player.getBoundingBox().expandTowards(look.scale(MAX_RANGE)).inflate(1.0D);
        Predicate<Entity> filter = e -> e instanceof LivingEntity && e != player;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end, entityBounds, filter);

        // 取较近的命中点作为爆炸位置
        Vec3 hitPos = resolveHitPos(eye, blockHit, entityHit);

        // 绘制蓝色粒子光线
        drawBeam(serverLevel, eye, hitPos);

        // 不破坏方块的爆炸（NONE 表示不破坏方块），提供爆炸粒子与音效
        level.explode(player, hitPos.x, hitPos.y, hitPos.z, (float) EXPLOSION_RADIUS,
                false, Level.ExplosionInteraction.NONE);

        // 随机伤害：一次性抽取，作用于爆炸半径内所有生物（不含使用者）
        RandomSource random = level.getRandom();
        int damage = random.nextIntBetweenInclusive(MIN_DAMAGE, MAX_DAMAGE);
        DamageSource damageSource = player.damageSources().explosion(player, null);

        AABB damageArea = new AABB(hitPos, hitPos).inflate(EXPLOSION_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageArea,
                e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.hurt(damageSource, damage);
        }

        // 通知栏（action bar）告知本次伤害
        Component message = Component.literal("\u00A79\u795E\u79D8\u786C\u5E01\u00A7r \u9020\u6210 \u00A7e"
                + damage + "\u00A7r \u70B9\u4F24\u5BB3\uFF01");
        player.displayClientMessage(message, true);

        // 进入冷却
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.consume(stack);
    }

    /**
     * 在方块命中点与实体命中点之间取距离玩家更近者；无实体命中时使用方块命中点。
     */
    private Vec3 resolveHitPos(Vec3 eye, BlockHitResult blockHit, EntityHitResult entityHit) {
        Vec3 blockPos = blockHit.getLocation();
        if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
            Vec3 entityPos = entityHit.getLocation();
            return entityPos.distanceToSqr(eye) < blockPos.distanceToSqr(eye) ? entityPos : blockPos;
        }
        return blockPos;
    }

    /**
     * 沿起点到终点均匀采样，生成蓝色尘埃粒子形成光线效果。
     */
    private void drawBeam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length <= 0.0D) {
            return;
        }
        Vec3 step = delta.normalize().scale(BEAM_STEP);
        Vec3 pos = from;
        for (double traveled = 0.0D; traveled <= length; traveled += BEAM_STEP) {
            level.sendParticles(BEAM_PARTICLE, pos.x, pos.y, pos.z, 2, 0.02D, 0.02D, 0.02D, 0.0D);
            pos = pos.add(step);
        }
    }
}
