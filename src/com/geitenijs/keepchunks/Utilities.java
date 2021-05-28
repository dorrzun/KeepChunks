package com.geitenijs.keepchunks;

import com.geitenijs.keepchunks.commands.CommandWrapper;
import com.geitenijs.keepchunks.updatechecker.UpdateCheck;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.*; //TODO: Fix this wildcard later..
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import java.util.logging.Level;

public class Utilities {

    public static FileConfiguration config;
    public static FileConfiguration data;
    private static File configFile = new File(Main.plugin.getDataFolder(), "config.yml");
    private static File dataFile = new File(Main.plugin.getDataFolder(), "data.yml");
    private static boolean updateAvailable;
    public static boolean debugMode;
    public static boolean permissionsEnabled;
    private static String updateVersion;
    public static ArrayList<Integer> permissionLevelLimit;
    public static HashSet<String> chunks;
    public static HashSet<Location> explored = new HashSet<>();
    public static Queue<Location> agenda = new LinkedList<>();
    public static HashMap<String,Integer> permissions;
    public static HashMap<String,Integer> tallies;
    public static HashMap<String,HashSet<String>> chunksByPlayer;
    public static int totalChunks;
    public static int totalRails;


    static {
        config = YamlConfiguration.loadConfiguration(new File(Main.plugin.getDataFolder(), "config.yml"));
        data = YamlConfiguration.loadConfiguration(new File(Main.plugin.getDataFolder(), "data.yml"));
        permissionLevelLimit = new ArrayList<>(data.getIntegerList("limits"));
        chunks = new HashSet<>(data.getStringList("chunks"));
        permissions = loadSavedPermissionData();
        tallies = loadSavedTalliesData();
        chunksByPlayer = loadSavedPlayerChunksData();
    }

    static void pluginBanner() {
        consoleBanner("");
        consoleBanner("&2 _     _                  &8 _______ _                 _          ");
        consoleBanner("&2(_)   | |                 &8(_______) |               | |  &2v" + Strings.VERSION);
        consoleBanner("&2 _____| |_____ _____ ____ &8 _      | |__  _   _ ____ | |  _  ___ ");
        consoleBanner("&2|  _   _) ___ | ___ |  _ \\&8| |     |  _ \\| | | |  _ \\| |_/ )/___)");
        consoleBanner("&2| |  \\ \\| ____| ____| |_| &8| |_____| | | | |_| | | | |  _ (|___ |");
        consoleBanner("&2|_|   \\_)_____)_____)  __/&8 \\______)_| |_|____/|_| |_|_| \\_|___/ ");
        consoleBanner("&2                    |_|   &8                                      ");
        consoleBanner("");
    }

    static void errorBanner() {
        consoleBanner("");
        consoleBanner("&c _______ ______  ______ _______ ______  ");
        consoleBanner("&c(_______|_____ \\(_____ (_______|_____ \\ ");
        consoleBanner("&c _____   _____) )_____) )     _ _____) )");
        consoleBanner("&c|  ___) |  __  /|  __  / |   | |  __  / ");
        consoleBanner("&c| |_____| |  \\ \\| |  \\ \\ |___| | |  \\ \\ ");
        consoleBanner("&c|_______)_|   |_|_|   |_\\_____/|_|   |_|");
        consoleBanner("");
    }

    static void createConfigs() {
        config.options().header(Strings.ASCIILOGO
                + "Copyright © " + Strings.COPYRIGHT + " " + Strings.AUTHOR + ", all rights reserved." +
                "\nInformation & Support: " + Strings.WEBSITE
                + "\n\ngeneral:"
                + "\n  colourfulconsole: Console messages will be coloured when this is enabled."
                + "\n  debug: When set to true, the plugin will log more information to the console."
                + "\n  releaseallprotection: Do you want to restrict the 'release all' command to the console?"
                + "\n  permissionsystem: Do you want to enable permission levels/chunk marking limits for users?"
                + "\n  permissionlimits: Set the number of chunks that can be loaded for each permission level 0-9."
                + "\n\tYou may change/remove values between the first and last entry. Default for players is 0, Admins have \"-1\" for unlimited."
                + "\nupdates:"
                + "\n  check: When enabled, the plugin will check for updates. No automatic downloads, just a subtle notification in the console."
                + "\n  notify: Would you like to get an in-game reminder of a new update? Requires permission 'keepchunks.notify.update'.");
        config.addDefault("general.colourfulconsole", true);
        config.addDefault("general.debug", true);
        config.addDefault("general.releaseallprotection", true);
        config.addDefault("general.permissionsystem", true);
        config.addDefault("general.permissionlimits", new ArrayList<>(Arrays.asList(0,9,18,36,50,100,250,500,750,-1)));
        config.addDefault("updates.check", true);
        config.addDefault("updates.notify", true);

        data.options().header(Strings.ASCIILOGO
                + "Copyright © " + Strings.COPYRIGHT + " " + Strings.AUTHOR + ", all rights reserved." +
                "\nInformation & Support: " + Strings.WEBSITE
                + "\n\nUnless you know what you're doing, it's best not to touch this file. All configurable options can be found in config.yml");
        data.addDefault("chunks", new ArrayList<>());
        data.addDefault("permissions",new HashMap<String,Integer>());
        data.addDefault("tallies",new HashMap<String,Integer>());
        data.addDefault("chunksByPlayer",new HashMap<String, ArrayList>());

        config.options().copyHeader(true);
        config.options().copyDefaults(true);
        data.options().copyHeader(true);
        data.options().copyDefaults(true);
        saveConfigFile();
        reloadConfigFile();
        saveDataFile();
        reloadDataFile();
    }

    static void registerCommandsAndCompletions() {
        Main.plugin.getCommand("keepchunks").setExecutor(new CommandWrapper());
        Main.plugin.getCommand("kc").setExecutor(new CommandWrapper());
        Main.plugin.getCommand("keepchunks").setTabCompleter(new CommandWrapper());
        Main.plugin.getCommand("kc").setTabCompleter(new CommandWrapper());
    }

    static void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new Events(), Main.plugin);
    }

    public static void loadChunks() {
        for (final String chunk : chunks) {
            final String[] chunkCoordinates = chunk.split("#");
            final int x = Integer.parseInt(chunkCoordinates[0]);
            final int z = Integer.parseInt(chunkCoordinates[1]);
            final String world = chunkCoordinates[2];
            if (debugMode) {
                consoleMsg(Strings.DEBUGPREFIX + "Loading chunk (" + x + "," + z + ") in world '" + world + "'.");
            }
            try {
                Bukkit.getServer().getWorld(world).loadChunk(x, z);
                Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
            } catch (NullPointerException ex) {
                if (debugMode)
                    consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
            }
        }
    }
    public static void getN(Location pos, String command) {    //Note on this loop: These four functions all check y +/- 1 to follow any sloped rails!
        for (int i = -1; i < 2; ++i) {
            Location candidate = new Location(pos.getWorld(), pos.getBlockX(), pos.getBlockY() + i, pos.getBlockZ() - 1);
            Material m = candidate.getBlock().getType();
            boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);
            if (isRail && !explored.contains(candidate) && !agenda.contains(candidate)) {   //If we haven't encountered/backlogged this rail for exploration, do so.
                if (Utilities.debugMode)
                    Utilities.consoleMsg(Strings.DEBUGPREFIX + "Found chunk (" + pos.getChunk().getX() + "," + pos.getChunk().getZ() + ") in world '" + pos.getWorld().getName() + "' while discovering rails at (" + pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ() + ").");

                updateData(candidate.getChunk(), command);
                agenda.add(candidate);
            }
        }
    }

    public static void getE(Location pos, String command) {
        for (int i = -1; i < 2; ++i) {
            Location candidate = new Location(pos.getWorld(), pos.getBlockX() + 1, pos.getBlockY() + i, pos.getBlockZ());
            Material m = candidate.getBlock().getType();
            boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);
            if (isRail && !explored.contains(candidate) && !agenda.contains(candidate)) {
                if (Utilities.debugMode) {
                    Utilities.consoleMsg(Strings.DEBUGPREFIX + "Found chunk (" + pos.getChunk().getX() + "," + pos.getChunk().getZ() + ") in world '" + pos.getWorld().getName() + "' while discovering rails at (" + pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ() + ").");
                }
                updateData(candidate.getChunk(), command);
                agenda.add(candidate);
            }
        }
    }

    public static void getS(Location pos, String command) {
        for (int i = -1; i < 2; ++i) {
            Location candidate = new Location(pos.getWorld(), pos.getBlockX(), pos.getBlockY() + i, pos.getBlockZ() + 1);
            Material m = candidate.getBlock().getType();
            boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);
            if (isRail && !explored.contains(candidate) && !agenda.contains(candidate)) {
                if (Utilities.debugMode) {
                    Utilities.consoleMsg(Strings.DEBUGPREFIX + "Found chunk (" + pos.getChunk().getX() + "," + pos.getChunk().getZ() + ") in world '" + pos.getWorld().getName() + "' while discovering rails at (" + pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ() + ").");
                }
                updateData(candidate.getChunk(), command);
                agenda.add(candidate);
            }
        }
    }

    public static void getW(Location pos, String command) {
        for (int i = -1; i < 2; ++i) {
            Location candidate = new Location(pos.getWorld(), pos.getBlockX() - 1, pos.getBlockY() + i, pos.getBlockZ());
            Material m = candidate.getBlock().getType();
            boolean isRail = (m == Material.RAIL || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL);
            if (isRail && !explored.contains(candidate) && !agenda.contains(candidate)) {
                if (debugMode) {
                    consoleMsg(Strings.DEBUGPREFIX + "Found chunk (" + pos.getChunk().getX() + "," + pos.getChunk().getZ() + ") in world '" + pos.getWorld().getName() + "' while discovering rails at (" + pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ() + ").");
                }
                updateData(candidate.getChunk(), command);
                agenda.add(candidate);
            }
        }
    }

    public static void getAdjacent(Location pos, String command) {
        getN(pos,command);
        getS(pos,command);
        getE(pos,command);
        getW(pos,command);
    }
    public static void updateData(Chunk currentChunk, String command) {
        final String world = currentChunk.getWorld().getName();
        int chunkX = currentChunk.getX();
        int chunkZ = currentChunk.getZ();
        for (int i = -1; i < 2; ++i) {  //Nested loop to also force load the 1x1 chunk perimeter surrounding the desired chunk.
            int x = chunkX + i;
            for (int j = -1; j < 2; ++j) {
                int z = chunkZ + j;
                final String chunk = x + "#" + z + "#" + world;

                /* Checks which command is calling updateData(), as it must handle some things differently. Calls are mutually exclusive,
                 * and the command parameter is sanitized by the command wrapper beforehand.*/
                if(command.equalsIgnoreCase("keeprail")) {
                    consoleMsg("We out here checking shit YO...");
                    if (!chunks.contains(chunk)) {
                        consoleMsg("Chunk is not yet loaded....let's do dat shit..");
                        if (debugMode)
                            consoleMsg(Strings.DEBUGPREFIX + "Marking chunk (" + x + "," + z + ") in world '" + world + "'...");

                        chunks.add(chunk);
                        Bukkit.getServer().getWorld(world).loadChunk(x, z);
                        Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
                        data.set("chunks", new ArrayList<>(Utilities.chunks));
                        saveDataFile();
                        reloadDataFile();
                        ++totalChunks;
                    }
                }else{
                    consoleMsg("RUH ROH MOTHHAFUCKA");
                    if (chunks.contains(chunk)) {
                        if (debugMode)
                            consoleMsg(Strings.DEBUGPREFIX + "Releasing chunk (" + x + "," + z + ") in world '" + world + "'...");

                        chunks.remove(chunk);
                        Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, false);
                        data.set("chunks", new ArrayList<>(chunks));
                        saveDataFile();
                        reloadDataFile();
                        ++totalChunks;
                    }
                }
            }
        }
    }
    public static Location truncateData(Location loc){
        loc.setX(loc.getBlockX());  //Truncate player coordinate floats back to repeating zeroes, to ensure no duplicate values/repeats during searching.
        loc.setY(loc.getBlockY());
        loc.setZ(loc.getBlockZ());
        loc.setPitch(0.0f);
        loc.setYaw(0.0f);
        return loc;
    }
    public static HashMap<String,Integer> loadSavedPermissionData(){
        if(data.getConfigurationSection("permissions") != null) {
            if(debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Found previous permission data in data.yml");
            Map<String, Object> rawData = data.getConfigurationSection("permissions").getValues(false);
            HashMap<String,Integer> formattedData = new HashMap<>();
            rawData.forEach((k, v) -> formattedData.put(k, (Integer) v));
            return formattedData;
        }
        else{
            if(debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "No previous permission data was found in data.yml");
            return new HashMap<>();
        }
    }
    public static HashMap<String,Integer> loadSavedTalliesData(){
        if(data.getConfigurationSection("tallies") != null) {
            if(debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Found previous chunk tally data in data.yml");
            Map<String, Object> rawData = data.getConfigurationSection("tallies").getValues(false);
            HashMap<String, Integer> formattedData = new HashMap<>();
            rawData.forEach((k, v) -> formattedData.put(k,(Integer)v));
            return formattedData;
        }
        else{
            if(debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "No previous permission data was found in data.yml");
            return new HashMap<>();
        }
    }
    public static HashMap<String,HashSet<String>> loadSavedPlayerChunksData() {
        if (data.getConfigurationSection("chunksByPlayer") != null) {
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Found previous chunk data for players in data.yml");
            Map<String, Object> rawData = data.getConfigurationSection("chunksByPlayer").getValues(false);
            HashMap<String, ArrayList<String>> partialData = new HashMap<>();
            HashMap<String, HashSet<String>> formattedData = new HashMap<>();

            rawData.forEach((k, v) -> partialData.put(k,(ArrayList<String>)(v)));
            partialData.forEach((k,v) -> formattedData.put(k,new HashSet(v)));
            return formattedData;
        } else {
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "No previous permission data was found in data.yml");
            return new HashMap<>();
        }
    }
    public static int checkPlayerPermission(Player p){
        String uid = p.getUniqueId().toString();
        if(!permissions.containsKey(uid)) {
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "No permission data found for " + uid + ". Creating new profile...");
            updatePlayerPermission(uid, 1);
        }
        return permissions.get(uid);
    }
    public static void updatePlayerPermission(String uid, int permissionLevel){
        permissions.put(uid,permissionLevel);
        chunksByPlayer.put(uid,new HashSet<>());
        HashMap<String,ArrayList> formattedPlayerChunkList = new HashMap();
        chunksByPlayer.forEach((k,v) -> formattedPlayerChunkList.put(k,new ArrayList(v)));
        data.createSection("permissions", permissions);
        data.createSection("chunksByPlayer",formattedPlayerChunkList);

        saveDataFile();
        reloadDataFile();
    }
    public static void addTally(String chunk, int x, int z, String world){
        if(tallies.containsKey(chunk)){
            tallies.put(chunk,tallies.get(chunk)+1);
            data.createSection("tallies", tallies);
        }else{
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Loading chunk (" + x + "," + z + ") in world '" + world + "'.");
            try {
                Bukkit.getServer().getWorld(world).loadChunk(x, z);
                Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
                tallies.put(chunk,1);
                data.createSection("tallies", tallies);
                chunks.add(chunk);
                data.set("chunks", new ArrayList<>(chunks));
            } catch (NullPointerException ex) {
                if (debugMode)
                    consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
            }
        }
    }
    public static void subtractTally(String chunk, int x, int z, String world){
        if(tallies.containsKey(chunk)){
            tallies.put(chunk,tallies.get(chunk)-1);
            if(tallies.get(chunk) == 0){
                try {
                    Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, false);
                    tallies.remove(chunk);
                    chunks.remove(chunk);
                } catch (NullPointerException ex) {
                    if (debugMode)
                        consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
                }
            }
            data.createSection("tallies",tallies);
            data.set("chunks",new ArrayList<>(chunks));

            saveDataFile();
            reloadDataFile();
        }else{
            consoleMsg(Strings.DEBUGPREFIX + "Tried to subtract from a nonexistent tally!");
        }
    }
    public static HashSet<String> getPlayerChunkList(String uid){ return chunksByPlayer.get(uid); }
    public static void addToPlayerChunkList(String uid, String chunk){
        HashMap<String,ArrayList> formattedPlayerChunkList = new HashMap();
        HashSet<String> chunkList = getPlayerChunkList(uid);

        chunkList.add(chunk);
        chunksByPlayer.put(uid,chunkList);
        chunksByPlayer.forEach((k,v) -> formattedPlayerChunkList.put(k,new ArrayList(v)));
        data.createSection("chunksByPlayer",formattedPlayerChunkList);
    }
    public static void removeFromPlayerChunkList(String uid, String chunk){
        HashMap<String,ArrayList> formattedPlayerChunkList = new HashMap();
        HashSet<String> chunkList = getPlayerChunkList(uid);

        chunkList.remove(chunk);
        chunksByPlayer.put(uid,chunkList);
        chunksByPlayer.forEach((k,v) -> formattedPlayerChunkList.put(k,new ArrayList(v)));
        data.createSection("chunksByPlayer",formattedPlayerChunkList);
    }
    public static void clearPlayerChunkList(Player p){
        String uid = p.getUniqueId().toString();
        HashSet<String> chunkList = chunksByPlayer.get(uid);
        chunkList.forEach((chunk) -> {
            String[] chunkCoordinates = chunk.split("#"); //No try/catch because data in playerList is already clean.
            int x = Integer.parseInt(chunkCoordinates[0]);
            int z = Integer.parseInt(chunkCoordinates[1]);
            String world = chunkCoordinates[2];
            removeFromPlayerChunkList(uid, chunk);
            subtractTally(chunk, x, z, world);
        });
        chunksByPlayer.put(uid,chunkList);
        data.createSection("chunksByPlayer",chunksByPlayer);

        saveDataFile();
        reloadDataFile();
    }
    public static void chunkLoadRoutine(Player p, String chunk){ //Add chunk ownership check and admin backdoor...
        final String uid = p.getUniqueId().toString();
        final int permissionLevel = checkPlayerPermission(p);
        final int playerChunkLimit = permissionLevelLimit.get(permissionLevel);
        final int x,z;
        final String world;
        try {
            String[] chunkCoordinates = chunk.split("#"); //Validate chunk before proceeding, saving time and no repeat checks.
            x = Integer.parseInt(chunkCoordinates[0]);
            z = Integer.parseInt(chunkCoordinates[1]);
            world = chunkCoordinates[2];
        }catch(NumberFormatException e){
            msg(p,Strings.UNUSABLE);
            return;
        }
        if(permissionsEnabled){
            if(getPlayerChunkList(uid).size() < playerChunkLimit || permissionLevel == -1){ //Allows admins unlimited loading.
                if(!getPlayerChunkList(uid).contains(chunk)) {
                    addTally(chunk,x,z,world);
                    addToPlayerChunkList(uid, chunk);
                    msg(p, "&fMarking chunk &9(" + x + "," + z + ")&f in world &6'" + world + "'&f.");

                    saveDataFile();
                    reloadDataFile();
                }else
                    msg(p, "You have already marked chunk &9(" + x + "," + z + ")&f in world &6'" + world + "'&f.");
            }else
                msg(p,"You do not have permission to mark chunks or you have reached your limit.");
        }else{  //Non-permission system routine...
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Loading chunk (" + x + "," + z + ") in world '" + world + "'.");
            try {
                Bukkit.getServer().getWorld(world).loadChunk(x, z);
                Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, true);
                msg(p, "&fMarked chunk &9(" + x + "," + z + ")&f in world &6'" + world + "'&f.");
                chunks.add(chunk);
                data.set("chunks", new ArrayList<>(chunks));

                saveDataFile();
                reloadDataFile();
            } catch (NullPointerException ex) {
                if (debugMode)
                    consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
            }
        }
    }
    public static void chunkUnloadRoutine(Player p, String chunk){
        final String uid = p.getUniqueId().toString();
        final int permissionLevel = checkPlayerPermission(p);
        final int playerChunkLimit = permissionLevelLimit.get(permissionLevel);
        final int x,z;
        final String world;
        try {
            String[] chunkCoordinates = chunk.split("#");
            x = Integer.parseInt(chunkCoordinates[0]);
            z = Integer.parseInt(chunkCoordinates[1]);
            world = chunkCoordinates[2];
        }catch(NumberFormatException e){
            msg(p,Strings.UNUSABLE);
            return;
        }
        if(permissionsEnabled){
            if(getPlayerChunkList(uid).size() < playerChunkLimit){
                subtractTally(chunk, x, z, world);
                removeFromPlayerChunkList(uid, chunk);
                msg(p, "&fUnmarking chunk &9(" + x + "," + z + ")&f in world &6'" + world + "'&f.");

                saveDataFile();
                reloadDataFile();
            }else
                msg(p,"You do not have permission to unmark chunks.");
        }else{
            if (debugMode)
                consoleMsg(Strings.DEBUGPREFIX + "Loading chunk (" + x + "," + z + ") in world '" + world + "'.");
            try {
                Bukkit.getServer().getWorld(world).setChunkForceLoaded(x, z, false);
                chunks.remove(chunk);

                saveDataFile();
                reloadDataFile();
            } catch (NullPointerException ex) {
                if (debugMode)
                    consoleMsg(Strings.DEBUGPREFIX + "World '" + world + "' doesn't exist, or isn't loaded in memory.");
            }
        }
    }
    static void setToggles(){
        debugMode = config.getBoolean("general.debug");
        permissionsEnabled = config.getBoolean("general.permissionsystem");
    }
    static void startSchedulers() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.plugin, Utilities::checkForUpdates, 190L, 216000L);
    }

    static void stopSchedulers() {
        Bukkit.getScheduler().cancelTasks(Main.plugin);
    }

    static void startMetrics() {
        Metrics metrics = new Metrics(Main.plugin);
        metrics.addCustomChart(new Metrics.SingleLineChart("loadedChunks", () -> chunks.size()));
        metrics.addCustomChart(new Metrics.SimplePie("worldeditVersion", () -> {
            final Plugin p = Bukkit.getPluginManager().getPlugin("WorldEdit");
            if (!(p instanceof WorldEditPlugin)) {
                return Strings.NOSTAT;
            }
            return Bukkit.getServer().getPluginManager().getPlugin("WorldEdit").getDescription().getVersion();
        }));
        metrics.addCustomChart(new Metrics.SimplePie("worldguardVersion", () -> {
            final Plugin p = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (!(p instanceof WorldGuardPlugin)) {
                return Strings.NOSTAT;
            }
            return Bukkit.getServer().getPluginManager().getPlugin("WorldGuard").getDescription().getVersion();
        }));
        metrics.addCustomChart(new Metrics.SimplePie("colourfulConsoleEnabled", () -> config.getString("general.colourfulconsole")));
        metrics.addCustomChart(new Metrics.SimplePie("debugEnabled", () -> config.getString("general.debug")));
        metrics.addCustomChart(new Metrics.SimplePie("releaseallProtectionEnabled", () -> config.getString("general.releaseallprotection")));
        metrics.addCustomChart(new Metrics.SimplePie("permissionSystemEnabled", () -> config.getString("general.permissionsystem")));
        metrics.addCustomChart(new Metrics.SimplePie("updateCheckEnabled", () -> config.getString("updates.check")));
        metrics.addCustomChart(new Metrics.SimplePie("updateNotificationEnabled", () -> config.getString("updates.notify")));
    }

    static void done() {
        consoleMsg(Strings.PLUGIN + " v" + Strings.VERSION + " has been enabled");
    }

    private static void checkForUpdates() {
        if (config.getBoolean("updates.check")) {
            UpdateCheck
                    .of(Main.plugin)
                    .resourceId(Strings.RESOURCEID)
                    .handleResponse((versionResponse, version) -> {
                        switch (versionResponse) {
                            case FOUND_NEW:
                                consoleMsg("A new release of " + Strings.PLUGIN + ", v" + version + ", is available! You are still on v" + Strings.VERSION + ".");
                                consoleMsg("To download this update, head over to " + Strings.WEBSITE + "/updates in your browser.");
                                updateVersion = version;
                                updateAvailable = true;
                                break;
                            case LATEST:
                                consoleMsg("You are running the latest version.");
                                updateAvailable = false;
                                break;
                            case UNAVAILABLE:
                                consoleMsg("An error occurred while checking for updates.");
                                updateAvailable = false;
                        }
                    }).check();
        }
    }

    static boolean updateAvailable() {
        return updateAvailable;
    }

    static String updateVersion() {
        return updateVersion;
    }

    public static void reloadConfigFile() {
        if (configFile == null) {
            configFile = new File(Main.plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    static void saveConfigFile() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + configFile, ex);
        }
    }

    public static void reloadDataFile() {
        if (dataFile == null) {
            dataFile = new File(Main.plugin.getDataFolder(), "data.yml");
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public static void saveDataFile() {
        if (data == null || dataFile == null) {
            return;
        }
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + dataFile, ex);
        }
    }

    public static void msg(final CommandSender s, String msg) {
        if (s instanceof Player) {
            msg = ChatColor.translateAlternateColorCodes('&', msg);
        } else {
            msg = ChatColor.translateAlternateColorCodes('&', Strings.INTERNALPREFIX + msg);
            if (!config.getBoolean("general.colourfulconsole")) {
                msg = ChatColor.stripColor(msg);
            }
        }
        s.sendMessage(msg);
    }

    public static void consoleMsg(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', Strings.INTERNALPREFIX + msg);
        if (!config.getBoolean("general.colourfulconsole")) {
            msg = ChatColor.stripColor(msg);
        }
        Bukkit.getServer().getConsoleSender().sendMessage(msg);
    }

    private static void consoleBanner(final String message) {
        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
