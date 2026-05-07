# 📦 Projectile Component Registry

> **Updated for Logic V2 (Ref: PCL-REG-002)**

The `ProjectileComponentRegistry` is the central database for all blocks that can be part of a projectile.
All components **MUST** be registered here to be recognized by the Scrapper and Physics Engine.

---

## 🚀 How to Register a Component

Registration uses a fluent **Builder Pattern** for clean and readable code.

### 1. Basic Structure (Standard Block)

```java
// In your ModBlocks or Registration class
ProjectileComponentRegistry.entry(MY_BLOCK.get())
    .asTail() // or .asFuelTank(), .asPayload(), .asOptionalModule()
    .setWeight(() -> 50.0) // Supplier<Double> for config reloading
    .register();
```

### 2. Available Component Types

#### 🍑 Tail (`.asTail()`)
The engine/propulsion unit. Must be at the back.
- `setBaseThrust(Supplier<Double>)`: Constant thrust.
- `setBoostThrust(Supplier<Double>)`: Initial high thrust.
- `setBoostDuration(Supplier<Double>)`: Duration of boost in ticks.
- `setMaxSpeed(Supplier<Double>)`: Terminal velocity cap.
- `setThrustType(Supplier<ThrustType>)`: `AFTERBURNER`, `PROPELLER`, or `NONE`.
- ... and more physics stats (drag, lift, fuel consumption).

#### ⛽ Fuel Tank (`.asFuelTank()`)
Provides fuel for the Tail.
- `setCapacity(Supplier<Double>)`: Fuel amount.

#### 📦 Payload (`.asPayload()`)
The warhead or function carrier. Must be at the front.
- *No specific physics stats, but can trigger events.*

#### 🧩 Optional Module (`.asOptionalModule()`)
Any mid-body component (guidance, stabilizer, etc).
- *Generic usage.*

---

## ⚡ Event System (Dynamic Logic)

Components can trigger logic via **Reflection-based Event Bindings**.
You provide the **fully qualified path** to a static method as a string.

### Binding Events
```java
// Inside the builder chain
.eventTrigger("com.mypackage.MyLogic.explode")
.eventSetup("com.mypackage.MyLogic.armFuze")
.eventTick("com.mypackage.MyLogic.emitSmoke")
```

### Writing Logic Methods (Nested Class Structure)
Since V2.1, logic methods are best organized in **static nested classes**. This ensures strict parameter validation operates only on the relevant parameters.

```java
public class ProjectileLogic {
    // Universal Context (Managed automatically)
    public static Level level;
    public static Entity projectile;
    // ...

    // NESTED CLASS: Groups parameters for a specific function
    public static class Explosive {
        // Only this parameter is required for this logic
        public static double explosion_power;

        public static void payloadExplosion() {
            // Access outer context via ProjectileLogic.level
            if (ProjectileLogic.level != null) {
                 // ... boom
            }
        }
    }
}
```

In your Registry:
```java
.eventTrigger("com.package.ProjectileLogic$Explosive::payloadExplosion")
.addParam("explosion_power", ...);
```
```

### Validating Logic
The Registry automatically validates that:
1.  The method exists.
2.  The signature is correct (Arguments match).
3.  The class is accessible.
*If validation fails, the game may crash or log a severe error depending on strictness settings.*

---

## 🔧 Custom Properties

You can attach arbitrary data to a component for use in your logic methods.

```java
.addParam("explosionRadius", 5.0)
.addParam("isIncendiary", true)
```

In your logic method, you can retrieve these (implementation details vary, usually via looking up the component data again from the entity).

---

## ⚠️ Important Rules

1.  **Config-First**: All numerical values (Weight, Thrust, etc.) should be passed as `Supplier<Double>` pointing to a `ModConfigSpec.ConfigValue`. Do **NOT** hardcode numbers.
2.  **Unique Keys**: A Block can only be registered ONCE.
3.  **Client/Server**: Registration happens on both sides, but Logic Methods are usually Server-Side only (ensure your logic class handles sides correctly).
