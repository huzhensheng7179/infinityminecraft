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
 * “雪碧风暴”事件处理器：持有该效果的玩家近战命中敌人时，
 * 使敌人减速，并额外承受一份「冰冻」伤害。
 *
 * <p>额外冰冻伤害随效果等级翻倍：I 级 2 点、II 级 4 点、III 级 8 点（即 2 × 2^amplifier）；
 * 减速固定为「缓慢 II」持续 5 秒。</p>
 *
 * <p>伤害管线要点（经反编译源码核验，NeoForge 21.1.252 / MC 1.21.1）：</p>
 * <ul>
 *   <li>额外伤害放在 {@link LivingDamageEvent.Post}——此时本体近战伤害已结算完毕、
 *       受害者 {@code invulnerableTime} 已被置为受击无敌帧（通常 20）；</li>
 *   <li>若直接再 {@code hurt}，无敌帧会吞掉这份冰冻伤害（{@code LivingEntity#hurt} 中
 *       {@code invulnerableTime > 10} 时，小于等于 lastHurt 的伤害被完全忽略），
 *       故先将 {@code invulnerableTime} 归零，让真实冰冻伤害完整生效；
 *       该次 {@code hurt} 内部会重新把无敌帧设回 20，不留额外空窗；</li>
 *   <li>不能在近战的 {@code LivingIncomingDamageEvent} 内嵌套 {@code hurt}——那会在本体伤害
 *       结算前污染无敌帧与 lastHurt，反而吞掉玩家这一刀；</li>
 *   <li>反射的是「冰冻」伤害（非 PLAYER_ATTACK），不会再次触发本处理器，无递归风险。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class XuebiStormHandler {

    private static final int SLOWDOWN_DURATION_TICKS = 100;  // 减速持续 5 秒
    private static final int SLOWDOWN_AMPLIFIER = 1;         // 缓慢 II
    private static final double BASE_FREEZE_DAMAGE = 2.0;    // I 级额外冰冻伤害

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // 仅近战攻击

        MobEffectInstance storm = attacker.getEffect(infinityminecraft.XUEBI_STORM);
        if (storm == null) return;

        LivingEntity victim = event.getEntity();
        if (victim == attacker) return;

        // 减速：缓慢 II，持续 5 秒
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                SLOWDOWN_DURATION_TICKS, SLOWDOWN_AMPLIFIER));

        // 额外冰冻伤害：2 × 2^amplifier（限制指数上限，避免极高 amplifier 整型溢出）
        int amplifier = Math.min(storm.getAmplifier(), 20);
        float extra = (float) (BASE_FREEZE_DAMAGE * Math.pow(2.0, amplifier));

        // 归零无敌帧，确保这份冰冻伤害不被本体近战的受击无敌帧吞掉
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().freeze(), extra);
    }
}
