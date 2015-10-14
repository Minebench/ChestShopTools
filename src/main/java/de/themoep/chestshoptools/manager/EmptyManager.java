package de.themoep.chestshoptools.manager;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Containers.AdminInventory;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.google.common.collect.ImmutableMap;
import de.themoep.chestshoptools.ChestShopTools;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
public class EmptyManager extends AbstractManager {
    String messageOwner;
    String messageBuyer;

    public EmptyManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
        messageOwner = ChatColor.translateAlternateColorCodes('&', config.getString("messages.owner", ""));
        messageBuyer = ChatColor.translateAlternateColorCodes('&', config.getString("messages.buyer", ""));
    }

    @EventHandler
    public void onTradeAttempt(PreTransactionEvent event) {
        if(!event.isCancelled()
                || !isManaged(event.getSign().getWorld())
                || event.getTransactionType() != TransactionEvent.TransactionType.BUY
                || event.getTransactionOutcome() != PreTransactionEvent.TransactionOutcome.NOT_ENOUGH_STOCK_IN_CHEST
                || PriceUtil.hasSellPrice(event.getSign().getLine(2))
                ) {
            return;
        }

        removeShop(event.getClient(), event.getOwner(), event.getSign(), event.getOwnerInventory());
    }

    @EventHandler
    public void onTrade(TransactionEvent event) {
        if(!isManaged(event.getSign().getWorld())
                || event.getTransactionType() != TransactionEvent.TransactionType.BUY
                || PriceUtil.hasSellPrice(event.getSign().getLine(2))
                || hasItems(event.getStock(), event.getOwnerInventory())
                ) {
            return;
        }
        final TransactionEvent finalEvent = event;
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            public void run() {
                removeShop(finalEvent.getClient(), finalEvent.getOwner(), finalEvent.getSign(), finalEvent.getOwnerInventory());
            }
        }, 1L);
    }

    private boolean removeShop(Player client, OfflinePlayer owner, Sign sign, Inventory inventory) {
        // Check if we can safely cleanup this shop. Adminshops don't need cleanup!
        boolean cleanupPossible = !(inventory instanceof AdminInventory)
                && inventory.getHolder() instanceof Chest
                && sign.equals(uBlock.getConnectedSign((Chest) inventory.getHolder()));

        if(!cleanupPossible) {
            return false;
        }

        Chest connectedChest = (Chest) inventory.getHolder();
        ShopDestroyedEvent destroyedEvent = new ShopDestroyedEvent(null, sign, connectedChest);
        ChestShop.callEvent(destroyedEvent);

        for(int i = 0; i < 4; i++) {
            sign.setLine(i, "");
        }

        // Remove enchantment info sign if there is one
        Block above = sign.getBlock().getRelative(BlockFace.UP);
        if(above.getState() instanceof Sign) {
            Sign enchSign = (Sign) above.getState();
            for(int i = 0; i < 4; i++) {
                enchSign.setLine(i, "");
            }
            enchSign.update(true);
        }
        sign.update(true);

        // We don't care about items which the shop shouldn't sell, remove them!
        inventory.setContents(new ItemStack[inventory.getSize() - 1]);

        Location loc = sign.getBlock().getLocation();
        Map<String, String> replacements = ImmutableMap.of(
                "world", loc.getWorld().getName(),
                "location", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
        );

        if(!messageBuyer.isEmpty()) {
            client.sendMessage(plugin.buildMsg(messageBuyer, replacements));
        }
        if(!messageOwner.isEmpty()) {
            if(owner.isOnline()) {
                owner.getPlayer().sendMessage(plugin.buildMsg(messageOwner, replacements));
            }
        }
        return true;
    }

    private boolean hasItems(ItemStack[] items, Inventory inventory) {
        for(ItemStack item : items) {
            if(!inventory.containsAtLeast(item, item.getAmount() * 2)) {
                return false;
            }
        }

        return true;
    }
}
