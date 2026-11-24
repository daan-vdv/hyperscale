package dev.daan.hyperscale.blockentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.daan.hyperscale.HyperscaleConfig;
import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.block.RecorderBlock;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import dev.daan.hyperscale.record.ClaimScanner;
import dev.daan.hyperscale.record.ClaimSnapshot;
import dev.daan.hyperscale.record.ItemTotals;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.record.RecordStatus;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class RecorderBlockEntity extends InventoryBlockEntity {
    private static final int SNAPSHOT_INTERVAL = 4;
    private static final Set<RecorderBlockEntity> SAMPLING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private RecordPhase phase = RecordPhase.IDLE;
    private RecordStatus status = RecordStatus.IDLE;
    private String detail = "";
    private int matched;
    private int tickSum;
    private long energySum;
    private long patternEnergy = -1L;
    private long cycleEnergy;
    private int runTicks;
    private int settleTicks;
    private int driftTicks;
    private boolean awaitingContinue;
    private boolean resumed;
    private int width;
    private int height;
    private int depth;
    private int inventoryCount;
    private boolean hasBox;
    private BlockPos outerMin = BlockPos.ZERO;
    private BlockPos outerMax = BlockPos.ZERO;
    private BlockPos innerMin = BlockPos.ZERO;
    private BlockPos innerMax = BlockPos.ZERO;
    private final List<BlockPos> inputs = new ArrayList<>();
    private final List<BlockPos> outputs = new ArrayList<>();
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    private final List<BlockPos> fluidOutputs = new ArrayList<>();
    private final List<BlockPos> powerPorts = new ArrayList<>();
    private final Set<BlockPos> inventories = new HashSet<>();
    private final Set<BlockPos> energyBlocks = new HashSet<>();
    private final Set<BlockPos> fluidBlocks = new HashSet<>();
    private ClaimSnapshot baselineSnapshot = ClaimSnapshot.EMPTY;
    private final Map<ResourceLocation, Integer> patternInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> patternOutputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleOutputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> patternFluidInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> patternFluidOutputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleFluidInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleFluidOutputs = new HashMap<>();

    public RecorderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECORDER.get(), pos, state, 1);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ModItems.EMPTY_IMAGE_CARD.get());
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    public static void serverTick(RecorderBlockEntity recorder) {
        recorder.tickServer();
    }

    public static void clearSamplingFor(Level level) {
        SAMPLING.removeIf(recorder -> recorder.getLevel() == level);
    }

    public static void clearAllSampling() {
        SAMPLING.clear();
    }

    public boolean isLearning() {
        return this.phase == RecordPhase.LEARNING;
    }

    public boolean isSampling() {
        return this.phase == RecordPhase.LEARNING && !this.awaitingContinue;
    }

    public static boolean blocksPlayerAccess(Level level, BlockPos pos) {
        for (RecorderBlockEntity recorder : SAMPLING) {
            if (recorder.getLevel() == level && recorder.blocksAccess(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean isClaimInventory(BlockPos pos) {
        return this.isSampling() && this.inventories.contains(pos);
    }

    public boolean isClaimFluidBlock(BlockPos pos) {
        return this.isSampling() && this.fluidBlocks.contains(pos);
    }

    public boolean inOuterBox(BlockPos pos) {
        if (!this.hasBox) {
            return false;
        }
        return pos.getX() >= this.outerMin.getX() && pos.getX() <= this.outerMax.getX()
                && pos.getY() >= this.outerMin.getY() && pos.getY() <= this.outerMax.getY()
                && pos.getZ() >= this.outerMin.getZ() && pos.getZ() <= this.outerMax.getZ();
    }

    /** Sealed during sampling: entire casing shell + interior + bound probes, but not the compiler itself. */
    public boolean blocksAccess(BlockPos pos) {
        if (!this.isSampling() || pos.equals(this.worldPosition)) {
            return false;
        }
        if (this.inputs.contains(pos) || this.outputs.contains(pos)
                || this.fluidInputs.contains(pos) || this.fluidOutputs.contains(pos)) {
            return true;
        }
        return this.inOuterBox(pos);
    }

    private void refreshSamplingRegistration() {
        if (this.isSampling()) {
            SAMPLING.add(this);
        } else {
            SAMPLING.remove(this);
        }
    }

    @Override
    public void setRemoved() {
        SAMPLING.remove(this);
        super.setRemoved();
    }

    public boolean awaitingContinue() {
        return this.awaitingContinue;
    }

    public RecordPhase phase() {
        return this.phase;
    }

    public RecordStatus status() {
        return this.status;
    }

    public String detail() {
        return this.detail;
    }

    public int matched() {
        return this.matched;
    }

    public int boundInputs() {
        return this.inputs.size() + this.fluidInputs.size();
    }

    public int boundOutputs() {
        return this.outputs.size() + this.fluidOutputs.size();
    }

    public List<BlockPos> inputPositions() {
        return Collections.unmodifiableList(this.inputs);
    }

    public List<BlockPos> outputPositions() {
        return Collections.unmodifiableList(this.outputs);
    }

    public boolean hasBox() {
        return this.hasBox;
    }

    public BlockPos innerMin() {
        return this.innerMin;
    }

    public BlockPos innerMax() {
        return this.innerMax;
    }

    public static Component describeProbeTarget(Level level, BlockPos probePos) {
        if (level == null || probePos == null || !level.isLoaded(probePos)) {
            return Component.translatable("gui.hyperscale.recorder.probe_none");
        }
        BlockState probeState = level.getBlockState(probePos);
        if (!probeState.hasProperty(ProbeBlock.FACING)) {
            return Component.translatable("gui.hyperscale.recorder.probe_none");
        }
        Direction facing = probeState.getValue(ProbeBlock.FACING);
        BlockPos targetPos = probePos.relative(facing.getOpposite());
        Component blockName = level.isLoaded(targetPos)
                ? level.getBlockState(targetPos).getBlock().getName()
                : Component.translatable("gui.hyperscale.recorder.probe_none");
        Component faceName = Component.translatable("gui.hyperscale.face." + facing.getSerializedName());
        return Component.translatable("gui.hyperscale.recorder.probe_target", blockName, faceName);
    }

    public int data(int index) {
        return switch (index) {
            case 0 -> this.phase.ordinal();
            case 1 -> this.awaitingContinue ? 1 : 0;
            case 2 -> this.matched;
            case 3 -> this.inventoryCount;
            case 4 -> this.width;
            case 5 -> this.height;
            case 6 -> this.depth;
            case 7 -> this.status.ordinal();
            case 8 -> Math.min(Short.MAX_VALUE, this.runTicks);
            case 9 -> this.settleTicks;
            case 10 -> this.inputs.size();
            case 11 -> this.outputs.size();
            default -> 0;
        };
    }

    /**
     * Scans the sealed casing room (when not actively sampling) so the Compiler GUI knows which
     * interior Input/Output probes and wall Power Ports are attached.
     */
    public void scanSealedRoom() {
        if (this.level == null || this.level.isClientSide || this.isSampling()) {
            return;
        }
        Direction facing = this.getBlockState().getValue(RecorderBlock.FACING);
        ClaimScanner.SealedResult result = ClaimScanner.buildSealed(this.level, this.worldPosition, facing);
        if (result.claim() == null) {
            return;
        }
        this.applySealedClaim(result.claim());
        this.sync();
    }

    public boolean openRemoteProbe(ServerPlayer player, boolean input, int index) {
        if (this.level == null || this.level.isClientSide || this.isSampling()) {
            return false;
        }
        List<BlockPos> list = input ? this.inputs : this.outputs;
        if (index < 0 || index >= list.size()) {
            return false;
        }
        BlockPos probePos = list.get(index);
        if (!this.level.isLoaded(probePos) || !(this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe)) {
            return false;
        }
        Component prefix = Component.translatable(
                input ? "gui.hyperscale.recorder.probe_input" : "gui.hyperscale.recorder.probe_output",
                index + 1
        );
        Component title = Component.translatable(
                "gui.hyperscale.recorder.probe_title",
                prefix,
                describeProbeTarget(this.level, probePos)
        );
        BlockPos compilerPos = this.worldPosition;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new StorageMenu(containerId, inventory, probePos, compilerPos, probe.items(), StorageKind.PROBE),
                title
        ), buffer -> {
            buffer.writeBlockPos(probePos);
            buffer.writeBlockPos(compilerPos);
        });
        return true;
    }

    private void applySealedClaim(ClaimScanner.Claim claim) {
        this.unbindAllProbes();
        this.inventories.clear();
        this.inventories.addAll(claim.inventories());
        this.energyBlocks.clear();
        this.energyBlocks.addAll(claim.energyBlocks());
        this.fluidBlocks.clear();
        this.fluidBlocks.addAll(claim.fluidBlocks());
        this.width = claim.width();
        this.height = claim.height();
        this.depth = claim.depth();
        this.inventoryCount = this.inventories.size();
        this.hasBox = true;
        this.outerMin = claim.outerMin();
        this.outerMax = claim.outerMax();
        this.innerMin = claim.innerMin();
        this.innerMax = claim.innerMax();

        for (BlockPos pos : claim.inputs()) {
            this.bindInternal(pos, ProbeMode.INPUT);
        }
        for (BlockPos pos : claim.outputs()) {
            this.bindInternal(pos, ProbeMode.OUTPUT);
        }
        for (BlockPos pos : claim.fluidInputs()) {
            this.bindFluidInternal(pos, ProbeMode.INPUT);
        }
        for (BlockPos pos : claim.fluidOutputs()) {
            this.bindFluidInternal(pos, ProbeMode.OUTPUT);
        }
        for (BlockPos pos : claim.powerPorts()) {
            if (this.level != null && this.level.isLoaded(pos)
                    && this.level.getBlockEntity(pos) instanceof PowerPortBlockEntity powerPort) {
                powerPort.setCompiler(this.worldPosition);
                this.powerPorts.add(pos.immutable());
            }
        }
    }

    private void bindInternal(BlockPos probePos, ProbeMode expectedMode) {
        if (this.level == null || !this.level.isLoaded(probePos)
                || !(this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe)
                || probe.mode() != expectedMode) {
            return;
        }
        if (probe.compiler() != null && !probe.compiler().equals(this.worldPosition)) {
            if (this.level.isLoaded(probe.compiler())
                    && this.level.getBlockEntity(probe.compiler()) instanceof RecorderBlockEntity other) {
                other.unbindProbe(probePos);
            }
        }
        if (expectedMode == ProbeMode.INPUT) {
            this.inputs.add(probePos.immutable());
        } else {
            this.outputs.add(probePos.immutable());
        }
        probe.setCompiler(this.worldPosition);
    }

    private void bindFluidInternal(BlockPos probePos, ProbeMode expectedMode) {
        if (this.level == null || !this.level.isLoaded(probePos)
                || !(this.level.getBlockEntity(probePos) instanceof FluidProbeBlockEntity probe)
                || probe.mode() != expectedMode) {
            return;
        }
        if (probe.compiler() != null && !probe.compiler().equals(this.worldPosition)) {
            if (this.level.isLoaded(probe.compiler())
                    && this.level.getBlockEntity(probe.compiler()) instanceof RecorderBlockEntity other) {
                other.unbindFluidProbe(probePos);
            }
        }
        if (expectedMode == ProbeMode.INPUT) {
            this.fluidInputs.add(probePos.immutable());
        } else {
            this.fluidOutputs.add(probePos.immutable());
        }
        probe.setCompiler(this.worldPosition);
    }

    public void unbindFluidProbe(BlockPos probePos) {
        this.fluidInputs.remove(probePos);
        this.fluidOutputs.remove(probePos);
        if (this.level != null && this.level.isLoaded(probePos)
                && this.level.getBlockEntity(probePos) instanceof FluidProbeBlockEntity probe
                && this.worldPosition.equals(probe.compiler())) {
            probe.clearCompiler();
        }
        if (this.phase == RecordPhase.LEARNING) {
            this.fail(RecordStatus.LOST_BIND);
        } else {
            this.sync();
        }
    }

    public boolean bindProbe(BlockPos probePos) {
        if (this.level == null || this.phase == RecordPhase.LEARNING || !this.level.isLoaded(probePos)) {
            return false;
        }
        if (!(this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe)) {
            return false;
        }
        if (probe.compiler() != null && !probe.compiler().equals(this.worldPosition)) {
            if (this.level.isLoaded(probe.compiler())
                    && this.level.getBlockEntity(probe.compiler()) instanceof RecorderBlockEntity other) {
                other.unbindProbe(probePos);
            }
        }
        this.inputs.remove(probePos);
        this.outputs.remove(probePos);
        if (probe.mode() == ProbeMode.INPUT) {
            this.inputs.add(probePos.immutable());
        } else {
            this.outputs.add(probePos.immutable());
        }
        probe.setCompiler(this.worldPosition);
        this.sync();
        return true;
    }

    public void unbindProbe(BlockPos probePos) {
        this.inputs.remove(probePos);
        this.outputs.remove(probePos);
        if (this.level != null && this.level.isLoaded(probePos)
                && this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe
                && this.worldPosition.equals(probe.compiler())) {
            probe.clearCompiler();
        }
        if (this.phase == RecordPhase.LEARNING) {
            this.fail(RecordStatus.LOST_BIND);
        } else {
            this.sync();
        }
    }

    private void unbindAllProbes() {
        if (this.level != null) {
            for (BlockPos pos : this.inputs) {
                this.clearProbeCompiler(pos);
            }
            for (BlockPos pos : this.outputs) {
                this.clearProbeCompiler(pos);
            }
            for (BlockPos pos : this.fluidInputs) {
                this.clearFluidProbeCompiler(pos);
            }
            for (BlockPos pos : this.fluidOutputs) {
                this.clearFluidProbeCompiler(pos);
            }
            for (BlockPos pos : this.powerPorts) {
                if (this.level.isLoaded(pos)
                        && this.level.getBlockEntity(pos) instanceof PowerPortBlockEntity powerPort
                        && this.worldPosition.equals(powerPort.compiler())) {
                    powerPort.clearCompiler();
                }
            }
        }
        this.inputs.clear();
        this.outputs.clear();
        this.fluidInputs.clear();
        this.fluidOutputs.clear();
        this.powerPorts.clear();
    }

    private void clearFluidProbeCompiler(BlockPos pos) {
        if (this.level != null && this.level.isLoaded(pos)
                && this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe
                && this.worldPosition.equals(probe.compiler())) {
            probe.clearCompiler();
        }
    }

    private void clearProbeCompiler(BlockPos pos) {
        if (this.level != null && this.level.isLoaded(pos)
                && this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe
                && this.worldPosition.equals(probe.compiler())) {
            probe.clearCompiler();
        }
    }

    public void stop() {
        if (this.phase == RecordPhase.IDLE && this.status == RecordStatus.IDLE) {
            return;
        }
        this.awaitingContinue = false;
        this.clearCycle();
        this.phase = RecordPhase.IDLE;
        this.refreshSamplingRegistration();
        this.setStatus(RecordStatus.IDLE, "");
    }

    public void recordTransfer(boolean input, ItemStack stack) {
        if (!this.isSampling()) {
            return;
        }
        ItemTotals.add(input ? this.cycleInputs : this.cycleOutputs, stack);
        this.settleTicks = 0;
        this.driftTicks = 0;
    }

    public void recordFluidTransfer(boolean input, FluidStack stack) {
        if (!this.isSampling() || stack.isEmpty()) {
            return;
        }
        ItemTotals.addFluid(input ? this.cycleFluidInputs : this.cycleFluidOutputs, stack);
        this.settleTicks = 0;
        this.driftTicks = 0;
    }

    public void recordEnergy(int amount) {
        if (!this.isSampling() || amount <= 0) {
            return;
        }
        this.cycleEnergy += amount;
        this.settleTicks = 0;
        this.driftTicks = 0;
    }

    public void startOrContinue() {
        if (this.phase == RecordPhase.LEARNING && this.awaitingContinue) {
            this.beginRun();
            return;
        }
        this.start();
    }

    public void start() {
        if (this.level == null || this.phase == RecordPhase.LEARNING) {
            return;
        }
        ItemStack card = this.items.getStackInSlot(0);
        if (card.isEmpty()) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.NEED_CARD, "");
            return;
        }
        if (!card.is(ModItems.EMPTY_IMAGE_CARD.get())) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.NEED_EMPTY, "");
            return;
        }

        Direction facing = this.getBlockState().getValue(RecorderBlock.FACING);
        ClaimScanner.SealedResult sealed = ClaimScanner.buildSealed(this.level, this.worldPosition, facing);
        if (sealed.claim() == null) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(sealed.error() != null ? sealed.error() : RecordStatus.NO_CASING, sealed.detail());
            return;
        }

        this.applySealedClaim(sealed.claim());
        if (this.boundInputs() == 0 || this.boundOutputs() == 0) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.NEED_BIND, "");
            return;
        }
        if (!this.inputsHaveItems()) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.NEED_ITEMS, "");
            return;
        }

        this.patternInputs.clear();
        this.patternOutputs.clear();
        this.patternFluidInputs.clear();
        this.patternFluidOutputs.clear();
        this.matched = 0;
        this.tickSum = 0;
        this.energySum = 0L;
        this.patternEnergy = -1L;
        this.phase = RecordPhase.LEARNING;
        this.beginRun();
    }

    private void beginRun() {
        if (this.level == null || this.phase != RecordPhase.LEARNING) {
            return;
        }
        if (!this.hasBox || !ClaimScanner.isShellIntact(this.level, this.outerMin, this.outerMax, this.worldPosition)) {
            this.fail(RecordStatus.SEAL_BROKEN);
            return;
        }
        if (ClaimScanner.hasPlayerInside(this.level, this.innerMin, this.innerMax)) {
            this.fail(RecordStatus.PLAYER_INSIDE);
            return;
        }
        if (ClaimScanner.hasContainerEntityInside(this.level, this.innerMin, this.innerMax)) {
            this.fail(RecordStatus.ENTITY_INSIDE);
            return;
        }
        if (!this.refreshBinds()) {
            this.fail(RecordStatus.LOST_BIND);
            return;
        }
        if (!this.inputsHaveItems()) {
            this.awaitingContinue = true;
            this.setStatus(RecordStatus.NEED_ITEMS, "");
            return;
        }
        this.resetProbeCooldowns();
        this.baselineSnapshot = ClaimSnapshot.capture(this.level, this.inventories, this.energyBlocks, this.fluidBlocks, this.innerMin, this.innerMax);
        this.clearCycle();
        this.awaitingContinue = false;
        this.runTicks = 0;
        this.settleTicks = 0;
        this.driftTicks = 0;
        this.refreshSamplingRegistration();
        this.setStatus(RecordStatus.SAMPLING, "");
        this.drawCorners();
    }

    private void resetProbeCooldowns() {
        if (this.level == null) {
            return;
        }
        for (BlockPos pos : this.inputs) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe) {
                probe.resetCooldown();
            }
        }
        for (BlockPos pos : this.outputs) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe) {
                probe.resetCooldown();
            }
        }
        for (BlockPos pos : this.fluidInputs) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe) {
                probe.resetCooldown();
            }
        }
        for (BlockPos pos : this.fluidOutputs) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe) {
                probe.resetCooldown();
            }
        }
    }

    private void tickServer() {
        if (this.level == null) {
            return;
        }
        if (!this.resumed) {
            this.resumed = true;
            if (this.phase == RecordPhase.LEARNING && !this.awaitingContinue) {
                this.awaitingContinue = false;
                this.clearCycle();
                this.phase = RecordPhase.IDLE;
                this.setStatus(RecordStatus.INTERRUPTED, "");
            }
        }
        if (this.phase != RecordPhase.LEARNING || this.awaitingContinue) {
            return;
        }
        if (this.runTicks % 10 == 0) {
            if (!this.hasBox || !ClaimScanner.isShellIntact(this.level, this.outerMin, this.outerMax, this.worldPosition)) {
                this.fail(RecordStatus.SEAL_BROKEN);
                return;
            }
            if (ClaimScanner.hasPlayerInside(this.level, this.innerMin, this.innerMax)) {
                this.fail(RecordStatus.PLAYER_INSIDE);
                return;
            }
            if (ClaimScanner.hasContainerEntityInside(this.level, this.innerMin, this.innerMax)) {
                this.fail(RecordStatus.ENTITY_INSIDE);
                return;
            }
            if (!this.refreshBinds()) {
                this.fail(RecordStatus.LOST_BIND);
                return;
            }
        }
        this.runTicks++;
        if (this.runTicks % SNAPSHOT_INTERVAL == 0) {
            if (this.inputsEmpty()) {
                ClaimSnapshot now = ClaimSnapshot.capture(this.level, this.inventories, this.energyBlocks, this.fluidBlocks, this.innerMin, this.innerMax);
                ClaimSnapshot.Diff diff = ClaimSnapshot.diff(this.baselineSnapshot, now, HyperscaleConfig.ENERGY_DRIFT_TOLERANCE.get());
                if (diff == null) {
                    this.driftTicks = 0;
                    this.settleTicks += SNAPSHOT_INTERVAL;
                    if (this.settleTicks >= RecordLimits.SETTLE_TICKS) {
                        this.closeRun();
                        return;
                    }
                } else {
                    this.settleTicks = 0;
                    this.driftTicks += SNAPSHOT_INTERVAL;
                    if (this.driftTicks >= RecordLimits.DRIFT_TIMEOUT_TICKS) {
                        this.fail(diff.status(), diff.detail());
                        return;
                    }
                }
            } else {
                this.settleTicks = 0;
                this.driftTicks = 0;
            }
        }
    }

    private void closeRun() {
        if (!this.refreshBinds()) {
            this.fail(RecordStatus.LOST_BIND);
            return;
        }
        ClaimSnapshot now = ClaimSnapshot.capture(this.level, this.inventories, this.energyBlocks, this.fluidBlocks, this.innerMin, this.innerMax);
        ClaimSnapshot.Diff diff = ClaimSnapshot.diff(this.baselineSnapshot, now, HyperscaleConfig.ENERGY_DRIFT_TOLERANCE.get());
        if (diff != null) {
            this.fail(diff.status(), diff.detail());
            return;
        }
        boolean anyInput = !this.cycleInputs.isEmpty() || !this.cycleFluidInputs.isEmpty();
        boolean anyOutput = !this.cycleOutputs.isEmpty() || !this.cycleFluidOutputs.isEmpty();
        if (!anyInput || !anyOutput) {
            this.fail(RecordStatus.EMPTY);
            return;
        }
        if (ItemTotals.same(this.cycleInputs, this.cycleOutputs)
                && ItemTotals.same(this.cycleFluidInputs, this.cycleFluidOutputs)) {
            this.fail(RecordStatus.PASSTHROUGH);
            return;
        }
        if (this.patternInputs.isEmpty() && this.patternFluidInputs.isEmpty()) {
            this.patternInputs.putAll(this.cycleInputs);
            this.patternOutputs.putAll(this.cycleOutputs);
            this.patternFluidInputs.putAll(this.cycleFluidInputs);
            this.patternFluidOutputs.putAll(this.cycleFluidOutputs);
            this.patternEnergy = this.cycleEnergy;
        } else {
            if (!ItemTotals.same(this.patternInputs, this.cycleInputs)
                    || !ItemTotals.same(this.patternOutputs, this.cycleOutputs)
                    || !ItemTotals.same(this.patternFluidInputs, this.cycleFluidInputs)
                    || !ItemTotals.same(this.patternFluidOutputs, this.cycleFluidOutputs)) {
                this.fail(RecordStatus.MISMATCH);
                return;
            }
            long allowedDelta = Math.max(200L, this.patternEnergy / 10L);
            if (Math.abs(this.cycleEnergy - this.patternEnergy) > allowedDelta) {
                this.fail(RecordStatus.ENERGY_MISMATCH);
                return;
            }
        }
        int actualTicks = Math.max(1, this.runTicks - this.settleTicks);
        this.tickSum += actualTicks;
        this.energySum += this.cycleEnergy;
        this.matched++;
        this.clearCycle();
        if (this.matched >= RecordLimits.CYCLES) {
            this.finish();
            return;
        }
        this.awaitingContinue = true;
        this.refreshSamplingRegistration();
        this.setStatus(RecordStatus.RUN_DONE, "");
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }

    private void finish() {
        ItemStack card = this.items.getStackInSlot(0);
        if (!card.is(ModItems.EMPTY_IMAGE_CARD.get())) {
            this.fail(RecordStatus.NEED_CARD);
            return;
        }
        int ticks = Math.max(1, this.tickSum / RecordLimits.CYCLES);
        int avgEnergy = (int) Math.min(Integer.MAX_VALUE, this.energySum / RecordLimits.CYCLES);
        ItemStack image = new ItemStack(ModItems.IMAGE.get());
        int machineCount = Math.max(1, Math.max(this.inventoryCount, Math.max(this.energyBlocks.size(), this.fluidBlocks.size())));
        image.set(ModDataComponents.PROCESS_IMAGE.get(), new ProcessImage(
                ItemTotals.freeze(this.patternInputs),
                ItemTotals.freeze(this.patternOutputs),
                ticks,
                avgEnergy,
                machineCount,
                ItemTotals.freezeFluids(this.patternFluidInputs),
                ItemTotals.freezeFluids(this.patternFluidOutputs)
        ));
        this.items.setStackInSlot(0, image);
        this.awaitingContinue = false;
        this.phase = RecordPhase.DONE;
        this.setStatus(RecordStatus.DONE, "");
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.1F);
            this.drawCorners();
        }
    }

    private boolean inputsHaveItems() {
        for (BlockPos pos : this.inputs) {
            if (!this.level.isLoaded(pos) || !(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)) {
                continue;
            }
            for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                if (!probe.items().getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        for (BlockPos pos : this.fluidInputs) {
            if (this.level.isLoaded(pos)
                    && this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe
                    && !probe.tank().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean inputsEmpty() {
        for (BlockPos pos : this.inputs) {
            if (!this.level.isLoaded(pos) || !(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)) {
                continue;
            }
            for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                if (!probe.items().getStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
        }
        for (BlockPos pos : this.fluidInputs) {
            if (this.level.isLoaded(pos)
                    && this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe
                    && !probe.tank().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean refreshBinds() {
        if (this.level == null) {
            return false;
        }
        boolean inputsOk = this.refreshList(this.inputs, ProbeMode.INPUT);
        boolean outputsOk = this.refreshList(this.outputs, ProbeMode.OUTPUT);
        boolean fluidInputsOk = this.refreshFluidList(this.fluidInputs, ProbeMode.INPUT);
        boolean fluidOutputsOk = this.refreshFluidList(this.fluidOutputs, ProbeMode.OUTPUT);
        return inputsOk && outputsOk && fluidInputsOk && fluidOutputsOk;
    }

    private boolean refreshFluidList(List<BlockPos> list, ProbeMode mode) {
        boolean allValid = true;
        Iterator<BlockPos> iterator = list.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!this.level.isLoaded(pos)
                    || !(this.level.getBlockEntity(pos) instanceof FluidProbeBlockEntity probe)
                    || probe.mode() != mode
                    || !this.worldPosition.equals(probe.compiler())) {
                iterator.remove();
                allValid = false;
            }
        }
        return allValid;
    }

    private boolean refreshList(List<BlockPos> list, ProbeMode mode) {
        boolean allValid = true;
        Iterator<BlockPos> iterator = list.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!this.level.isLoaded(pos)
                    || !(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)
                    || probe.mode() != mode
                    || !this.worldPosition.equals(probe.compiler())) {
                iterator.remove();
                allValid = false;
            }
        }
        return allValid;
    }

    private void fail(RecordStatus status) {
        this.fail(status, "");
    }

    private void fail(RecordStatus status, String detail) {
        this.awaitingContinue = false;
        this.clearCycle();
        this.phase = RecordPhase.FAILED;
        this.setStatus(status, detail);
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    private void clearCycle() {
        this.cycleInputs.clear();
        this.cycleOutputs.clear();
        this.cycleFluidInputs.clear();
        this.cycleFluidOutputs.clear();
        this.cycleEnergy = 0L;
        this.runTicks = 0;
        this.settleTicks = 0;
        this.driftTicks = 0;
    }

    private void setStatus(RecordStatus status, String detail) {
        this.status = status;
        this.detail = detail == null ? "" : detail;
        this.refreshSamplingRegistration();
        this.sync();
    }

    private void drawCorners() {
        if (!(this.level instanceof ServerLevel server) || !this.hasBox) {
            return;
        }
        double x0 = this.outerMin.getX();
        double y0 = this.outerMin.getY();
        double z0 = this.outerMin.getZ();
        double x1 = this.outerMax.getX() + 1.0D;
        double y1 = this.outerMax.getY() + 1.0D;
        double z1 = this.outerMax.getZ() + 1.0D;
        double[] xs = {x0, x1};
        double[] ys = {y0, y1};
        double[] zs = {z0, z1};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    server.sendParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
                }
            }
        }
    }

    public void release() {
        SAMPLING.remove(this);
        this.unbindAllProbes();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Phase", this.phase.name());
        tag.putString("Status", this.status.name());
        tag.putString("Detail", this.detail);
        tag.putInt("Matched", this.matched);
        tag.putBoolean("AwaitingContinue", this.awaitingContinue);
        tag.putInt("Width", this.width);
        tag.putInt("Height", this.height);
        tag.putInt("Depth", this.depth);
        tag.putInt("InventoryCount", this.inventoryCount);
        tag.putBoolean("HasBox", this.hasBox);
        tag.putLong("InnerMin", this.innerMin.asLong());
        tag.putLong("InnerMax", this.innerMax.asLong());
        this.writePositions(tag, "Inputs", this.inputs);
        this.writePositions(tag, "Outputs", this.outputs);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Phase", this.phase.name());
        tag.putString("Status", this.status.name());
        tag.putString("Detail", this.detail);
        tag.putInt("Matched", this.matched);
        tag.putInt("TickSum", this.tickSum);
        tag.putLong("EnergySum", this.energySum);
        tag.putLong("PatternEnergy", this.patternEnergy);
        tag.putBoolean("AwaitingContinue", this.awaitingContinue);
        tag.putInt("Width", this.width);
        tag.putInt("Height", this.height);
        tag.putInt("Depth", this.depth);
        tag.putInt("InventoryCount", this.inventoryCount);
        tag.putBoolean("HasBox", this.hasBox);
        tag.putLong("BoxMin", this.outerMin.asLong());
        tag.putLong("BoxMax", this.outerMax.asLong());
        tag.putLong("InnerMin", this.innerMin.asLong());
        tag.putLong("InnerMax", this.innerMax.asLong());
        this.writePositions(tag, "Inputs", this.inputs);
        this.writePositions(tag, "Outputs", this.outputs);
        this.writePositions(tag, "FluidInputs", this.fluidInputs);
        this.writePositions(tag, "FluidOutputs", this.fluidOutputs);
        this.writePositions(tag, "PowerPorts", this.powerPorts);
        this.writePositions(tag, "Inventories", this.inventories);
        this.writePositions(tag, "EnergyBlocks", this.energyBlocks);
        this.writePositions(tag, "FluidBlocks", this.fluidBlocks);
        tag.put("PatternInputs", this.saveTotals(this.patternInputs));
        tag.put("PatternOutputs", this.saveTotals(this.patternOutputs));
        tag.put("PatternFluidInputs", this.saveTotals(this.patternFluidInputs));
        tag.put("PatternFluidOutputs", this.saveTotals(this.patternFluidOutputs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.phase = this.readEnum(RecordPhase.class, tag.getString("Phase"), RecordPhase.IDLE);
        this.status = this.readEnum(RecordStatus.class, tag.getString("Status"), RecordStatus.IDLE);
        this.detail = tag.getString("Detail");
        this.matched = Math.max(0, tag.getInt("Matched"));
        this.tickSum = Math.max(0, tag.getInt("TickSum"));
        this.energySum = Math.max(0L, tag.getLong("EnergySum"));
        this.patternEnergy = tag.contains("PatternEnergy") ? tag.getLong("PatternEnergy") : -1L;
        this.awaitingContinue = tag.getBoolean("AwaitingContinue");
        this.width = tag.getInt("Width");
        this.height = tag.getInt("Height");
        this.depth = tag.getInt("Depth");
        this.inventoryCount = tag.getInt("InventoryCount");
        this.hasBox = tag.getBoolean("HasBox");
        this.outerMin = BlockPos.of(tag.getLong("BoxMin"));
        this.outerMax = BlockPos.of(tag.getLong("BoxMax"));
        this.innerMin = tag.contains("InnerMin") ? BlockPos.of(tag.getLong("InnerMin")) : this.outerMin.offset(1, 1, 1);
        this.innerMax = tag.contains("InnerMax") ? BlockPos.of(tag.getLong("InnerMax")) : this.outerMax.offset(-1, -1, -1);
        this.readPositions(tag.getLongArray("Inputs"), this.inputs);
        this.readPositions(tag.getLongArray("Outputs"), this.outputs);
        if (tag.contains("FluidInputs")) {
            this.readPositions(tag.getLongArray("FluidInputs"), this.fluidInputs);
        }
        if (tag.contains("FluidOutputs")) {
            this.readPositions(tag.getLongArray("FluidOutputs"), this.fluidOutputs);
        }
        if (tag.contains("PowerPorts")) {
            this.readPositions(tag.getLongArray("PowerPorts"), this.powerPorts);
        }
        if (tag.contains("Inventories")) {
            this.readPositions(tag.getLongArray("Inventories"), this.inventories);
        }
        if (tag.contains("EnergyBlocks")) {
            this.readPositions(tag.getLongArray("EnergyBlocks"), this.energyBlocks);
        }
        if (tag.contains("FluidBlocks")) {
            this.readPositions(tag.getLongArray("FluidBlocks"), this.fluidBlocks);
        }
        if (tag.contains("PatternInputs")) {
            this.readTotals(tag.getList("PatternInputs", Tag.TAG_COMPOUND), this.patternInputs);
        }
        if (tag.contains("PatternOutputs")) {
            this.readTotals(tag.getList("PatternOutputs", Tag.TAG_COMPOUND), this.patternOutputs);
        }
        if (tag.contains("PatternFluidInputs")) {
            this.readTotals(tag.getList("PatternFluidInputs", Tag.TAG_COMPOUND), this.patternFluidInputs);
        }
        if (tag.contains("PatternFluidOutputs")) {
            this.readTotals(tag.getList("PatternFluidOutputs", Tag.TAG_COMPOUND), this.patternFluidOutputs);
        }
        this.resumed = false;
    }

    private <T extends Enum<T>> T readEnum(Class<T> type, String name, T fallback) {
        try {
            return name == null || name.isEmpty() ? fallback : Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private void writePositions(CompoundTag tag, String key, Collection<BlockPos> positions) {
        long[] values = new long[positions.size()];
        int index = 0;
        for (BlockPos pos : positions) {
            values[index++] = pos.asLong();
        }
        tag.putLongArray(key, values);
    }

    private void readPositions(long[] values, Collection<BlockPos> into) {
        into.clear();
        for (long value : values) {
            into.add(BlockPos.of(value));
        }
    }

    private ListTag saveTotals(Map<ResourceLocation, Integer> totals) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : totals.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", entry.getKey().toString());
            tag.putInt("Count", entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private void readTotals(ListTag list, Map<ResourceLocation, Integer> into) {
        into.clear();
        for (Tag tag : list) {
            if (!(tag instanceof CompoundTag compound)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(compound.getString("Id"));
            if (id != null) {
                into.put(id, compound.getInt("Count"));
            }
        }
    }
}
