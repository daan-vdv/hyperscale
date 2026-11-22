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

import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.record.ClaimScanner;
import dev.daan.hyperscale.record.ItemTotals;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.record.RecordStatus;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class RecorderBlockEntity extends InventoryBlockEntity {
    private static final Set<RecorderBlockEntity> SAMPLING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private RecordPhase phase = RecordPhase.IDLE;
    private RecordStatus status = RecordStatus.IDLE;
    private String detail = "";
    private int matched;
    private int tickSum;
    private int runTicks;
    private int settleTicks;
    private boolean awaitingContinue;
    private boolean resumed;
    private int width;
    private int height;
    private int depth;
    private int inventoryCount;
    private boolean hasBox;
    private BlockPos boxMin = BlockPos.ZERO;
    private BlockPos boxMax = BlockPos.ZERO;
    private final List<BlockPos> inputs = new ArrayList<>();
    private final List<BlockPos> outputs = new ArrayList<>();
    private final Set<BlockPos> inventories = new HashSet<>();
    private final Map<ResourceLocation, Integer> baseline = new HashMap<>();
    private final Map<ResourceLocation, Integer> patternInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> patternOutputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleInputs = new HashMap<>();
    private final Map<ResourceLocation, Integer> cycleOutputs = new HashMap<>();

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

    public boolean inClaimBox(BlockPos pos) {
        if (!this.hasBox) {
            return false;
        }
        return pos.getX() >= this.boxMin.getX() && pos.getX() <= this.boxMax.getX()
                && pos.getY() >= this.boxMin.getY() && pos.getY() <= this.boxMax.getY()
                && pos.getZ() >= this.boxMin.getZ() && pos.getZ() <= this.boxMax.getZ();
    }

    /** Sealed during sampling: claim box + bound probes, but not the compiler itself. */
    public boolean blocksAccess(BlockPos pos) {
        if (!this.isSampling() || pos.equals(this.worldPosition)) {
            return false;
        }
        if (this.inputs.contains(pos) || this.outputs.contains(pos)) {
            return true;
        }
        return this.inClaimBox(pos);
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
        return this.inputs.size();
    }

    public int boundOutputs() {
        return this.outputs.size();
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
            case 8 -> this.runTicks;
            case 9 -> this.settleTicks;
            case 10 -> this.inputs.size();
            case 11 -> this.outputs.size();
            default -> 0;
        };
    }

    public boolean bindProbe(BlockPos probePos) {
        if (this.level == null || this.phase == RecordPhase.LEARNING) {
            return false;
        }
        if (!(this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe)) {
            return false;
        }
        if (probe.compiler() != null && !probe.compiler().equals(this.worldPosition)) {
            if (this.level.getBlockEntity(probe.compiler()) instanceof RecorderBlockEntity other) {
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
        if (this.level != null && this.level.getBlockEntity(probePos) instanceof ProbeBlockEntity probe
                && this.worldPosition.equals(probe.compiler())) {
            probe.clearCompiler();
        }
        if (this.phase == RecordPhase.LEARNING) {
            this.fail(RecordStatus.LOST_BIND);
        } else {
            this.sync();
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
        if (!this.refreshBinds()) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.LOST_BIND, "");
            return;
        }
        if (this.inputs.isEmpty() || this.outputs.isEmpty()) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.NEED_BIND, "");
            return;
        }

        List<BlockPos> terminals = new ArrayList<>(this.inputs);
        terminals.addAll(this.outputs);
        ClaimScanner.Claim claim = ClaimScanner.build(this.level, terminals);
        if (claim == null) {
            this.phase = RecordPhase.FAILED;
            this.setStatus(RecordStatus.TOO_LARGE, "");
            return;
        }

        this.inventories.clear();
        this.inventories.addAll(claim.inventories());
        this.width = claim.width();
        this.height = claim.height();
        this.depth = claim.depth();
        this.inventoryCount = this.inventories.size();
        this.hasBox = true;
        this.boxMin = claim.min();
        this.boxMax = claim.max();
        this.patternInputs.clear();
        this.patternOutputs.clear();
        this.matched = 0;
        this.tickSum = 0;
        this.phase = RecordPhase.LEARNING;
        this.beginRun();
    }

    private void beginRun() {
        if (this.level == null || this.phase != RecordPhase.LEARNING) {
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
        this.baseline.clear();
        this.baseline.putAll(ItemTotals.snapshot(this.level, this.inventories));
        this.clearCycle();
        this.awaitingContinue = false;
        this.runTicks = 0;
        this.settleTicks = 0;
        this.refreshSamplingRegistration();
        this.setStatus(RecordStatus.SAMPLING, "");
        this.drawFrame();
    }

    private void tickServer() {
        if (this.level == null) {
            return;
        }
        if (!this.resumed) {
            this.resumed = true;
            if (this.phase == RecordPhase.LEARNING) {
                this.awaitingContinue = false;
                this.clearCycle();
                this.phase = RecordPhase.IDLE;
                this.setStatus(RecordStatus.INTERRUPTED, "");
            }
        }
        if (this.phase != RecordPhase.LEARNING || this.awaitingContinue) {
            return;
        }
        // Hoppers ignore item entities with pickup delay > 0 — keep refreshing so Q-drops
        // cannot be vacuumed into claim inventories (droppers/chests) mid-run.
        this.sealDroppedItems();
        if (this.level.getGameTime() % 20L == 0L) {
            if (!this.refreshBinds()) {
                this.fail(RecordStatus.LOST_BIND);
                return;
            }
            this.drawFrame();
        }
        this.runTicks++;
        if (this.inputsEmpty()) {
            ResourceLocation drift = ItemTotals.firstDiff(this.baseline, ItemTotals.snapshot(this.level, this.inventories));
            if (drift == null) {
                this.settleTicks++;
                if (this.settleTicks >= RecordLimits.SETTLE_TICKS) {
                    this.closeRun();
                    return;
                }
            } else {
                this.settleTicks = 0;
            }
        } else {
            this.settleTicks = 0;
        }
        if (this.runTicks % 10 == 0) {
            this.sync();
        }
    }

    private void closeRun() {
        if (!this.refreshBinds()) {
            this.fail(RecordStatus.LOST_BIND);
            return;
        }
        ResourceLocation drift = ItemTotals.firstDiff(this.baseline, ItemTotals.snapshot(this.level, this.inventories));
        if (drift != null) {
            this.fail(RecordStatus.DRIFT, drift.toString());
            return;
        }
        if (this.cycleInputs.isEmpty() || this.cycleOutputs.isEmpty()) {
            this.fail(RecordStatus.EMPTY);
            return;
        }
        if (ItemTotals.same(this.cycleInputs, this.cycleOutputs)) {
            this.fail(RecordStatus.PASSTHROUGH);
            return;
        }
        if (this.patternInputs.isEmpty()) {
            this.patternInputs.putAll(this.cycleInputs);
            this.patternOutputs.putAll(this.cycleOutputs);
        } else if (!ItemTotals.same(this.patternInputs, this.cycleInputs)
                || !ItemTotals.same(this.patternOutputs, this.cycleOutputs)) {
            this.fail(RecordStatus.MISMATCH);
            return;
        }
        this.tickSum += Math.max(20, this.runTicks);
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
        int ticks = Math.max(20, this.tickSum / RecordLimits.CYCLES);
        ItemStack image = new ItemStack(ModItems.IMAGE.get());
        image.set(ModDataComponents.PROCESS_IMAGE.get(), new ProcessImage(
                ItemTotals.freeze(this.patternInputs),
                ItemTotals.freeze(this.patternOutputs),
                ticks
        ));
        this.items.setStackInSlot(0, image);
        this.awaitingContinue = false;
        this.phase = RecordPhase.DONE;
        this.setStatus(RecordStatus.DONE, "");
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.1F);
            this.drawFrame();
        }
    }

    private boolean inputsHaveItems() {
        for (BlockPos pos : this.inputs) {
            if (!(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)) {
                continue;
            }
            for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                if (!probe.items().getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean inputsEmpty() {
        for (BlockPos pos : this.inputs) {
            if (!(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)) {
                continue;
            }
            for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                if (!probe.items().getStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean refreshBinds() {
        if (this.level == null) {
            return false;
        }
        return this.refreshList(this.inputs, ProbeMode.INPUT) && this.refreshList(this.outputs, ProbeMode.OUTPUT);
    }

    private boolean refreshList(List<BlockPos> list, ProbeMode mode) {
        Iterator<BlockPos> iterator = list.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!(this.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)
                    || probe.mode() != mode
                    || !this.worldPosition.equals(probe.compiler())) {
                iterator.remove();
                return false;
            }
        }
        return true;
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
        this.runTicks = 0;
        this.settleTicks = 0;
    }

    private void setStatus(RecordStatus status, String detail) {
        this.status = status;
        this.detail = detail == null ? "" : detail;
        this.refreshSamplingRegistration();
        this.sync();
    }

    private void sealDroppedItems() {
        if (!this.hasBox || this.level == null) {
            return;
        }
        AABB area = new AABB(
                this.boxMin.getX(), this.boxMin.getY(), this.boxMin.getZ(),
                this.boxMax.getX() + 1.0D, this.boxMax.getY() + 1.0D, this.boxMax.getZ() + 1.0D
        );
        for (ItemEntity item : this.level.getEntitiesOfClass(ItemEntity.class, area)) {
            item.setPickUpDelay(40);
        }
    }

    private void drawFrame() {
        if (!(this.level instanceof ServerLevel server) || !this.hasBox) {
            return;
        }
        double x0 = this.boxMin.getX();
        double y0 = this.boxMin.getY();
        double z0 = this.boxMin.getZ();
        double x1 = this.boxMax.getX() + 1.0D;
        double y1 = this.boxMax.getY() + 1.0D;
        double z1 = this.boxMax.getZ() + 1.0D;
        this.edge(server, x0, y0, z0, x1, y0, z0);
        this.edge(server, x0, y1, z0, x1, y1, z0);
        this.edge(server, x0, y0, z1, x1, y0, z1);
        this.edge(server, x0, y1, z1, x1, y1, z1);
        this.edge(server, x0, y0, z0, x0, y1, z0);
        this.edge(server, x1, y0, z0, x1, y1, z0);
        this.edge(server, x0, y0, z1, x0, y1, z1);
        this.edge(server, x1, y0, z1, x1, y1, z1);
        this.edge(server, x0, y0, z0, x0, y0, z1);
        this.edge(server, x1, y0, z0, x1, y0, z1);
        this.edge(server, x0, y1, z0, x0, y1, z1);
        this.edge(server, x1, y1, z0, x1, y1, z1);
    }

    private void edge(ServerLevel server, double x0, double y0, double z0, double x1, double y1, double z1) {
        double length = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
        int steps = Math.max(1, (int) Math.ceil(length));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            server.sendParticles(ParticleTypes.END_ROD, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public void release() {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Phase", this.phase.name());
        tag.putString("Status", this.status.name());
        tag.putString("Detail", this.detail);
        tag.putInt("Matched", this.matched);
        tag.putInt("TickSum", this.tickSum);
        tag.putBoolean("AwaitingContinue", this.awaitingContinue);
        tag.putInt("Width", this.width);
        tag.putInt("Height", this.height);
        tag.putInt("Depth", this.depth);
        tag.putInt("InventoryCount", this.inventoryCount);
        tag.putBoolean("HasBox", this.hasBox);
        tag.putLong("BoxMin", this.boxMin.asLong());
        tag.putLong("BoxMax", this.boxMax.asLong());
        this.writePositions(tag, "Inputs", this.inputs);
        this.writePositions(tag, "Outputs", this.outputs);
        this.writePositions(tag, "Inventories", this.inventories);
        tag.put("Baseline", this.saveTotals(this.baseline));
        tag.put("PatternInputs", this.saveTotals(this.patternInputs));
        tag.put("PatternOutputs", this.saveTotals(this.patternOutputs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.phase = this.readEnum(RecordPhase.class, tag.getString("Phase"), RecordPhase.IDLE);
        this.status = this.readEnum(RecordStatus.class, tag.getString("Status"), RecordStatus.IDLE);
        this.detail = tag.getString("Detail");
        this.matched = tag.getInt("Matched");
        this.tickSum = tag.getInt("TickSum");
        this.awaitingContinue = tag.getBoolean("AwaitingContinue");
        this.width = tag.getInt("Width");
        this.height = tag.getInt("Height");
        this.depth = tag.getInt("Depth");
        this.inventoryCount = tag.getInt("InventoryCount");
        this.hasBox = tag.getBoolean("HasBox");
        this.boxMin = BlockPos.of(tag.getLong("BoxMin"));
        this.boxMax = BlockPos.of(tag.getLong("BoxMax"));
        this.readPositions(tag.getLongArray("Inputs"), this.inputs);
        this.readPositions(tag.getLongArray("Outputs"), this.outputs);
        this.readPositions(tag.getLongArray("Inventories"), this.inventories);
        this.readTotals(tag.getList("Baseline", net.minecraft.nbt.Tag.TAG_COMPOUND), this.baseline);
        this.readTotals(tag.getList("PatternInputs", net.minecraft.nbt.Tag.TAG_COMPOUND), this.patternInputs);
        this.readTotals(tag.getList("PatternOutputs", net.minecraft.nbt.Tag.TAG_COMPOUND), this.patternOutputs);
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
        for (net.minecraft.nbt.Tag tag : list) {
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
