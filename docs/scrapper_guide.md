# 🧭 Projectile Scrapper Guide

This document explains the **Scrapper System** used in **Projectile Libs**.
The scrapper is responsible for analyzing blocks in the world (or other sources) and converting them into a valid `List<ScrappedComponent>` ready for projectile assembly.

---

## 🏗️ Core Concept: `IProjectileScrapper`

All scrappers must implement the `IProjectileScrapper` interface.

```java
public interface IProjectileScrapper {
    /**
     * Scans the source and returns a list of components.
     * @return List of valid, positioned components.
     */
    List<ScrappedComponent> scan();
}
```

### `ScrappedComponent` Record
The result of a scan is a list of these records:
```java
public record ScrappedComponent(
    ProjectileData data,  // The static data from Registry
    BlockPos relativePos, // Position relative to Anchor (0,0,0)
    BlockPos sourcePos    // Absolute world position (for debug/events)
) {}
```
- **relativePos**: The Anchor (usually the Tail or back of the missile) is at `(0,0,0)`. Positive values extend towards the nose.

---

## 🌍 WorldBlockScrapper (The Standard)

The `WorldBlockScrapper` is the default implementation for converting placed blocks into a projectile.

### Logic: "Smart Discovery with Terminal Lock"

1.  **Uniform Orientation**:
    - **ALL** components (Tails, Tanks, Payloads) **MUST** face the same direction (Forward).
    - No more "Tail faces backwards" exceptions.

2.  **Terminal Lock**:
    - If you start the scan by clicking a **TAIL**: The scanner locks the "Back" boundary. It will **only scan forward**.
    - If you start the scan by clicking a **PAYLOAD**: The scanner locks the "Front" boundary. It will **only scan backward**.
    - This prevents grabbing parts of adjacent projectiles when they are stacked.

3.  **Smart Discovery**:
    - **Scanning Backward** (towards engine): Stops **ON** a Tail (Inclusive) or **BEFORE** a Payload (Exclusive).
    - **Scanning Forward** (towards nose): Stops **BEFORE** a Tail (Exclusive) or **ON** a Payload (Inclusive).

4.  **Anchor Repulsion**:
    - The Origin `(0,0,0)` is **ALWAYS** placed at the furthest "Back" block found.
    - Even if you scan a missile with no Tail (just Payload + Tank), the Anchor will be at the back of the Tank, conceptually where the Tail *would* be.

---

## 🛠️ Creating a Custom Scrapper

To create a custom scrapper (e.g., for loading from a schematic, an item inventory, or a network packet), follow this pattern:

### 1. Create the Class
```java
public class MyCustomScrapper implements IProjectileScrapper {
    private final MySource source;

    public MyCustomScrapper(MySource source) {
        this.source = source;
    }

    @Override
    public List<ScrappedComponent> scan() {
        List<ScrappedComponent> result = new ArrayList<>();
        
        // 1. Iterate over your source
        // 2. Validate components using ProjectileComponentRegistry
        // 3. Determine an Anchor point (Origin)
        // 4. Add to result list
        
        return result;
    }
}
```

### 2. Using the Registry
Always use `ProjectileComponentRegistry` to safely identify components.

```java
// Check if block is valid
if (ProjectileComponentRegistry.isComponent(block)) {
    // Get Data
    ProjectileData data = ProjectileComponentRegistry.getData(block).orElse(null);
    if (data != null) {
        // Add to list
        result.add(new ScrappedComponent(data, relativePos, originalPos));
    }
}
```

### 3. Example: ScrapperInventory (Concept)
*Currently a stub in the codebase.*
```java
public class ScrapperInventory implements IProjectileScrapper {
    // ...
    @Override
    public List<ScrappedComponent> scan() {
        // Iterate over ItemStack slots
        // If Item is a component BlockItem:
        //   - Add to list
        //   - Auto-assign relative positions (e.g. 0,1,2 sequence)
        return list;
    }
}
```

---

## ⚠️ Important Rules for Scrappers

1.  **Always Return `ProjectileData`**: Never guess or hardcode properties. Use the Registry.
2.  **Respect the Anchor**: The assembled projectile expects consistency. `(0,0,0)` should physically be the rear-most element (or the Tail connection point).
3.  **Validate**: Don't add Air blocks or non-component blocks to the list unless your specific logic requires "Spacers".
