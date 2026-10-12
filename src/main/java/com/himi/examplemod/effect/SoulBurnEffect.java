package com.himi.examplemod.effect;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 灵魂火灼烧——「破灭·炎烧炽铁」右键强化后的下次近战命中施加。
 *
 * <p>以蓝色灵魂火焰粒子包裹受击目标（构造第三参 {@code SOUL_FIRE_FLAME}），
 * 每 20 tick（每秒）造成 {@value #TICK_DAMAGE} 点灵魂火伤害（自定义伤害类型
 * {@code infinitycraft:ruination_soul_fire}，经标签无视护甲/抗性/无敌帧）。</p>
 *
 * <p>由于本伤害是 MobEffect 周期结算、而非火焰方块点燃的原火，天然不受水/雨浇灭；
 * 「燃烧期间不能回血」与「被攻击时无视伤害吸收」分别由
 * {@link com.himi.examplemod.event.RuinationDefenseHandler} 与
 * {@link com.himi.examplemod.event.RuinationCombatHandler} 统一处理。</p>
 */
public class SoulBurnEffect extends MobEffect {

    /** 每次结算的灵魂火伤害（每秒 3 点）。 */
    public static final float TICK_DAMAGE = 3.0F;

    public SoulBurnEffect() {
        // 负面；颜色取灵魂火焰的青蓝；粒子用原版 SOUL_FIRE_FLAME 呈现蓝色火焰
        super(MobEffectCategory.HARMFUL, 0x2f88ff,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME);
    }

    /** 每 20 tick 结算一次伤害。 */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return true;
        }
        Holder<DamageType> type = entity.damageSources().damageTypes
                .getHolderOrThrow(infinitycraft.RUINATION_SOUL_FIRE);
        // 灼烧伤害以受击者自身为直接来源（不指定引发实体），保证对末影人等不触发受击闪避
        entity.invulnerableTime = 0;
        entity.hurt(new DamageSource(type, entity), TICK_DAMAGE);
        return true;
    }
}
