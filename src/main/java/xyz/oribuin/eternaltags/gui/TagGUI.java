package xyz.oribuin.eternaltags.gui;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.mattstudios.mfgui.gui.components.ItemBuilder;
import me.mattstudios.mfgui.gui.guis.GuiItem;
import me.mattstudios.mfgui.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.hook.PAPI;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TagGUI {

    private final EternalTags plugin;
    private final DataManager data;
    private final TagManager tagManager;
    private final Player player;

    public TagGUI(final EternalTags plugin, final Player player) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
        this.player = player;

    }

    /**
     * Create and open the GUI for the player
     */
    public void createGUI() {
        final PaginatedGui gui = new PaginatedGui(6, format(this.plugin.getMenuConfig().getString("menu-name"), player, StringPlaceholders.empty()));
        gui.setUpdating(true);
        gui.setDefaultClickAction(event -> {
            event.setResult(Event.Result.DENY);
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).updateInventory();
        });

        // Add the border slots
        final List<Integer> borderSlots = new ArrayList<>();
        for (int i = 45; i < 54; i++) borderSlots.add(i);
        borderSlots.forEach(integer -> gui.setItem(integer, fillerItem()));

        gui.setItem(47, ItemBuilder.from(this.getGUIItem("previous-page", null, player)).asGuiItem(event -> gui.previous()));
        gui.setItem(51, ItemBuilder.from(this.getGUIItem("next-page", null, player)).asGuiItem(event -> gui.next()));

        gui.setItem(49, ItemBuilder.from(this.getGUIItem("clear-tag", null, player)).asGuiItem(event -> {

            this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "cleared-tag");
            this.plugin.getManager(DataManager.class).updateUser(event.getWhoClicked().getUniqueId(), null);
            event.getWhoClicked().closeInventory();

        }));

        this.tagManager.getPlayersTag(player).forEach(tag -> gui.addItem(ItemBuilder.from(this.getGUIItem("tag", tag, player))
                .asGuiItem(event -> {

                    if (!this.tagManager.getTags().contains(tag)) {
                        event.getWhoClicked().closeInventory();
                        return;
                    }

                    final TagEquipEvent x = new TagEquipEvent(player, tag);
                    Bukkit.getPluginManager().callEvent(x);
                    if (x.isCancelled()) {
                        return;
                    }

                    event.getWhoClicked().closeInventory();
                    this.data.updateUser(event.getWhoClicked().getUniqueId(), tag);
                    this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "changed-tag", StringPlaceholders.single("tag", tag.getTag()));
                })));

        gui.open(player);
    }

    /**
     * Get an itemstack based on a configuration path.
     *
     * @param path   The config path
     * @param tag    A potential tag for StringPlaceholders
     * @param player The player
     * @return The itemstack formed from the gui
     */
    private ItemStack getGUIItem(final String path, final @Nullable Tag tag, Player player) {
        final FileConfiguration config = this.plugin.getMenuConfig();

        StringPlaceholders placeholders = StringPlaceholders.empty();

        // Define tag placeholders if the tag isnt null
        if (tag != null) {
            placeholders = StringPlaceholders.builder("tag", tag.getTag())
                    .addPlaceholder("description", tag.getDescription())
                    .addPlaceholder("id", tag.getId())
                    .addPlaceholder("name", tag.getName())
                    .build();
        }

        // Create the lore
        final List<String> lore = new ArrayList<>();
        StringPlaceholders finalPlaceholders = placeholders;
        config.getStringList(path + ".lore").forEach(s -> {
            // TODO, Add multi line splitting
//            int number = 0;
//
//            while (number < s.length()) {
//                number += 40;
//
//                if (number == 40 && )
//            }
            lore.add(format(s, player, finalPlaceholders));
        });

        // Create the item builder
        final ItemBuilder item = ItemBuilder.from(Material.valueOf(config.getString(path + ".material")))
                .setName(format(config.getString(path + ".name"), player, finalPlaceholders))
                .setLore(lore)
                .setAmount(config.getInt(path + ".amount"))
                .glow(config.getBoolean(path + ".glow"));

        // Define the texture String
        final String texture = config.getString(path + ".texture");

        // Check if can apply texture
        if (texture != null) {
            item.setSkullTexture(texture);
        }

        ItemStack itemStack = item.build();

        if (config.get(path + ".model") != null && config.getInt(path + ".model") != -1) {
            itemStack = NBTEditor.set(itemStack, config.getInt(path + ".model"), "CustomModelData");
        }

        // Build the new itemstack
        return itemStack;
    }

    private String format(String string, Player player, StringPlaceholders placeholders) {
        return HexUtils.colorify(PAPI.apply(player, placeholders.apply(string)));
    }

    private GuiItem fillerItem() {
        return ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).setName(HexUtils.colorify("&a")).asGuiItem();
    }

}
