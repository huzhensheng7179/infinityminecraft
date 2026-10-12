package com.himi.examplemod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 神秘硬币终点 3D 爆炸（纯视觉 GeckoLib 动画实体）。
 *
 * <p>常驻命中点，按 {@link #DATA_RADIUS}（来自伤害档位 impactRadius）整体缩放，
 * 播放一次性 explode 动画（核心鼓胀 + 六向尖刺迸出再回收），{@link #LIFE_TICKS} 后自动 discard。</p>
 */
public class CoinBlastEntity extends Entity implements GeoEntity {

    /** 爆炸规模半径（渲染整体缩放依据）。 */
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(CoinBlastEntity.class, EntityDataSerializers.FLOAT);

    /** 存活 tick 数，与动画时长匹配。 */
    public static final int LIFE_TICKS = 20;

    private static final RawAnimation EXPLODE_ANIM = RawAnimation.begin().thenPlay("animation.coin_blast.explode");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CoinBlastEntity(EntityType<? extends CoinBlastEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void configureRadius(double radius) {
        this.entityData.set(DATA_RADIUS, (float) radius);
    }

    public float scaleRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 3.0F);
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
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(EXPLODE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Radius")) {
            this.entityData.set(DATA_RADIUS, tag.getFloat("Radius"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", this.scaleRadius());
    }
}
