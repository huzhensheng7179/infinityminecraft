package com.himi.examplemod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 烈焰蝾螈：一只使用 GeckoLib 播放骨骼动画的两栖怪物。
 *
 * <p>模型资源见 {@code assets/infinitycraft/geo/flame_salamander.geo.json}，动画见
 * {@code animations/flame_salamander.animation.json}（idle / walk / attack / vomit / inwater / death），
 * 贴图见 {@code textures/entity/flame_salamander.png}。</p>
 *
 * <p>行为（需求约定）：</p>
 * <ul>
 *   <li>16 点血量，免疫火焰（实体类型注册了 fireImmune）。</li>
 *   <li>近战命中点燃目标；5.5 格内有目标且有冷却时，原地喷吐火焰 4 秒（vomit 动画），
 *       前方约 4 格锥形区域产生大量火焰粒子，命中生物点燃 4 秒并受 2 点火焰伤害，冷却 12 秒。</li>
 *   <li>血量降到 4 点以下时停止攻击，像苦力怕一样逼近玩家并自爆（威力 2，不破坏方块但点燃环境）；
 *       自爆炸死时无掉落（见 {@code FlameSalamanderDropHandler}），被玩家击杀则正常掉落且不再爆炸。</li>
 * </ul>
 *
 * <p>动画控制器按「死亡 &gt; 喷吐 &gt; 攻击 &gt; 游泳 &gt; 行走 &gt; 待机」优先级选取当前动画。</p>
 *
 * <p>注册见 {@code infinitycraft#FLAME_SALAMANDER}，渲染见 {@code client/renderer/FlameSalamanderRenderer}，
 * 刷新见 {@code data/infinitycraft/neoforge/biome_modifier/flame_salamander_*.json}。</p>
 */
public class FlameSalamander extends Monster implements GeoEntity {

    // ===================== 动画（键名与 animation.json 顶层键一致，无 animation. 前缀） =====================
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation VOMIT = RawAnimation.begin().thenLoop("vomit");
    private static final RawAnimation IN_WATER = RawAnimation.begin().thenLoop("inwater");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("death");

    /** 近战攻击动画持续窗口（tick），期间控制器锁定播放 attack，之后自动回落。 */
    private static final int ATTACK_ANIM_TICKS = 20;

    // ===================== 喷吐火焰参数 =====================
    /** 喷吐持续 tick 数：4 秒。 */
    private static final int BREATH_TICKS = 80;
    /** 喷吐冷却 tick 数：12 秒。 */
    private static final int BREATH_COOLDOWN_TICKS = 240;
    /** 喷吐命中/粒子前伸距离：4 格。 */
    private static final double BREATH_RANGE = 4.0D;
    /** 开始喷吐的目标距离阈值（格）。 */
    private static final double BREATH_TRIGGER_DISTANCE = 5.5D;
    /** 喷吐锥形判定的夹角余弦（约 45° 以内）。 */
    private static final double BREATH_CONE_COS = 0.7D;

    // ===================== 低血量自爆参数 =====================
    /** 血量不高于该值时进入自爆状态（4 点 = 2 颗心）。 */
    private static final float FUSE_HEALTH_THRESHOLD = 4.0F;
    /** 与目标距离不大于该值时开始引信蓄力。 */
    private static final float FUSE_TRIGGER_DISTANCE = 2.2F;
    /** 引信蓄力时长（tick）：靠近后 2 秒才引爆，目标中途逃离则重置（同苦力怕）。 */
    private static final int FUSE_BURN_TICKS = 40;
    /** 自爆寻找最近玩家的半径（格）。 */
    private static final double FUSE_PLAYER_SEARCH_RADIUS = 16.0D;
    /** 自爆爆炸威力。 */
    private static final float FUSE_EXPLOSION_POWER = 2.0F;

    /** 攻击标记：服务端在挥击时置位并同步到客户端，供动画控制器选取 attack 动画。 */
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(FlameSalamander.class, EntityDataSerializers.BOOLEAN);
    /** 喷吐标记：服务端在喷吐火焰期间置位并同步，供动画控制器选取 vomit 动画。 */
    private static final EntityDataAccessor<Boolean> DATA_BREATHING =
            SynchedEntityData.defineId(FlameSalamander.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 攻击动画剩余 tick 计时（仅服务端）。 */
    private int attackAnimCooldown;
    /** 喷吐剩余 tick（仅服务端，>0 表示正在喷吐）。 */
    private int breathTicks;
    /** 喷吐冷却剩余 tick（仅服务端）。 */
    private int breathCooldown;
    /** 本次喷吐已命中过的实体，防止同一次喷吐重复结算。 */
    private final Set<UUID> breathHits = new HashSet<>();
    /** 是否已进入自爆蓄力状态（仅服务端逻辑）。 */
    private boolean fuseStarted;
    /** 是否已引爆（引爆后不再重置蓄力标记）。 */
    private boolean hasDetonated;
    /** 引信蓄力已持续 tick（仅服务端，距离不足时归零）。 */
    private int fuseBurnTicks;
    /** 自爆蓄力期间嘶声提示音的间隔计时。 */
    private int fuseHissTimer;
    /** 客户端运动检测：上一 tick 的水平位置。 */
    private double lastClientX;
    private double lastClientZ;
    /** 客户端运动检测：本 tick 是否发生位移。 */
    private boolean clientMoving;

    public FlameSalamander(EntityType<? extends FlameSalamander> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // ===================== AI =====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FuseGoal());
        this.goalSelector.addGoal(2, new FlameBreathGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new FloatGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** 近战命中：继承原先生成伤害，并额外点燃目标，呼应「烈焰」主题；同时触发攻击动画窗口。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.fuseStarted) {
            // 自爆蓄力阶段停止攻击，改为追击引爆（需求：低血量停止攻击并靠近玩家）
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.igniteForSeconds(4);
        }
        this.triggerAttackAnimation();
        return hit;
    }

    // ===================== Tick =====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            double dx = this.getX() - this.lastClientX;
            double dz = this.getZ() - this.lastClientZ;
            this.clientMoving = (dx * dx + dz * dz) > 1.0E-4D;
            this.lastClientX = this.getX();
            this.lastClientZ = this.getZ();
        } else {
            if (this.attackAnimCooldown > 0) {
                this.attackAnimCooldown--;
                if (this.attackAnimCooldown == 0) {
                    this.entityData.set(DATA_ATTACKING, false);
                }
            }
            if (this.breathCooldown > 0) {
                this.breathCooldown--;
            }
        }
    }

    /** 触发一次攻击动画（近战命中时调用）。 */
    private void triggerAttackAnimation() {
        if (this.level().isClientSide) {
            return;
        }
        this.attackAnimCooldown = ATTACK_ANIM_TICKS;
        this.entityData.set(DATA_ATTACKING, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACKING, false);
        builder.define(DATA_BREATHING, false);
    }

    // ===================== 喷吐火焰 =====================

    /** 结束本次喷吐：进入 12 秒冷却并复位动画标记。 */
    private void endBreath() {
        this.breathTicks = 0;
        this.breathCooldown = BREATH_COOLDOWN_TICKS;
        this.entityData.set(DATA_BREATHING, false);
    }

    /** 喷吐每 tick：朝目标注视、定身，向前方 4 格锥形撒火焰粒子并结算点燃与伤害。 */
    private void breathTick(ServerLevel level, LivingEntity target) {
        Vec3 look = this.getLookAngle();
        Vec3 eye = this.getEyePosition();
        RandomSource random = level.getRandom();

        // 大量火焰粒子：沿视线随机采样，锥距越远散布越大
        for (int i = 0; i < 4; i++) {
            double t = 0.6D + (BREATH_RANGE - 0.6D) * random.nextDouble();
            Vec3 p = eye.add(look.scale(t));
            double spread = 0.18D + t * 0.1D;
            level.sendParticles(ParticleTypes.FLAME,
                    p.x + (random.nextDouble() - 0.5D) * spread,
                    p.y + (random.nextDouble() - 0.5D) * spread,
                    p.z + (random.nextDouble() - 0.5D) * spread,
                    3, 0.02D, 0.02D, 0.02D, 0.01D);
        }
        if (random.nextInt(4) == 0) {
            Vec3 smokePos = eye.add(look.scale(BREATH_RANGE * random.nextDouble()));
            level.sendParticles(ParticleTypes.LARGE_SMOKE, smokePos.x, smokePos.y, smokePos.z,
                    2, 0.1D, 0.1D, 0.1D, 0.01D);
        }

        // 命中结算：前方 4 格、约 45° 锥内的生物，本次喷吐只结算一次
        AABB area = this.getBoundingBox().expandTowards(look.scale(BREATH_RANGE)).inflate(1.0D);
        for (Entity entity : level.getEntities(this, area)) {
            if (!(entity instanceof LivingEntity other) || other == this || !other.isAlive()) {
                continue;
            }
            if (other.getType() == this.getType()) {
                continue; // 同类不受己方喷吐影响
            }
            Vec3 to = other.position().add(0.0D, other.getBbHeight() * 0.5D, 0.0D).subtract(eye);
            double len = to.length();
            if (len < 1.0E-3D || len > BREATH_RANGE + other.getBbWidth() * 0.5D || to.dot(look) / len < BREATH_CONE_COS) {
                continue;
            }
            if (!this.breathHits.add(other.getUUID())) {
                continue;
            }
            other.igniteForSeconds(4);
            other.hurt(this.damageSources().onFire(), 2.0F);
        }
    }

    // ===================== 低血量自爆 =====================

    /** 引爆：产生威力 2 的爆炸，不破坏方块但点燃环境，并使自身死于「自身爆炸」伤害（无掉落判定用）。 */
    private void detonate() {
        this.hasDetonated = true;
        Level level = this.level();
        level.explode(this, this.getX(), this.getY(), this.getZ(),
                FUSE_EXPLOSION_POWER, true, Level.ExplosionInteraction.NONE);
        this.hurt(this.damageSources().explosion(this, this), 100.0F);
    }

    /** 喷吐火焰 Goal：射程内站桩喷 4 秒，冷却 12 秒；自爆蓄力时不使用。 */
    private class FlameBreathGoal extends Goal {
        @Override
        public boolean canUse() {
            if (FlameSalamander.this.level().isClientSide
                    || FlameSalamander.this.fuseStarted
                    || FlameSalamander.this.deathTime > 0) {
                return false;
            }
            if (FlameSalamander.this.breathTicks > 0 || FlameSalamander.this.breathCooldown > 0) {
                return false;
            }
            LivingEntity target = FlameSalamander.this.getTarget();
            return target != null && target.isAlive()
                    && FlameSalamander.this.distanceTo(target) <= BREATH_TRIGGER_DISTANCE;
        }

        @Override
        public void start() {
            FlameSalamander.this.breathTicks = BREATH_TICKS;
            FlameSalamander.this.breathHits.clear();
            FlameSalamander.this.entityData.set(DATA_BREATHING, true);
            FlameSalamander.this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F,
                    0.8F + FlameSalamander.this.random.nextFloat() * 0.4F);
        }

        @Override
        public boolean canContinueToUse() {
            return FlameSalamander.this.breathTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = FlameSalamander.this.getTarget();
            if (target == null || !target.isAlive()) {
                FlameSalamander.this.endBreath();
                return;
            }
            FlameSalamander.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            FlameSalamander.this.getNavigation().stop();
            FlameSalamander.this.breathTicks--;
            if (FlameSalamander.this.breathTicks <= 0) {
                FlameSalamander.this.endBreath();
                return;
            }
            if (FlameSalamander.this.level() instanceof ServerLevel serverLevel) {
                FlameSalamander.this.breathTick(serverLevel, target);
            }
        }

        @Override
        public void stop() {
            if (FlameSalamander.this.breathTicks > 0) {
                FlameSalamander.this.endBreath();
            }
        }

        @Override
        public EnumSet<Goal.Flag> getFlags() {
            return EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK);
        }
    }

    /** 低血量自爆 Goal：血量 ≤4 时停止其他攻击行为，逼近玩家并引爆，类似苦力怕。 */
    private class FuseGoal extends Goal {
        @Override
        public boolean canUse() {
            return !FlameSalamander.this.level().isClientSide
                    && !FlameSalamander.this.hasDetonated
                    && FlameSalamander.this.getHealth() <= FUSE_HEALTH_THRESHOLD;
        }

        @Override
        public void start() {
            FlameSalamander.this.fuseStarted = true;
            FlameSalamander.this.fuseBurnTicks = 0;
            FlameSalamander.this.fuseHissTimer = 0;
            FlameSalamander.this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.9F);
        }

        @Override
        public boolean canContinueToUse() {
            // 回血脱离低血量则取消自爆蓄力（引爆后绝不回头）
            return !FlameSalamander.this.hasDetonated
                    && FlameSalamander.this.getHealth() <= FUSE_HEALTH_THRESHOLD;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = FlameSalamander.this.getTarget();
            if (target == null || !target.isAlive()) {
                Player player = FlameSalamander.this.level().getNearestPlayer(
                        FlameSalamander.this, FUSE_PLAYER_SEARCH_RADIUS);
                if (player != null) {
                    FlameSalamander.this.setTarget(player);
                }
                target = FlameSalamander.this.getTarget();
            }
            if (target == null || !target.isAlive()) {
                return;
            }
            FlameSalamander.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (FlameSalamander.this.distanceTo(target) <= FUSE_TRIGGER_DISTANCE) {
                // 进入引信：站定蓄力 2 秒，起手播嘶声预警，蓄满才引爆
                FlameSalamander.this.getNavigation().stop();
                if (FlameSalamander.this.fuseBurnTicks++ == 0) {
                    FlameSalamander.this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 1.2F);
                }
                if (FlameSalamander.this.fuseBurnTicks >= FUSE_BURN_TICKS) {
                    FlameSalamander.this.detonate();
                }
                return;
            }
            // 目标脱离引爆范围：引信重置，继续追击（同苦力怕）
            FlameSalamander.this.fuseBurnTicks = 0;
            FlameSalamander.this.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.4D);
            // 蓄力期间周期性嘶声预警（原版苦力怕节奏的简化实现）
            FlameSalamander.this.fuseHissTimer--;
            if (FlameSalamander.this.fuseHissTimer <= 0) {
                FlameSalamander.this.fuseHissTimer = 60;
                FlameSalamander.this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F,
                        0.85F + FlameSalamander.this.random.nextFloat() * 0.3F);
            }
        }

        @Override
        public void stop() {
            FlameSalamander.this.fuseBurnTicks = 0;
            if (!FlameSalamander.this.hasDetonated) {
                FlameSalamander.this.fuseStarted = false;
            }
        }

        @Override
        public EnumSet<Goal.Flag> getFlags() {
            return EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP);
        }
    }

    // ===================== GeckoLib =====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<FlameSalamander> state) {
        // 死亡：deathTime 由 LivingEntity 在客户端同样递增，可直接用于驱动死亡动画
        if (this.isRemoved() || this.deathTime > 0) {
            state.setAndContinue(DEATH);
            return PlayState.CONTINUE;
        }
        if (this.entityData.get(DATA_BREATHING)) {
            return state.setAndContinue(VOMIT);
        }
        if (this.entityData.get(DATA_ATTACKING)) {
            return state.setAndContinue(ATTACK);
        }
        if (this.isInWater()) {
            return state.setAndContinue(IN_WATER);
        }
        if (this.clientMoving) {
            return state.setAndContinue(WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
