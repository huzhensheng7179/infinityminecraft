package com.himi.examplemod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * 雪碧——一种饮品。
 *
 * <p>与普通食物唯一的区别是使用动画：重写 {@link #getUseAnimation} 返回
 * {@link UseAnim#DRINK}，从而播放“饮用”动作；饮用音效沿用
 * {@code Item#getDrinkingSound()} 的默认值 {@code GENERIC_DRINK}，无需额外处理。</p>
 */
public class XuebiItem extends Item {
    public XuebiItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
