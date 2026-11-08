package me.karven.orderium.data;

import me.karven.orderium.obj.SortType;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.CustomItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.obj.orderitem.VanillaItem;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DataCache {
    private static final DataCache INSTANCE = new DataCache();

    public static @NotNull DataCache getInstance() {
        return INSTANCE;
    }

    private static final Registry<BlockType> BLOCK_REGISTRY = Registry.BLOCK;
    private final ArrayList<OrderItem> itemsAZ = new ArrayList<>();
    private List<OrderItem> itemsZA;

    private final Set<CustomItem> customItems = ConcurrentHashMap.newKeySet();
    private final Set<BlacklistedItem> blacklist = ConcurrentHashMap.newKeySet();

    private void setBlacklistAndCustomItems(Collection<BlacklistedItem> blacklist, Collection<CustomItem> customItems) {
        this.blacklist.clear();
        this.customItems.clear();
        this.blacklist.addAll(blacklist);
        this.customItems.addAll(customItems);
    }

    public void setItems(Collection<VanillaItem> vanillaItems, Collection<BlacklistedItem> blacklistedItems, Collection<CustomItem> customItems) {
        itemsAZ.clear();

        final Collection<OrderItem> set = new TreeSet<>(AlgoUtils.getComparator(SortType.A_Z));
        set.addAll(vanillaItems);
        itemsAZ.addAll(set);

        itemsAZ.addAll(customItems);

        for (BlacklistedItem e : blacklistedItems) {
            itemsAZ.removeIf(orderItem -> orderItem.getItemStack().equals(e.getItemStack()));
        }

        itemsZA = itemsAZ.reversed();

        Log.info("Loaded " + itemsAZ.size() + " items.");
        setBlacklistAndCustomItems(blacklistedItems, customItems);
    }

    public List<OrderItem> getItems(SortType sortType) {
        return Collections.unmodifiableList(
                switch (sortType) {
                    case A_Z -> itemsAZ;
                    case Z_A -> itemsZA;
                    default -> throw new IllegalArgumentException("Sort type is not supported. Must be A_Z or Z_A, found " + sortType);
                }
        );
    }

    public Set<CustomItem> getCustomItems() { return customItems; }
    public Set<BlacklistedItem> getBlacklist() { return blacklist; }

    public BlockType getBlockType(@KeyPattern String identifier) {
        return BLOCK_REGISTRY.get(Key.key(identifier));
    }
}
