package com.himi.examplemod.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 炽热邪眼：一只使用 GeckoLib 骨骼动画的飞行怪物，通体炽热，近战命中会点燃目标。
 *
 * <p>模型资源见 {@code assets/infinitycraft/geo/scorching_eye.geo.json}，动画见
 * {@code animations/scorching_eye.animation.json}（idle / walk / attack / death），
 * 贴图见 {@code textures/entity/scorching_eye.png}（骨骼根为 firebat，含左右翅膀、下颚、双腿）。</p>
 *
 * <p>行为（需求约定）：</p>
 * <ul>
 *   <li>生命值 12 点（6 颗心），免疫火焰，全程悬停飞行（无重力 + {@link FlyingMoveControl}）。</li>
 *   <li>主动追击玩家与铁傀儡，近战命中额外点燃目标 4 秒，呼应「炽热」主题。</li>
 *   <li>成群刷新：见 {@code data/infinitycraft/neoforge/biome_modifier/scorching_eye_*.json}，
 *       下界、热砂世界地下、主世界洞穴（极少）三处生成，每群 4~10 只。</li>
 * </ul>
 *
 * <p>动画控制器按「死亡 &gt; 攻击 &gt; 移动 &gt; 待机」优先级选取当前动画。</p>
 *
 * <p>注册见 {@code infinitycraft#SCORCHING_EYE}，渲染见 {@code client/renderer/ScorchingEyeRenderer}。</p>
 */
public class ScorchingEye extends Monster implements GeoEntity {

    // ===================== 动画（键名与 animation.json 顶层键一致，无 animation. 前缀） =====================
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("death");

    /** 近战攻击动画持续窗口（tick），期间控制器锁定播放 attack，之后自动回落。 */
    private static final int ATTACK_ANIM_TICKS = 24;

    /** 攻击动画标记：服务端在挥击时置位并同步到客户端，供动画控制器选取 attack 动画。 */
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(ScorchingEye.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 攻击动画剩余 tick（仅服务端）。 */
    private int attackAnimCooldown;
    /** 客户端运动检测：上一 tick 的空间位置。 */
    private double lastX;
    private double lastY;
    private double lastZ;
    /** 客户端运动检测：本 tick 是否发生位移（用于切换 walk 动画）。 */
    private boolean clientMoving;

    public ScorchingEye(EntityType<? extends ScorchingEye> type, Level level) {
        super(type, level);
        // 飞行控制：转向平滑度 20，抵达目标点无需精确朝向（近战悬停用）
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // 悬停飞行：不受重力影响，靠 FlyingMoveControl / FlyingPathNavigation 主动位移
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ===================== 导航与 AI =====================

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        // 无目标时进行三维随机飞行游荡（原版飞行寻路，靠 FlyingPathNavigation 在空中随机选点飞过去）
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** 近战命中：继承原先生成伤害，并额外点燃目标 4 秒，呼应「炽热」主题；同时触发攻击动画窗口。 */
    @Override
    public boolean doHurtTarget(Entity target) {
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
            double dx = this.getX() - this.lastX;
            double dy = this.getY() - this.lastY;
            double dz = this.getZ() - this.lastZ;
            this.clientMoving = (dx * dx + dy * dy + dz * dz) > 1.0E-4D;
            this.lastX = this.getX();
            this.lastY = this.getY();
            this.lastZ = this.getZ();
        } else if (this.attackAnimCooldown > 0) {
            this.attackAnimCooldown--;
            if (this.attackAnimCooldown == 0) {
                this.entityData.set(DATA_ATTACKING, false);
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
    }

    // ===================== GeckoLib =====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<ScorchingEye> state) {
        // 死亡：deathTime 由 LivingEntity 在客户端同样递增，可直接用于驱动死亡动画
        if (this.isRemoved() || this.deathTime > 0) {
            state.setAndContinue(DEATH);
            return PlayState.CONTINUE;
        }
        if (this.entityData.get(DATA_ATTACKING)) {
            return state.setAndContinue(ATTACK);
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
