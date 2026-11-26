---
navigation:
  title: Sealed Test Chamber
  icon: hyperscale:recorder_casing
  parent: capturing/index.md
  position: 10
item_ids:
  - hyperscale:recorder
  - hyperscale:recorder_casing
  - hyperscale:empty_image_card
  - hyperscale:image
---

# Building a Sealed Test Chamber

To prevent exploits and guarantee accurate measurements, the <ItemLink id="recorder" /> requires your factory to be completely enclosed inside a **hollow rectangular box** made of <ItemLink id="recorder_casing" />.

## Chamber Rules

* **Size:** Minimum `3×3×3` outer dimensions (1×1×1 interior), maximum `32×32×32`.
* **Complete Shell:** Every single block on all 6 outer faces—including edges and corners—must be <ItemLink id="recorder_casing" />, <ItemLink id="recorder" />, or <ItemLink id="recorder_power" />.
* **Compiler Placement:** Place the <ItemLink id="recorder" /> anywhere in an outer wall, **facing outward** (away from the interior).
* **Empty of Players:** You cannot stand inside the chamber when clicking **Start**. You can open interior probes remotely from the Compiler GUI's **Main** or **3D** tab!

## Example 5×3×5 Chamber (Cutaway View)

The interactive scene below shows a simple **Furnace** setup inside a Compiler Casing chamber (front wall removed so you can see inside—in-game, seal the front wall completely!):

<GameScene zoom="5" interactive={true}>
  {/* Floor 5x5 */}
  <Block x="0" y="0" z="0" id="hyperscale:recorder_casing" />
  <Block x="1" y="0" z="0" id="hyperscale:recorder_casing" />
  <Block x="2" y="0" z="0" id="hyperscale:recorder_casing" />
  <Block x="3" y="0" z="0" id="hyperscale:recorder_casing" />
  <Block x="4" y="0" z="0" id="hyperscale:recorder_casing" />
  <Block x="0" y="0" z="1" id="hyperscale:recorder_casing" />
  <Block x="1" y="0" z="1" id="hyperscale:recorder_casing" />
  <Block x="2" y="0" z="1" id="hyperscale:recorder_casing" />
  <Block x="3" y="0" z="1" id="hyperscale:recorder_casing" />
  <Block x="4" y="0" z="1" id="hyperscale:recorder_casing" />
  <Block x="0" y="0" z="2" id="hyperscale:recorder_casing" />
  <Block x="1" y="0" z="2" id="hyperscale:recorder_casing" />
  <Block x="2" y="0" z="2" id="hyperscale:recorder_casing" />
  <Block x="3" y="0" z="2" id="hyperscale:recorder_casing" />
  <Block x="4" y="0" z="2" id="hyperscale:recorder_casing" />
  <Block x="0" y="0" z="3" id="hyperscale:recorder_casing" />
  <Block x="1" y="0" z="3" id="hyperscale:recorder_casing" />
  <Block x="2" y="0" z="3" id="hyperscale:recorder_casing" />
  <Block x="3" y="0" z="3" id="hyperscale:recorder_casing" />
  <Block x="4" y="0" z="3" id="hyperscale:recorder_casing" />
  <Block x="0" y="0" z="4" id="hyperscale:recorder_casing" />
  <Block x="1" y="0" z="4" id="hyperscale:recorder_casing" />
  <Block x="2" y="0" z="4" id="hyperscale:recorder_casing" />
  <Block x="3" y="0" z="4" id="hyperscale:recorder_casing" />
  <Block x="4" y="0" z="4" id="hyperscale:recorder_casing" />

  {/* Back wall & side walls */}
  <Block x="0" y="1" z="0" id="hyperscale:recorder_casing" />
  <Block x="1" y="1" z="0" id="hyperscale:recorder_casing" />
  <Block x="2" y="1" z="0" id="hyperscale:recorder_power" />
  <Block x="3" y="1" z="0" id="hyperscale:recorder_casing" />
  <Block x="4" y="1" z="0" id="hyperscale:recorder_casing" />
  <Block x="0" y="2" z="0" id="hyperscale:recorder_casing" />
  <Block x="1" y="2" z="0" id="hyperscale:recorder_casing" />
  <Block x="2" y="2" z="0" id="hyperscale:recorder_casing" />
  <Block x="3" y="2" z="0" id="hyperscale:recorder_casing" />
  <Block x="4" y="2" z="0" id="hyperscale:recorder_casing" />

  <Block x="0" y="1" z="1" id="hyperscale:recorder_casing" />
  <Block x="0" y="1" z="2" id="hyperscale:recorder_casing" />
  <Block x="0" y="1" z="3" id="hyperscale:recorder_casing" />
  <Block x="0" y="1" z="4" id="hyperscale:recorder_casing" />
  <Block x="0" y="2" z="1" id="hyperscale:recorder_casing" />
  <Block x="0" y="2" z="2" id="hyperscale:recorder_casing" />
  <Block x="0" y="2" z="3" id="hyperscale:recorder_casing" />
  <Block x="0" y="2" z="4" id="hyperscale:recorder_casing" />

  {/* Front wall partial with Compiler */}
  <Block x="2" y="1" z="4" id="hyperscale:recorder" />

  {/* Interior Factory: Furnace + Input/Output Probes */}
  <Block x="2" y="1" z="2" id="minecraft:furnace" p:facing="south" />
  <Block x="2" y="2" z="2" id="hyperscale:recorder_input" p:facing="up" />
  <Block x="1" y="1" z="2" id="hyperscale:recorder_input" p:facing="west" />
  <Block x="3" y="1" z="2" id="hyperscale:recorder_output" p:facing="east" />

  <BlockAnnotation x="2" y="1" z="4" color="#4B6A9B">
    Image Compiler: sits in the outer wall facing outward. Insert an Empty Image Card here.
  </BlockAnnotation>
  <BlockAnnotation x="2" y="2" z="2" color="#3CA66B">
    Recorder Input #1 (Top): back face points DOWN into the furnace top slot for Raw Iron.
  </BlockAnnotation>
  <BlockAnnotation x="1" y="1" z="2" color="#3CA66B">
    Recorder Input #2 (Side): back face points EAST into the furnace fuel slot for Coal.
  </BlockAnnotation>
  <BlockAnnotation x="3" y="1" z="2" color="#D9822B">
    Recorder Output: back face points WEST to pull smelted Iron Ingots out of the furnace.
  </BlockAnnotation>
  <BlockAnnotation x="2" y="1" z="0" color="#E6C235">
    Compiler Power Port (optional): supplies FE power from outside into powered interior machines.
  </BlockAnnotation>
  <IsometricCamera yaw="210" pitch="30" />
</GameScene>

## Crafting Recipes

<Row>
  <RecipeFor id="hyperscale:recorder" />
  <RecipeFor id="hyperscale:recorder_casing" />
</Row>
<RecipeFor id="hyperscale:empty_image_card" />
