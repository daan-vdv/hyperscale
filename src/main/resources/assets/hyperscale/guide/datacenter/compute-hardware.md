---
navigation:
  title: Server Racks & Compute Hardware
  icon: hyperscale:server_rack
  parent: datacenter/index.md
  position: 20
item_ids:
  - hyperscale:server_rack
  - hyperscale:server
  - hyperscale:cpu_4c
  - hyperscale:cpu
  - hyperscale:cpu_16c
  - hyperscale:cpu_64c
  - hyperscale:cpu_128c
  - hyperscale:ram_4gb_ddr4
  - hyperscale:ram_4gb_ddr5
  - hyperscale:ram
  - hyperscale:ram_64gb_ddr4
  - hyperscale:ram_64gb_ddr5
  - hyperscale:ram_256gb_ddr5
  - hyperscale:ram_512gb_ddr3
  - hyperscale:ram_512gb_ddr5
  - hyperscale:gpu_t1
  - hyperscale:gpu_t2
  - hyperscale:gpu_t3
---

# Server Racks & Compute Hardware

The <ItemLink id="server_rack" /> is a **2-block-tall** compute chassis with **4 hardware slots**:

1. **<ItemLink id="server" />** *(required)* — Base blade chassis.
2. **CPU** *(required)* — Provides vCPU cores, clock speed, L3 cache RAM, and energy efficiency.
3. **RAM** *(required)* — Sets max parallel replica capacity and memory bandwidth speed.
4. **GPU** *(optional)* — Accelerates *all* running containers across the hall with parallel compute, VRAM, and extra threads.

<Row>
  <RecipeFor id="hyperscale:server_rack" />
  <RecipeFor id="hyperscale:server" />
</Row>

## CPUs

| CPU | vCPU Cores | Clock Speed | L3 Cache RAM | Energy Overhead |
|---|---|---|---|---|
| <ItemLink id="cpu_4c" /> | 4c | `0.90×` | — | Normal |
| <ItemLink id="cpu" /> | 8c | `1.00×` | — | Normal |
| <ItemLink id="cpu_16c" /> | 16c | `1.25×` | `+2 GB` | `-5%` |
| <ItemLink id="cpu_64c" /> | 64c | `1.60×` | `+8 GB` | `-10%` |
| <ItemLink id="cpu_128c" /> | 128c | `2.20×` | `+16 GB` | `-20%` |

## RAM

Notice the trade-offs between **capacity** (max parallel replicas) and **DDR generation** (bandwidth tick speed): for example, <ItemLink id="ram_512gb_ddr3" /> offers massive cheap capacity at `0.85×` speed, while **DDR5** sticks boost tick speed up to `1.35×`.

| RAM | Capacity | Generation | Bandwidth Speed |
|---|---|---|---|
| <ItemLink id="ram_4gb_ddr4" /> / <ItemLink id="ram_4gb_ddr5" /> | 4 GB | DDR4 / DDR5 | `1.00×` / `1.25×` |
| <ItemLink id="ram" /> | 16 GB | DDR4 | `1.00×` |
| <ItemLink id="ram_64gb_ddr4" /> / <ItemLink id="ram_64gb_ddr5" /> | 64 GB | DDR4 / DDR5 | `1.05×` / `1.25×` |
| <ItemLink id="ram_256gb_ddr5" /> | 256 GB | DDR5 | `1.30×` |
| <ItemLink id="ram_512gb_ddr3" /> | 512 GB | DDR3 ECC | `0.85×` |
| <ItemLink id="ram_512gb_ddr5" /> | 512 GB | DDR5 ECC | `1.35×` |

## GPUs (Optional 4th Slot)

| GPU | TFLOPS | Parallel Accel | Bonus VRAM | Compute Threads | Active Power |
|---|---|---|---|---|---|
| <ItemLink id="gpu_t1" /> | 16 | `+35%` | `+12 GB` | `+4` | `+20 FE/t` |
| <ItemLink id="gpu_t2" /> | 64 | `+80%` | `+48 GB` | `+16` | `+80 FE/t` |
| <ItemLink id="gpu_t3" /> | 256 | `+160%` | `+192 GB` | `+64` | `+250 FE/t` |

## Hardware Wear & Refurbishing

Active CPUs, RAM, GPUs, and Drives slowly wear over time (`0.5×` wear when cool, `3×` wear if overheating above 80°C). Craft **any degraded or failed component** with **1 Redstone + 1 Gold Ingot** in a crafting grid to refurbish it back to **100% health**!
