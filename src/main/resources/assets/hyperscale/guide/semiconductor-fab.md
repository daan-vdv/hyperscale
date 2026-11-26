---
navigation:
  title: Semiconductor Fab
  icon: hyperscale:wafer_press
  position: 20
item_ids:
  - hyperscale:wafer_press
  - hyperscale:lithography_etcher
  - hyperscale:silicon
  - hyperscale:pcb
  - hyperscale:silicon_wafer
  - hyperscale:doped_wafer
  - hyperscale:quantum_wafer
  - hyperscale:mask_logic
  - hyperscale:mask_memory
  - hyperscale:mask_storage
  - hyperscale:mask_tensor
  - hyperscale:logic_die
  - hyperscale:memory_die
  - hyperscale:nand_die
  - hyperscale:tensor_die
  - hyperscale:quantum_die
---

# Semiconductor Fab

Every datacenter component in Hyperscale starts in your two-machine **Semiconductor Fab**: the <ItemLink id="wafer_press" /> and the <ItemLink id="lithography_etcher" />.

<GameScene zoom="6" interactive={true}>
  <Block x="0" y="0" z="0" id="hyperscale:wafer_press" />
  <Block x="1" y="0" z="0" id="hyperscale:lithography_etcher" />
  <BlockAnnotation x="0" y="0" z="0" color="#38BDF8">
    Wafer Press: presses Raw Silicon into Wafers and Copper + Redstone into PCBs. Requires FE power.
  </BlockAnnotation>
  <BlockAnnotation x="1" y="0" z="0" color="#C084FC">
    Lithography Etcher: etches Wafers + Catalysts using a reusable Photomask. Pipe Water for EUV dies.
  </BlockAnnotation>
  <IsometricCamera yaw="215" pitch="30" />
</GameScene>

## Step 1: Raw Silicon

Smelt or blast **Nether Quartz** to create <ItemLink id="silicon" />. Any modded silicon tagged `#c:silicon` (AE2, Mekanism, Refined Storage) works automatically in all recipes.

<RecipeFor id="hyperscale:silicon" />

## Step 2: Wafer Press

The <ItemLink id="wafer_press" /> requires **FE power** from any side and presses wafers and circuit boards.

<RecipeFor id="hyperscale:wafer_press" />

### Wafer Press Recipes

<Row>
  <RecipeFor id="hyperscale:silicon_wafer" />
  <RecipeFor id="hyperscale:pcb" />
</Row>
<Row>
  <RecipeFor id="hyperscale:doped_wafer" />
  <RecipeFor id="hyperscale:quantum_wafer" />
</Row>

## Step 3: Lithography Etcher & Photomasks

The <ItemLink id="lithography_etcher" /> uses **FE power** and a **reusable Photomask** placed in its **top purple slot** (photomasks are **never consumed**).

<RecipeFor id="hyperscale:lithography_etcher" />

### Reusable Photomasks

<Row>
  <RecipeFor id="hyperscale:mask_logic" />
  <RecipeFor id="hyperscale:mask_memory" />
</Row>
<Row>
  <RecipeFor id="hyperscale:mask_storage" />
  <RecipeFor id="hyperscale:mask_tensor" />
</Row>

### Etching Silicon Dies

Insert a Photomask, Wafer, and Catalyst. For **EUV recipes** (<ItemLink id="tensor_die" /> at `250 mB` and <ItemLink id="quantum_die" /> at `500 mB`), pipe **Water** into the Etcher or right-click it with a Water Bucket.

<Row>
  <RecipeFor id="hyperscale:logic_die" />
  <RecipeFor id="hyperscale:memory_die" />
</Row>
<Row>
  <RecipeFor id="hyperscale:nand_die" />
  <RecipeFor id="hyperscale:tensor_die" />
</Row>
<RecipeFor id="hyperscale:quantum_die" />

> **Recursive Automation Tip:** Both Fab machines expose standard item, fluid, and FE capabilities. Once you build an [Image Compiler](capturing/index.md), you can encase your own Semiconductor Fab inside `Compiler Casing` and virtualize wafer & die manufacturing right inside your datacenter!
