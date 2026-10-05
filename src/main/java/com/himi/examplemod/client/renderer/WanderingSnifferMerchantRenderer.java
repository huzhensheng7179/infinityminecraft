package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.SnifferHatModel;
import com.himi.examplemod.client.renderer.layer.SnifferHatLayer;
import com.himi.examplemod.entity.WanderingSnifferMerchant;

import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 流浪嗅探兽商人渲染器：复用原版嗅探兽模型与贴图，额外叠加一顶程序化帽子（{@link SnifferHatLayer}）。
 */
public class WanderingSnifferMerchantRenderer extends MobRenderer<WanderingSnifferMerchant, SnifferModel<WanderingSnifferMerchant>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/sniffer/sniffer.png");

    public WanderingSnifferMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new SnifferModel<>(context.bakeLayer(ModelLayers.SNIFFER)), 1.1F);
        this.addLayer(new SnifferHatLayer(this, context.bakeLayer(SnifferHatModel.HAT_LAYER)));
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingSnifferMerchant entity) {
        return TEXTURE;
    }
}
