package com.himi.examplemod.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * “铜铸之刃”剑的材料等级。
 * - 耐久 550（{@link net.minecraft.world.item.TieredItem} 会用 getUses() 作为物品耐久）；
 * - 攻击伤害加成 0：最终 6 点伤害由 SwordItem.createAttributes 的参数（5 + 玩家基础 1）决定；
 * - 攻速由 createAttributes 参数 -1.9 决定（玩家基础攻速 4.0 − 1.9 = 2.1）；
 * - 挖掘速度沿用较高级别，便于破坏剑可加速的方块；不可修复（无修复材料）。
 */
public enum CopperForgedBladeTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 550;
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
        return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
