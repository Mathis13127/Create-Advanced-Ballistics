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
 * Debug placeholder block for the <b>propeller tail</b> variant.
 *
 * <p>A low-speed aquatic / aerial propulsion alternative to the standard
 * rocket tail. Provides weaker but more efficient thrust at low velocities.
 * Will be replaced by a cosmetic Blockbench model in a future release.</p>
 *
 * @see com.pixel.create_ordnance.registry.ProjectileComponentType#TAIL
 * @see com.pixel.create_ordnance.registry.ProjectileComponentRegistry
 */
public class DebugPropellerTailBlock extends DirectionalBlock implements IWrenchable {

    public static final MapCodec<DebugPropellerTailBlock> CODEC = simpleCodec(DebugPropellerTailBlock::new);

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    // Hitbox: [[0.25,0,0.25,0.75,1,0.75]] -> [4, 0, 4, 12, 16, 12]
    private static final double MIN = 4.0;
    private static final double MAX = 12.0;

    // Axis Y: [4, 0, 4] -> [12, 16, 12]
    public static final VoxelShape SHAPE_AXIS_Y = Stream.of(
            Block.box(MIN, 0.0, MIN, MAX, 16.0, MAX)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    // Axis Z: [4, 4, 0] -> [12, 12, 16]
    public static final VoxelShape SHAPE_AXIS_Z = Stream.of(
            Block.box(MIN, MIN, 0.0, MAX, MAX, 16.0)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    // Axis X: [0, 4, 4] -> [16, 12, 12]
    public static final VoxelShape SHAPE_AXIS_X = Stream.of(
            Block.box(0.0, MIN, MIN, 16.0, MAX, MAX)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public DebugPropellerTailBlock(Properties p_i48415_1_) {
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
            case UP, DOWN -> SHAPE_AXIS_Y;
            case NORTH, SOUTH -> SHAPE_AXIS_Z;
            case EAST, WEST -> SHAPE_AXIS_X;
        };
    }
}
