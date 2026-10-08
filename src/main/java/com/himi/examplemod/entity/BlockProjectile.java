package com.himi.examplemod.entity;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 反之弓发射的方块弹射物。
 *
 * <p>特性：</p>
 * <ul>
 *   <li>不受重力（{@link #getDefaultGravity()} 返回 0），沿发射方向直线飞行；</li>
 *   <li>命中<b>方块</b>：若落点可放置则在该处放置携带的方块，随后消失；</li>
 *   <li>命中<b>生物</b>：按发射时算好的伤害造成伤害；若为岩浆块则点燃目标，若为雪块则附加冰冻伤害与缓慢；</li>
 *   <li>飞行超过 {@link #MAX_LIFE_TICKS} 仍未命中则自动消失，避免无重力弹射物泄漏。</li>
 * </ul>
 *
 * <p>携带的方块以注册表 id 存入同步数据（{@link #DATA_BLOCK_ID}），使客户端渲染器能还原方块外观；
 * 伤害与方块 id 亦写入 NBT 以便存档持久化。</p>
 */
public class BlockProjectile extends net.minecraft.world.entity.projectile.ThrowableProjectile {

    /** 携带方块的注册表 id（同步到客户端用于渲染）。 */
    private static final EntityDataAccessor<Integer> DATA_BLOCK_ID =
            SynchedEntityData.defineId(BlockProjectile.class, EntityDataSerializers.INT);

    /** 最大飞行时长：300 ticks（15 秒）后仍未命中则消失。 */
    private static final int MAX_LIFE_TICKS = 300;

    /** 雪块命中附加的冰冻伤害（走无视护甲/无敌帧的自定义冰冻伤害类型）。 */
    private static final float SNOW_FREEZE_DAMAGE = 5.0F;

    /** 岩浆块点燃时长（秒）。 */
    private static final int MAGMA_BURN_SECONDS = 8;

    /** 雪块缓慢：缓慢 III，持续 5 秒。 */
    private static final int SNOW_SLOW_TICKS = 100;
    private static final int SNOW_SLOW_AMPLIFIER = 2;

    /** 本发命中生物的伤害（服务端使用，无需同步）。 */
    private float damage;

    public BlockProjectile(EntityType<? extends BlockProjectile> type, Level level) {
        super(type, level);
    }

    /** 配置携带的方块与伤害（发射方在 addFreshEntity 之前调用）。 */
    public void configure(BlockState state, float damage) {
        this.damage = damage;
        this.entityData.set(DATA_BLOCK_ID, BuiltInRegistries.BLOCK.getId(state.getBlock()));
    }

    /** 取携带方块的默认方块状态（客户端渲染、服务端命中放置共用）。 */
    public BlockState getStoredBlockState() {
        Block block = BuiltInRegistries.BLOCK.byId(this.entityData.get(DATA_BLOCK_ID));
        return block.defaultBlockState();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // ThrowableProjectile/Projectile 未定义额外同步数据，此处仅定义携带方块 id（不能调用抽象的 super）
        builder.define(DATA_BLOCK_ID, BuiltInRegistries.BLOCK.getId(Blocks.COBBLESTONE));
    }

    /** 不受重力：返回 0，使方块沿直线飞行不下坠。 */
    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        DamageSource projectileSource = this.damageSources().mobProjectile(this, owner);
        target.hurt(projectileSource, this.damage);

        Block block = this.getStoredBlockState().getBlock();
        if (target instanceof LivingEntity victim) {
            if (block == Blocks.MAGMA_BLOCK) {
                // 岩浆块：点燃目标
                victim.igniteForSeconds(MAGMA_BURN_SECONDS);
            } else if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW) {
                // 雪块：冰冻伤害（无视护甲/无敌帧）+ 缓慢
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        SNOW_SLOW_TICKS, SNOW_SLOW_AMPLIFIER));
                victim.hurt(snowFreezeSource(victim), SNOW_FREEZE_DAMAGE);
            }
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) {
            return;
        }
        BlockPos placePos = result.getBlockPos().relative(result.getDirection());
        BlockState existing = this.level().getBlockState(placePos);
        if (existing.canBeReplaced()) {
            BlockState stored = this.getStoredBlockState();
            this.level().setBlock(placePos, stored, Block.UPDATE_ALL);
            SoundType soundType = stored.getSoundType();
            this.level().playSound(null, placePos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        }
        this.discard();
    }

    /** 复用项目已有的自定义冰冻伤害类型（经标签配置为无视护甲/抗性/无敌帧）。 */
    private DamageSource snowFreezeSource(Entity victim) {
        Holder<DamageType> holder = victim.damageSources().damageTypes
                .getHolderOrThrow(infinitycraft.CANT_CATCH_ME_FREEZE);
        return new DamageSource(holder, this, this.getOwner());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ReverseDamage", this.damage);
        tag.putInt("ReverseBlock", this.entityData.get(DATA_BLOCK_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("ReverseDamage");
        if (tag.contains("ReverseBlock")) {
            this.entityData.set(DATA_BLOCK_ID, tag.getInt("ReverseBlock"));
        }
    }
}
