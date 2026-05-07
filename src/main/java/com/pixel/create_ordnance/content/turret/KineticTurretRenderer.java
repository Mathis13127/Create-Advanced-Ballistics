package com.pixel.create_ordnance.content.turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import com.pixel.create_ordnance.CreateOrdnance;

/**
 * Renderer for the Kinetic Turret.
 *
 * <p>Renders:
 * <ul>
 *   <li>2 pitch {@code SHAFT_HALF} PartialModels — kinetic network speed</li>
 *   <li>1 yaw {@code SHAFT_HALF} PartialModel — independent speed from below</li>
 *   <li>The Java model with yaw/pitch rotations</li>
 * </ul>
 */
public class KineticTurretRenderer extends KineticBlockEntityRenderer<KineticTurretBlockEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateOrdnance.MODID, "textures/block/kinetic_turret_texture.png");

    private final KineticTurretModel model;
    private final RenderType renderType;

    public KineticTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new KineticTurretModel(context.bakeLayer(KineticTurretModel.LAYER));
        this.renderType = RenderType.entityCutoutNoCull(TEXTURE);
    }

    @Override
    protected void renderSafe(KineticTurretBlockEntity be, float partialTicks,
                              PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        renderShafts(be, ms, buffer, light);
        renderTurretModel(be, partialTicks, ms, buffer, light, overlay);
    }

    /**
     * Renders the 3 shaft PartialModels with independent speeds.
     * Pitch shafts use the kinetic network speed, yaw shaft uses the speed from below.
     */
    private void renderShafts(KineticTurretBlockEntity be, PoseStack ms,
                              MultiBufferSource buffer, int light) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(KineticTurretBlock.FACING);
        Direction.Axis pitchAxis = facing.getClockWise().getAxis();
        BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());

        // --- Pitch shafts (driven by kinetic network) ---
        for (Direction direction : Iterate.directions) {
            if (direction.getAxis() != pitchAxis)
                continue;

            SuperByteBuffer shaft = CachedBuffers.partialFacing(
                    AllPartialModels.SHAFT_HALF, state, direction);

            float offset = getRotationOffsetForPosition(be, pos, pitchAxis);
            float angle = (time * be.getSpeed() * 3f / 10) % 360;
            angle += offset;
            angle = angle / 180f * (float) Math.PI;

            kineticRotationTransform(shaft, be, pitchAxis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }

        // --- Yaw shaft (independent — speed read from block below/above) ---
        boolean inverted = state.getValue(KineticTurretBlock.INVERTED);
        Direction yawDir = inverted ? Direction.UP : Direction.DOWN;
        float yawSpeed = be.getYawSpeedFromBelow();
        SuperByteBuffer yawShaft = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF, state, yawDir);

        float yawOffset = getRotationOffsetForPosition(be, pos, Direction.Axis.Y);
        float yawAngle = (time * yawSpeed * 3f / 10) % 360;
        yawAngle += yawOffset;
        yawAngle = yawAngle / 180f * (float) Math.PI;

        kineticRotationTransform(yawShaft, be, Direction.Axis.Y, yawAngle, light);
        yawShaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    /**
     * Renders the turret Java model with yaw/pitch rotations.
     */
    private void renderTurretModel(KineticTurretBlockEntity be, float partialTicks,
                                   PoseStack ms, MultiBufferSource buffer,
                                   int light, int overlay) {
        ms.pushPose();

        boolean inverted = be.getBlockState().getValue(KineticTurretBlock.INVERTED);
        Direction facing = be.getBlockState().getValue(KineticTurretBlock.FACING);

        if (inverted) {
            // Inverted: base at top, model hangs upward
            ms.translate(0.5, -0.5, 0.5);
            ms.mulPose(Axis.ZP.rotationDegrees(180));
            ms.mulPose(Axis.XP.rotationDegrees(180));
        } else {
            // Normal: base at bottom, model hangs downward
            ms.translate(0.5, 1.5, 0.5);
            ms.mulPose(Axis.ZP.rotationDegrees(180));
        }

        // Rotate the entire model according to block FACING
        ms.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));

        // Get interpolated angles
        float yaw = be.getInterpolatedYaw(partialTicks);
        float pitch = be.getInterpolatedPitch(partialTicks);

        // Render the model with rotations applied internally
        VertexConsumer consumer = buffer.getBuffer(renderType);
        model.render(ms, consumer, light, overlay, yaw, pitch);

        ms.popPose();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(KineticTurretBlockEntity be, BlockState state) {
        return null;
    }

    @Override
    public boolean shouldRenderOffScreen(KineticTurretBlockEntity be) {
        return true;
    }
}
