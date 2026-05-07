package com.pixel.create_ordnance.content.turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pixel.create_ordnance.CreateOrdnance;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KineticTurretItemRenderer extends CustomRenderedItemModelRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateOrdnance.MODID, "textures/block/kinetic_turret_texture.png");

    private KineticTurretModel turretModel;

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model,
                          PartialItemModelRenderer renderer,
                          ItemDisplayContext transformType,
                          PoseStack ms, MultiBufferSource buffer,
                          int light, int overlay) {
        if (turretModel == null) {
            turretModel = new KineticTurretModel(
                    Minecraft.getInstance().getEntityModels()
                            .bakeLayer(KineticTurretModel.LAYER));
        }

        // Base class already translated to (0.5, 0.5, 0.5) = block center
        ms.translate(0, -0.725, 0);
        ms.scale(0.4f, 0.4f, 0.4f);

        // Render shafts (handles its own coordinate space internally)
        renderShafts(ms, renderer, light);

        // Turret model: entity transform from block center
        ms.translate(0, 1.5, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(180));

        RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        turretModel.render(ms, consumer, light, overlay, 0, 0);
    }

    private void renderShafts(PoseStack ms, PartialItemModelRenderer renderer, int light) {
        BakedModel shaftModel = AllPartialModels.SHAFT_HALF.get();

        // Move to block origin so rotate-around-center works correctly
        ms.pushPose();
        ms.translate(-0.5, -0.5, -0.5);

        // Pitch shaft - East
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.ZP.rotationDegrees(-90));
        ms.translate(-0.5, -0.5, -0.5);
        renderer.render(shaftModel, light);
        ms.popPose();

        // Pitch shaft - West
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.ZP.rotationDegrees(90));
        ms.translate(-0.5, -0.5, -0.5);
        renderer.render(shaftModel, light);
        ms.popPose();

        // Yaw shaft - Down
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.XP.rotationDegrees(180));
        ms.translate(-0.5, -0.5, -0.5);
        renderer.render(shaftModel, light);
        ms.popPose();

        ms.popPose();
    }
}
