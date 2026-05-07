package com.pixel.create_ordnance.content.entity;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;

public class ProjectileDataIO {

    public static void writeAdditional(ProjectileEntity e, CompoundTag compound) {
        CompoundTag pd = new CompoundTag();

        // 1. Core ID Structure (NEW V3.5 Protocol)
        ScrapResult result = e.getScrapResult();
        if (result != null) {
            pd.put("ScrapResult", result.writeToNBT());
        }
        pd.putLong("LastConfigVersion", e.lastConfigVersion);

        // 2. Physical Properties (Snapshot for redundancy or client-side load)
        pd.putInt("Length", e.projectileLength);
        pd.putInt("TotalBlockCount", e.totalBlockCount);
        pd.putFloat("Scale", e.projectileScale);
        pd.putDouble("TotalMass", e.totalMass);
        pd.putDouble("TotalStability", e.totalStability);
        pd.putDouble("MaxFuel", e.maxFuel);
        pd.putDouble("FuelConsumption", e.fuelConsumption);
        pd.putString("ThrustType", e.thrustType.name());

        // 3. Thrust Parameters
        pd.putDouble("BaseThrust", e.baseThrust);
        pd.putDouble("MaxNozzleTilt", e.maxNozzleTilt);
        pd.putDouble("OptimalDensity", e.optimalDensity);
        pd.putDouble("DensityBandwidth", e.densityBandwidth);

        pd.putDouble("TotalLateralArea", e.totalLateralArea);

        // Dual-Point Stats
        pd.putDouble("MassFront", e.massFront);
        pd.putDouble("MassBack", e.massBack);
        pd.putDouble("VolumeFront", e.volumeFront);
        pd.putDouble("VolumeBack", e.volumeBack);
        pd.putDouble("AreaFrontalFront", e.areaFrontalFront);
        pd.putDouble("AreaFrontalBack", e.areaFrontalBack);
        pd.putDouble("AreaLateralFront", e.areaLateralFront);
        pd.putDouble("AreaLateralBack", e.areaLateralBack);
        pd.put("CenterFront", vec3ToNbt(e.centerFront));
        pd.put("CenterBack", vec3ToNbt(e.centerBack));

        // Runtime State
        pd.putDouble("CurrentFuel", e.currentFuel);
        pd.putBoolean("IsExtinguished", e.isExtinguished());
        pd.putDouble("ResidualViscosity", e.getResidualViscosity());
        pd.putInt("TotalChunksLoaded", e.getTotalChunksLoadedCount());

        // 4. Guidance State (Standardized Interface for addons/external)
        CompoundTag guidance = new CompoundTag();
        guidance.putFloat("PitchDemand", e.getPitchDemand());
        guidance.putFloat("YawDemand", e.getYawDemand());
        guidance.putFloat("RollDemand", e.getRollDemand());
        guidance.putFloat("Throttle", e.getThrottle());
        guidance.putDouble("MaxNozzleTilt", e.maxNozzleTilt);
        pd.put("Guidance", guidance);

        // Transforms
        pd.putFloat("QuatW", e.orientation.w);
        pd.putFloat("QuatX", e.orientation.x);
        pd.putFloat("QuatY", e.orientation.y);
        pd.putFloat("QuatZ", e.orientation.z);

        pd.putFloat("AngVelX", e.angularVelocity.x);
        pd.putFloat("AngVelY", e.angularVelocity.y);
        pd.putFloat("AngVelZ", e.angularVelocity.z);

        pd.put("ScanPos", NbtUtils.writeBlockPos(e.scanPos));
        pd.put("CenterOfMass", vec3ToNbt(e.centerOfMass));
        pd.put("CenterOfPressure", vec3ToNbt(e.centerOfPressure));

        // Motion Persistence
        pd.put("Velocity", vec3ToNbt(e.getDeltaMovement()));

        // Compat: Vista TV Link
        if (e.linkedTvPos != null) {
            pd.put("LinkedTV", NbtUtils.writeBlockPos(e.linkedTvPos));
        }

        compound.put("ProjectileData", pd);
    }

    public static void readAdditional(ProjectileEntity e, CompoundTag compound) {
        if (!compound.contains("ProjectileData"))
            return;
        CompoundTag pd = compound.getCompound("ProjectileData");

        // 1. Core ID Structure
        if (pd.contains("ScrapResult")) {
            e.setScrapResult(ScrapResult.readFromNBT(pd.getCompound("ScrapResult")));
        }
        e.lastConfigVersion = pd.getLong("LastConfigVersion");

        // 2. Static fields (Snapshot)
        e.projectileLength = pd.getInt("Length");
        e.totalBlockCount = pd.contains("TotalBlockCount") ? pd.getInt("TotalBlockCount") : 1;
        e.projectileScale = pd.getFloat("Scale");
        e.totalMass = pd.getDouble("TotalMass");
        e.totalStability = pd.getDouble("TotalStability");
        e.maxFuel = pd.getDouble("MaxFuel");
        e.fuelConsumption = pd.getDouble("FuelConsumption");
        if (pd.contains("ThrustType")) {
            e.thrustType = ThrustType
                    .valueOf(pd.getString("ThrustType"));
        }

        // 3. Thrust Params
        e.baseThrust = pd.getDouble("BaseThrust");
        e.maxNozzleTilt = pd.getDouble("MaxNozzleTilt");
        e.optimalDensity = pd.getDouble("OptimalDensity");
        e.densityBandwidth = pd.getDouble("DensityBandwidth");
        e.totalLateralArea = pd.contains("TotalLateralArea") ? pd.getDouble("TotalLateralArea") : 1.0;

        // Unified Dual-Point Stats (V5.0)
        e.massFront = pd.getDouble("MassFront");
        e.massBack = pd.getDouble("MassBack");
        e.volumeFront = pd.getDouble("VolumeFront");
        e.volumeBack = pd.getDouble("VolumeBack");
        e.areaFrontalFront = pd.getDouble("AreaFrontalFront");
        e.areaFrontalBack = pd.getDouble("AreaFrontalBack");
        e.areaLateralFront = pd.getDouble("AreaLateralFront");
        e.areaLateralBack = pd.getDouble("AreaLateralBack");
        if (pd.contains("CenterFront"))
            e.centerFront = nbtToVec3(pd.getCompound("CenterFront"));
        if (pd.contains("CenterBack"))
            e.centerBack = nbtToVec3(pd.getCompound("CenterBack"));

        // 5. Runtime data
        e.currentFuel = pd.getDouble("CurrentFuel");
        e.setExtinguished(pd.getBoolean("IsExtinguished"));
        e.setResidualViscosity(pd.contains("ResidualViscosity") ? pd.getDouble("ResidualViscosity") : 0.02);
        if (pd.contains("TotalChunksLoaded")) {
            e.totalChunksLoadedCount = pd.getInt("TotalChunksLoaded");
        }

        // 4. Guidance State
        if (pd.contains("Guidance")) {
            CompoundTag guidance = pd.getCompound("Guidance");
            e.setPitchDemand(guidance.getFloat("PitchDemand"));
            e.setYawDemand(guidance.getFloat("YawDemand"));
            e.setRollDemand(guidance.getFloat("RollDemand"));
            if (guidance.contains("Throttle")) {
                e.setThrottle(guidance.getFloat("Throttle"));
            }
        }

        // 6. Transforms
        if (pd.contains("QuatW")) {
            e.orientation.set(pd.getFloat("QuatX"), pd.getFloat("QuatY"), pd.getFloat("QuatZ"), pd.getFloat("QuatW"));
        }
        e.prevOrientation.set(e.orientation);
        if (pd.contains("AngVelX")) {
            e.angularVelocity.set(pd.getFloat("AngVelX"), pd.getFloat("AngVelY"), pd.getFloat("AngVelZ"));
        }

        e.scanPos = NbtUtils.readBlockPos(pd, "ScanPos").orElse(BlockPos.ZERO);
        if (pd.contains("CenterOfMass"))
            e.centerOfMass = nbtToVec3(pd.getCompound("CenterOfMass"));
        if (pd.contains("CenterOfPressure"))
            e.centerOfPressure = nbtToVec3(pd.getCompound("CenterOfPressure"));

        // 7. Motion Load
        if (pd.contains("Velocity")) {
            e.setDeltaMovement(nbtToVec3(pd.getCompound("Velocity")));
        }

        // 8. Compat: Vista TV Link
        e.linkedTvPos = NbtUtils.readBlockPos(pd, "LinkedTV").orElse(null);
    }

    private static CompoundTag quatToNbt(Quaternionf q) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("x", q.x);
        tag.putFloat("y", q.y);
        tag.putFloat("z", q.z);
        tag.putFloat("w", q.w);
        return tag;
    }

    private static Quaternionf nbtToQuat(CompoundTag tag) {
        if (tag == null || !tag.contains("w"))
            return new Quaternionf();
        return new Quaternionf(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"), tag.getFloat("w"));
    }

    private static CompoundTag vec3ToNbt(Vec3 vec) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", vec.x);
        tag.putDouble("Y", vec.y);
        tag.putDouble("Z", vec.z);
        return tag;
    }

    private static Vec3 nbtToVec3(CompoundTag tag) {
        if (tag == null || !tag.contains("X"))
            return Vec3.ZERO;
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }
}