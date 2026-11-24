# Hyperscale

**Hyperscale** is a NeoForge `1.21.1` datacenter & factory-virtualization mod. Instead of building hundreds of laggy machines, you build a **Semiconductor Fab**, encase any working automation setup *once* inside a sealed **Compiler Casing** chamber to compile it into a portable **Image Card**, and scale replicas cleanly inside a real datacenter hall.

> **Early Alpha Playtest Notice:** Textures and crafting recipes are still Work-in-Progress! Feedback on mechanics, capture edge cases/exploits, **recipe balance**, and modpack compatibility is very welcome via [GitHub Issues](https://github.com/daan-vdv/hyperscale/issues).
>
> **New to Hyperscale?** Read the full **[Playtester Guide (docs/GUIDE.md)](docs/GUIDE.md)** for step-by-step instructions on the Semiconductor Fab, capturing factories, cooling/radiator loops, and troubleshooting!

## Installing (Playtest)

1. Download the latest `hyperscale-*.jar` from [GitHub Releases](https://github.com/daan-vdv/hyperscale/releases).
2. Drop it into your **Minecraft 1.21.1 (NeoForge)** `mods/` folder (works standalone or inside packs like ATM10 / ATM10TTS).
3. **Recommended companion mods:** JEI (full Wafer Press & Lithography recipe support), Jade (live cluster/rack/watercooler tooltips), and any FE power mod (e.g., Mekanism, AE2).
4. Press **`L`** in-game and open the **Hyperscale** advancement tab for a guided progression tree.

## Core Mechanics

- **2-Machine Semiconductor Fab (`Wafer Press` → `Lithography Etcher`):**
  - Smelt Quartz into Raw Silicon (`#c:silicon` — also accepts AE2 / Mekanism / Refined Storage Silicon automatically).
  - Press **Silicon Wafers**, **PCBs**, **Doped Wafers**, and **EUV Quantum Wafers** in the `Wafer Press`.
  - Etch **Logic, Memory, NAND, Tensor, and Quantum Dies** in the `Lithography Etcher` using reusable **Photomasks** (plus Water for EUV chips).
  - *Recursive bonus:* You can encase your own Silicon Fab in `Compiler Casing` to virtualize chip manufacturing!
- **Image Compiler & Sealed Test Chamber:** Encase any vanilla or modded automation (Furnaces, Mekanism machines, Wafer Presses, etc.) in `Compiler Casing` with `Recorder Input` / `Recorder Output` probes (plus `Fluid Probes` and `Compiler Power Port`). After 3 verified calibration runs (with full interior item, fluid, energy, `ItemEntity`, and block-integrity verification), the factory is compiled onto an **Image Card**.
- **Multi-Container Orchestrator (`Interface`):** Deploy multiple images in parallel with **uncapped replicas**, live **RAM / vCPU / GPU / Storage / Power / Temperature / PUE** telemetry, per-container **Eco / Balanced / Overclock** profiles, and an AE2-style **Storage Browser** tab.
- **Overlapping Hardware Tiers (`Server Rack` — 4 Slots):**
  - **CPUs** (`4c Edge` → `128c Quantum`), **RAM** (`4 GB DDR4` → `512 GB DDR3/DDR5 ECC`), and optional **GPUs** (`Compute` / `Datacenter` / `Tensor`).
- **AE2-Style Drive Bay Storage & RAID (`Drive Bay` + `Ports`):**
  - `HDD`, `SATA SSD`, and `NVMe SSD` drives with `JBOD`, `RAID 1`, and `RAID 5` fault tolerance against thermal wear + failed-drive **Salvage**.
  - **Input Ports**, **Ghost-Whitelisted Output Ports**, and **Fluid Ports** (`64,000 mB`).
- **Switched Hybrid Cables:** 6-way connecting cables that bridge datacenter halls *and* route FE power into your `Network Switch` & `Backup Generator` UPS banks.
- **Active Cooling, Radiator Loops, PUE & UPS:**
  - `Server Fans` (`+600W`) and `Watercooler Controllers` (`+4,000W` with active Water consumption; placing `Server Fans` directly adjacent forms a closed-loop radiator saving up to **75%** water) manage hall temperature (`22°C–100°C`), unlocking Cold Turbo (`+15%`), Overclock mode, and the **`-10%` FE PUE Efficiency Bonus** (`PUE ≤ 1.20`).
  - `Backup Generator` (2×2×2 multiblock) adds a shared **`10,000,000 FE`** UPS battery buffer.

## Building & Releasing

```bash
./gradlew build
./gradlew runClient
```

Pushing a tag like `v0.5.0-alpha.1` triggers `.github/workflows/release.yml` to automatically build and publish a GitHub Release with the `.jar` attached.

## License & Permissions

**All Rights Reserved** — Copyright © 2026 `daan-vdv` & `TheDestroyer7420` (see [LICENSE](LICENSE)).

- **Modpacks:** You are welcome to include Hyperscale in CurseForge / Modrinth modpacks!
- **Forks, Addons & Other Uses:** We are very open to granting permissions—just open a [GitHub Issue](https://github.com/daan-vdv/hyperscale/issues) or reach out and ask.
