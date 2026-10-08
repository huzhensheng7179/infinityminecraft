package com.himi.examplemod.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

/**
 * 雪碧——一种饮品。
 *
 * <p>与普通食物唯一的区别是使用动画：重写 {@link #getUseAnimation} 返回
 * {@link UseAnim#DRINK}，从而播放“饮用”动作；饮用音效沿用
 * {@code Item#getDrinkingSound()} 的默认值 {@code GENERIC_DRINK}，无需额外处理。</p>
 *
 * <p>用「药水 + 糖 ×2 + 岩浆膏」调出来的雪碧会带上那份药水的效果：效果以原版的
 * {@code minecraft:potion_contents} 组件形式存在瓶子上（写入方见
 * {@code recipe/PotionInheritingShapelessRecipe}），饮用时在雪碧风暴之外按原版药水的方式再施加一遍。</p>
 */
public class XuebiItem extends Item {
    public XuebiItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    /**
     * 喝完时补上继承来的药水效果。雪碧自身的雪碧风暴（食物组件里的效果）仍由 super 负责，
     * 这里只处理药水内容那一份，施加方式与 {@code PotionItem} 保持一致：瞬间型效果走
     * {@link MobEffect#applyInstantenousEffect}（治疗、伤害这类不能当普通效果加），其余直接 addEffect，且只在服务端做。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        // 必须在 super 之前取：super 会消耗 1 个，瓶子见底后组件就随空物品一起消失了
        PotionContents inherited = stack.get(DataComponents.POTION_CONTENTS);
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (inherited != null && !level.isClientSide) {
            Player player = livingEntity instanceof Player p ? p : null;
            inherited.forEachEffect(instance -> {
                MobEffect effect = instance.getEffect().value();
                if (effect.isInstantenous()) {
                    effect.applyInstantenousEffect(player, player, livingEntity, instance.getAmplifier(), 1.0F);
                } else {
                    livingEntity.addEffect(instance);
                }
            });
        }
        return result;
    }

    /** 有继承效果时按原版药水的写法列出（用的是原版翻译键，不新增文案）；普通雪碧不加行。 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            contents.addPotionTooltip(tooltipComponents::add, 1.0F, context.tickRate());
        }
    }
}
