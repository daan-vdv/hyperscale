---
navigation:
  title: Datacenter Hall
  icon: hyperscale:switch
  position: 40
item_ids:
  - hyperscale:switch
  - hyperscale:cable
  - hyperscale:image_registry
---

# Building Your Datacenter Hall

A **Datacenter Hall** is a connected network of Hyperscale blocks. Blocks connect either by **touching directly** or through **Hybrid Cables** (<ItemLink id="cable" />)—**as long as the network includes at least one <ItemLink id="switch" />**.

## Example Datacenter Hall

<GameScene zoom="5" interactive={true}>
  <Block x="0" y="0" z="0" id="hyperscale:server_rack" p:facing="south" p:half="lower" />
  <Block x="0" y="1" z="0" id="hyperscale:server_rack" p:facing="south" p:half="upper" />
  <Block x="1" y="0" z="0" id="hyperscale:server_rack" p:facing="south" p:half="lower" />
  <Block x="1" y="1" z="0" id="hyperscale:server_rack" p:facing="south" p:half="upper" />
  <Block x="2" y="0" z="0" id="hyperscale:switch" />
  <Block x="3" y="0" z="0" id="hyperscale:image_registry" />
  <Block x="4" y="0" z="0" id="hyperscale:drive_bay" />
  <Block x="2" y="0" z="1" id="hyperscale:cable" p:north="machine" p:south="machine" p:east="machine" />
  <Block x="3" y="0" z="1" id="hyperscale:port" p:mode="input" />
  <Block x="4" y="0" z="1" id="hyperscale:port" p:mode="output" />
  <Block x="2" y="0" z="2" id="hyperscale:interface" p:facing="south" p:half="lower" p:part="primary" />

  <BlockAnnotation x="2" y="0" z="0" color="#E6C235">
    Network Switch: required control plane & FE power hub. Pipe FE into the Switch or any linked Hybrid Cable.
  </BlockAnnotation>
  <BlockAnnotation x="0" y="0" z="0" color="#38BDF8">
    Server Racks (2 blocks tall): hold Server chassis, CPU, RAM, and optional GPU.
  </BlockAnnotation>
  <BlockAnnotation x="3" y="0" z="0" color="#C084FC">
    Image Registry: stores up to 9 compiled Image Cards for the hall.
  </BlockAnnotation>
  <BlockAnnotation x="4" y="0" z="0" color="#3CA66B">
    Drive Bay: holds HDD, SATA SSD, and NVMe SSD drives with JBOD / RAID 1 / RAID 5 support.
  </BlockAnnotation>
  <BlockAnnotation x="2" y="0" z="2" color="#22D3EE">
    Interface: opens the Hyperscale Orchestrator to deploy containers and browse storage.
  </BlockAnnotation>
  <IsometricCamera yaw="210" pitch="28" />
</GameScene>

## Core Networking Blocks

* **<ItemLink id="switch" />** — The brain and power hub of the hall. Pipe FE power directly into the Switch (or into any switched Hybrid Cable). Also includes 6 fallback Image Card slots.
* **<ItemLink id="cable" />** — 6-way connecting cable that links distant racks, ports, cooling units, and UPS banks to your Switch *and* routes FE power into the hall.
* **<ItemLink id="image_registry" />** — Dedicated 9-slot library for your compiled <ItemLink id="image" /> cards.

## Recipes

<Row>
  <RecipeFor id="hyperscale:switch" />
  <RecipeFor id="hyperscale:cable" />
</Row>
<RecipeFor id="hyperscale:image_registry" />

## Next Topics

* [Orchestrator & Containers](orchestrator.md)
* [Server Racks, CPUs, RAM & GPUs](compute-hardware.md)
* [Drive Bays, RAID & Ports](storage-and-ports.md)
* [Active Cooling, PUE & Backup UPS](cooling-and-ups.md)
