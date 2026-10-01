package com.himi.examplemod.jei;

import com.himi.examplemod.infinityminecraft;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI 插件 - 注册配方催化剂和物品信息
 */
@JeiPlugin
public class InfinityMinecraftJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(infinityminecraft.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 注册无暇辰星的 JEI 信息描述
        registration.addIngredientInfo(
                new ItemStack(infinityminecraft.FLAWLESS_STAR.get()),
                VanillaTypes.ITEM_STACK,
                net.minecraft.network.chat.Component.translatable("jei.infinityminecraft.flawless_star.desc")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册锻造台为自定义锻造配方的催化剂（JEI 已自动处理 RecipeType.SMITHING）
    }
}
