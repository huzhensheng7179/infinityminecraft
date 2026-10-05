package com.himi.examplemod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * 令 人 超 勇 的 啤 酒——一种饮品。
 *
 * <p>与雪碧一致，重写 {@link #getUseAnimation} 返回 {@link UseAnim#DRINK} 播放“饮用”动作；
 * 饮用音效沿用默认的 {@code GENERIC_DRINK}。</p>
 *
 * <p>饮用后获得「超勇」效果（下次近战攻击伤害翻倍，翻倍后消耗），
 * 效果逻辑见 {@link com.himi.examplemod.event.SuperBraveBeerHandler}。</p>
 */
public class BeerItem extends Item {
    public BeerItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
