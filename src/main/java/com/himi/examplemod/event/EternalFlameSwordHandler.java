package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * “在烈焰中永恒”剑事件处理器。
 * 主手持该剑进行近战攻击时，造成的伤害无视一切减伤——
 * 护甲、保护类附魔、抗性提升等减免全部归零，等同“虚空伤害”的不可减免特性。
 *
 * 说明：
 * - 减免修改器只能在 {@link LivingIncomingDamageEvent} 阶段生效（伤害管线中减免计算之前），
 *   在 {@code LivingDamageEvent.Pre} 阶段修改减免为时已晚；
 * - 仍保留近战攻击类型（PLAYER_ATTACK），因此死亡消息正常，且横扫/暴击/锋利等剑类机制照常生效。
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class EternalFlameSwordHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        // 仅近战攻击
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        // 攻击者主手必须持有“在烈焰中永恒”
        if (!attacker.getMainHandItem().is(infinitycraft.ETERNAL_IN_FLAMES.get())) return;

        // 无视一切减伤：护甲、保护类附魔、抗性提升
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
        event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0.0F);
        event.addReductionModifier(DamageContainer.Reduction.MOB_EFFECTS, (container, reduction) -> 0.0F);
    }
}
