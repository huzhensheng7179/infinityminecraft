package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * “你跑不过我你信不信”事件处理器。
 *
 * <p>触发：玩家同时拥有「巧乐兹风暴」与「雪碧风暴」时，移除两者并转化为持续 60 秒的本 buff。</p>
 *
 * <p>buff 期间：</p>
 * <ul>
 *   <li>移动速度 +40%、台阶高度 +0.5（0.6→1.1，可直接跨过 1 格高方块）；</li>
 *   <li>每秒对周身半径 12 格内所有生物（除自己）施加缓慢 III，并造成 6 点「无视护甲/buff抗性/无敌帧」
 *       的冰冻伤害（自定义伤害类型 {@code infinitycraft:cant_catch_me_freeze}，
 *       经标签 bypasses_armor / bypasses_resistance / bypasses_cooldown / is_freezing 配置）；</li>
 *   <li>代价：每秒流失 1 点生命（扁平扣血，无视护甲/抗性/吸收，附带扣血音效与受击动画；触及 0 时走 genericKill 正常死亡）；</li>
 *   <li>冲刺时上述全部效果翻倍：速度 +80%、台阶 +1.0、光环伤害 12、缓慢 VI、自损 2。</li>
 * </ul>
 *
 * <p>速度/台阶以「瞬时属性修改器」维护，仅在数值变化时 {@code addOrUpdateTransientModifier}
 * 以避免每 tick 重复下发同步包；两属性均为 setSyncable，服务端修改会自动同步到客户端。
 * buff 消失时移除修改器。仅在服务端结算，伤害为服务端权威。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class CantCatchMeHandler {

    private static final int COMBINED_DURATION_TICKS = 1200;    // 合成后持续 60 秒
    private static final int AURA_RADIUS = 12;                  // 光环半径
    private static final float BASE_AURA_FREEZE_DAMAGE = 6.0F;  // 每秒 6 点冰冻伤害
    private static final int BASE_SLOWDOWN_AMPLIFIER = 2;       // 缓慢 III
    private static final int SLOWDOWN_DURATION_TICKS = 40;      // 缓慢持续 2 秒（每秒刷新，留缓冲）
    private static final float BASE_SELF_DRAIN = 1.0F;          // 每秒流失 1 点生命
    private static final double BASE_STEP_HEIGHT_BONUS = 0.5;   // 台阶高度 +0.5，可跨 1 格
    private static final double BASE_SPEED_BONUS = 0.4;         // 移动速度 +40%

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "cant_catch_me.speed");
    private static final ResourceLocation STEP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "cant_catch_me.step_height");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return; // 仅服务端结算；速度/台阶为可同步属性，会下发到客户端

        // 1) 合成触发：同时拥有巧乐兹风暴与雪碧风暴 -> 移除两者，转化为「你跑不过我你信不信」
        if (player.hasEffect(infinitycraft.CHOCO_STORM)
                && player.hasEffect(infinitycraft.XUEBI_STORM)) {
            player.removeEffect(infinitycraft.CHOCO_STORM);
            player.removeEffect(infinitycraft.XUEBI_STORM);
            player.addEffect(new MobEffectInstance(infinitycraft.CANT_CATCH_ME, COMBINED_DURATION_TICKS, 0));
        }

        // 2) 无本 buff 时清理属性修改器并返回
        if (!player.hasEffect(infinitycraft.CANT_CATCH_ME)) {
            removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
            removeModifier(player, Attributes.STEP_HEIGHT, STEP_MODIFIER_ID);
            return;
        }

        // 冲刺时全部效果翻倍
        boolean sprinting = player.isSprinting();
        double multiplier = sprinting ? 2.0 : 1.0;

        // 提速 +40%（冲刺 +80%）；抬高台阶 +0.5（冲刺 +1.0），可直接跨过 1 格高方块
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID,
                BASE_SPEED_BONUS * multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, Attributes.STEP_HEIGHT, STEP_MODIFIER_ID,
                BASE_STEP_HEIGHT_BONUS * multiplier, AttributeModifier.Operation.ADD_VALUE);

        // 每秒结算一次光环与自损
        if (player.tickCount % 20 != 0) return;

        // 光环：半径 12 格内所有生物（除自己）获得缓慢，并承受穿透冰冻伤害
        int slowAmplifier = sprinting ? (BASE_SLOWDOWN_AMPLIFIER + 1) * 2 - 1 : BASE_SLOWDOWN_AMPLIFIER; // III -> VI
        float auraDamage = (float) (BASE_AURA_FREEZE_DAMAGE * multiplier);
        DamageSource freeze = cantCatchMeFreeze(player);
        AABB area = player.getBoundingBox().inflate(AURA_RADIUS);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player || !target.isAlive()) continue;
            if (target.distanceTo(player) > AURA_RADIUS) continue;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWDOWN_DURATION_TICKS, slowAmplifier));
            target.hurt(freeze, auraDamage);
        }

        // 自损：每秒流失 1 点生命（冲刺 2 点）。扁平扣血无视护甲/抗性/吸收；触及 0 时走伤害管线正常死亡
        float drain = (float) (BASE_SELF_DRAIN * multiplier);
        float remaining = player.getHealth() - drain;
        if (remaining > 0.0F) {
            player.setHealth(remaining);
            // 扁平扣血不走伤害管线，手动广播受击事件：播放扣血音效 + 红闪/倾斜受击动画
            ((ServerLevel) player.level()).broadcastDamageEvent(player, player.damageSources().generic());
        } else {
            player.hurt(player.damageSources().genericKill(), player.getHealth());
        }
    }

    /** 自定义冰冻伤害源：经标签配置为无视护甲/抗性/无敌帧，且归类为冰冻伤害（冻结视觉/死亡信息）。 */
    private static DamageSource cantCatchMeFreeze(Player player) {
        Holder<DamageType> type = player.damageSources().damageTypes
                .getHolderOrThrow(infinitycraft.CANT_CATCH_ME_FREEZE);
        return new DamageSource(type, player);
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.amount() == amount) return; // 数值未变则不重复下发同步包
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
