---
navigation:
  title: Factory Capturing
  icon: hyperscale:recorder
  position: 30
---

# Factory Capturing & Compilation

The heart of Hyperscale is the **Image Compiler** (<ItemLink id="recorder" />). It lets you take *any* working vanilla or modded automation—a Furnace, a Mekanism ore-processing line, a Create contraption, or even your own Wafer Press—verify its exact inputs, outputs, fluids, power draw, and cycle time, and compile it onto a portable <ItemLink id="image" /> card.

Once compiled, you can tear down the physical factory and run **dozens or hundreds of virtual replicas** inside your [Datacenter Hall](../datacenter/index.md).

## How Capturing Works (3 Steps)

1. **[Build a Sealed Test Chamber](test-chamber.md)** — Encase your factory in a hollow rectangular box of <ItemLink id="recorder_casing" /> (`3×3×3` up to `32×32×32`) with the <ItemLink id="recorder" /> in one of the outer walls.
2. **[Attach Probes & Power](probes-and-power.md)** — Place <ItemLink id="recorder_input" /> / <ItemLink id="recorder_output" /> (and optional Fluid Probes & <ItemLink id="recorder_power" />) to feed and collect your factory's batch.
3. **[Run 3 Calibration Cycles](troubleshooting.md)** — Insert an <ItemLink id="empty_image_card" />, load the same input batch 3 times, and let the compiler verify there is zero item, fluid, energy, or block drift.

## What Can Be Captured?

* **Vanilla machines:** Furnaces, Blast Furnaces, Smokers, Brewing Stands, Crafters, etc.
* **Modded machines & multi-step lines:** Mekanism, AE2, Thermal, Create, Hyperscale Fab machines, and any mod using standard NeoForge item/fluid/FE capabilities.
* **Blocked for balance:** Wireless / cross-dimensional blocks (like Ender Chests, Quantum Entangloporters, Flux Points) are listed in `#hyperscale:compiler_forbidden` and cannot be inside the chamber.
