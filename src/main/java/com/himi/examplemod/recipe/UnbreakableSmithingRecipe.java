package com.himi.examplemod.recipe;

import com.himi.examplemod.infinitycraft;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

/**
 * 自定义锻造配方：使用无暇辰星升级模板 + 无暇辰星，在锻造台中为任意带耐久物品（含下界合金）附上"无法破坏"。
 * 配方槽位：无暇辰星升级模板 + 任意带耐久物品 + 无暇辰星
 */
public class UnbreakableSmithingRecipe extends SmithingTransformRecipe {

    public UnbreakableSmithingRecipe() {
        super(
                Ingredient.of(infinitycraft.FLAWLESS_STAR_TEMPLATE.get()),
                Ingredient.EMPTY,
                Ingredient.of(infinitycraft.FLAWLESS_STAR.get()),
                ItemStack.EMPTY
        );
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        // 模板槽位：必须是无暇辰星升级模板
        if (!input.template().is(infinitycraft.FLAWLESS_STAR_TEMPLATE.get())) return false;
        // 基础槽位：必须带有耐久且尚未拥有无法破坏
        ItemStack base = input.base();
        if (base.getMaxDamage() <= 0) return false;
        if (base.has(DataComponents.UNBREAKABLE)) return false;
        // 附加槽位：必须是无暇辰星
        return input.addition().is(infinitycraft.FLAWLESS_STAR.get());
    }

    /**
     * 覆盖 base 槽合法性判断：原版锻造台的 base 槽只接受"被某个锻造配方认领为 base"的物品
     * （槽谓词 = 所有 SMITHING 配方 anyMatch(isBaseIngredient)）。下界合金没有任何原版配方以它为 base，
     * 而本配方 base 为 Ingredient.EMPTY（继承的 isBaseIngredient 恒 false），导致下界合金武器无法放入 base 槽。
     * 这里声明"任意带耐久且尚未无法破坏的物品"都可作为 base，与 matches() 的 base 条件保持一致，
     * 使下界合金等物品能通过 base 槽谓词。不影响原版配方，也不触碰掉落/其他模组逻辑。
     */
    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getMaxDamage() > 0 && !stack.has(DataComponents.UNBREAKABLE);
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
        return infinitycraft.UNBREAKABLE_SMITHING_SERIALIZER.get();
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
