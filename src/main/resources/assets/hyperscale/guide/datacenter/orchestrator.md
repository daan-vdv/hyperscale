---
navigation:
  title: Orchestrator & Containers
  icon: hyperscale:interface
  parent: datacenter/index.md
  position: 10
item_ids:
  - hyperscale:interface
---

# Hyperscale Orchestrator (Interface)

The <ItemLink id="interface" /> is a 2-block-wide workstation that opens the **Hyperscale Orchestrator**—your control panel for deploying compiled images, scaling replicas, monitoring live telemetry, and browsing Drive Bay storage.

<RecipeFor id="hyperscale:interface" />

## Deploying & Scaling Containers

1. Insert compiled <ItemLink id="image" /> cards into an <ItemLink id="image_registry" /> (or <ItemLink id="switch" />).
2. Open the <ItemLink id="interface" /> and click **`+`** next to any Image to deploy a container.
3. Scale replicas up or down with **`+` / `-`**:
   * Normal click: **±1 replica**
   * **`Shift` + click**: **±5 replicas**
   * **`Ctrl` + click**: **±10 replicas**
4. Multiple different images can run in parallel inside the same hall—each replica consumes vCPU, RAM, FE power, and generates heat.

## Container Power Profiles

Each running container can be toggled between three profiles:

| Profile | Speed | Power & Heat | Requirement |
|---|---|---|---|
| **Eco** | Slower | Lowest FE & heat | Always available |
| **Balanced** | `1.0×` | Standard FE & heat | Always available |
| **Overclock** | **`1.5×`** | Higher FE & heat | Hall temperature **≤ 55°C** |

## Live Telemetry & Storage Tab

* The top bar shows live **RAM**, **vCPU**, **GPU**, **Storage**, **FE/t Power**, **Temperature (°C)**, **PUE**, and **S.M.A.R.T.** hardware wear alerts.
* Switch to the **Storage** tab to browse all items stored across your hall's Drive Bays (left-click to take a full stack, right-click to take 1 item).
