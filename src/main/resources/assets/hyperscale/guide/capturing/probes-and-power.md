---
navigation:
  title: Probes, Power & Binder
  icon: hyperscale:recorder_input
  parent: capturing/index.md
  position: 20
item_ids:
  - hyperscale:recorder_input
  - hyperscale:recorder_output
  - hyperscale:recorder_fluid_input
  - hyperscale:recorder_fluid_output
  - hyperscale:recorder_power
  - hyperscale:binder
---

# Probes, Power Ports & Binder

Probes are the **boundary** between your test batch and the factory inside the chamber. The compiler measures everything that leaves an Input Probe and everything that arrives in an Output Probe.

## Probe Orientation

* **Back face connects to your machine:** The front screen faces *away* from the machine; the **back face** pushes items/fluids into (or pulls them from) the adjacent machine.
* **Hold `Shift` while placing** to flip the facing direction.
* **Pipes work too!** Probes also support external pipes inside the chamber (such as Mekanism Logistical Transporters or Mechanical Pipes). Set the pipe on an Input Probe to **Pull** and on an Output Probe to **Push**—pipes automatically pause until a calibration run is actively sampling!

## Item Probes

* <ItemLink id="recorder_input" /> — Holds your starting input items for a run. You can place multiple Input Probes (e.g., one on top of a Furnace for ore, one on the side for fuel).
* <ItemLink id="recorder_output" /> — Collects the finished products produced by your factory.

<Row>
  <RecipeFor id="hyperscale:recorder_input" />
  <RecipeFor id="hyperscale:recorder_output" />
</Row>

## Fluid Probes

* <ItemLink id="recorder_fluid_input" /> — Supplies input fluids (e.g., Water for a Lithography Etcher or Mekanism machine). Right-click with buckets or pipe fluids in before starting.
* <ItemLink id="recorder_fluid_output" /> — Collects output fluids produced during the run.

<Row>
  <RecipeFor id="hyperscale:recorder_fluid_input" />
  <RecipeFor id="hyperscale:recorder_fluid_output" />
</Row>

## Compiler Power Port

* <ItemLink id="recorder_power" /> — Placed directly in the **outer wall** of your Compiler Casing chamber.
* Feed FE power into the **outside** face. The port meters exact FE consumption and delivers it into your interior machine or power cable.
* **Important:** Make sure your external FE source can keep interior machines topped back up to their starting energy level before the run settles!

<RecipeFor id="hyperscale:recorder_power" />

## Binder

Probes placed inside a sealed chamber are detected automatically, or you can use the <ItemLink id="binder" /> to manually link/unlink probes: right-click the **Image Compiler**, then right-click each probe.

<RecipeFor id="hyperscale:binder" />
