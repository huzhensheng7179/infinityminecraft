package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * “令 人 超 勇 的 啤 酒”事件处理器：持有「超勇」效果的玩家进行近战攻击时，
 * 本次伤害翻倍，随后消耗掉该效果（“下次攻击翻倍”为一次性）。
 *
 * <p>翻倍在 {@link LivingDamageEvent.Pre} 阶段通过 {@code setNewDamage(base × 2)} 实现——
 * 与“火与钢”的倍率提升处于同一管线阶段（护甲/附魔/抗性减免之后、吸收之前），
 * 故翻倍作用于减免后的实际伤害。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class SuperBraveBeerHandler {

    private static final float DAMAGE_MULTIPLIER = 2.0F;

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // 仅近战攻击
        if (!attacker.hasEffect(infinitycraft.SUPER_BRAVE)) return;

        // 本次近战伤害翻倍
        event.setNewDamage(event.getNewDamage() * DAMAGE_MULTIPLIER);
        // 一次性：翻倍后消耗「超勇」效果
        attacker.removeEffect(infinitycraft.SUPER_BRAVE);
    }
}
