---
navigation:
  title: Getting Started
  icon: hyperscale:interface
  position: 10
---

# Getting Started

Hyperscale follows a clean 4-stage progression loop. You can also press **`L`** in-game and open the **Hyperscale** advancement tab for a visual checklist.

## 1. Build a Semiconductor Fab (Tier 0)

Before you can craft servers, CPUs, RAM, or drives, you need **PCBs** and **etched silicon dies**:

1. Smelt or blast **Nether Quartz** to get <ItemLink id="silicon" /> (Hyperscale also accepts Silicon from AE2, Mekanism, or Refined Storage via `#c:silicon`).
2. Craft a <ItemLink id="wafer_press" /> and supply it with FE power to press <ItemLink id="silicon_wafer" /> and <ItemLink id="pcb" />.
3. Craft a <ItemLink id="lithography_etcher" /> and reusable **Photomasks** (<ItemLink id="mask_logic" />, <ItemLink id="mask_memory" />, <ItemLink id="mask_storage" />) to etch dies.

See [Semiconductor Fab](semiconductor-fab.md) for all recipes and EUV upgrades.

## 2. Capture Your First Factory

Instead of scaling physical machines, capture a working machine *once*:

1. Build a hollow sealed box of <ItemLink id="recorder_casing" /> with an <ItemLink id="recorder" /> in the outer wall.
2. Put your working factory inside (for example, a Furnace or modded crusher), with <ItemLink id="recorder_input" /> and <ItemLink id="recorder_output" /> probes attached.
3. Insert an <ItemLink id="empty_image_card" /> into the compiler, load inputs, and run **3 verified calibration cycles** to produce a compiled <ItemLink id="image" /> card.

See [Factory Capturing](capturing/index.md) for step-by-step instructions and a 3D chamber diagram.

## 3. Assemble a Starter Datacenter Hall (Tier 1)

Place these blocks touching each other or connected via <ItemLink id="cable" />:

* <ItemLink id="switch" /> — The hub of your hall. Pipe FE power into the Switch (or into any switched Hybrid Cable).
* <ItemLink id="server_rack" /> — Insert a <ItemLink id="server" />, a **CPU**, and **RAM** (plus an optional **GPU**).
* <ItemLink id="image_registry" /> — Holds up to 9 compiled Image Cards.
* <ItemLink id="drive_bay" /> — Insert **HDD**, **SATA SSD**, or **NVMe SSD** drives for unified item storage.
* <ItemLink id="port" /> — Right-click to toggle **Input** / **Output** mode for piping items in and out.
* <ItemLink id="interface" /> — Opens the **Hyperscale Orchestrator** to deploy containers and scale replicas!

## 4. Scale, Cool & Protect (Tier 2 – 4)

* Add <ItemLink id="server_fan" /> blocks and a <ItemLink id="watercooler_controller" /> (with adjacent fans for up to **-75% water usage**) to keep temperatures below 50°C for **Cold Turbo (+15% speed)** and **Overclock**.
* Build a `2×2×2` <ItemLink id="backup_generator" /> UPS multiblock for a **10,000,000 FE** shared battery buffer.
* Configure **RAID 1** or **RAID 5** on your Drive Bays so thermal hardware wear never destroys your stored items.
