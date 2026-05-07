package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.Event;

/**
 * Fired after the {@link com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer
 * ProjectileAnalyzer} has computed a projectile's {@link ProjectileStats}.
 *
 * <p>Listeners can read and <b>modify</b> any stat before it is finalized.
 * This is the primary extension point for addon developers who want to tweak
 * physics parameters without replacing the analyzer itself.</p>
 *
 * <h3>Dual-point model</h3>
 * <p>Properties ending in {@code Front} refer to the <b>nose half</b> of the
 * projectile and those ending in {@code Back} refer to the <b>tail half</b>.
 * These paired values feed the dual-point force system (drag, lift, buoyancy)
 * that produces emergent torque around the center of mass.</p>
 *
 * <h3>Example — addon modifying stats</h3>
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener((ProjectileAnalyzeEvent e) -> {
 *     // Double the base thrust of every projectile
 *     e.setBaseThrust(e.getBaseThrust() * 2.0);
 * });
 * }</pre>
 *
 * @see ProjectileStats
 * @see ScrapResult
 */
public class ProjectileAnalyzeEvent extends Event {

    private final ScrapResult scrapResult;

    // ── Global stats ──────────────────────────────────────────
    private double totalMass;
    private Vec3 centerOfMass;
    private double totalStability;
    private Vec3 centerOfPressure;
    private double totalFuel;
    private double totalFuelConsumption;
    private ThrustType thrustType;
    private double baseThrust;
    private double maxNozzleTilt;
    private double optimalDensity;
    private double densityBandwidth;
    private double totalLateralArea;

    private double massFront;
    private double massBack;
    private double volumeFront;
    private double volumeBack;
    private double areaFrontalFront;
    private double areaFrontalBack;
    private double areaLateralFront;
    private double areaLateralBack;
    private Vec3 centerFront;
    private Vec3 centerBack;

    public ProjectileAnalyzeEvent(ScrapResult scrapResult, ProjectileStats stats) {
        this.scrapResult = scrapResult;

        // Initialize mutable fields from stats
        this.totalMass = stats.totalMass();
        this.centerOfMass = stats.centerOfMass();
        this.totalStability = stats.totalStability();
        this.centerOfPressure = stats.centerOfPressure();
        this.totalFuel = stats.totalFuel();
        this.totalFuelConsumption = stats.totalFuelConsumption();
        this.thrustType = stats.thrustType();
        this.baseThrust = stats.baseThrust();
        this.maxNozzleTilt = stats.maxNozzleTilt();
        this.optimalDensity = stats.optimalDensity();
        this.densityBandwidth = stats.densityBandwidth();
        this.totalLateralArea = stats.totalLateralArea();

        this.massFront = stats.massFront();
        this.massBack = stats.massBack();
        this.volumeFront = stats.volumeFront();
        this.volumeBack = stats.volumeBack();
        this.areaFrontalFront = stats.areaFrontalFront();
        this.areaFrontalBack = stats.areaFrontalBack();
        this.areaLateralFront = stats.areaLateralFront();
        this.areaLateralBack = stats.areaLateralBack();
        this.centerFront = stats.centerFront();
        this.centerBack = stats.centerBack();

    }

    /** Returns the raw scan result (1-D block list) that produced these stats. Read-only. */
    public ScrapResult getScrapResult() {
        return scrapResult;
    }

    /**
     * Builds the final {@link ProjectileStats} record from the (potentially modified)
     * fields of this event. Called internally by the analyzer after all listeners
     * have processed the event — addon code should <b>not</b> call this directly.
     *
     * @return an immutable snapshot of all current stat values.
     */
    public ProjectileStats createFinalStats() {
        return new ProjectileStats(
                totalMass, centerOfMass, totalStability, centerOfPressure,
                totalFuel, totalFuelConsumption, thrustType, baseThrust,
                maxNozzleTilt, 0, // estimatedTerminalVelocity re-calculated on re-analyze
                optimalDensity, densityBandwidth,
                massFront, massBack, volumeFront, volumeBack,
                areaFrontalFront, areaFrontalBack, areaLateralFront, areaLateralBack,
                centerFront, centerBack,
                totalLateralArea);
    }

    // ── Global properties ────────────────────────────────────────
    // totalMass, centerOfMass, totalStability, centerOfPressure,
    // totalFuel, totalFuelConsumption, totalLateralArea

    /** Total dry mass of the projectile (kg). */
    public double getTotalMass() {
        return totalMass;
    }

    /** @param totalMass new total dry mass (kg). */
    public void setTotalMass(double totalMass) {
        this.totalMass = totalMass;
    }

    /** World-space center of mass (pivot point for all torque calculations). */
    public Vec3 getCenterOfMass() {
        return centerOfMass;
    }

    public void setCenterOfMass(Vec3 centerOfMass) {
        this.centerOfMass = centerOfMass;
    }

    /** Aerodynamic stability factor — higher values shift CoP further back. */
    public double getTotalStability() {
        return totalStability;
    }

    public void setTotalStability(double totalStability) {
        this.totalStability = totalStability;
    }

    /** World-space center of pressure (aerodynamic force application point). */
    public Vec3 getCenterOfPressure() {
        return centerOfPressure;
    }

    public void setCenterOfPressure(Vec3 centerOfPressure) {
        this.centerOfPressure = centerOfPressure;
    }

    /** Total fuel capacity (arbitrary units, consumed at {@link #getTotalFuelConsumption()} per tick). */
    public double getTotalFuel() {
        return totalFuel;
    }

    public void setTotalFuel(double totalFuel) {
        this.totalFuel = totalFuel;
    }

    /** Fuel consumed per tick while thrust is active. */
    public double getTotalFuelConsumption() {
        return totalFuelConsumption;
    }

    public void setTotalFuelConsumption(double totalFuelConsumption) {
        this.totalFuelConsumption = totalFuelConsumption;
    }

    // ── Propulsion properties ──────────────────────────────────
    // thrustType, baseThrust, maxNozzleTilt, optimalDensity, densityBandwidth

    /** Engine type — determines thrust curve and Lorentz factor behavior. */
    public ThrustType getThrustType() {
        return thrustType;
    }

    public void setThrustType(ThrustType thrustType) {
        this.thrustType = thrustType;
    }

    /** Base thrust force (Newtons) before Lorentz and fuel scaling. */
    public double getBaseThrust() {
        return baseThrust;
    }

    public void setBaseThrust(double baseThrust) {
        this.baseThrust = baseThrust;
    }

    /** Maximum nozzle tilt angle (radians) for vectorial thrust steering. */
    public double getMaxNozzleTilt() {
        return maxNozzleTilt;
    }

    public void setMaxNozzleTilt(double maxNozzleTilt) {
        this.maxNozzleTilt = maxNozzleTilt;
    }

    /** Optimal medium density for buoyancy equilibrium (kg/m³). */
    public double getOptimalDensity() {
        return optimalDensity;
    }

    public void setOptimalDensity(double optimalDensity) {
        this.optimalDensity = optimalDensity;
    }

    /** Density tolerance band around {@link #getOptimalDensity()} for smooth buoyancy transition. */
    public double getDensityBandwidth() {
        return densityBandwidth;
    }

    public void setDensityBandwidth(double densityBandwidth) {
        this.densityBandwidth = densityBandwidth;
    }

    /** Total lateral cross-section area (m²) — affects side-drag and lift. */
    public double getTotalLateralArea() {
        return totalLateralArea;
    }

    public void setTotalLateralArea(double totalLateralArea) {
        this.totalLateralArea = totalLateralArea;
    }

    // ── Dual-point properties (front = nose, back = tail) ──────
    // Paired values feeding the dual-point force model.
    // Modifying front/back ratios changes the emergent torque profile.

    /** Mass of the nose half (kg). */
    public double getMassFront() {
        return massFront;
    }

    public void setMassFront(double massFront) {
        this.massFront = massFront;
    }

    /** Mass of the tail half (kg). */
    public double getMassBack() {
        return massBack;
    }

    public void setMassBack(double massBack) {
        this.massBack = massBack;
    }

    /** Volume of the nose half (m³) — affects buoyancy force distribution. */
    public double getVolumeFront() {
        return volumeFront;
    }

    public void setVolumeFront(double volumeFront) {
        this.volumeFront = volumeFront;
    }

    /** Volume of the tail half (m³). */
    public double getVolumeBack() {
        return volumeBack;
    }

    public void setVolumeBack(double volumeBack) {
        this.volumeBack = volumeBack;
    }

    /** Frontal cross-section area of the nose half (m²) — affects head-on drag. */
    public double getAreaFrontalFront() {
        return areaFrontalFront;
    }

    public void setAreaFrontalFront(double areaFrontalFront) {
        this.areaFrontalFront = areaFrontalFront;
    }

    /** Frontal cross-section area of the tail half (m²). */
    public double getAreaFrontalBack() {
        return areaFrontalBack;
    }

    public void setAreaFrontalBack(double areaFrontalBack) {
        this.areaFrontalBack = areaFrontalBack;
    }

    /** Lateral cross-section area of the nose half (m²) — affects side-drag and lift. */
    public double getAreaLateralFront() {
        return areaLateralFront;
    }

    public void setAreaLateralFront(double areaLateralFront) {
        this.areaLateralFront = areaLateralFront;
    }

    /** Lateral cross-section area of the tail half (m²). */
    public double getAreaLateralBack() {
        return areaLateralBack;
    }

    public void setAreaLateralBack(double areaLateralBack) {
        this.areaLateralBack = areaLateralBack;
    }

    /** Geometric center of the nose half (world-space) — force application point for front. */
    public Vec3 getCenterFront() {
        return centerFront;
    }

    public void setCenterFront(Vec3 centerFront) {
        this.centerFront = centerFront;
    }

    /** Geometric center of the tail half (world-space) — force application point for back. */
    public Vec3 getCenterBack() {
        return centerBack;
    }

    public void setCenterBack(Vec3 centerBack) {
        this.centerBack = centerBack;
    }

}