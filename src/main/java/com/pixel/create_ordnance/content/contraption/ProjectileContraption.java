package com.pixel.create_ordnance.content.contraption;

import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.registry.ModContraptionTypes;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ProjectileContraption extends Contraption {

    private Vec3 pivotOffset = Vec3.ZERO;

    @Override
    public ContraptionType getType() {
        return ModContraptionTypes.PROJECTILE.get();
    }

    @Override
    public boolean assemble(Level level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canBeStabilized(Direction direction, BlockPos pos) {
        return true;
    }

    /**
     * V3.5 Assembly logic.
     * Reconstructs the visual structure from a list of IDs.
     * All projectiles are assembled vertically (Y axis) by default.
     * Index 0 is at (0,0,0).
     */
    public void assembleFromIds(ScrapResult result, ProjectileStats stats) {
        this.blocks.clear();
        this.pivotOffset = stats.centerOfMass();

        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = result.blockIds().size() - 1, maxZ = 0;

        for (int y = 0; y < result.blockIds().size(); y++) {
            Block block = BuiltInRegistries.BLOCK.get(result.blockIds().get(y));
            BlockState state = block.defaultBlockState();

            // Heuristic: If it has FACING, it's likely a Tail or Payload.
            // In linear assembly (UP-facing missile), these should point DOWN (exhaust/nose
            // logic check?)
            // Actually, for a missile looking UP, Tails point DOWN.
            if (state.hasProperty(BlockStateProperties.FACING)) {
                state = state.setValue(BlockStateProperties.FACING, Direction.UP);
            }
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
            }

            BlockPos pos = new BlockPos(0, y, 0);
            this.blocks.put(pos, new StructureBlockInfo(pos, state, null));
        }

        this.anchor = BlockPos.ZERO;
        double margin = 0.2;
        this.bounds = new AABB(minX + margin, minY, minZ + margin, maxX + 1 - margin, maxY + 1, maxZ + 1 - margin);
    }

    public Vec3 getPivotOffset() {
        return pivotOffset;
    }

    @Override
    public CompoundTag writeNBT(HolderLookup.Provider registries, boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(registries, spawnPacket);
        tag.putDouble("PivotOffsetX", pivotOffset.x);
        tag.putDouble("PivotOffsetY", pivotOffset.y);
        tag.putDouble("PivotOffsetZ", pivotOffset.z);
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
        super.readNBT(world, nbt, spawnData);
        if (nbt.contains("PivotOffsetX")) {
            this.pivotOffset = new Vec3(
                    nbt.getDouble("PivotOffsetX"),
                    nbt.getDouble("PivotOffsetY"),
                    nbt.getDouble("PivotOffsetZ"));
        }
    }
}