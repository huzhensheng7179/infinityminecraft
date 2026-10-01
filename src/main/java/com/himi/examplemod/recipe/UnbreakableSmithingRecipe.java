package com.himi.examplemod.recipe;

import com.himi.examplemod.infinityminecraft;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

/**
 * 自定义锻造配方：使用无暇辰星在锻造台中为任意带耐久物品附上"无法破坏"。
 * 配方槽位：任意锻造模板 + 任意带耐久物品 + 无暇辰星
 */
public class UnbreakableSmithingRecipe extends SmithingTransformRecipe {

    public UnbreakableSmithingRecipe() {
        super(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.EMPTY,
                Ingredient.of(infinityminecraft.FLAWLESS_STAR.get()),
                ItemStack.EMPTY
        );
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        // 模板槽位：必须是锻造模板物品
        if (!(input.template().getItem() instanceof SmithingTemplateItem)) return false;
        // 基础槽位：必须带有耐久且尚未拥有无法破坏
        ItemStack base = input.base();
        if (base.getMaxDamage() <= 0) return false;
        if (base.has(DataComponents.UNBREAKABLE)) return false;
        // 附加槽位：必须是无暇辰星
        return input.addition().is(infinityminecraft.FLAWLESS_STAR.get());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = input.base().copy();
        // 附上无法破坏组件
        result.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true; // 动态配方，不显示在配方书中
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return infinityminecraft.UNBREAKABLE_SMITHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    /**
     * 配方序列化器 - 由于配方无额外数据，序列化为空
     */
    public static class Serializer implements RecipeSerializer<UnbreakableSmithingRecipe> {

        private static final MapCodec<UnbreakableSmithingRecipe> CODEC =
                MapCodec.unit(UnbreakableSmithingRecipe::new);

        private static final StreamCodec<RegistryFriendlyByteBuf, UnbreakableSmithingRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> { /* 无数据需要编码 */ },
                        buf -> new UnbreakableSmithingRecipe()
                );

        @Override
        public MapCodec<UnbreakableSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UnbreakableSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
