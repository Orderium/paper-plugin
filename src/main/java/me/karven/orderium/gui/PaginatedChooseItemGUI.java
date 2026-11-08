package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.config.util.component.SearchGUITypeConfig;
import me.karven.orderium.config.util.component.SortsOrderConfig;
import me.karven.orderium.data.DataCache;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.paginated.ChestGuiPage;
import me.karven.orderium.guiframework.paginated.PaginatedGUI;
import me.karven.orderium.obj.orderitem.BlacklistedItem;
import me.karven.orderium.obj.orderitem.EnchantableItem;
import me.karven.orderium.obj.orderitem.OrderItem;
import me.karven.orderium.storage.Storage;
import me.karven.orderium.utils.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.karven.orderium.Orderium.plugin;

@NullMarked
public class PaginatedChooseItemGUI extends PaginatedGUI<OrderItem> {
    private final Config config;
    private final int sortOrderIndex;
    private final @Nullable String searchQuery;

    public PaginatedChooseItemGUI(final Player player) {
        this(Config.config, 0, player);
    }

    public PaginatedChooseItemGUI(
            final Config config,
            final int sortOrderIndex,
            final Player player
    ) {
        this(config, sortOrderIndex, null, player);
    }
    public PaginatedChooseItemGUI(
        final Config config,
        final int sortOrderIndex,
        final @Nullable String searchQuery,
        final Player player
    ) {
        this.config = config;
        this.sortOrderIndex = sortOrderIndex;
        this.searchQuery = searchQuery;

        final List<OrderItem> sortedItems = DataCache.getInstance().getItems(config.chooseItemGUIConfig.sortsOrderConfig.index(sortOrderIndex));
        final List<OrderItem> items = searchQuery == null ? sortedItems : AlgoUtils.searchItem(searchQuery, sortedItems);

        super(
                config.chooseItemGUIConfig.rows,
                Values.minimessage.deserialize(config.chooseItemGUIConfig.title),
                items,
                OrderItem::getItemStack,
                context -> {
                    final InventoryClickEvent e = context.event();
                    final OrderItem orderItem = context.object();
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
                    final ItemStack guiItemStack = context.itemStack();
                    if (PDCUtils.isBlacklist(guiItemStack.getItemMeta())) return;
                    ItemStack orderItemStack = orderItem.getItemStack();
                    final BlacklistedItem blacklistedItem = new BlacklistedItem(orderItemStack.serializeAsBytes(), orderItemStack);
                    final Storage storage = plugin.getStorage();
                    storage.runAsync("add a blacklisted item", () -> storage.items().addBlacklisted(blacklistedItem));
                    p.sendRichMessage("<green>Item added to blacklist. Reload to take effects");
                    guiItemStack.editMeta(PDCUtils::setBlacklist);
                },
                player,
                config.chooseItemGUIConfig.slots
        );
    }

    @Override
    protected void populateButtons(final ChestGuiPage gui, final int index) {
        final SortsOrderConfig sortOrder = config.chooseItemGUIConfig.sortsOrderConfig;

        final Supplier<ChestGuiPage> previousPageSupplier = this.previousPageSupplier(index);
        if (previousPageSupplier != null)
            gui.addItem(
                    config.chooseItemGUIConfig.backButton.item(e -> PlayerUtils.clickBack(e, previousPageSupplier.get())),
                    config.chooseItemGUIConfig.backButton.slot
            );

        final Supplier<ChestGuiPage> nextPageSupplier = this.nextPageSupplier(index);
        if (nextPageSupplier != null) {
            gui.addItem(
                    config.chooseItemGUIConfig.nextButton.item(event -> PlayerUtils.clickNext(event, nextPageSupplier.get())),
                    config.chooseItemGUIConfig.nextButton.slot
            );
        }

        gui.addItem(
                config.chooseItemGUIConfig.sortButton.item(
                        e -> {
                            if (!(e.getWhoClicked() instanceof Player p)) return;
                            final int nextIndex = this.sortOrderIndex == sortOrder.orderArray.size() - 1 ? 0 : this.sortOrderIndex + 1;
                            new PaginatedChooseItemGUI(
                                    this.config,
                                    nextIndex,
                                    this.searchQuery,
                                    this.player
                            ).openPage(index);
                            PlayerUtils.playSound(p, config.sortSound);
                        },
                        sortOrder.index(this.sortOrderIndex)
                ),
                config.chooseItemGUIConfig.sortButton.slot
        );

        gui.addItem(
                config.chooseItemGUIConfig.searchButton.item(_ -> {
                    final Consumer<String> searchAction = query -> {
                        final PaginatedChooseItemGUI searchedGUI = new PaginatedChooseItemGUI(this.config, this.sortOrderIndex, query, this.player);
                        DispatchUtil.entity(player, searchedGUI::open);
                    };

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
}
