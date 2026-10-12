package com.himi.examplemod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 神秘硬币发射的 3D 长方体光束（纯视觉 GeckoLib 动画实体）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>实体常驻在玩家眼睛坐标，把「命中点相对起点的偏移向量」存入同步数据 {@link #DATA_TARGET}，
 *       实体本身不位移，规避原版位置插值把光束拉模糊；渲染器据该向量决定朝向与长度。</li>
 *   <li>光束沿路径向前「生长」：{@link #GROW_TICKS} 内从 0 伸长到满，之后维持到 {@link #LIFE_TICKS} 消散。</li>
 *   <li>不参与物理、无碰撞、不推动其它实体，仅用于渲染。</li>
 * </ul>
 */
public class CoinBeamEntity extends Entity implements GeoEntity {

    /**
     * 命中点相对起点的偏移向量（同步到客户端供渲染器计算朝向/长度）。
     * 原版没有 Vec3 序列化器，用三个 FLOAT 分量同步。
     */
    private static final EntityDataAccessor<Float> DATA_TX =
            SynchedEntityData.defineId(CoinBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TY =
            SynchedEntityData.defineId(CoinBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TZ =
            SynchedEntityData.defineId(CoinBeamEntity.class, EntityDataSerializers.FLOAT);

    /** 生长所需 tick 数（光束由 0 伸长到满长度）。 */
    public static final int GROW_TICKS = 4;
    /** 总存活 tick 数，之后自动 discard。 */
    public static final int LIFE_TICKS = 14;

    private static final RawAnimation SHOOT_ANIM = RawAnimation.begin().thenLoop("animation.coin_beam.shoot");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CoinBeamEntity(EntityType<? extends CoinBeamEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    /** 发射方在 addFreshEntity 之前调用，设定命中偏移向量。 */
    public void configure(Vec3 targetOffset) {
        this.entityData.set(DATA_TX, (float) targetOffset.x);
        this.entityData.set(DATA_TY, (float) targetOffset.y);
        this.entityData.set(DATA_TZ, (float) targetOffset.z);
    }

    public Vec3 targetOffset() {
        return new Vec3(this.entityData.get(DATA_TX), this.entityData.get(DATA_TY), this.entityData.get(DATA_TZ));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TX, 0.0F);
        builder.define(DATA_TY, 0.0F);
        builder.define(DATA_TZ, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= LIFE_TICKS) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(SHOOT_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Tx") && tag.contains("Ty") && tag.contains("Tz")) {
            this.entityData.set(DATA_TX, tag.getFloat("Tx"));
            this.entityData.set(DATA_TY, tag.getFloat("Ty"));
            this.entityData.set(DATA_TZ, tag.getFloat("Tz"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 v = this.targetOffset();
        tag.putFloat("Tx", (float) v.x);
        tag.putFloat("Ty", (float) v.y);
        tag.putFloat("Tz", (float) v.z);
    }
}
