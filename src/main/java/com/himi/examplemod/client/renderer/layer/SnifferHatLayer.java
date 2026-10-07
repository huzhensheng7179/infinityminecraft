package com.himi.examplemod.client.renderer.layer;

import com.himi.examplemod.client.model.SnifferHatModel;
import com.himi.examplemod.entity.WanderingSnifferMerchant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 帽子渲染层：把程序化帽子（{@link SnifferHatModel}）叠加到嗅探兽头部，跟随头部动画。
 *
 * <p>帽子贴图为从嗅探兽主体采样的红棕皮革色（含织纹与帽带），渲染时以白色 tint 呈现贴图原色。</p>
 */
public class SnifferHatLayer extends RenderLayer<WanderingSnifferMerchant, SnifferModel<WanderingSnifferMerchant>> {

    private static final ResourceLocation HAT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("infinitycraft", "textures/entity/wandering_sniffer_merchant/hat.png");
    /** 白色 tint：不额外上色，直接呈现帽子贴图本身的嗅探兽主体配色。 */
    private static final int HAT_COLOR = 0xFFFFFFFF;

    private final SnifferHatModel hatModel;

    public SnifferHatLayer(RenderLayerParent<WanderingSnifferMerchant, SnifferModel<WanderingSnifferMerchant>> parent, ModelPart hatRoot) {
        super(parent);
        this.hatModel = new SnifferHatModel(hatRoot);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, WanderingSnifferMerchant entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        this.hatModel.syncToParent(this.getParentModel().root());
        pose.pushPose();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(HAT_TEXTURE));
        this.hatModel.renderToBuffer(pose, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, HAT_COLOR);
        pose.popPose();
    }
}
