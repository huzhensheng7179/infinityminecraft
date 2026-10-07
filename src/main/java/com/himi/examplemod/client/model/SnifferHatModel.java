package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.WanderingSnifferMerchant;
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

/**
 * 流浪嗅探兽商人的程序化帽子模型（几何建模 + 专用贴图）。
 *
 * <p>帽子贴图取自嗅探兽「红绿交接」配色：上部帽顶为红棕（嗅探兽主体皮革色）、
 * 下部帽檐为苔绿（嗅探兽背部苔藓色），绿色帽檐将红色头部与帽子区分开。</p>
 *
 * <p>模型层级刻意镜像原版嗅探兽的 {@code root → bone → body → head} 结构，渲染前由
 * {@link com.himi.examplemod.client.renderer.layer.SnifferHatLayer} 逐帧复制父模型对应部件的姿态，
 * 使帽子始终贴合头部并跟随其动画（行走/嗅探/掘地等）。</p>
 *
 * <p>帽子为方正造型：宽帽檐（绿）+ 方帽顶（红）。</p>
 */
public class SnifferHatModel extends EntityModel<WanderingSnifferMerchant> {

    public static final ModelLayerLocation HAT_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "sniffer_merchant_hat"), "main");

    private final ModelPart root;
    private final ModelPart bone;
    private final ModelPart body;
    private final ModelPart head;

    public SnifferHatModel(ModelPart baked) {
        this.root = baked.getChild("root");
        this.bone = this.root.getChild("bone");
        this.body = this.bone.getChild("body");
        this.head = this.body.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        // 镜像原版嗅探兽各部件的偏移，确保帽子定位与头部一致
        PartDefinition rootDef = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition boneDef = rootDef.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition bodyDef = boneDef.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        // 头部原点：头顶约 y=-7.5，头部中心 z=-6；帽子以此为中心叠放
        PartDefinition headDef = bodyDef.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 6.5F, -19.48F));
        // 方正帽子：方帽顶（红，取贴图左半）+ 宽帽檐（绿，取贴图右半）；贴图 128x64
        headDef.addOrReplaceChild("hat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.5F, -14.0F, -11.5F, 11.0F, 5.0F, 11.0F, new CubeDeformation(0.0F))  // 方帽顶（红）
                        .texOffs(64, 0)
                        .addBox(-8.5F, -9.0F, -13.5F, 17.0F, 1.0F, 15.0F, new CubeDeformation(0.0F)), // 宽帽檐（绿）
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 逐帧复制父模型（原版嗅探兽模型）root→bone→body→head 的姿态，使帽子贴合并跟随头部动画。 */
    public void syncToParent(ModelPart parentRoot) {
        // 原版嗅探兽模型层级为逐级嵌套：root → bone → body → head
        copyPose(parentRoot, this.root);
        ModelPart parentBone = parentRoot.getChild("bone");
        copyPose(parentBone, this.bone);
        ModelPart parentBody = parentBone.getChild("body");
        copyPose(parentBody, this.body);
        ModelPart parentHead = parentBody.getChild("head");
        copyPose(parentHead, this.head);
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }

    @Override
    public void setupAnim(WanderingSnifferMerchant entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // 姿态由 syncToParent 逐帧复制，无需在此驱动
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(pose, vertexConsumer, packedLight, packedOverlay, color);
    }
}
