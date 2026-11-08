package me.karven.orderium.guiframework.paginated;

import me.karven.orderium.guiframework.InventoryGUI;
import me.karven.orderium.guiframework.InventoryItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public abstract class PaginatedGUI<T> {
    protected final int rows;
    protected final Component title;
    protected final Player player;
    protected final @Nullable ChestGuiPage[] pageCache;
    protected final List<T> elementList;
    protected final Function<T, ItemStack> convertFunction;
    protected final Consumer<ItemClickContext<T>> clickAction;
    protected final List<Integer> slots;

    public PaginatedGUI(
            final int rows,
            final Component title,
            final List<T> items,
            final Function<T, ItemStack> convertFunction,
            final Consumer<ItemClickContext<T>> clickAction,
            final Player player,
            final List<Integer> slots
            ) {
        this.rows = rows;
        this.title = title;
        this.elementList = items;
        this.convertFunction = convertFunction;
        this.clickAction = clickAction;
        this.player = player;
        this.slots = slots;
        this.pageCache = new ChestGuiPage[elementList.size() / slots.size() + 1];
    }


    public void open() {
        // show the first page to the player
        if (pageCache[0] != null) {
            pageCache[0].open(player);
            return;
        }
        openFirstPage();
    }

    /**
     * Open a specific page of this paginated GUI.
     * @param index the index of the page.
     * @throws IndexOutOfBoundsException if the index is out of bound.
     */
    public void openPage(final int index) throws IndexOutOfBoundsException {
        final ChestGuiPage page = getPageAt(index);
        if (page == null) throw new IndexOutOfBoundsException("page index is out of bound for paginated GUI");
        page.open(this.player);
    }

    public InventoryGUI getFirstPage() {
        final InventoryGUI firstPage = getPageAt(0);
        if (firstPage == null) throw new AssertionError();
        return firstPage;
    }

    public void openFirstPage() {
        getFirstPage().open(player);
    }

    protected abstract void populateButtons(final ChestGuiPage page, final int index);

    /**
     * Get a page's previous page supplier
     * @param index the index of the page
     * @return the supplier that supplies the previous page of the provided page, or {@code null} if the page index is out of bound
     */
    protected @Nullable Supplier<ChestGuiPage> previousPageSupplier(final int index) {
        if (index >= pageCache.length || index < 1) return null;
        return () -> {
            final ChestGuiPage previousPage = getPageAt(index - 1);
            if (previousPage == null) throw new AssertionError();
            return previousPage;
        };
    }

    /**
     * Get a page's next page supplier
     * @param index the index of the page
     * @return the supplier that supplies the next page of the provided page, or {@code null} if the page index is out of bound
     */
    protected @Nullable Supplier<ChestGuiPage> nextPageSupplier(final int index) {
        if (index >= pageCache.length - 1 || index < 0) return null;
        return () -> {
            final ChestGuiPage nextPage = getPageAt(index + 1);
            if (nextPage == null) throw new AssertionError();
            return nextPage;
        };
    }

    /**
     * Skip to a specific page index
     * Used by refresh and sort buttons to preserve page after refreshing
     * @param skipTo the index of page to skip to
     * @return the page at that index
     */
    protected InventoryGUI skipPages(final int skipTo) {
        int pageIndex = Math.min(skipTo, pageCache.length - 1);
        final InventoryGUI page = getPageAt(pageIndex);
        if (page == null) throw new AssertionError();
        return page;
    }

    /**
     * Get a page at a specific page index.
     * @param index the index of the page to get.
     * @return the page at that index or {@code null} if the specified index is not within the amount of pages.
     */
    protected @Nullable ChestGuiPage getPageAt(final int index) {
        if (index < pageCache.length && index >= 0) {
            final ChestGuiPage page = pageCache[index];
            if (page != null) return pageCache[index];
        }
        if (pageCache.length <= index) return null;
        final int startIndex = index * slots.size();
        final ChestGuiPage page = new ChestGuiPage(this, index);
        final int endIndex = Math.min(startIndex + slots.size(), elementList.size());
        for (int i = startIndex; i < endIndex; i++) {
            final T object = elementList.get(i);
            final ItemStack item = convertFunction.apply(object);
            final InventoryItem inventoryItem = new InventoryItem(
                    item,
                    inventoryClickEvent -> clickAction.accept(new ItemClickContext<>(object, item, inventoryClickEvent, index))
            );
            page.addItem(inventoryItem, slots.get(i - startIndex));
        }
        pageCache[index] = page;
        return page;
    }
}
