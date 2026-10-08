package com.himi.examplemod.jei;

import com.himi.examplemod.recipe.PotionInheritingShapelessRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 雪碧那条「继承药水效果」的无序合成在 JEI 工作台配方页里的展示适配。
 *
 * <p>问题出在原料的展示方式上：JEI 的默认扩展把每个 {@link Ingredient} 直接展开成
 * {@link Ingredient#getItems()} 塞进槽位，而 {@code infinitycraft:potions} 标签展开出来的是
 * <b>不带 {@code minecraft:potion_contents} 组件</b>的药水物品——1.21.1 已经没有「水瓶」这件物品，
 * 缺组件的药水只会画成一支「不可合成的药水」，玩家看不出这里其实「任意药水皆可、效果会被带走」。</p>
 *
 * <p>本扩展接管 {@link PotionInheritingShapelessRecipe}（注册时用的是精确类匹配，
 * 优先级高于原版为 {@code CraftingRecipe} 准备的默认扩展），排版仍交给
 * {@link ICraftingGridHelper} 完成，只是把药水槽的展示换成三种水瓶，
 * 同时把真正的药水标签补写进隐形原料，保证 JEI 按药水搜索时这条配方依然能被检索到。</p>
 */
public class PotionShapelessCraftingExtension implements ICraftingCategoryExtension<PotionInheritingShapelessRecipe> {

    @Override
    public void setRecipe(
            RecipeHolder<PotionInheritingShapelessRecipe> recipeHolder,
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses) {
        PotionInheritingShapelessRecipe recipe = recipeHolder.value();
        craftingGridHelper.createAndSetOutputs(builder, List.of(recipe.getResultItem(null)));

        List<Ingredient> ingredients = recipe.getIngredients();
        List<List<ItemStack>> inputs = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            boolean potion = isPotionIngredient(ingredient);
            inputs.add(potion ? waterBottleVariants(ingredient) : List.of(ingredient.getItems()));
            if (potion) {
                // 槽位里展示的是水瓶，检索不到别的药水；把原始原料补进隐形原料，搜索行为保持和原版一致
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(ingredient);
            }
        }
        // 宽高传 0 即按无序合成排版，JEI 会自动补上「无序」角标
        craftingGridHelper.createAndSetInputs(builder, inputs, 0, 0);
    }

    /**
     * 把药水原料画成水瓶：1.21.1 的水瓶是「内容为 water 的药水」，而不是单独的物品，
     * 所以照着标签里原有的药水物品逐个换上水瓶内容，瓶型（普通/喷溅/滞留）保持不变。
     */
    private static List<ItemStack> waterBottleVariants(Ingredient ingredient) {
        List<ItemStack> bottles = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            bottles.add(PotionContents.createItemStack(stack.getItem(), Potions.WATER));
        }
        return bottles;
    }

    private static boolean isPotionIngredient(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getItems()) {
            if (isPotionItem(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPotionItem(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }
}
