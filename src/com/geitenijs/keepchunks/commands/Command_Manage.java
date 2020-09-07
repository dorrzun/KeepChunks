package com.geitenijs.keepchunks.commands;

import com.geitenijs.keepchunks.Strings;
import com.geitenijs.keepchunks.Utilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class Command_Manage implements CommandExecutor {

    public boolean onCommand(final CommandSender s, final Command c, final String label, final String[] args) {
        if (s instanceof Player) {
            Player p = (Player) s;
            p.openInventory(Utilities.manager);

        } else {
            Utilities.msg(s, Strings.ONLYPLAYER);
            return false;
        }
        return true;
    }






}


