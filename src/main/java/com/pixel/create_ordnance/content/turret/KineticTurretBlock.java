package com.pixel.create_ordnance.content.turret;

import com.pixel.create_ordnance.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Kinetic Turret block — a single-block turret with independent yaw/pitch axes.
 *
 * <p>Kinetic connections:
 * <ul>
 *   <li>Pitch LEFT/RIGHT (horizontal, perpendicular to FACING) — kinetic pass-through</li>
 * </ul>
 *
 * <p>The yaw axis is NOT part of the kinetic network. Instead, the block entity
 * reads the speed of whatever shaft is placed below it (like a Mechanical Bearing).
 * This gives truly independent yaw and pitch control.</p>
 */
public class KineticTurretBlock extends KineticBlock implements IBE<KineticTurretBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

    public KineticTurretBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(INVERTED, false));
    }

    // --- IRotate ---

    @Override
    public Axis getRotationAxis(BlockState state) {
        // Pitch axis = perpendicular to FACING
        // FACING=NORTH → clockwise = EAST → axis X
        return state.getValue(FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        // Only pitch shafts connect to the kinetic network (pass-through)
        // Yaw (DOWN) is read directly from the block below — not connected
        return face.getAxis() == getRotationAxis(state);
    }

    // --- Placement ---

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean inverted = context.getClickedFace() == Direction.DOWN;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(INVERTED, inverted);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, INVERTED);
    }

    // --- Shape ---

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // --- IBE ---

    @Override
    public Class<KineticTurretBlockEntity> getBlockEntityClass() {
        return KineticTurretBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticTurretBlockEntity> getBlockEntityType() {
        return ModBlockEntities.KINETIC_TURRET.get();
    }
}
