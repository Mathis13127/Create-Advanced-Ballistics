package com.pixel.create_ordnance.content.blocks;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.common.property.Properties;

/**
 * Debug placeholder block for the <b>FUEL_TANK</b> component.
 *
 * <p>Stores fuel capacity that extends burn time and total impulse of the projectile.
 * Will be replaced by a cosmetic Blockbench model in a future release.</p>
 *
 * @see com.pixel.create_ordnance.registry.ProjectileComponentType#FUEL_TANK
 * @see com.pixel.create_ordnance.registry.ProjectileComponentRegistry
 */
public class DebugFuelTankBlock extends DirectionalBlock implements IWrenchable {

    public static final MapCodec<DebugFuelTankBlock> CODEC = simpleCodec(DebugFuelTankBlock::new);

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    private static final double MIN_VOXEL = 4.0;
    private static final double MAX_VOXEL = 12.0;
    private static final double MIN_COORD = 0.0;
    private static final double MAX_COORD = 16.0;

    public static final VoxelShape SHAPE_NORTH = Stream.of(
            Block.box(MIN_VOXEL, MIN_VOXEL, MIN_COORD, MAX_VOXEL, MAX_VOXEL, MAX_COORD)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public static final VoxelShape SHAPE_SOUTH = Stream.of(
            Block.box(MIN_VOXEL, MIN_VOXEL, MIN_COORD, MAX_VOXEL, MAX_VOXEL, MAX_COORD)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public static final VoxelShape SHAPE_EAST = Stream.of(
            Block.box(MIN_COORD, MIN_VOXEL, MIN_VOXEL, MAX_COORD, MAX_VOXEL, MAX_VOXEL)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public static final VoxelShape SHAPE_WEST = Stream.of(
            Block.box(MIN_COORD, MIN_VOXEL, MIN_VOXEL, MAX_COORD, MAX_VOXEL, MAX_VOXEL)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public static final VoxelShape SHAPE_AXIS_Y = Stream.of(
            Block.box(MIN_VOXEL, MIN_COORD, MIN_VOXEL, MAX_VOXEL, MAX_COORD, MAX_VOXEL)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public DebugFuelTankBlock(Properties p_i48415_1_) {
        super(p_i48415_1_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP, DOWN -> SHAPE_AXIS_Y;
        };
    }
}