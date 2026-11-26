---
navigation:
  title: Drive Bays, RAID & Ports
  icon: hyperscale:drive_bay
  parent: datacenter/index.md
  position: 30
item_ids:
  - hyperscale:drive_bay
  - hyperscale:port
  - hyperscale:fluid_port
  - hyperscale:hdd
  - hyperscale:hdd_256gb
  - hyperscale:hdd_1tb
  - hyperscale:ssd_16gb
  - hyperscale:ssd_64gb
  - hyperscale:nvme_64gb
  - hyperscale:nvme_256gb
---

# Drive Bays, RAID & Ports

Containers read their inputs from and write their outputs into your hall's unified **Drive Bay** storage pool.

<Row>
  <RecipeFor id="hyperscale:drive_bay" />
  <RecipeFor id="hyperscale:port" />
  <RecipeFor id="hyperscale:fluid_port" />
</Row>

## Storage Drives

Insert drives into a <ItemLink id="drive_bay" />. Choose between **cheap bulk HDDs** or **ultra-fast NVMe SSDs** for high-replica I/O bandwidth:

| Drive | Type | Max Items | Max Types | I/O Bandwidth |
|---|---|---|---|---|
| <ItemLink id="hdd" /> | HDD | 16,384 | 16 | 16 items/t |
| <ItemLink id="hdd_256gb" /> | HDD | 65,536 | 32 | 24 items/t |
| <ItemLink id="hdd_1tb" /> | HDD | 262,144 | 64 | 32 items/t |
| <ItemLink id="ssd_16gb" /> | SATA SSD | 4,096 | 16 | 64 items/t |
| <ItemLink id="ssd_64gb" /> | SATA SSD | 16,384 | 32 | 128 items/t |
| <ItemLink id="nvme_64gb" /> | NVMe SSD | 16,384 | 32 | 256 items/t |
| <ItemLink id="nvme_256gb" /> | NVMe SSD | 65,536 | 64 | 512 items/t |

## RAID & Failed-Drive Salvage

Open a <ItemLink id="drive_bay" /> and click the **RAID** button to choose your redundancy level:

* **JBOD** — 100% usable capacity, no redundancy. If a drive fails from thermal wear, click **Salvage** in the Drive Bay GUI to recover most items and restore the drive to 50% health (or craft it with Redstone + Gold to refurbish to 100%).
* **RAID 1 (Mirror)** — Mirrors data across drives so a failed drive causes **zero data loss**.
* **RAID 5 (Parity)** — Stripes parity across 3+ drives for higher usable capacity with **single-drive fault tolerance**.

## Item Ports & Ghost Whitelist

* **<ItemLink id="port" />** — **Right-click** with an empty hand to toggle between **Input** (green) and **Output** (orange). **Shift + right-click** (or right-click with an item) to open its GUI.
* **Input Mode:** Pipe raw materials in from chests, AE2, or Mekanism pipes—items are automatically stored in your Drive Bays.
* **Output Mode:** Automatically exports finished container outputs to adjacent pipes/inventories. You can also set up to **3 Ghost Whitelist slots** in the Output Port GUI to export *only* specific items!

## Fluid Ports

* **<ItemLink id="fluid_port" />** — `64,000 mB` internal tank for fluid-using or fluid-producing images.
* Right-click to toggle **Input / Output** mode; supports modded fluid pipes and buckets.
