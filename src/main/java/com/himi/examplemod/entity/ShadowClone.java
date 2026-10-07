package com.himi.examplemod.entity;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 影流分身：由「影流项链」在佩戴者受到致命伤害时于其原位生成的替身。
 *
 * <p>特性：外观与召唤者（玩家）完全一致（见 client 渲染层，使用玩家模型与其皮肤），不显示名字；
 * 没有碰撞箱（玩家与其他实体可直接穿过，但仍可被攻击命中）；没有任何 AI，不移动、不攻击；
 * 被任何非召唤者（怪物或其他玩家）击中时立即消失，并给攻击者施加反胃 III、缓慢 III、黑暗 II 各 12 秒（因此怪物可以「杀死」分身，不会一直围着它）；
 * 召唤者无法攻击自己的分身；无论是否被攻击，存在 20 秒（400 ticks）后自动消失。</p>
 */
public class ShadowClone extends Mob {

    /** 存在时长：20 秒 = 400 ticks，到时自动消失。 */
    private static final int LIFE_TICKS = 400;

    /** 被攻击时施加给攻击者的负面效果时长：12 秒 = 240 ticks。 */
    private static final int DEBUFF_TICKS = 240;

    /** 近战命中判定半径：敌对怪物锁定本分身并进入此距离（且有视线）时，视为被击中。 */
    private static final double MELEE_REACH = 2.5D;

    @Nullable
    private UUID ownerUUID;

    public ShadowClone(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** 影流分身没有任何 AI：不注册任何目标/移动行为，保持原地静止。 */
    @Override
    protected void registerGoals() {
        // 故意为空：无 AI
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 存在满 20 秒后自动消失
        if (this.tickCount >= LIFE_TICKS) {
            this.playDisappearEffect();
            this.discard();
            return;
        }
        // 主动检测近战怪物：让锁定本分身并贴身的敌对生物能可靠地「打到」分身
        if (this.tickCount % 2 == 0) {
            this.checkMeleeAttackers();
        }
    }

    /**
     * 被攻击时的处理：
     * <ul>
     *   <li>召唤者对自己的分身的攻击被完全无效化（不施加效果、不消失）；</li>
     *   <li>任何非召唤者的攻击者（怪物或其他玩家）都会获得反胃 III / 缓慢 III / 黑暗 II 各 12 秒；</li>
     *   <li>分身被任何非召唤者击中后立即消失（怪物可以「杀死」分身，从而不会一直围着它）。</li>
     * </ul>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        Entity attacker = source.getEntity();
        // 召唤者不能攻击自己的分身
        if (attacker != null && attacker.getUUID().equals(this.ownerUUID)) {
            return false;
        }
        // 被攻击（怪物远程 / 其他玩家）：给攻击者施加负面效果，然后分身立即消失
        if (attacker instanceof LivingEntity living) {
            this.onAttackedBy(living);
        } else {
            this.playDisappearEffect();
            this.discard();
        }
        return true;
    }

    /**
     * 主动检测近战攻击者：原版怪物的近战攻击在某些情况下不会对无 AI、无碰撞的分身触发
     * {@code doHurtTarget}，导致怪物围着分身却打不掉它。这里每 2 tick 主动扫描——
     * 只要有敌对生物（{@link Enemy}）锁定了本分身（{@code getTarget() == this}）、
     * 进入近战距离（{@link #MELEE_REACH}）且对本分身有视线，就视为被它击中：
     * 给该怪物施加负面效果并让分身消失。
     */
    private void checkMeleeAttackers() {
        if (this.isRemoved()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        AABB area = this.getBoundingBox().inflate(MELEE_REACH);
        List<Mob> attackers = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob instanceof Enemy
                        && mob.getTarget() == this
                        && mob.hasLineOfSight(this));
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (Mob mob : attackers) {
            double dist = mob.distanceToSqr(this);
            if (dist < best) {
                best = dist;
                nearest = mob;
            }
        }
        if (nearest != null) {
            this.onAttackedBy(nearest);
        }
    }

    /** 被非召唤者击中：给攻击者施加反胃 III / 缓慢 III / 黑暗 II 各 12 秒，播放消失特效并移除分身。 */
    private void onAttackedBy(LivingEntity attacker) {
        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DEBUFF_TICKS, 2));
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_TICKS, 2));
        attacker.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DEBUFF_TICKS, 1));
        this.playDisappearEffect();
        this.discard();
    }

    /** 消失特效：浓烟粒子 + 末影传送音效。 */
    private void playDisappearEffect() {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        double x = this.getX();
        double y = this.getY() + 0.9D;
        double z = this.getZ();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 0.5D, 0.9D, 0.5D, 0.02D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 30, 0.5D, 0.9D, 0.5D, 0.05D);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    // 无碰撞箱：不可被推动、不可被碰撞（玩家与其他实体可直接穿过），但仍可被攻击命中
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    // 不因远离玩家而自然消失（由 20 秒计时统一控制）
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("ShadowOwner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("ShadowOwner")) {
            this.ownerUUID = tag.getUUID("ShadowOwner");
        }
    }
}
