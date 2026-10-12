package com.himi.examplemod.client.model;

import com.himi.examplemod.infinitycraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 奶龙面具的原版盔甲风格模型（由 Blockbench「Model - mc1.17+ / Mojang mappings」导出适配 1.21.1）。
 *
 * <p>几何与 {@code 资源文件/奶龙面具.java} 完全一致：仅一个 {@code head} 部件，含两块立方体——
 * 面部面板（{@code texOffs(0,0)}，8×8×2，贴在脸前）与环绕绑带（{@code texOffs(0,12)}，10×2×10）。
 * 坐标沿用原版头部部件空间（头顶 y=-8、脸前 z 负方向），因此佩戴时像头盔一样贴合头部。</p>
 *
 * <p>贴图为 64×64 UV 图集：{@code textures/entity/nailong_mask.png}。</p>
 */
public class NailongMaskModel extends EntityModel<LivingEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "nailong_mask"), "main");

    private final ModelPart head;

    public NailongMaskModel(ModelPart root) {
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -7.0F, 8.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-5.0F, -5.0F, -6.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /** 供 Curios 渲染器直接取用头部部件：调用方先把 PoseStack 定位到佩戴者头部再 render。 */
    public ModelPart head() {
        return this.head;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // 静态面具：姿态完全由调用方通过 humanoid.head.translateAndRotate 驱动，这里不额外变换
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
            int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
