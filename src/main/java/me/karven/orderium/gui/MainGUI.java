package me.karven.orderium.gui;

import io.papermc.paper.dialog.Dialog;
import me.karven.orderium.config.Config;
import me.karven.orderium.config.util.component.SearchGUITypeConfig;
import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.paginated.ChestGuiPage;
import me.karven.orderium.guiframework.paginated.ItemClickContext;
import me.karven.orderium.guiframework.paginated.PaginatedGUI;
import me.karven.orderium.order.Order;
import me.karven.orderium.utils.AlgoUtils;
import me.karven.orderium.utils.DispatchUtil;
import me.karven.orderium.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.karven.orderium.Orderium.plugin;

public class MainGUI extends PaginatedGUI<Order> {
    private final Config config;
    private final String search;
    private final int sortIndex;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public MainGUI(final @NotNull Player player) {
        this(player, 0, "");
    }

    public MainGUI(final @NotNull Player player, final String search) {
        this(player, 0, search);
    }

    public MainGUI(final @NotNull Player player, final int sortIndex, final @NotNull String search) {
        final Config config = Config.config;

        final List<Order> activeOrders = plugin.getOrderService().getSortedOrders(config.mainGUIConfig.sortsOrderConfig.index(sortIndex)).stream().filter(Order::isActive).toList();
        final List<Order> matchedOrders = search.isEmpty() ? activeOrders : AlgoUtils.searchOrder(search, activeOrders);
        final Consumer<ItemClickContext<Order>> clickAction = context -> {
            final InventoryClickEvent event = context.event();
            final Order order = context.object();
            if (event.getClick() == ClickType.RIGHT && player.hasPermission("orderium.admin.edit-orders")) {
                final Dialog dialog = AdminToolGUI.createEditOrder(context.object());
                player.showDialog(dialog);
                return;
            }
            if (player.getUniqueId().equals(order.getOwnerUniqueId())) {
                player.sendRichMessage(config.deliverSelf);
                return;
            }
            if (!order.isActive()) {
                final MainGUI mainGUI = new MainGUI(player, sortIndex, search);
                final InventoryGUI skippedPage = mainGUI.skipPages(context.pageIndex());
                skippedPage.open(player);
                return;
            }
            order.openDelivery(player);
        };
        super(
                config.mainGUIConfig.rows,
                mm.deserialize(config.mainGUIConfig.title),
                matchedOrders,
                Order::mainGUIItemStack,
                clickAction,
                player,
                config.mainGUIConfig.orderConfig.slots
        );
        this.config = config;
        this.search = search;
        this.sortIndex = sortIndex;
    }

    @Override
    protected void populateButtons(final @NotNull ChestGuiPage gui, final int index) {
        final Supplier<ChestGuiPage> previousPageSupplier = this.previousPageSupplier(index);
        if (previousPageSupplier != null) {
            gui.addItem(
                    config.mainGUIConfig.backButton.item(e -> PlayerUtils.clickBack(e, previousPageSupplier.get())),
                    config.mainGUIConfig.backButton.slot
            );
        }

        final Supplier<ChestGuiPage> nextPageSupplier = this.nextPageSupplier(index);
        if (nextPageSupplier != null) {
            gui.addItem(
                    config.mainGUIConfig.nextButton.item(event -> PlayerUtils.clickNext(event, nextPageSupplier.get())),
                    config.mainGUIConfig.nextButton.slot
            );
        }

        gui.addItem(
                config.mainGUIConfig.refreshButton.item(_ -> {
                    final MainGUI mainGUI = new MainGUI(player, sortIndex, search);
                    final InventoryGUI skippedPage = mainGUI.skipPages(index);
                    skippedPage.open(player);

                    PlayerUtils.playSound(player, config.refreshSound);

                }),
                config.mainGUIConfig.refreshButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.sortButton.item(_ -> {
                    final MainGUI mainGUI = new MainGUI(player, sortIndex + 1 == config.mainGUIConfig.sortsOrderConfig.orderArray.size() ? 0 : sortIndex + 1, search);
                    final InventoryGUI skippedPage = mainGUI.skipPages(index);
                    skippedPage.open(player);
                    PlayerUtils.playSound(player, config.sortSound);

                }, config.mainGUIConfig.sortsOrderConfig.index(sortIndex)),
                config.mainGUIConfig.sortButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.searchButton.item(_ -> {
                    final Consumer<String> searchAction = query -> DispatchUtil.entity(player, () -> {
                        final MainGUI mainGUI = new MainGUI(this.player, this.sortIndex, query);
                        mainGUI.open();
                    });

                    if (config.mainGUIConfig.searchGUITypeConfig.searchGUIType == SearchGUITypeConfig.SearchGUIType.DIALOG) {
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
                config.mainGUIConfig.searchButton.slot
        );

        gui.addItem(
                config.mainGUIConfig.yourOrdersButton.item(_ -> YourOrderGUI.open(player)),
                config.mainGUIConfig.yourOrdersButton.slot
        );
    }

}
