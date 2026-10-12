package com.himi.examplemod.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
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
 * 镰恶魔：一只使用 GeckoLib 骨骼动画的飘浮近战怪物，手持巨镰，白天在阳光下会自燃。
 *
 * <p>模型资源见 {@code assets/infinitycraft/geo/scythe_reaper.geo.json}，动画见
 * {@code animations/scythe_reaper.animation.json}（idle / walk / attack / despawn），
 * 贴图见 {@code textures/entity/scythe_reaper.png}（骨骼根 h_body，双臂 h_left_arm / h_right_arm）。</p>
 *
 * <p>行为（需求约定）：</p>
 * <ul>
 *   <li>生命值 30 点（15 颗心），近战伤害 10（5 颗心），主动攻击玩家与铁傀儡。</li>
 *   <li>飘浮移动：无重力 + {@link FlyingMoveControl}，靠 {@link FlyingPathNavigation} 三维寻路。</li>
 *   <li>白天阳光自燃：服务端每 tick 命中 {@link LivingEntity#isSunBurnTick()} 时点燃自身，
 *       与凋灵骷髅/骷髅一致（下界无天空、canSeeSky 为 false，故下界不受影响）。</li>
 *   <li>成群刷新：见 {@code data/infinitycraft/neoforge/biome_modifier/scythe_reaper_*.json}，
 *       下界为主要生成地、主世界夜晚少量，每群固定 4 只。掉落见 {@code loot_table/entities/scythe_reaper.json}。</li>
 * </ul>
 *
 * <p>动画控制器按「攻击 &gt; 移动 &gt; 待机」优先级选取当前动画。</p>
 *
 * <p>注册见 {@code infinitycraft#SCYTHE_REAPER}，渲染见 {@code client/renderer/ScytheReaperRenderer}。</p>
 */
public class ScytheReaper extends Monster implements GeoEntity {

    // ===================== 动画（键名与 animation.json 顶层键一致，无 animation. 前缀） =====================
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    /** 攻击动画持续窗口（tick），期间控制器锁定播放 attack，之后自动回落。 */
    private static final int ATTACK_ANIM_TICKS = 34;

    /** 阳光点燃的燃烧时长（tick）。 */
    private static final int SUN_BURN_TICKS = 100;

    /** 攻击动画标记：服务端在挥击时置位并同步到客户端，供动画控制器选取 attack 动画。 */
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(ScytheReaper.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 攻击动画剩余 tick（仅服务端）。 */
    private int attackAnimCooldown;
    /** 客户端运动检测：上一 tick 的空间位置。 */
    private double lastX;
    private double lastY;
    private double lastZ;
    /** 客户端运动检测：本 tick 是否发生位移（用于切换 walk 动画）。 */
    private boolean clientMoving;

    public ScytheReaper(EntityType<? extends ScytheReaper> type, Level level) {
        super(type, level);
        // 飘浮控制：转向平滑度 10，抵达目标点无需精确朝向（近战悬停用）
        this.moveControl = new FlyingMoveControl(this, 10, true);
        // 飘浮：不受重力影响，靠 FlyingMoveControl / FlyingPathNavigation 主动位移
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FLYING_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        // 无目标时进行三维随机飞行游荡（原版飞行寻路，靠 FlyingPathNavigation 在空中随机选点飞过去）
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 0.85D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** 近战挥镰：继承原先生成伤害（含 10 点攻击力），并触发攻击动画。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
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
        } else {
            if (this.attackAnimCooldown > 0) {
                this.attackAnimCooldown--;
                if (this.attackAnimCooldown == 0) {
                    this.entityData.set(DATA_ATTACKING, false);
                }
            }
            // 白天阳光下自燃（同凋灵骷髅/骷髅）：isSunBurnTick 已内含「露天 + 白天 + 无火 + 非雨中」判定
            if (this.level() instanceof ServerLevel && this.isSunBurnTick()) {
                this.igniteForTicks(SUN_BURN_TICKS);
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

    private PlayState predicate(AnimationState<ScytheReaper> state) {
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
