# Hyperscale

A datacenter hall for Minecraft 1.21.1 on NeoForge. Clip an Image Compiler onto an existing factory, prove a steady rate, and run that Image on racks. Heat and power on the interface are status words only. There is no engineer yet.

## Digitize a factory

Craft an Empty Image Card (iron, paper, redstone) and a Binder (iron, redstone, stick).

1. Place a **Recorder Input** against the start of your line, like a hopper. The picture face is outside. The other face must touch the machine or hopper you feed.
2. Place a **Recorder Output** against the end the same way.
3. Place an **Image Compiler** nearby.
4. Right-click the compiler with the Binder, then right-click each input and output. The action bar says when a terminal is bound. Sneak-right-click a terminal with the Binder to unbind.
5. Put a batch in the inputs (for example 8 iron ore, and coal through another input if the furnace needs it). Empty card in the compiler. Press Start.
6. The compiler pushes that batch and waits until the inputs are empty and the machines in the claim are back to how they started. That is run 1. Load the same batch again, press Next run. Do that three times. Take the Image.

A run is finished when every input is empty, the photographed inventories inside the claim match again, and nothing has moved for about two seconds. That is how it knows the ore finished smelting without a timer. Probe inventories are ignored for the photo.

There is no sealed room, no dye channel, and no sample clock. The Image stores what crossed the terminals in a run and how long that run took.

Break and replace old recorder blocks from earlier versions.

## Run the hall

Place a Switch. Put the other hall blocks against the switch, or against each other: Interface, input Ports, output Ports, Server Rack, and Drive Bay.

Right-click a Port to flip it between input and output. Sneak-right-click a Port to open its slot. Hoppers can insert into an input port and pull from an output port.

Right-click the Server Rack and insert a Server, a CPU, and RAM. Right-click the Drive Bay and insert Images (up to six). Holding a hall part (Image, Server, CPU, RAM, HDD) inserts it; anything else, including a full hand of junk, opens the GUI. The Binder still binds.

Put the ingredients in the input ports. The output ports receive the recorded results. One port holds one item type. Use more ports when the image has more ingredients or results. Right-click the Interface to pick which Image runs and read its I/O and runtime. Look at hall blocks for a one-line status. One server runs one selected Image at a time.

## Requirements

Java 21 JDK. Minecraft 1.21.1 runs on Java 21, and this project compiles and launches the game with a Java 21 toolchain. Gradle can start on a newer JDK. If `gradlew runClient` cannot find a JDK 21, install one or point `JAVA_HOME` at it.

This project was created from the official NeoForge 1.21.1 ModDevGradle MDK.

## Run the client

From this folder:

```bat
gradlew runClient
```

In PowerShell:

```powershell
.\gradlew.bat runClient
```

The first run downloads Gradle, Minecraft, and NeoForge, so it can take a while. The game opens with this mod loaded. Look in the Hyperscale creative tab.

## Build

```bat
gradlew build
```

The mod jar is written to `build/libs`.
