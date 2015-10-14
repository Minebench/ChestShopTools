package de.themoep.chestshoptools;

import com.Acrobot.ChestShop.Configuration.Messages;
import de.themoep.chestshoptools.manager.EmptyManager;
import de.themoep.chestshoptools.manager.EnchantManager;
import de.themoep.chestshoptools.manager.TradeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * ChestShopTools
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ChestShopTools extends JavaPlugin {

    private EmptyManager emptMan;
    private EnchantManager enchMan;
    private TradeManager tradeMan;

    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)	{
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "[CST] Reloaded Config");
                return true;
            }
        }
        return false;
    }

    private void loadConfig() {
        reloadConfig();

        if(emptMan != null) {
            HandlerList.unregisterAll(emptMan);
        }
        emptMan = new EmptyManager(this, getConfig().getConfigurationSection("removeempty"));

        if(enchMan != null) {
            HandlerList.unregisterAll(enchMan);
        }
        enchMan = new EnchantManager(this, getConfig().getConfigurationSection("enchantsigns"));

        if(tradeMan != null) {
            HandlerList.unregisterAll(tradeMan);
        }
        tradeMan = new TradeManager(this, getConfig().getConfigurationSection("tradeinfo"));
    }


    public String buildMsg(String msg, Map<String, String> replacements) {
        for(Map.Entry<String, String> entry : replacements.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return Messages.prefix(msg);
    }

}
