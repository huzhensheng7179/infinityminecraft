package com.himi.examplemod.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * “在烈焰中永恒”剑的材料等级。
 * - 耐久 2700（{@link net.minecraft.world.item.TieredItem} 会用 getUses() 作为物品耐久）；
 * - 攻击伤害加成 0：最终 50 点伤害由 SwordItem.createAttributes 的参数（49 + 玩家基础 1）决定；
 * - 挖掘速度/错误方块沿用下界合金级别，便于破坏蜘蛛网等剑可加速的方块。
 */
public enum EternalFlameTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 2700;
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
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
