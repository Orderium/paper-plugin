package me.karven.orderium.guiframework.paginated;

import me.karven.orderium.guiframework.InventoryGUI;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ChestGuiPage extends InventoryGUI {
    public <T> ChestGuiPage(final PaginatedGUI<T> parent, final int index) {
        super(parent.rows, parent.title, true);
        parent.populateButtons(this, index);
    }
}
