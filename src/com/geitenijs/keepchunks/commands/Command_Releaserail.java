package com.geitenijs.keepchunks.commands;

import com.geitenijs.keepchunks.Strings;
import com.geitenijs.keepchunks.Utilities;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

import static com.geitenijs.keepchunks.Utilities.agenda;
import static com.geitenijs.keepchunks.Utilities.explored;
import static com.geitenijs.keepchunks.Utilities.totalChunks;
import static com.geitenijs.keepchunks.Utilities.totalRails;
import static com.geitenijs.keepchunks.Utilities.getAdjacent;
import static com.geitenijs.keepchunks.Utilities.truncateData;

public class Command_Releaserail implements CommandExecutor, TabCompleter {

    public boolean onCommand(final CommandSender s, final Command c, final String label, final String[] args) {
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("current")) {
                if (s instanceof Player) {
                    Location loc = ((Player) s).getLocation();
                    loc = truncateData(loc);
                    Material m = loc.getBlock().getType();
                    boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);

                    if (isRail) {
                        String command = args[0];
                        Utilities.msg(s, "&7&oLooking for rails...");
                        agenda.add(loc);
                        while (!agenda.isEmpty()) {
                            Location cur = agenda.peek();
                            agenda.remove();
                            explored.add(cur);
                            getAdjacent(cur,command);
                            ++totalRails;
                        }
                        Utilities.msg(s, "&fFound &c" + totalRails + "&f rails!");
                        Utilities.msg(s, "&fReleased a total of &9" + totalChunks + "&f chunks in world &6'" + loc.getWorld().getName() + "'&f.");
                    } else {
                        Utilities.msg(s, "&cThere doesn't seem to be a rail at your location.");
                    }
                    totalRails = 0;
                    totalChunks = 0;
                    explored.clear();
                } else {
                    Utilities.msg(s, Strings.ONLYPLAYER);
                }
            } else {
                Utilities.msg(s, Strings.RELEASERAILUSAGE);
            }
        } else if (args.length == 6) {
            if (args[1].equalsIgnoreCase("coords")) {
                try {
                    final int x = Integer.parseInt(args[2]);
                    final int y = Integer.parseInt(args[3]);
                    final int z = Integer.parseInt(args[4]);
                    final String world = args[5];
                    if (Bukkit.getWorld(world) == null) {
                        Utilities.msg(s, "&cWorld &f'" + world + "'&c doesn't exist, or isn't loaded in memory.");
                        return false;
                    }
                    World realWorld = Bukkit.getWorld(world);
                    Location loc = truncateData(new Location(realWorld, x, y, z));
                    Material m = loc.getBlock().getType();
                    boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);

                    if (isRail) {
                        String command = args[0];
                        Utilities.msg(s, "&7&oLooking for rails...");
                        agenda.add(loc);
                        while (!agenda.isEmpty()) {
                            Location cur = agenda.peek();
                            agenda.remove();
                            explored.add(cur);
                            getAdjacent(cur,command);
                            ++totalRails;
                        }
                        Utilities.msg(s, "&fFound &c" + totalRails + "&f rails!");
                        Utilities.msg(s, "&fReleased a total of &9" + totalChunks + "&f chunks in world &6'" + loc.getWorld().getName() + "'&f.");
                    } else {
                        Utilities.msg(s, "&cThere doesn't seem to be a rail at that location.");
                        return true;
                    }
                    totalRails = 0;
                    totalChunks = 0;
                    explored.clear();
                } catch (NumberFormatException ex) {
                    Utilities.msg(s, Strings.UNUSABLE);
                }
            } else {
                Utilities.msg(s, Strings.RELEASERAILUSAGE);
            }
        } else {
            Utilities.msg(s, Strings.RELEASERAILUSAGE);
        }
        return true;
    }
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        ArrayList<String> tabs = new ArrayList<>();
        String[] newArgs = CommandWrapper.getArgs(args);
        if (newArgs.length == 1) {
            tabs.add("current");
            tabs.add("coords");
        }
        if (args[1].equals("coords")) {
            if (s instanceof Player) {
                Player player = (Player) s;
                Location loc = player.getLocation();
                if (newArgs.length == 2) {
                    tabs.add(String.valueOf(loc.getBlockX()));
                }
                if (newArgs.length == 3) {
                    tabs.add(String.valueOf(loc.getBlockY()));
                }
                if (newArgs.length == 4) {
                    tabs.add(String.valueOf(loc.getBlockZ()));
                }
                if (newArgs.length == 5) {
                    tabs.add(loc.getWorld().getName());
                }
            } else {
                if (newArgs.length == 2) {
                    tabs.add("<0>");
                }
                if (newArgs.length == 3) {
                    tabs.add("<0>");
                }
                if (newArgs.length == 4) {
                    tabs.add("<0>");
                }
                if (newArgs.length == 5) {
                    tabs.add("<world>");
                }
            }
        }
        if (args[1].equals("current")) {
            tabs.clear();
        }
        return CommandWrapper.filterTabs(tabs, args);
    }
}