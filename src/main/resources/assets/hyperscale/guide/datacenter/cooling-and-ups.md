---
navigation:
  title: Cooling, PUE & Backup UPS
  icon: hyperscale:watercooler_controller
  parent: datacenter/index.md
  position: 40
item_ids:
  - hyperscale:server_fan
  - hyperscale:watercooler_controller
  - hyperscale:backup_generator
---

# Active Cooling, PUE & Backup UPS

Running virtualized replicas generates **Heat (Watts)**. Keeping your hall cool unlocks speed bonuses, cuts power draw, and prevents hardware wear!

## Air & Liquid Cooling

* **<ItemLink id="server_fan" />** — Provides **`+600W`** air cooling (`15 FE/t`).
* **<ItemLink id="watercooler_controller" />** — Provides **`+4,000W`** liquid cooling (`120 FE/t`) when supplied with **Water** (pipe Water in or right-click with a Water Bucket). If it runs dry, it drops to `+1,200W`.

### Closed-Loop Radiator Bonus (-75% Water)

Place **<ItemLink id="server_fan" />** blocks **directly touching** a <ItemLink id="watercooler_controller" /> to form a closed-loop radiator! Each adjacent fan reduces water consumption by **25%**, up to **-75% water usage** with 3 or more adjacent fans:

<GameScene zoom="6" interactive={true}>
  <Block x="1" y="0" z="1" id="hyperscale:watercooler_controller" />
  <Block x="0" y="0" z="1" id="hyperscale:server_fan" />
  <Block x="2" y="0" z="1" id="hyperscale:server_fan" />
  <Block x="1" y="1" z="1" id="hyperscale:server_fan" />
  <BlockAnnotation x="1" y="0" z="1" color="#38BDF8">
    Watercooler Controller: +4,000W liquid cooling. Pipe Water into any open face!
  </BlockAnnotation>
  <BlockAnnotation x="1" y="1" z="1" color="#94A3B8">
    3 adjacent Server Fans: each cuts Watercooler water usage by 25% (-75% total) AND adds +600W cooling each!
  </BlockAnnotation>
  <IsometricCamera yaw="215" pitch="30" />
</GameScene>

## Temperature & PUE Thresholds

| Hall Condition | Effect |
|---|---|
| **≤ 50°C (Liquid Cooled)** | **Cold Turbo (`+15%` speed)** & **Overclock** unlocked! |
| **PUE ≤ 1.20 (and < 75°C)** | **`-10%` Compute FE** efficiency bonus! |
| **80°C – 100°C** | Thermal throttling (down to `40%` speed) + **`3×` hardware wear** |
| **100°C** | **Emergency Thermal Shutdown** until cooled below `78°C` |

## 2×2×2 Backup Generator (UPS)

Place **8× <ItemLink id="backup_generator" />** blocks in a solid **`2×2×2` cube** connected to your hall (directly or via <ItemLink id="cable" />) to add a shared **`10,000,000 FE`** Uninterruptible Power Supply (UPS) battery buffer that keeps your datacenter running through power spikes.

<GameScene zoom="5" interactive={true}>
  <Block x="0" y="0" z="0" id="hyperscale:backup_generator" />
  <Block x="1" y="0" z="0" id="hyperscale:backup_generator" />
  <Block x="0" y="0" z="1" id="hyperscale:backup_generator" />
  <Block x="1" y="0" z="1" id="hyperscale:backup_generator" />
  <Block x="0" y="1" z="0" id="hyperscale:backup_generator" />
  <Block x="1" y="1" z="0" id="hyperscale:backup_generator" />
  <Block x="0" y="1" z="1" id="hyperscale:backup_generator" />
  <Block x="1" y="1" z="1" id="hyperscale:backup_generator" />
  <Block x="2" y="0" z="1" id="hyperscale:cable" p:west="machine" p:east="machine" p:straight="x" />
  <Block x="3" y="0" z="1" id="hyperscale:switch" />
  <BlockAnnotation x="0" y="1" z="1" color="#E6C235">
    2×2×2 Backup Generator Multiblock: +10,000,000 FE shared UPS buffer for your datacenter hall.
  </BlockAnnotation>
  <IsometricCamera yaw="215" pitch="28" />
</GameScene>

## Recipes

<Row>
  <RecipeFor id="hyperscale:server_fan" />
  <RecipeFor id="hyperscale:watercooler_controller" />
  <RecipeFor id="hyperscale:backup_generator" />
</Row>
