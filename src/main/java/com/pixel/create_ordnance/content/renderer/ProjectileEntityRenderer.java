package com.pixel.create_ordnance.content.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.render.OrientedContraptionEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ProjectileEntityRenderer extends OrientedContraptionEntityRenderer {

    public ProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(OrientedContraptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double cameraX, double cameraY,
            double cameraZ) {
        if (entity instanceof ProjectileEntity prj && !prj.isReadyForRender()) {
            return false;
        }
        return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public void render(OrientedContraptionEntity entity, float entityYaw, float partialTicks,
            PoseStack ms, MultiBufferSource buffers,
            int light) {
        super.render(entity, entityYaw, partialTicks, ms, buffers, light);
    }
}