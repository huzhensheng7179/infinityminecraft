package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.ScorchingEye;
import com.himi.examplemod.infinitycraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 炽热邪眼的 GeckoLib 模型资源绑定：几何体、动画、贴图。
 *
 * <p>贴图含翅膀薄片，使用 {@link RenderType#entityCutoutNoCull} 渲染，避免背面剔除导致翅膀/体积缺失。</p>
 */
public class ScorchingEyeModel extends GeoModel<ScorchingEye> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "geo/scorching_eye.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "animations/scorching_eye.animation.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/scorching_eye.png");

    @Override
    public ResourceLocation getModelResource(ScorchingEye animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ScorchingEye animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ScorchingEye animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(ScorchingEye animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
