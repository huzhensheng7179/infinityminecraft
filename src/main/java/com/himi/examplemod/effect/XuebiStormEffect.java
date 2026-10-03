package com.himi.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * “雪碧风暴”效果。
 *
 * <p>本类自身不含 tick 逻辑：其“攻击附带冰冻伤害与减速”的行为由
 * {@link com.himi.examplemod.event.XuebiStormHandler} 在伤害结算后统一处理。</p>
 */
public class XuebiStormEffect extends MobEffect {
    public XuebiStormEffect() {
        // 有益效果；颜色取雪碧般的清透淡青绿
        super(MobEffectCategory.BENEFICIAL, 0x9BE8C8);
    }
}
