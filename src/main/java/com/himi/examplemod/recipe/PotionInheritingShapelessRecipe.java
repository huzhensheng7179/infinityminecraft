package com.himi.examplemod.recipe;

import com.himi.examplemod.infinitycraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 会继承药水效果的无序合成配方（雪碧：药水 + 糖 ×2 + 岩浆膏）。
 *
 * <p>配方类型依旧是原版工作台（{@code RecipeType.CRAFTING}），只是换用本模组的序列化器，
 * 因此直接继承原版 {@link ShapelessRecipe}：槽位数量校验、重复原料的分配（NeoForge 的
 * RecipeMatcher）等匹配逻辑完全沿用，无需重写 {@code matches}。</p>
 *
 * <p>唯一的行为差别在 {@link #assemble}：原版无序合成只会返回结果物品的空白副本，
 * 这里额外从合成格中找出第一份带 {@code minecraft:potion_contents} 组件的药水，
 * 把它的药水内容原样写到产物上——产物由此"记住"自己是用什么药水调出来的，
 * 饮用时按 {@link com.himi.examplemod.item.XuebiItem} 施加这份继承效果。</p>
 */
public class PotionInheritingShapelessRecipe extends ShapelessRecipe {

    /** 无序合成最多占用 3×3 工作台的全部槽位 */
    private static final int MAX_INGREDIENTS = 9;

    public PotionInheritingShapelessRecipe(
            String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        PotionContents contents = findPotionContents(input);
        if (contents != null) {
            result.set(DataComponents.POTION_CONTENTS, contents);
        }
        return result;
    }

    /** 取合成格中第一份药水的 potion_contents 组件；格内没有药水则返回 null。 */
    private static PotionContents findPotionContents(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                return contents;
            }
        }
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return infinitycraft.POTION_SHAPELESS_SERIALIZER.get();
    }

    /**
     * 序列化器：JSON 结构与原版 {@code minecraft:crafting_shapeless} 完全一致
     * （group / category / ingredients / result），仅类型换成本模组的
     * {@code infinitycraft:potion_shapeless}，因此 JEI 等按原版无序合成处理即可。
     */
    public static class Serializer implements RecipeSerializer<PotionInheritingShapelessRecipe> {

        private static final MapCodec<PotionInheritingShapelessRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                                CraftingBookCategory.CODEC.fieldOf("category")
                                        .orElse(CraftingBookCategory.MISC)
                                        .forGetter(ShapelessRecipe::category),
                                ItemStack.STRICT_CODEC.fieldOf("result")
                                        .forGetter((PotionInheritingShapelessRecipe recipe) -> recipe.getResultItem(null)),
                                Ingredient.CODEC_NONEMPTY.listOf()
                                        .fieldOf("ingredients")
                                        .flatXmap(
                                                ingredients -> {
                                                    Ingredient[] array = ingredients.toArray(Ingredient[]::new);
                                                    if (array.length == 0) {
                                                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                                                    }
                                                    if (array.length > MAX_INGREDIENTS) {
                                                        return DataResult.error(() -> "Too many ingredients for shapeless recipe."
                                                                + " The maximum is: " + MAX_INGREDIENTS);
                                                    }
                                                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, array));
                                                },
                                                DataResult::success)
                                        .forGetter(ShapelessRecipe::getIngredients))
                        .apply(instance, PotionInheritingShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PotionInheritingShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<PotionInheritingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PotionInheritingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static PotionInheritingShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new PotionInheritingShapelessRecipe(group, category, result, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, PotionInheritingShapelessRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
        }
    }
}
