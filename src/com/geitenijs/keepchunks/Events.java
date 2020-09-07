package com.geitenijs.keepchunks;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
//TODO: Move GUI listeners to a separate listener class and only register them at startup if config file says so.
//Armor stand and hopper for "arrows"
public class Events implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent e) {
        final Chunk currentChunk = e.getChunk();
        final String chunk = currentChunk.getX() + "#" + currentChunk.getZ() + "#"
                + currentChunk.getWorld().getName();
        if (new HashSet<>(Utilities.chunks).contains(chunk)) {
            if (Utilities.config.getBoolean("general.debug")) {
                Utilities.consoleMsg(Strings.DEBUGPREFIX + "Chunk (" + currentChunk.getX() + "," + currentChunk.getZ() + ") in world '" + currentChunk.getWorld().getName() + "' is unloading, while it should be force-loaded.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent e) {
        final List<String> worlds = new ArrayList<>();
        for (final String chunk : Utilities.chunks) {
            final String world = chunk.split("#")[2];
            worlds.add(world.toLowerCase());
        }
        if (worlds.contains(e.getWorld().getName().toLowerCase())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldLoad(WorldLoadEvent e) {
        for (final String chunk : Utilities.chunks) {
            final String[] chunkCoordinates = chunk.split("#");
            final int x = Integer.parseInt(chunkCoordinates[0]);
            final int z = Integer.parseInt(chunkCoordinates[1]);
            final String world = chunkCoordinates[2];
            if (Utilities.config.getBoolean("general.debug")) {
                Utilities.consoleMsg(Strings.DEBUGPREFIX + "Loading chunk (" + x + "," + z + ") in world '" + world + "'.");
            }
            try {
                Bukkit.getServer().getWorld(world).loadChunk(x, z);
                Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
            } catch (NullPointerException ex) {
                if (Utilities.config.getBoolean("general.debug")) {
                    Utilities.consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpdateAvailable(PlayerJoinEvent e) {
        if ((e.getPlayer().hasPermission("keepchunks.notify.update")) && Utilities.config.getBoolean("updates.check") && Utilities.config.getBoolean("updates.notify") && Utilities.updateAvailable()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(Main.plugin, () -> {
                Utilities.msg(e.getPlayer(), Strings.GAMEPREFIX + "&fA new release of &a" + Strings.PLUGIN + "&f is available!");
                Utilities.msg(e.getPlayer(), Strings.GAMEPREFIX + "&fCurrent version: &a" + Strings.VERSION + "&f; New version: &a" + Utilities.updateVersion() + "&f.");
                Utilities.msg(e.getPlayer(), Strings.GAMEPREFIX + "&fTo download the update, visit this website:");
                Utilities.msg(e.getPlayer(), Strings.GAMEPREFIX + "&a" + Strings.WEBSITE + "&f.");
            }, 90L);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!Utilities.menuWindows.contains(e.getInventory())) return;
        //Else, call handler for each window.
        e.setCancelled(true);
        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        final Player p = (Player) e.getWhoClicked();
        p.openInventory(Utilities.users);
        p.sendMessage("You clicked at slot " + e.getRawSlot());
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryDragEvent e){
        if (e.getInventory() == Utilities.manager) {
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final InventoryOpenEvent e){
        if(e.getInventory() == Utilities.manager)
        initMainMenu();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(final InventoryCloseEvent e){
        deleteItems();
    }
    protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);

        return item;
    }
    public void initMainMenu() {
        Utilities.manager.addItem(createGuiItem(Material.PLAYER_HEAD, "Manage Player Chunks", "§aEdit & View", "§b Marked chunks by players"));
    }
    public void deleteItems(){Utilities.manager.clear();}
}
