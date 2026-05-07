package com.pixel.create_ordnance.foundation.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class StateHelper {

    public static boolean isOnAxis(BlockState state, Direction.Axis axis) {
        if (state.hasProperty(BlockStateProperties.AXIS))
            return state.getValue(BlockStateProperties.AXIS) == axis;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            if (axis.isVertical())
                return false;
            return state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == axis;
        }
        if (state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING).getAxis() == axis;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == axis;
        return true; // Default to true if no orientation property (safe fallback)
    }

    public static Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return null;
    }

    public static Direction getDirectionFromRotation(float pitch, float yaw) {
        if (pitch <= -90)
            return Direction.UP;
        if (pitch >= 90)
            return Direction.DOWN;
        return Direction.fromYRot(yaw);
    }

    public static BlockPos rotatePosToUp(BlockPos pos, Direction forward) {
        if (forward == Direction.UP)
            return pos;
        return switch (forward) {
            case DOWN -> new BlockPos(pos.getX(), -pos.getY(), -pos.getZ());
            case NORTH -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case SOUTH -> new BlockPos(pos.getX(), pos.getZ(), -pos.getY());
            case WEST -> new BlockPos(pos.getY(), -pos.getX(), pos.getZ());
            case EAST -> new BlockPos(-pos.getY(), pos.getX(), pos.getZ());
            default -> pos;
        };
    }

    public static BlockPos rotatePosFromUp(BlockPos pos, Direction forward) {
        if (forward == Direction.UP)
            return pos;
        return switch (forward) {
            case DOWN -> new BlockPos(pos.getX(), -pos.getY(), -pos.getZ());
            case NORTH -> new BlockPos(pos.getX(), pos.getZ(), -pos.getY());
            case SOUTH -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case WEST -> new BlockPos(-pos.getY(), pos.getX(), pos.getZ());
            case EAST -> new BlockPos(pos.getY(), -pos.getX(), pos.getZ());
            default -> pos;
        };
    }

    public static Vec3 rotateVecFromUp(Vec3 vec, Direction forward) {
        if (forward == Direction.UP)
            return vec;
        return switch (forward) {
            case DOWN -> new Vec3(vec.x, -vec.y, -vec.z);
            case NORTH -> new Vec3(vec.x, vec.z, -vec.y);
            case SOUTH -> new Vec3(vec.x, -vec.z, vec.y);
            case WEST -> new Vec3(-vec.y, vec.x, vec.z);
            case EAST -> new Vec3(vec.y, -vec.x, vec.z);
            default -> vec;
        };
    }
}