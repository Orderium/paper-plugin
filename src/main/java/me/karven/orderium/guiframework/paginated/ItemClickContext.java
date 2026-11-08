package me.karven.orderium.guiframework.paginated;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public record ItemClickContext <T> (
        T object,
        ItemStack itemStack,
        InventoryClickEvent event,
        int pageIndex) {
}
