package com.himi.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * “巧乐兹风暴”效果。
 * 该效果本身不含每 tick 逻辑，其两大特性分别由外部实现：
 * - 任意方块如冰面般滑行：由 {@link com.himi.examplemod.mixin.BlockFrictionMixin}
 *   在方块摩擦查询处将摩擦系数改为冰面值（0.98）；
 * - 受击反射冰冻伤害：由 {@link com.himi.examplemod.event.ChocoStormHandler}
 *   在 {@code LivingIncomingDamageEvent} 中对伤害来源单位施加冰冻伤害（1级4/2级8/3级16）。
 *
 * 类别设为 BENEFICIAL（有益），使其可被牛奶/死亡清除并按增益效果处理。
 */
public class ChocoStormEffect extends MobEffect {

    public ChocoStormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9EDCF0);
    }
}
