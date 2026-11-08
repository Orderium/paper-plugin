package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.config.util.component.SearchGUITypeConfig;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.InventoryItem;
import me.karven.orderium.obj.SortType;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.utils.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static me.karven.orderium.Orderium.plugin;

@Deprecated
@NullMarked
public class ChooseItemGUI {
    private static final List<InventoryGUI> AZ = new ArrayList<>();
    private static final List<InventoryGUI> ZA = new ArrayList<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void init() {
        AZ.clear();
        ZA.clear();

        createPages(AZ, SortType.A_Z);
        createPages(ZA, SortType.Z_A);
    }

    private static List<InventoryGUI> getPages(SortType sortType) {
        switch (sortType) {
            case A_Z -> { return AZ; }
            case Z_A -> { return ZA; }
        }
        return AZ;
    }

    public static InventoryGUI getGUI(int sortIdx, int pageIdx) {
        return getPages(Config.config.chooseItemGUIConfig.sortsOrderConfig.index(sortIdx)).get(pageIdx);
    }

    public static InventoryGUI getGUI(int sortIdx, String search) {
        final List<InventoryGUI> pages = new ArrayList<>();
        createPages(pages, Config.config.chooseItemGUIConfig.sortsOrderConfig.index(sortIdx), search);
        return pages.getFirst();
    }

    private static void addButtons(InventoryGUI gui, List<InventoryGUI> pages, SortType sortType, final int idx, final int pagesAmount, final Config config) {
        final List<SortType> sortOrder = config.chooseItemGUIConfig.sortsOrderConfig.orderArray;
        final int sortIdx = sortOrder.indexOf(sortType);

        if (idx > 0)
            gui.addItem(
                config.chooseItemGUIConfig.backButton.item(e -> PlayerUtils.clickBack(e, pages.get(idx - 1))),
                config.chooseItemGUIConfig.backButton.slot
        );

        if (idx + 1 < pagesAmount)
            gui.addItem(
                    config.chooseItemGUIConfig.nextButton.item(e -> PlayerUtils.clickNext(e, pages.get(idx + 1))),
                    config.chooseItemGUIConfig.nextButton.slot
            );

        gui.addItem(
                config.chooseItemGUIConfig.sortButton.item(e -> {
                    if (!(e.getWhoClicked() instanceof Player p)) return;
                    final int nextIdx = sortIdx == sortOrder.size() - 1 ? 0 : sortIdx + 1;
                    PlayerUtils.openGUI(p, getGUI(nextIdx, idx), false);
                    PlayerUtils.playSound(p, config.sortSound);
                }, sortType),
                config.chooseItemGUIConfig.sortButton.slot
        );

        gui.addItem(
                config.chooseItemGUIConfig.searchButton.item(e -> {
                    if (!(e.getWhoClicked() instanceof final Player player)) return;

                    final Consumer<String> searchAction = query ->
                            DispatchUtil.entity(
                                    player,
                                    () -> PlayerUtils.openGUI(player, ChooseItemGUI.getGUI(sortIdx, query), true)
                            );

                    if (config.chooseItemGUIConfig.searchGUITypeConfig.searchGUIType == SearchGUITypeConfig.SearchGUIType.DIALOG) {
                        player.showDialog(SearchDialog.getDialog(searchAction));
                        return;
                    }

                    SignGUI.newSession(
                            player, searchAction,
                            config.signGUIConfig.signLines,
                            config.signGUIConfig.signType(),
                            config.signGUIConfig.queryLine
                    );

                }),
                config.chooseItemGUIConfig.searchButton.slot
        );
    }

    private static void createPages(List<InventoryGUI> pages, SortType sortType) {
        createPages(pages, sortType, plugin.getDataCache().getItems(sortType));
    }

    private static void createPages(List<InventoryGUI> pages, SortType sortType, String search) {
        if (search.isEmpty()) {
            createPages(pages, sortType);
            return;
        }

        final List<OrderItem> items = AlgoUtils.searchItem(search, plugin.getDataCache().getItems(sortType));
        createPages(pages, sortType, items);
    }

    private static void createPages(List<InventoryGUI> pages, SortType sortType, Collection<OrderItem> items) {
        final Config config = Config.config;
        final int pagesAmount = ConvertUtils.calculatePageAmount(items.size(), config.chooseItemGUIConfig.slots.size());

        InventoryGUI gui = initPage(pages, sortType, 0, pagesAmount, config);
        int idx = 0, cnt = 0;
        for (OrderItem orderItem : items) {
            if (cnt == config.chooseItemGUIConfig.slots.size()) {
                cnt = 0;
                idx++;
                pages.add(gui);
                gui = initPage(pages, sortType, idx, pagesAmount, config);
            }
            final InventoryItem guiItem = new InventoryItem(orderItem.getItemStack());
            guiItem.setOnClick(e -> {
                if (!(e.getWhoClicked() instanceof Player p)) return;
                if (e.getClick() != ClickType.RIGHT || !p.hasPermission("orderium.admin.blacklist")) {

                    if (
                            !config.enchantGUIConfig.enabled ||
                            !(orderItem instanceof EnchantableItem enchantableItem)
                    ) {
                        Dialog dialog = NewOrderDialog.getDialog(orderItem);
                        PlayerUtils.openDialog(p, dialog);
                        return;
                    }
                    EnchantGUI enchantGUI = new EnchantGUI(enchantableItem, (enchantedItem) -> {
                        Dialog dialog = NewOrderDialog.getDialog(enchantedItem);
                        PlayerUtils.openDialog(p, dialog);
                    });
                    InventoryGUI inventoryEnchantGUI = enchantGUI.getGUI();
                    if (inventoryEnchantGUI != null) PlayerUtils.openGUI(p, inventoryEnchantGUI, false);
                    return;
                }
                final ItemStack guiItemStack = guiItem.getItem();
                if (PDCUtils.isBlacklist(guiItemStack.getItemMeta())) return;
                ItemStack orderItemStack = orderItem.getItemStack();
                final BlacklistedItem blacklistedItem = new BlacklistedItem(orderItemStack.serializeAsBytes(), orderItemStack);
                final Storage storage = plugin.getStorage();
                storage.runAsync("add a blacklisted item", () -> storage.items().addBlacklisted(blacklistedItem));
                p.sendRichMessage("<green>Item added to blacklist. Reload to take effects");
                guiItemStack.editMeta(PDCUtils::setBlacklist);
            });
            gui.addItem(guiItem, config.chooseItemGUIConfig.slots.get(cnt));

            cnt++;
        }
        pages.add(gui);
    }

    private static InventoryGUI initPage(List<InventoryGUI> pages, SortType sortType, int idx, int pagesAmount, final Config config) {
        InventoryGUI gui = new InventoryGUI(config.chooseItemGUIConfig.rows, mm.deserialize(config.chooseItemGUIConfig.title), true);
        addButtons(gui, pages, sortType, idx, pagesAmount, config);
        return gui;
    }
}
