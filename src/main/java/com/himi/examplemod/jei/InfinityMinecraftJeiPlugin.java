package com.himi.examplemod.jei;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.recipe.PotionInheritingShapelessRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 插件 - 注册配方催化剂和物品信息
 */
@JeiPlugin
public class InfinityMinecraftJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        // 接管 potion_shapeless 在工作台配方页的展示（药水槽不再画成「不可合成的药水」）。
        // JEI 按配方的具体类做精确匹配，因此这里注册的扩展优先于原版的 CraftingRecipe 默认扩展。
        registration.getCraftingCategory()
                .addExtension(PotionInheritingShapelessRecipe.class, new PotionShapelessCraftingExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 数据驱动：为本模组所有在 lang 中定义了 jei.<modid>.<path>.desc 的物品注册 JEI 信息页。
        // 新增物品只需补一条 jei.*.desc 语言键即可自动显示，无需改代码（与 CurioTooltipHandler 的 .desc 机制同理）。
        Language language = Language.getInstance();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!infinitycraft.MODID.equals(id.getNamespace())) continue;
            String key = "jei." + id.getNamespace() + "." + id.getPath() + ".desc";
            if (language.has(key)) {
                registration.addIngredientInfo(
                        new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(key));
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册锻造台为自定义锻造配方的催化剂（JEI 已自动处理 RecipeType.SMITHING）
    }
}
