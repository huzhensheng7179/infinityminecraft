package com.himi.examplemod.item;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * “铜铸之刃”剑：6 点伤害、攻速 2.1、耐久 550（数值见 {@link CopperForgedBladeTier}）。
 *
 * <p>核心特性：可在铁砧上与任意药水合成，把药水效果熔铸进剑身（同种效果取最高等级），
 * 近战命中时为受击目标附加已熔铸的全部效果——铁砧熔铸与攻击附加逻辑见
 * {@link com.himi.examplemod.event.CopperForgedBladeHandler}。</p>
 *
 * <p>熔铸的效果以原版 {@code minecraft:potion_contents} 组件的 customEffects 保存，随物品一同存储与同步；
 * 本类只负责在物品提示中像药水一样列出这些效果与时长（复用原版 {@link PotionContents#addPotionTooltip}）。</p>
 */
public class CopperForgedBladeItem extends SwordItem {

    public CopperForgedBladeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // 像药水一样显示已熔铸的效果名、等级与时长（仅在有效果时）
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            contents.addPotionTooltip(tooltipComponents::add, 1.0F, context.tickRate());
        }
    }
}
