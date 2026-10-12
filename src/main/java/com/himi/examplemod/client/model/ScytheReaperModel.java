package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.ScytheReaper;
import com.himi.examplemod.infinitycraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 镰恶魔的 GeckoLib 模型资源绑定：几何体、动画、贴图。
 *
 * <p>模型含薄片面（袍摆/刀刃等）与飘动部件，使用 {@link RenderType#entityCutoutNoCull}
 * 渲染，避免背面剔除导致穿帮。</p>
 */
public class ScytheReaperModel extends GeoModel<ScytheReaper> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "geo/scythe_reaper.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "animations/scythe_reaper.animation.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/scythe_reaper.png");

    @Override
    public ResourceLocation getModelResource(ScytheReaper animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ScytheReaper animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ScytheReaper animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(ScytheReaper animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
