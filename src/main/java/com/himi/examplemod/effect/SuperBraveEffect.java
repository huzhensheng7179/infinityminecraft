package com.himi.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * “超勇”效果：饮用「令 人 超 勇 的 啤 酒」后获得。
 *
 * <p>本类自身不含 tick 逻辑；其“下次近战攻击伤害翻倍（翻倍后消耗）”的行为由
 * {@link com.himi.examplemod.event.SuperBraveBeerHandler} 在伤害结算阶段统一处理。</p>
 *
 * <p>类别设为 BENEFICIAL（有益），使其可被牛奶/死亡清除并按增益效果处理。</p>
 */
public class SuperBraveEffect extends MobEffect {
    public SuperBraveEffect() {
        // 有益效果；颜色取啤酒般的琥珀金
        super(MobEffectCategory.BENEFICIAL, 0xE0A33C);
    }
}
