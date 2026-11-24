# Hyperscale Playtester Guide

Welcome to **Hyperscale**! This guide walks through the full progression loop—from your first Silicon Wafer to compiling modded factories and scaling a liquid-cooled datacenter hall.

> Tip: Press **`L`** in-game and open the **Hyperscale** advancement tab for a quick visual checklist, and use **JEI** (`R` / `U`) to browse all crafting, Wafer Press, and Lithography Etcher recipes.

---

## 1. Semiconductor Fab (Tier 0)

Before building datacenter hardware, you need PCBs and etched silicon dies:

1. **Raw Silicon (`#c:silicon`):** Smelt or blast **Nether Quartz** in a Furnace/Blast Furnace (or use Silicon from AE2, Mekanism, or Refined Storage).
2. **Wafer Press** *(requires FE power)*:
   - `2× Silicon` → **Silicon Wafer**
   - `2× Copper Ingot` + `2× Redstone` → **2× Printed Circuit Board (PCB)**
   - `1× Silicon Wafer` + `1× Diamond` → **Doped Silicon Wafer** *(Mid/Late tier)*
   - `1× Doped Silicon Wafer` + `1× Netherite Scrap` → **EUV Quantum Wafer** *(Endgame tier)*
3. **Lithography Etcher** *(requires FE power; Water required for EUV recipes)*:
   - Place a reusable **Photomask** (`Logic`, `Memory`, `NAND`, or `Tensor`) in the **top (purple)** slot—it is **never consumed**.
   - Insert a **Wafer** and **Catalyst** (`Copper`, `Redstone`, `Gold`, `Diamond`, or `Amethyst`) to etch **Logic, Memory, NAND, Tensor, and Quantum Dies**.
   - For **Tensor Dies** (`250 mB Water`) and **Quantum Core Dies** (`500 mB Water`), right-click the Etcher with a Water Bucket or pipe Water into it.

> **Recursive Trick:** Both Fab machines expose standard item, fluid, and FE capabilities—later you can encase your own Silicon Fab inside a `Compiler Casing` room and virtualize chip production inside your datacenter!

---

## 2. How to Capture & Compile a Factory

To turn any physical vanilla or modded automation into a portable **Image Card**:

### Step 1: Build a Sealed Test Chamber
1. Build a **hollow rectangular box** (minimum `3×3×3` outer, maximum `32×32×32`) made completely of **Compiler Casing**.
2. Place the **Image Compiler** (`recorder`) anywhere in one of the outer walls (facing outward).
3. *(Optional, for powered machines)* Place one or more **Compiler Power Ports** in the outer walls, with the **back face** touching your interior machine or power cable. Supply FE to the outside face.

### Step 2: Place Your Factory & Probes Inside
1. Build your working automation inside the hollow chamber (e.g., a Furnace, Mekanism Crusher, Create line, or Wafer Press).
2. Place **Recorder Input** and **Recorder Output** probes (and/or **Recorder Fluid Input / Output** probes for fluids) inside the chamber:
   - **Orientation matters:** The **back face** (opposite the front screen face) connects to your factory.
   - Hold **Shift** while placing to flip the facing direction.
   - Probes automatically pump items/fluids to/from an adjacent machine on their back face, **and** they also work with pipes like **Mekanism Logistical Transporters / Mechanical Pipes** (set Input pipe to *Pull*, Output pipe to *Push*—pipes only transfer while a run is actively sampling!).

### Step 3: Run 3 Verified Calibration Cycles
1. Right-click the **Image Compiler** and insert an **Empty Image Card**.
2. Load your input batch into the **Recorder Input** probe(s) (you can open probes remotely from the Compiler GUI's **Main** or **3D** tab!).
   - *Example (Furnace):* Put `8× Raw Iron` in Input #1 (top of furnace) and `1× Coal` in Input #2 (side of furnace).
3. Click **Start**. The chamber seals and locks until the run settles.
4. Once Run `1/3` finishes, reload the **exact same input batch** and click **Next run** for Runs `2/3` and `3/3`.
5. After 3 matching runs, your **Image Card** is written! You can now tear down the physical factory.

### Common Compiler Errors & Fixes
| Status Message | Cause & Fix |
|---|---|
| *Encase the factory in Compiler Casing* | The outer shell has a gap or corner missing. Every face and edge of the hollow box must be `Compiler Casing`, `Image Compiler`, or `Compiler Power Port`. |
| *Storage changed / Interior fluid changed* | An internal machine or chest did not return to its exact starting state after the run (e.g., leftover fuel/items or a hidden buffer draining). Make sure your batch cleanly completes with zero leftover drift. |
| *Interior power dropped without refill* | An interior machine used stored FE without being topped back up through a `Compiler Power Port`. |
| *Blocks inside the casing were modified or destroyed* | A block inside the chamber was placed, broken, or blown up during sampling. |
| *Forbidden block inside* | A wireless/cross-dimensional block (e.g., Quantum Entangloporter, Ender Chest, Flux Point) is in `#hyperscale:compiler_forbidden`. |

---

## 3. Building & Running Your Datacenter Hall

All datacenter blocks connect either directly (touching) or via **Hybrid Cables**—**as long as the cable network is plugged into at least one Network Switch**.

1. **Network Switch:** The control plane & power hub. Pipe FE into the Switch (or into any switched **Hybrid Cable**).
2. **Server Racks (2 blocks tall):** Populate with:
   - `Server` chassis + `CPU` + `RAM` (required) + optional `GPU` (4th slot for parallel speed, VRAM, and compute threads).
3. **Image Registry:** Holds up to 9 compiled **Image Cards** for the hall (the Switch also has 6 fallback slots).
4. **Drive Bay + Ports:**
   - Insert `HDD`, `SATA SSD`, or `NVMe SSD` drives for unified AE2-style item storage.
   - Click the RAID button in the Drive Bay GUI to switch between **JBOD**, **RAID 1** (mirror), and **RAID 5** (parity) to protect data against drive wear. Failed drives in JBOD can be **Salvaged** in the GUI.
   - **Port (Input / Output):** Right-click to toggle mode. Output Ports have a **3-slot Ghost Whitelist** for dedicated exports (leave empty to auto-export finished products).
   - **Fluid Port:** `64,000 mB` tank (right-click to toggle Input / Output; supports pipes and buckets).
5. **Interface:** Opens the **Hyperscale Orchestrator**:
   - Click **`+`** on an Image to deploy a container; scale replicas up/down (`Shift` = ±5, `Ctrl` = ±10).
   - Toggle per-container profiles: **Eco**, **Balanced**, or **Overclock** *(1.5× speed when hall temp ≤ 55°C)*.
   - Switch to the **Storage** tab to browse and take items directly from your Drive Bays.
6. **Backup Generator (2×2×2 multiblock):** Place 8 blocks in a `2×2×2` cube connected to your hall to add a shared **`10,000,000 FE`** UPS battery buffer.

---

## 4. Cooling, Radiator Loops & Hardware Wear

Running containers generates **Heat (Watts)**. Keep your hall cool to maximize efficiency and avoid hardware wear:

- **Server Fan:** `+600W` air cooling (`15 FE/t`).
- **Watercooler Controller:** `+4,000W` liquid cooling (`120 FE/t`) when supplied with **Water** (pipe or right-click with a bucket; falls back to `+1,200W` dry if empty).
  - **Closed-Loop Radiator Bonus:** Place **Server Fans** directly touching a Watercooler Controller—each adjacent fan cuts water consumption by **25%** (up to **-75% water use** with 3+ adjacent fans!).
- **Temperature Thresholds:**
  - **≤ 50°C (with Liquid Cooling):** **Cold Turbo (+15% speed)** & **Overclock** unlocked!
  - **PUE ≤ 1.20 (and < 75°C):** **-10% Compute FE** efficiency bonus!
  - **80°C – 100°C:** Thermal throttling (down to 40% speed) + **3× hardware wear**.
  - **100°C:** Emergency thermal shutdown until cooled below 78°C.
- **Refurbishing Hardware:** Active CPUs, RAM, GPUs, and Drives slowly wear over time (0.5× wear when cool, 3× when overheating). Craft any damaged or failed component with **1 Redstone + 1 Gold Ingot** to refurbish it back to **100%**.

---

## 5. Config & Modpack Customization

- Server config (`hyperscale-server.toml`) lets you tune:
  - `energyMultiplier`, `baseReplicaFePerTick`, `enableThermal`, `wearRateMultiplier`, `watercoolerMbPerTick`, `useAllowlist`, and `energyDriftTolerance`.
- All **Wafer Press** (`hyperscale:wafer_press`) and **Lithography Etcher** (`hyperscale:lithography`) recipes are fully data-driven JSON recipes.
- Block tags `#hyperscale:compiler_forbidden` and `#hyperscale:compiler_allowed` can be customized via datapacks.
