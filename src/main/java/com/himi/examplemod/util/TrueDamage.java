package com.himi.examplemod.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 真伤工具：施加“无视一切”的伤害。
 *
 * <p>实现思路（与本项目「你跑不过我你信不信」「杰厕灵」的扁平扣血同源，并针对“打到别人”做了致死归属处理）：</p>
 * <ul>
 *   <li>非致死：直接 {@link LivingEntity#setHealth(float)} 扣血，绕过整个 hurt / DamageEvent 管线，
 *       因而无视护甲、buff、饰品免伤、无敌帧、以及各类在 hurt 里“限伤/减免”的代码效果；
 *       再手动 {@link ServerLevel#broadcastDamageEvent} 广播受击，保留扣血音效与红闪受击动画。</li>
 *   <li>致死：改走 {@link LivingEntity#hurt} 并用 {@code Float.MAX_VALUE}，以触发正常的死亡流程
 *       （掉落、击杀统计、进度、死亡播报），并让伤害来源（getEntity）拿到击杀归属。</li>
 * </ul>
 */
public final class TrueDamage {

    private TrueDamage() {
    }

    /**
     * 对目标施加无视一切的真伤。
     *
     * @param victim 受击目标（须为服务端实体）
     * @param amount 伤害值（真实扣除的生命）
     * @param source 伤害来源；致死分支用它触发正常死亡并给予击杀归属
     */
    public static void apply(LivingEntity victim, float amount, DamageSource source) {
        if (victim.level().isClientSide() || !victim.isAlive()) {
            return;
        }
        float health = victim.getHealth();
        if (health <= amount) {
            // 致死：走伤害管线，正常死亡、掉落、播报、归属
            victim.hurt(source, Float.MAX_VALUE);
        } else {
            // 非致死：扁平扣血绕过管线，再手动广播受击事件补上音效/红闪
            victim.setHealth(health - amount);
            ((ServerLevel) victim.level()).broadcastDamageEvent(victim, source);
        }
    }
}
