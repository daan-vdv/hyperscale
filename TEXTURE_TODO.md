# Hyperscale — Remaining Textures & Models Checklist

A reference list of every block (and optional item) in **Hyperscale** that still needs a custom texture/model, including **real-world (IRL) hardware references**, design cues, and Minecraft 1.21.1 technical notes.

---

## 1. Blocks Needing Custom Textures / Models (5 Blocks)

All 5 remaining blocks belong to the **Image Compiler / Sealed Test Chamber** family, so giving them a cohesive industrial-telemetry look (matching the existing dark-slate/steel datacenter palette) will make the sealed factory room look awesome.

### 1.1 `hyperscale:recorder` — **Image Compiler**
- **Current state:** Placeholder (`minecraft:white_concrete` + `minecraft:observer_front`, slate-blue tint).
- **Footprint & Orientation:** `1×1×1` full block, 6-way `facing` (placed in the wall of the `Compiler Casing`, front face pointing outward toward the player).
- **IRL References:**
  - **Industrial PLC / SCADA Control Panel** (e.g., *Siemens SIMATIC HMI* or *Allen-Bradley PanelView* mounted flush in a cabinet wall)
  - **Test-Chamber Data Acquisition (DAQ) Controller** (e.g., *National Instruments PXI Chassis* / *Keysight BenchVue* rack controller)
  - **Environmental Test Chamber Controller** (e.g., *Espec* or *Thermotron* chamber wall touchscreen + status LEDs + card slot)
- **Visual / Design Notes:**
  - **Front face (outward):** Small glowing telemetry screen/waveform (optional animated `.mcmeta` strip!), a horizontal **card reader slot** for the Empty Image Card, and 3 status indicator LEDs (representing the 3 calibration runs).
  - **Back face (into the casing):** Sensor/scanner lens or laser-grid emitter facing the factory inside.
  - **Sides:** Brushed dark-slate chassis matching `Compiler Casing` so it sits flush in the wall.

---

### 1.2 `hyperscale:recorder_casing` — **Compiler Casing**
- **Current state:** Simple auto-generated 16×16 translucent PNG (`recorder_casing.png`).
- **Footprint & Orientation:** `1×1×1` transparent block (`TransparentBlock`, `"render_type": "minecraft:translucent"`). Adjacent casing faces automatically cull like vanilla glass.
- **IRL References:**
  - **Cleanroom / EMC Shielded Observation Window** (tempered safety glass in a heavy bolted steel/aluminum extrusion frame)
  - **Environmental / Burn-In Test Chamber Glass Door** (thick insulated glass with subtle etched measurement grid or corner brackets)
  - **Server Room Cold-Aisle Containment Glass Panel** (e.g., *APC NetShelter* containment glass with dark anodized aluminum framing)
- **Visual / Design Notes:**
  - Keep the **center very clear** (low alpha, ~10–20% cyan/blue tint) so players can easily see their factory inside.
  - Crisp **dark-metallic bolted frame** around the outer 1–2px border (32×32 or 16×16) with subtle corner fasteners/rivets so a 3×3×3 or 7×7×7 sealed room reads like a reinforced glass enclosure.
  - Needs to tile cleanly side-by-side (`cube_all`).

---

### 1.3 `hyperscale:recorder_input` — **Recorder Input**
- **Current state:** Placeholder (`minecraft:white_concrete` + `minecraft:dropper_front`, green tint).
- **Footprint & Orientation:** `1×1×1` block, 6-way `facing`. Placed **inside** the casing against a machine/hopper; the **back** (`facing.getOpposite()`) pushes items into the machine, while the **front** (`facing`) is the visible outside face.
- **IRL References:**
  - **Automated Material Feeder / Inline Optical Counter** (e.g., *Keyence* inline optical sensor chute / industrial parts feeder)
  - **Datacenter / Lab Pneumatic Sample Injector** (green-accented intake port with directional chevron arrows)
  - **Factory Conveyor Intake Gate** with green "IN" LED ring
- **Visual / Design Notes:**
  - **Strong green accent (`#3CA66B`)** so it is unmistakably an **Input** even in the Compiler's 3D preview!
  - **Front face:** Green-rimmed intake hatch / status panel with an inward arrow or `"IN"` icon.
  - **Back face (machine-facing):** Hopper-like nozzle / chute flange that visually connects to a Furnace, Chest, or Mekanism machine.
  - **Sides:** Subtle directional chevrons pointing toward the back face so players immediately see which way items flow.

---

### 1.4 `hyperscale:recorder_output` — **Recorder Output**
- **Current state:** Placeholder (`minecraft:white_concrete` + `minecraft:dispenser_front`, orange tint).
- **Footprint & Orientation:** `1×1×1` block, 6-way `facing`. Placed **inside** the casing against a machine/hopper; the **back** (`facing.getOpposite()`) pulls finished items out of the machine.
- **IRL References:**
  - **Industrial Ejection Chute / Product Collector** (e.g., automated sorting/QA output hopper with optical barcode scanner)
  - **Lab Telemetry Output Collector** (amber/orange-accented extraction port with outward chevron arrows)
  - **Factory QA Inspection Gate** with amber "OUT" LED ring
- **Visual / Design Notes:**
  - **Strong amber/orange accent (`#D9822B`)** to contrast clearly against the green `Recorder Input` in both world view and the 3D preview.
  - **Front face:** Amber-rimmed collection bin / output display.
  - **Back face (machine-facing):** Suction/extraction flange touching the machine.
  - **Sides:** Directional chevrons pointing away from the machine (opposite of `Recorder Input`).

---

### 1.5 `hyperscale:recorder_power` — **Compiler Power Port**
- **Current state:** Placeholder (reuses `recorder.json` with a gold tint).
- **Footprint & Orientation:** `1×1×1` block, 6-way `facing` (placed in the wall of the `Compiler Casing`, front face outward for external FE cables, back face inward for interior machines/cables).
- **IRL References:**
  - **Industrial Bulkhead High-Voltage Feedthrough / Power Pass-Through** (e.g., *Amphenol* or *Harting* heavy-duty industrial bulkhead connector)
  - **Datacenter Metered PDU / Watt-Hour Sub-Meter** (e.g., *APC Metered Rack PDU* or *Schneider PowerLogic* panel meter with yellow/gold high-voltage markings)
  - **Electric Utility Smart Meter / Shunt Busbar Feedthrough** (copper coils + digital kWh/FE readout)
- **Visual / Design Notes:**
  - **Yellow/gold & copper accents (`#E6C235`)** with a subtle lightning-bolt / high-voltage icon so it's instantly recognizable as power.
  - **Front face (outside wall):** Heavy-duty power socket / cable coupling + small live wattage meter bar.
  - **Back face (inside wall):** Matching interior power terminal where Mekanism Universal Cables / modded cables plug in.
  - **Sides:** Same dark-slate casing border as `Image Compiler` so it blends cleanly into the `Compiler Casing` wall.

---

## 2. Optional Polish — Item Icons (16×16 / 32×32)

These items currently have very small/simple placeholder PNGs (`< 300 bytes`) and could be upgraded to match your detailed `cpu.png` and `ram.png`:

| Item ID | Name | IRL Reference |
| :--- | :--- | :--- |
| `hyperscale:empty_image_card` | Empty Image Card | Blank *SD / CFexpress* card, *M.2 NVMe SSD* stick, or blank *LTO data tape cartridge* |
| `hyperscale:image` | Compiled Image | Glowing/labeled *NVMe drive / optical data cartridge* with a lit blue/cyan circuit trace showing it contains data |
| `hyperscale:binder` | Binder | Handheld *Fluke Network Cable Tester / Barcode Scanner / RF Linker* tool |
| `hyperscale:server` | Server | *1U / 2U Rackmount Server Blade* (e.g., *Dell PowerEdge* / *Supermicro* pizza-box server) |
| `hyperscale:hdd` | HDD *(legacy/optional)* | *3.5" Enterprise SAS/SATA Hard Drive* (e.g., *Seagate Exos* / *WD Ultrastar*) |

---

## 3. Already Completed (Custom Models & Textures Done)

- `hyperscale:backup_generator` (2×2×2 multiblock, animated battery core)
- `hyperscale:cable` (Hybrid Cable)
- `hyperscale:drive_bay` (Drive Bay)
- `hyperscale:interface` (2-wide Datacenter Workbench, animated screens)
- `hyperscale:port` (Input Port & Output Port)
- `hyperscale:server_fan` (Server Fan, animated rotor)
- `hyperscale:server_rack` (2-tall Server Rack, animated LEDs)
- `hyperscale:switch` (Network Switch)
- `hyperscale:watercooler_controller` (Watercooler Controller)

---

## 4. Export Checklist for Blockbench → Minecraft 1.21.1
- **Lowercase filenames only** (`[a-z0-9_.-]`).
- **Element bounds:** Keep all `from`/`to` coordinates within **`[-16, 32]`**.
- **Rotations:** Use single-axis rotations (`"axis": "x"|"y"|"z"`) with angles in **`{-45, -22.5, 0, 22.5, 45}`** (avoid Blockbench 5's 3-axis `{x, y, z}` export format).
- **Animated textures:** Vertical strip PNGs (`W × (W * frames)`) just need a matching `.png.mcmeta` (`{"animation":{"frametime":2}}`).
- **Remove `"tintindex": 0`** once custom colored textures replace the white-concrete placeholders (and remove the tint entries in `HyperscaleClient.java`).
