package com.himi.examplemod.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * “破灭”剑柄的材料等级。
 *
 * <p>“破灭”是一件可在 8 种形态间切换的剑（{@link RuinationItem}）。它的攻击伤害与攻速
 * 并非由本等级决定，而是随形态写入 {@code ATTRIBUTE_MODIFIERS} 组件动态替换；本等级只提供：
 * - 耐久 2000（{@link net.minecraft.world.item.TieredItem} 会用 getUses() 作为物品耐久）；
 * - 攻击伤害加成 0：具体伤害由各形态的属性组件给出（见 {@link RuinationItem}）；
 * - 挖掘速度/错误方块沿用下界合金级别，便于破坏蜘蛛网等剑可加速的方块；
 * - 不可用材料修复（{@link Ingredient#EMPTY}）。</p>
 */
public enum RuinationTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 2000;
    }

    @Override
    public float getSpeed() {
        return 9.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
