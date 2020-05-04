package de.themoep.chestshoptools;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.NumberUtil;
import com.Acrobot.ChestShop.Configuration.Messages;
import com.Acrobot.ChestShop.Events.ItemInfoEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.google.common.collect.ImmutableMap;
import de.themoep.ShowItem.api.ItemDataTooLongException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;

import java.util.Set;
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

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length > 0) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            return true;
        }
        Block lookingAt = ((Player) sender).getTargetBlock((Set<Material>) null, 10);
        if (lookingAt == null) {
            sender.sendMessage(ChatColor.RED + "Please look at a shop sign or chest!" + ChatColor.DARK_GRAY + " (0)");
            return true;
        }
        Sign shopSign = null;
        BlockState state = lookingAt.getState(false);
        if (state instanceof InventoryHolder) {
            shopSign = uBlock.getConnectedSign(state);
        } else if (state instanceof Sign && ChestShopSign.isValid(lookingAt)) {
            shopSign = (Sign) state;
        }
        if (shopSign == null) {
            sender.sendMessage(ChatColor.RED + "Please look at a shop sign or chest!" + ChatColor.DARK_GRAY + " (1)");
            return true;
        }

        String name = shopSign.getLine(ChestShopSign.NAME_LINE);
        String quantity = shopSign.getLine(ChestShopSign.QUANTITY_LINE);
        String prices = shopSign.getLine(ChestShopSign.PRICE_LINE);
        String material = shopSign.getLine(ChestShopSign.ITEM_LINE);

        String ownerName = NameManager.getFullUsername(name);
        ownerName = ownerName != null ? ownerName : name;
        ItemStack item = MaterialUtil.getItem(material);

        if (item == null || !NumberUtil.isInteger(quantity)) {
            sender.sendMessage(Messages.prefix(Messages.INVALID_SHOP_DETECTED));
            return true;
        }

        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Besitzer: " + ChatColor.WHITE + ownerName));
        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Anzahl: " + ChatColor.WHITE + quantity));
        sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Preis: " + ChatColor.WHITE + prices));
        if (plugin.getShowItem() == null) {
            sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Item: " + ChatColor.WHITE + material));
        } else {
            try {
                String json = plugin.getShowItem().getItemConverter().createComponent(item, Level.OFF).toJsonString((Player) sender);

                JSONObject textJson = new JSONObject(ImmutableMap.of("text", Messages.prefix(ChatColor.GREEN + "Item: ")));
                plugin.getShowItem().tellRaw(sender, textJson.toJSONString() + "," + json);

            } catch (ItemDataTooLongException e) {
                sender.sendMessage(Messages.prefix(ChatColor.GREEN + "Item: " + ChatColor.WHITE + material + ChatColor.RED + " (Data too long)"));
                plugin.getLogger().log(Level.WARNING, "Error while trying to show info of shop at "
                        + lookingAt.getWorld().getName() + "/" + lookingAt.getX() + "/" + lookingAt.getY() + "/" + lookingAt.getZ() + ": " + e.getMessage());
            }
        }
        new ItemInfoEvent(sender, item).callEvent();

        return true;
    }
}
