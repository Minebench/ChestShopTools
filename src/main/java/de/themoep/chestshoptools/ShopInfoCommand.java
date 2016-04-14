package de.themoep.chestshoptools;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.NumberUtil;
import com.Acrobot.ChestShop.Configuration.Messages;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * ChestShopTools
 * Copyright (C) 2016 Max Lee (https://github.com/Phoenix616/)
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
public class ShopInfoCommand implements CommandExecutor {

    private final ChestShopTools plugin;

    public ShopInfoCommand(ChestShopTools plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)	{
        if(args.length > 0) {
            return false;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            return true;
        }
        Block lookingAt = ((Player) sender).getTargetBlock(
                ImmutableSet.of(
                        Material.WALL_SIGN,
                        Material.CHEST,
                        Material.TRAPPED_CHEST,
                        Material.SIGN_POST
                ),
                10
        );
        if(lookingAt == null) {
            sender.sendMessage(ChatColor.RED + "Please look at a shop sign or chest!" + ChatColor.DARK_GRAY + " (0)");
            return true;
        }
        Sign shopSign = null;
        if(lookingAt.getState() instanceof Chest) {
            shopSign = uBlock.getConnectedSign((Chest) lookingAt.getState());
        } else if(lookingAt.getState() instanceof Sign && ChestShopSign.isValid(lookingAt)) {
            shopSign = (Sign) lookingAt.getState();
        }
        if(shopSign == null) {
            sender.sendMessage(ChatColor.RED + "Please look at a shop sign or chest!" + ChatColor.DARK_GRAY + " (1)");
            return true;
        }

        String name = shopSign.getLine(ChestShopSign.NAME_LINE);
        String quantity = shopSign.getLine(ChestShopSign.QUANTITY_LINE);
        String prices = shopSign.getLine(ChestShopSign.PRICE_LINE);
        String material = shopSign.getLine(ChestShopSign.ITEM_LINE);

        String ownerName = NameManager.getFullUsername(name);
        ItemStack item = MaterialUtil.getItem(material);

        if(item == null || !NumberUtil.isInteger(quantity)) {
            sender.sendMessage(Messages.prefix(Messages.INVALID_SHOP_DETECTED));
            return true;
        }

        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Besitzer: " + ChatColor.WHITE + ownerName));
        if(plugin.getShowItem() == null) {
            sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Item: " + ChatColor.WHITE + material));
        } else {
            String itemJson = plugin.getShowItem().getItemConverter().itemToJson(item, Level.FINE);

            String icon = "";
            if(plugin.getShowItem().useIconRp()) {
                icon = plugin.getShowItem().getIconRpMap().getIcon(item, true);
            }

            JSONArray textJson = new JSONArray();

            textJson.add(new JSONObject(ImmutableMap.of("text", Messages.prefix(ChatColor.GREEN + "Item: "))));

            JSONObject hoverJson = new JSONObject();
            hoverJson.put("action", "show_item");
            hoverJson.put("value", itemJson);
            ChatColor nameColor = plugin.getShowItem().getItemConverter().getNameColor(item);

            if(plugin.getShowItem().useIconRp()) {
                JSONObject iconJson = new JSONObject();
                iconJson.put("text", icon);
                iconJson.put("hoverEvent", hoverJson);

                textJson.add(iconJson);
            }

            JSONObject typeJson = new JSONObject();
            typeJson.put("translate", plugin.getShowItem().getItemConverter().getTranslationKey(item));
            JSONArray translateWith = new JSONArray();
            translateWith.addAll(plugin.getShowItem().getItemConverter().getTranslateWith(item));
            if(!translateWith.isEmpty()) {
                typeJson.put("with", translateWith);
            }

            typeJson.put("hoverEvent", hoverJson);
            typeJson.put("color", nameColor.name().toLowerCase());

            textJson.add(typeJson);

            String itemName = plugin.getShowItem().getItemConverter().getCustomName(item);
            if(!itemName.isEmpty()) {
                textJson.add(new JSONObject(ImmutableMap.of("text", ": ", "color", "green")));

                JSONObject nameJson = new JSONObject();
                nameJson.put("text", itemName);
                nameJson.put("hoverEvent", hoverJson);
                nameJson.put("color", nameColor.name().toLowerCase());

                textJson.add(nameJson);
            }

            plugin.getShowItem().tellRaw((Player) sender, textJson.toString());
        }
        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Anzahl: " + ChatColor.WHITE + quantity));
        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Preis: " + ChatColor.WHITE + prices));

        return true;
    }
}
