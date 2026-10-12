package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.NailongMaskModel;
import com.himi.examplemod.infinitycraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * 奶龙面具的 Curios 头饰渲染器：以「原版盔甲」方式把面具模型贴合在佩戴者头部。
 * <p>
 * 与旧的 GeckoLib 方案不同，这里直接复用 {@link NailongMaskModel}（几何坐标与原版 head 部件同空间），
 * 渲染时先 {@code humanoid.head.translateAndRotate} 把 PoseStack 移动到头部部件（含头部 yaw/pitch 与潜行下沉），
 * 再原样渲染面具立方体，因此无需缩放/位移补偿即可像头盔一样贴合、并跟随头部转动。
 */
public class NailongMaskCurioRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/nailong_mask.png");

    private final NailongMaskModel model;

    public NailongMaskCurioRenderer() {
        // Curios 惰性实例化本渲染器（首次渲染时），此时模型层定义已完成注册，可安全 bakeLayer
        this.model = new NailongMaskModel(Minecraft.getInstance().getEntityModels().bakeLayer(NailongMaskModel.LAYER));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                                                                          PoseStack poseStack,
                                                                          RenderLayerParent<T, M> renderLayerParent,
                                                                          MultiBufferSource buffers, int light,
                                                                          float limbSwing, float limbSwingAmount,
                                                                          float partialTicks, float ageInTicks,
                                                                          float netHeadYaw, float headPitch) {
        EntityModel<T> parentModel = renderLayerParent.getModel();
        if (!(parentModel instanceof HumanoidModel<?> humanoid)) {
            return; // 仅在有人形头部的实体上佩戴显示
        }

        poseStack.pushPose();
        humanoid.head.translateAndRotate(poseStack);

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.head().render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
