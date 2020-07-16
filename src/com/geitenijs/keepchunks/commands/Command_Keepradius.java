package com.geitenijs.keepchunks.commands;

import com.geitenijs.keepchunks.Strings;
import com.geitenijs.keepchunks.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Command_Keepradius implements CommandExecutor, TabCompleter {

    public boolean onCommand(final CommandSender s, final Command c, final String label, final String[] args) {
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("current")) {
                try {
                    if (s instanceof Player) {
                        final Location start = ((Player) s).getLocation();
                        final Chunk currentChunk = start.getChunk();
                        final String world = currentChunk.getWorld().getName();
                        final int radius = Integer.parseInt(args[2]);
                        final Location tl = new Location(currentChunk.getWorld(), currentChunk.getX() - radius, 0, currentChunk.getZ() - radius);
                        final Location br = new Location(currentChunk.getWorld(), currentChunk.getX() + radius, 0, currentChunk.getZ() + radius);

                        if (Bukkit.getWorld(world) == null) {
                            Utilities.msg(s, "&cWorld &f'" + world + "'&c doesn't exist, or isn't loaded in memory.");
                            return false;
                        }
                        if(radius < 1) {
                            Utilities.msg(s, Strings.KEEPRADIUSUSAGE);
                            return false;
                        }

                        if(Utilities.config.getBoolean("general.debug")) {
                            Utilities.consoleMsg("Starting at " + tl.getX() + " " + tl.getZ() + "and iterating to " + br.getX() + " " + br.getZ());
                        }

                        for (int x = (int)tl.getX(); x <= br.getX(); ++x) {
                            for (int z = (int)tl.getZ(); z <= br.getZ(); ++z) {
                                Utilities.consoleMsg("Found chunk (" + x + "," + z + ") in world " + world + ".");
                                updateData(x,z, world);
                            }
                        }
                    } else {
                        Utilities.msg(s, Strings.ONLYPLAYER);
                        return false;
                    }
                }catch(NumberFormatException ex){
                    Utilities.msg(s, Strings.UNUSABLE);
                    return false;
                }
            }else {
                Utilities.msg(s, Strings.KEEPRADIUSUSAGE);
                return false;
            }
        } else if (args.length == 6) {
            if (args[1].equalsIgnoreCase("coords")) {
                try {
                    final int x = Integer.parseInt(args[2]);
                    final int z = Integer.parseInt(args[3]);
                    final int radius = Integer.parseInt(args[4]);
                    final String world = args[5];
                    final Location start = new Location(Bukkit.getWorld(world),x,0,z);
                    final Chunk startChunk = start.getChunk();
                    final Location tl = new Location(startChunk.getWorld(), startChunk.getX() - radius, 0, startChunk.getZ() - radius);
                    final Location br = new Location(startChunk.getWorld(), startChunk.getX() + radius, 0, startChunk.getZ() + radius);

                    if (Bukkit.getWorld(world) == null) {
                        Utilities.msg(s, "&cWorld &f'" + world + "'&c doesn't exist, or isn't loaded in memory.");
                        return false;
                    }
                    if(radius < 1) {
                        Utilities.msg(s, Strings.KEEPRADIUSUSAGE);
                        return false;
                    }
                    if(Utilities.config.getBoolean("general.debug")) {
                        Utilities.consoleMsg("Starting at " + tl.getX() + " " + tl.getZ() + "and iterating to " + br.getX() + " " + br.getZ());
                    }

                    for (int i = (int)tl.getX(); i <= br.getX(); ++i) {
                        for (int j = (int)tl.getZ(); j <= br.getZ(); ++j) {
                            Utilities.consoleMsg("Found chunk (" + i + "," + j + ") in world " + world + ".");
                            updateData(i,j, world);
                        }
                    }
                } catch (NumberFormatException ex) {
                    Utilities.msg(s, Strings.UNUSABLE);
                }
            } else {
                Utilities.msg(s, Strings.KEEPRADIUSUSAGE);
            }
        } else {
            Utilities.msg(s, Strings.KEEPRADIUSUSAGE);
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
                    tabs.add(String.valueOf(loc.getChunk().getX()));
                }
                if (newArgs.length == 3) {
                    tabs.add(String.valueOf(loc.getChunk().getZ()));
                }
                if (newArgs.length == 4) {
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
                    tabs.add("<world>");
                }
            }
        }
        if (args[1].equals("current")) {
            tabs.clear();
        }
        return CommandWrapper.filterTabs(tabs, args);
    }

    public void updateData(int x, int z, String world) {
        final String chunk = x + "#" + z + "#" + world;
        if (!Utilities.chunks.contains(chunk) && !Bukkit.getServer().getWorld(world).isChunkForceLoaded(x, z)) {
            if (Utilities.config.getBoolean("general.debug")) {
                Utilities.consoleMsg(Strings.DEBUGPREFIX + "Marking chunk (" + x + "," + z + ") in world '" + world + "'...");
            }
            Utilities.chunks.add(chunk);
            Bukkit.getServer().getWorld(world).loadChunk(x, z);
            Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
            Utilities.data.set("chunks", new ArrayList<>(Utilities.chunks));
            Utilities.saveDataFile();
            Utilities.reloadDataFile();
        }
    }
}