package com.himi.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * “你跑不过我你信不信”效果——巧乐兹风暴与雪碧风暴同时存在时合成得到。
 *
 * <p>本类为纯标记效果，不含 tick 逻辑；其全部行为（提速、抬高台阶、周身冰冻光环、
 * 每秒生命流失、冲刺翻倍）由 {@link com.himi.examplemod.event.CantCatchMeHandler}
 * 在玩家 tick 中统一维护，以便按冲刺状态动态调整属性修改器。</p>
 */
public class CantCatchMeEffect extends MobEffect {
    public CantCatchMeEffect() {
        // 中性：强力增益但伴随每秒生命流失的代价；颜色取疾风般的亮青
        super(MobEffectCategory.NEUTRAL, 0x8EE9FF);
    }
}
