package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.FlameSalamander;
import com.himi.examplemod.infinitycraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 烈焰蝾螈的 GeckoLib 模型资源绑定：几何体、动画、贴图。
 *
 * <p>贴图为不透明的蝾螈皮肤（textures/entity/flame_salamander.png），使用
 * {@link RenderType#entityCutoutNoCull} 渲染，避免背面剔除导致体积感缺失。</p>
 */
public class FlameSalamanderModel extends GeoModel<FlameSalamander> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "geo/flame_salamander.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "animations/flame_salamander.animation.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/flame_salamander.png");

    @Override
    public ResourceLocation getModelResource(FlameSalamander animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FlameSalamander animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FlameSalamander animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(FlameSalamander animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
