package dev.daan.hyperscale.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.daan.hyperscale.hall.StackCount;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Persistent item storage stored on an HDD/SSD/NVMe ItemStack (AE2-style storage cell).
 */
public record DriveContents(List<StackCount> items) {
    public static final DriveContents EMPTY = new DriveContents(List.of());

    public static final Codec<DriveContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StackCount.CODEC.listOf().fieldOf("items").forGetter(DriveContents::items)
    ).apply(instance, DriveContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DriveContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC),
            DriveContents::items,
            DriveContents::new
    );

    public DriveContents {
        items = normalize(items);
    }

    public int totalItems() {
        int sum = 0;
        for (StackCount entry : this.items) {
            sum += entry.count();
        }
        return sum;
    }

    public int typeCount() {
        return this.items.size();
    }

    public int countOf(ResourceLocation itemId) {
        for (StackCount entry : this.items) {
            if (entry.item().equals(itemId)) {
                return entry.count();
            }
        }
        return 0;
    }

    public int countOf(Item item) {
        return this.countOf(BuiltInRegistries.ITEM.getKey(item));
    }

    public boolean containsType(ResourceLocation itemId) {
        return this.countOf(itemId) > 0;
    }

    /**
     * Returns a new DriveContents with up to {@code amount} of {@code itemId} added,
     * respecting {@code maxItems} and {@code maxTypes}.
     */
    public InsertResult insert(ResourceLocation itemId, int amount, int maxItems, int maxTypes) {
        if (amount <= 0) {
            return new InsertResult(this, 0);
        }
        int freeSpace = Math.max(0, maxItems - this.totalItems());
        if (freeSpace <= 0) {
            return new InsertResult(this, 0);
        }
        boolean hasType = this.containsType(itemId);
        if (!hasType && this.typeCount() >= maxTypes) {
            return new InsertResult(this, 0);
        }
        int accepted = Math.min(amount, freeSpace);
        Map<ResourceLocation, Integer> map = this.toMap();
        map.merge(itemId, accepted, Integer::sum);
        return new InsertResult(fromMap(map), accepted);
    }

    /**
     * Returns a new DriveContents with up to {@code amount} of {@code itemId} removed.
     */
    public ExtractResult extract(ResourceLocation itemId, int amount) {
        if (amount <= 0) {
            return new ExtractResult(this, 0);
        }
        int current = this.countOf(itemId);
        if (current <= 0) {
            return new ExtractResult(this, 0);
        }
        int removed = Math.min(amount, current);
        Map<ResourceLocation, Integer> map = this.toMap();
        int remaining = current - removed;
        if (remaining <= 0) {
            map.remove(itemId);
        } else {
            map.put(itemId, remaining);
        }
        return new ExtractResult(fromMap(map), removed);
    }

    private Map<ResourceLocation, Integer> toMap() {
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
        for (StackCount entry : this.items) {
            map.put(entry.item(), entry.count());
        }
        return map;
    }

    private static DriveContents fromMap(Map<ResourceLocation, Integer> map) {
        if (map.isEmpty()) {
            return EMPTY;
        }
        List<StackCount> list = new ArrayList<>(map.size());
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            if (entry.getValue() > 0) {
                list.add(new StackCount(entry.getKey(), entry.getValue()));
            }
        }
        return new DriveContents(list);
    }

    private static List<StackCount> normalize(List<StackCount> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>();
        for (StackCount entry : raw) {
            if (entry != null && entry.count() > 0) {
                merged.merge(entry.item(), entry.count(), Integer::sum);
            }
        }
        List<StackCount> result = new ArrayList<>(merged.size());
        for (Map.Entry<ResourceLocation, Integer> entry : merged.entrySet()) {
            result.add(new StackCount(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    public record InsertResult(DriveContents contents, int accepted) {
    }

    public record ExtractResult(DriveContents contents, int extracted) {
    }
}
