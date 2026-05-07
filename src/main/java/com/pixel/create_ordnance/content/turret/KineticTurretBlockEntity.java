package com.pixel.create_ordnance.content.turret;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Block entity for the Kinetic Turret.
 *
 * <p>Two independent rotation axes:
 * <ul>
 *   <li><b>Pitch</b> — driven by the kinetic network (horizontal shaft pass-through)</li>
 *   <li><b>Yaw</b> — reads the speed of the shaft block below (not connected kinetically)</li>
 * </ul>
 *
 * <p>This is similar to how a Mechanical Bearing reads shaft speed without the
 * bearing plate being part of the kinetic network.</p>
 */
public class KineticTurretBlockEntity extends KineticBlockEntity {

    private float yawAngle;
    private float prevYawAngle;
    private float pitchAngle;
    private float prevPitchAngle;

    public static final float MIN_PITCH = -42.5f;
    public static final float MAX_PITCH = 42.5f;
    private static final float GEAR_RATIO = 12f;

    public KineticTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        prevYawAngle = yawAngle;
        prevPitchAngle = pitchAngle;

        // Pitch: driven by this block's kinetic network (horizontal shafts)
        float pitchSpeed = getSpeed();
        if (pitchSpeed != 0) {
            float angular = convertToAngular(pitchSpeed) / GEAR_RATIO;
            pitchAngle = Mth.clamp(pitchAngle + angular, MIN_PITCH, MAX_PITCH);
        }

        // Yaw: driven by the shaft below (independent, not in our kinetic network)
        float yawSpeed = getYawSpeedFromBelow();
        if (yawSpeed != 0) {
            float angular = convertToAngular(yawSpeed) / GEAR_RATIO;
            yawAngle = (yawAngle + angular) % 360;
            if (yawAngle < 0)
                yawAngle += 360;
        }
    }

    public boolean isInverted() {
        return getBlockState().getValue(KineticTurretBlock.INVERTED);
    }

    /**
     * Reads the kinetic speed of the shaft providing yaw rotation.
     * Normal: reads from below. Inverted: reads from above.
     */
    public float getYawSpeedFromBelow() {
        if (level == null) return 0;
        boolean inverted = isInverted();
        BlockPos target = inverted ? getBlockPos().above() : getBlockPos().below();
        Direction shaftDir = inverted ? Direction.DOWN : Direction.UP;
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof KineticBlockEntity kbe) {
            BlockState targetState = level.getBlockState(target);
            Block targetBlock = targetState.getBlock();
            if (targetBlock instanceof IRotate rotatable) {
                if (rotatable.hasShaftTowards(level, target, targetState, shaftDir)) {
                    return kbe.getSpeed();
                }
            }
        }
        return 0;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX() - 2.5, pos.getY() - 2.5, pos.getZ() - 2.5,
                        pos.getX() + 3.5, pos.getY() + 3.5, pos.getZ() + 3.5);
    }

    // --- Interpolation for smooth client rendering ---

    public float getInterpolatedYaw(float partialTicks) {
        float diff = yawAngle - prevYawAngle;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return prevYawAngle + diff * partialTicks;
    }

    public float getInterpolatedPitch(float partialTicks) {
        return Mth.lerp(partialTicks, prevPitchAngle, pitchAngle);
    }

    public float getYawAngle() {
        return yawAngle;
    }

    public float getPitchAngle() {
        return pitchAngle;
    }

    // --- NBT persistence ---

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("YawAngle", yawAngle);
        tag.putFloat("PitchAngle", pitchAngle);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        yawAngle = tag.getFloat("YawAngle");
        pitchAngle = tag.getFloat("PitchAngle");
        prevYawAngle = yawAngle;
        prevPitchAngle = pitchAngle;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
