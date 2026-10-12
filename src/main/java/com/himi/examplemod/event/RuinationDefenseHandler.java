package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 「破灭」防御/免疫类效果处理器（服务端权威）。
 *
 * <ul>
 *   <li>7 末地更新（不是）：手持时 20% 概率免疫所受伤害，并免疫中毒与凋零；强化期封锁附近末影人瞬移；</li>
 *   <li>灵魂火灼烧（炎烧炽铁右键强化命中）：{@code SOUL_BURN} 期间目标不能回血。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class RuinationDefenseHandler {

    private static final float IMMUNE_CHANCE = 0.2F;   // 末地更新：20% 免伤
    private static final double ENDERMAN_BLOCK_RADIUS = 32.0D; // 末影人禁瞬移判定半径

    // 末地更新：手持时 20% 概率免疫受到的伤害
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(victim instanceof Player player)) return;
        if (!RuinationBuffs.isHoldingForm(player, 7)) return;
        if (victim.getRandom().nextFloat() < IMMUNE_CHANCE) {
            event.setAmount(0.0F);
        }
    }

    // 灵魂火灼烧期间不能回血
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(infinitycraft.SOUL_BURN)) {
            event.setCanceled(true);
        }
    }

    // 末地更新：手持时免疫中毒与凋零
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!RuinationBuffs.isHoldingForm(player, 7)) return;
        MobEffectInstance inst = event.getEffectInstance();
        MobEffect effect = inst.getEffect().value();
        if (effect == MobEffects.POISON.value() || effect == MobEffects.WITHER.value()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // 末地更新强化期：封锁判定半径内的末影人随机瞬移
    @SubscribeEvent
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        if (!(event.getEntity() instanceof EnderMan enderman)) return;
        if (enderman.level().isClientSide()) return;
        for (Player p : enderman.level().getEntitiesOfClass(Player.class,
                enderman.getBoundingBox().inflate(ENDERMAN_BLOCK_RADIUS))) {
            if (RuinationBuffs.hasEnder(p)
                    && p.distanceToSqr(enderman) <= ENDERMAN_BLOCK_RADIUS * ENDERMAN_BLOCK_RADIUS) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
