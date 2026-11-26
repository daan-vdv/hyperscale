---
navigation:
  title: Hyperscale Manual
  position: 0
---

# Welcome to Hyperscale

**Hyperscale** is a datacenter and factory-virtualization mod. Instead of building hundreds of laggy physical machines, you manufacture your own semiconductors, encase any working automation setup *once* inside a sealed **Compiler Casing** chamber to compile it onto a portable **Image Card**, and scale replicas cleanly inside a liquid-cooled datacenter hall.

## How to Use This Guide

* Use the **sidebar on the left** to jump to any topic, or press the **search icon** to find any block, item, or mechanic.
* **Hold `G`** while hovering over any Hyperscale block or item in your inventory or JEI to jump straight to its manual page.
* **3D Scenes** with zoom controls are interactive: **left-click drag** to rotate, **right-click drag** to pan, and hover highlighted blocks for quick notes.

## Quick Navigation

* [Getting Started](getting-started.md) — Step-by-step progression from Tier 0 to Endgame
* [Semiconductor Fab](semiconductor-fab.md) — Raw Silicon, Wafer Press, Lithography Etcher & Dies
* [Factory Capturing](capturing/index.md) — Sealed Test Chambers, Probes, Calibration & Troubleshooting
* [Datacenter Hall](datacenter/index.md) — Switches, Racks, Orchestrator, Storage, RAID, Cooling & UPS

<GameScene zoom="5" interactive={true}>
  <Block x="0" y="0" z="0" id="hyperscale:server_rack" p:facing="south" p:half="lower" />
  <Block x="0" y="1" z="0" id="hyperscale:server_rack" p:facing="south" p:half="upper" />
  <Block x="1" y="0" z="0" id="hyperscale:server_rack" p:facing="south" p:half="lower" />
  <Block x="1" y="1" z="0" id="hyperscale:server_rack" p:facing="south" p:half="upper" />
  <Block x="2" y="0" z="0" id="hyperscale:switch" />
  <Block x="3" y="0" z="0" id="hyperscale:drive_bay" />
  <Block x="4" y="0" z="0" id="hyperscale:image_registry" />
  <Block x="2" y="0" z="1" id="hyperscale:cable" p:north="machine" p:south="machine" p:straight="z" />
  <Block x="2" y="0" z="2" id="hyperscale:interface" p:facing="south" p:half="lower" p:part="primary" />
  <Block x="3" y="0" z="1" id="hyperscale:port" p:mode="input" />
  <Block x="4" y="0" z="1" id="hyperscale:port" p:mode="output" />
  <Block x="-1" y="0" z="0" id="hyperscale:watercooler_controller" />
  <Block x="-1" y="1" z="0" id="hyperscale:server_fan" />
  <IsometricCamera yaw="210" pitch="28" />
</GameScene>
