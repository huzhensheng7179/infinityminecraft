package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * “巧乐兹风暴”事件处理器：受击时对伤害来源单位反射冰冻伤害。
 * 伤害随效果等级提升：1 级 4 点、2 级 8 点、3 级 16 点（即 4 × 2^amplifier）。
 *
 * 说明：
 * - 反射出去的是「冰冻」伤害，故本次 incoming 若本身即为冰冻伤害则跳过，
 *   以避免两个持有该效果的单位互相反射形成死循环；
 * - 仅对存在「伤害来源单位」（{@code source.getEntity()} 为 LivingEntity）的伤害反射，
 *   摔落/岩浆等无来源实体的伤害不触发。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class ChocoStormHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        MobEffectInstance storm = victim.getEffect(infinityminecraft.CHOCO_STORM);
        if (storm == null) return;

        // 防递归：反射出去的是冰冻伤害，若本次即冰冻伤害则不再反射
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) return;

        // 仅对「伤害来源单位」反射
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == victim) return;

        // 4 × 2^amplifier（限制指数上限，避免指令给予极高 amplifier 时整型溢出）
        int amplifier = Math.min(storm.getAmplifier(), 20);
        float damage = (float) (4.0 * Math.pow(2.0, amplifier));
        attacker.hurt(attacker.damageSources().freeze(), damage);
    }
}
