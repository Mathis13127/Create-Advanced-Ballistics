package com.pixel.create_ordnance.content.turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pixel.create_ordnance.CreateOrdnance;

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
 * Java model for the Kinetic Turret, adapted from Blockbench export.
 *
 * <p>Model hierarchy:
 * <pre>
 * base (renamed from "static")
 * ├── Yaw  → rotates around Y axis
 * │   └── pitch → rotates around X axis
 * │       └── Launch_silo → barrel
 * │           └── Launch_plateform → back_plate → droite/guauche (doors)
 * </pre>
 *
 * <p>Texture: 512x512 ({@code kinetic_turret_texture.png})</p>
 */
public class KineticTurretModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "kinetic_turret"), "main");

    private final ModelPart base;
    private final ModelPart yaw;
    private final ModelPart pitch;

    public KineticTurretModel(ModelPart root) {
        this.base = root.getChild("base");
        this.yaw = this.base.getChild("Yaw");
        this.pitch = this.yaw.getChild("pitch");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition base = partdefinition.addOrReplaceChild("base",
                CubeListBuilder.create()
                        .texOffs(256, 0)
                        .addBox(-7.0F, -16.0F, -8.0F, 14.0F, 15.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(196, 264)
                        .addBox(7.0F, -16.0F, -8.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(200, 264)
                        .addBox(7.0F, -16.0F, 7.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(204, 264)
                        .addBox(-8.0F, -16.0F, -8.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(264, 201)
                        .addBox(-8.0F, -16.0F, 7.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(256, 52)
                        .addBox(3.0F, -1.0F, -7.0F, 5.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 264)
                        .addBox(7.0F, -16.0F, -7.0F, 1.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(78, 264)
                        .addBox(-8.0F, -16.0F, -7.0F, 1.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(256, 67)
                        .addBox(-8.0F, -1.0F, -7.0F, 5.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(160, 264)
                        .addBox(-3.0F, -1.0F, 3.0F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(264, 164)
                        .addBox(-3.0F, -1.0F, -7.0F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        base.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(208, 264)
                        .addBox(-7.0F, -14.0F, -8.0F, 1.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(212, 264)
                        .addBox(-7.0F, -14.0F, -23.0F, 1.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-7.0F, 6.0F, 15.0F, 0.0F, 0.0F, 1.5708F));

        PartDefinition yawPart = base.addOrReplaceChild("Yaw",
                CubeListBuilder.create()
                        .texOffs(256, 31)
                        .addBox(-8.0F, -21.0F, -8.0F, 16.0F, 5.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(256, 96)
                        .addBox(-10.0F, -42.0F, -3.0F, 2.0F, 26.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 264)
                        .addBox(8.0F, -42.0F, -3.0F, 2.0F, 26.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition pitchPart = yawPart.addOrReplaceChild("pitch",
                CubeListBuilder.create()
                        .texOffs(256, 82)
                        .addBox(-8.0F, 8.0F, -3.0F, 16.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(256, 89)
                        .addBox(-8.0F, -7.0F, -3.0F, 16.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(108, 264)
                        .addBox(7.0F, -6.0F, -3.0F, 1.0F, 14.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(122, 264)
                        .addBox(-8.0F, -6.0F, -3.0F, 1.0F, 14.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -42.0F, 0.0F));

        PartDefinition launchSilo = pitchPart.addOrReplaceChild("Launch_silo",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(5.0F, -5.0F, -31.0F, 2.0F, 6.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 68)
                        .addBox(-7.0F, -5.0F, -31.0F, 2.0F, 6.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 0)
                        .addBox(-7.0F, 3.0F, -31.0F, 2.0F, 6.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 136)
                        .addBox(-5.0F, 7.0F, -31.0F, 4.0F, 2.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(132, 136)
                        .addBox(1.0F, 7.0F, -31.0F, 4.0F, 2.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 200)
                        .addBox(1.0F, -5.0F, -31.0F, 4.0F, 2.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(132, 200)
                        .addBox(-5.0F, -5.0F, -31.0F, 4.0F, 2.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 68)
                        .addBox(5.0F, 3.0F, -31.0F, 2.0F, 6.0F, 62.0F, new CubeDeformation(0.0F))
                        .texOffs(264, 136)
                        .addBox(-5.0F, -5.0F, 31.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(188, 264)
                        .addBox(-7.0F, -5.0F, 31.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(136, 264)
                        .addBox(-5.0F, 7.0F, 31.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(264, 185)
                        .addBox(5.0F, -5.0F, 31.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition launchPlatform = launchSilo.addOrReplaceChild("Launch_plateform",
                CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, 21.0F));

        PartDefinition backPlate = launchPlatform.addOrReplaceChild("back_plate",
                CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        backPlate.addOrReplaceChild("droite",
                CubeListBuilder.create()
                        .texOffs(264, 152)
                        .addBox(0.0F, -6.0F, -2.0F, 5.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 0.0F, 12.0F));

        backPlate.addOrReplaceChild("guauche",
                CubeListBuilder.create()
                        .texOffs(264, 140)
                        .addBox(-5.0F, -6.0F, -2.0F, 5.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 0.0F, 12.0F));

        return LayerDefinition.create(meshdefinition, 512, 512);
    }

    /**
     * Renders the turret model with the given yaw and pitch angles.
     *
     * @param poseStack  the current pose stack
     * @param consumer   the vertex consumer for the render type
     * @param light      packed light
     * @param overlay    packed overlay
     * @param yawDeg     yaw angle in degrees
     * @param pitchDeg   pitch angle in degrees
     */
    public void render(PoseStack poseStack, VertexConsumer consumer,
                       int light, int overlay, float yawDeg, float pitchDeg) {
        yaw.yRot = (float) Math.toRadians(yawDeg);
        pitch.xRot = (float) Math.toRadians(pitchDeg);
        base.render(poseStack, consumer, light, overlay);
    }
}
