# Create: Advanced Ballistics

**Create: Advanced Ballistics** is a Minecraft mod (NeoForge 1.21.1) that brings an ultra-realistic and modular ballistics system to the **Create** ecosystem.

This project allows you to design, scan, and launch custom projectiles built block by block, simulating complex physics ranging from advanced aerodynamics to fuel management.

---

## Main Features

### 1. Modular Projectile System
Unlike standard entities, projectiles in this mod are assembled from real blocks in the world. Each block has its own physical properties:
- **Tails (Engines)**: Provide thrust, handle nozzle tilt, and consume fuel.
- **Fuel Tanks**: Store the fuel required for flight.
- **Payloads**: Determine the effect on impact (e.g., explosions via `ExplosiveBehavior`).
- **Optional Modules (Fins, Ballast, etc.)**: Influence stability, drag, and mass.

### 2. High-Precision Physics Simulation (V2 Protocol)
The mod uses a sophisticated physics engine that handles:
- **Force and Torque Integration**: Precise calculation of Center of Mass (CoM) and Center of Pressure (CoP).
- **Two-Point Aerodynamic Model**: Simulates lift and drag forces independently on the front and rear of the projectile.
- **3-Phase Simulation Pipeline**:
  1. **FORCE Phase**: Calculation of environmental (Gravity, Buoyancy, Spatial Attraction), aerodynamic, and propulsion forces.
  2. **TORQUE Phase**: Calculation of torques (Viscous Damping, Attraction Alignment) reacting to accumulated forces.
  3. **ACCUMULATION Phase**: Final vector summation for trajectory integration.

### 3. Attraction Systems & Spatial Gravity (`AttractionModule`)
A revolutionary system for simulating celestial bodies or gravitational wells:
- **Radial Forces**: Attraction (or repulsion) based on a customizable power law.
- **PD Orbital Controller**: Automatic maintenance of an orbital radius via a Proportional-Derivative (PD) controller.
- **Vortex & Spirals**: Tangential forces allowing for spiral orbits.
- **Automatic Alignment**: Torque orienting the projectile's "nose" towards the attraction source.

### 4. High-Speed Stability (Partial Ticks & Chunk Loading)
To prevent projectiles from phasing through blocks at high speeds, the system implements:
- **Partial Tick Simulation**: Splitting the trajectory into segments if speed exceeds a threshold. One segment is verified per server tick.
- **Dynamic Chunk Loading**: Forced loading of chunks along the trajectory to ensure collision detection, even at extreme distances.

### 5. Kinetic Turrets
Turrets integrated into Create's kinetic system:
- **Yaw/Pitch Rotation**: Controlled by the rotation speed of shafts.
- **Independent Mechanism**: Uses a gear ratio system for smooth and precise aiming.

### 6. CGS (Control Guidance System)
A standardized control interface for piloting projectiles:
- **Control Axes**: Pitch, Yaw, Roll, and Throttle.
- **Guidance API**: Ready for integration with guidance systems (AI or commands).

---

## Technical Details

### Analysis and Scanning (`ProjectileAnalyzer`)
The mod scans block structures in real-time to validate their geometry:
- **Golden Rule**: The engine (Tail) must be at the rear (index 0) and the warhead (Payload) at the front.
- **Strict Validation**: Detailed error reporting for invalid geometries.

### 7. Debug Tools & Telemetry
The mod includes powerful tools for testing and debugging:
- **BlackBox Telemetry**: Records every simulation tick (forces, torques, position, fuel). Accessible via `/ordnance blackbox`.
- **Attraction Commands**: Dynamic management of gravitational sources via `/ordnance attraction`.
- **Registry Inspector**: Real-time visualization of physical properties for every registered block via `/ordnance registry`.
- **Real-Time Analysis**: Detailed projectile scan report directly in-game.

---

## API for Developers
The mod is designed to be extensible:
- **ProjectileBehavior**: Create your own component behaviors (e.g., homing heads, cargo drops).
- **Physics Modules**: Add new forces to the universe (e.g., wind, magnetic fields).
- **NeoForge Events**: Hooks for projectile launch, tick, and death.

---

## Dependencies
- **Minecraft** 1.21.1
- **NeoForge**
- **Create** 0.6+

---

## Credits
Developed with passion to push the limits of mechanics in Minecraft. 🚀
