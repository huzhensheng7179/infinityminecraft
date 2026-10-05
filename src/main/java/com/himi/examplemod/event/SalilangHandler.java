package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * “萨日朗”剑事件处理器：主手持该剑近战攻击时，
 * 若目标在本次伤害结算前的生命值高于其最大生命的 50%，则为目标附加凋零效果。
 *
 * <p>判定放在 {@link LivingDamageEvent.Pre} 阶段读取 {@code victim.getHealth()}——
 * 此时伤害尚未应用，得到的是“被攻击时”的生命值，符合“攻击半血以上生物”的语义。
 * 凋零为持续伤害效果，通过 {@code addEffect} 施加，不受无敌帧影响。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class SalilangHandler {

    private static final float HEALTH_THRESHOLD = 0.5F;    // 生命比例阈值（>50%）
    private static final int WITHER_DURATION_TICKS = 100;  // 凋零持续 5 秒
    private static final int WITHER_AMPLIFIER = 0;         // 凋零 I

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // 仅近战攻击
        if (!attacker.getMainHandItem().is(infinityminecraft.SA_RI_LANG.get())) return;

        LivingEntity victim = event.getEntity();
        if (victim == attacker) return;

        // 目标被攻击时生命 > 50% 最大生命 → 附加凋零 I / 5 秒
        if (victim.getHealth() > victim.getMaxHealth() * HEALTH_THRESHOLD) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, WITHER_AMPLIFIER));
        }
    }
}
