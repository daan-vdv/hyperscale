---
navigation:
  title: Calibration & Troubleshooting
  icon: hyperscale:empty_image_card
  parent: capturing/index.md
  position: 30
---

# Calibration Runs & Troubleshooting

## Running 3 Calibration Cycles

1. Right-click the <ItemLink id="recorder" /> and insert an <ItemLink id="empty_image_card" />.
2. Open your <ItemLink id="recorder_input" /> probe(s) remotely from the Compiler GUI's **Main** or **3D** tab and load your input batch.
   * *Furnace example:* `8× Raw Iron` in Input #1 (top) and `1× Coal` in Input #2 (side).
3. Click **Start**. The casing locks until all items/fluids finish moving and the chamber settles.
4. When **Run 1/3** completes, reload the **exact same input batch** and click **Next run** for Runs `2/3` and `3/3`.
5. After 3 identical runs, your compiled <ItemLink id="image" /> card is written!

> **Tip:** Why `8× Raw Iron + 1× Coal`? Because `1 Coal` smelts *exactly* `8` items with zero leftover burn time or leftover items. Always pick a batch size that leaves your interior machines completely clean!

## Common Compiler Errors & Fixes

| Status Message | Cause & Fix |
|---|---|
| **Encase the factory in Compiler Casing** | The outer shell has a gap or missing corner. Every block of the hollow box walls, floor, ceiling, and edges must be `Compiler Casing`, `Image Compiler`, or `Compiler Power Port`. |
| **Players cannot be inside the casing** | Step outside the chamber before clicking **Start**. Use the Compiler GUI's **Main** or **3D** tab to load probes from outside. |
| **Storage changed / Interior fluid changed** | An interior machine, hopper, or tank did not return to its exact starting contents (e.g., leftover fuel or a half-filled internal buffer). Adjust your batch size so every machine finishes cleanly. |
| **Interior power dropped without refill** | A powered machine inside drained its internal FE buffer and wasn't refilled to 100%. Connect a continuous FE source to a <ItemLink id="recorder_power" /> in the outer wall. |
| **That run did not match** | Run 2 or Run 3 produced different items, fluids, or energy usage than Run 1. Load the exact same input counts each run. |
| **Items only moved through** | Inputs passed straight to outputs with no transformation (e.g., a chest or bare pipe). |
| **Blocks inside the casing were modified or destroyed** | A block inside was placed, broken, or changed during sampling. |
| **Forbidden block inside** | A wireless or cross-dimensional block (e.g., Ender Chest, Quantum Entangloporter, Flux Point) is in `#hyperscale:compiler_forbidden`. Remove it from the chamber. |
