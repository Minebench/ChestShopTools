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
import de.themoep.chestshoptools.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

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
    private Map<UUID, String> offlineMessages = new HashMap<UUID, String>();

    public EmptyManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
        messageOwner = ChatColor.translateAlternateColorCodes('&', config.getString("messages.owner", ""));
        messageBuyer = ChatColor.translateAlternateColorCodes('&', config.getString("messages.buyer", ""));
        try {
            offlineMessages = (Map<UUID, String>) readMap("offlinemessages.map");
        } catch(ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "offlinemessages.map did not contain a UUID-String-Map or is corrupted.");
        }
    }

    @Override
    public void disable() {
        writeMap(offlineMessages, "offlinemessages.map");
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        if(offlineMessages.containsKey(playerId)) {
            final String msg = offlineMessages.remove(playerId);
            new BukkitRunnable() {
                public void run() {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if(player != null && player.isOnline()) {
                        player.sendMessage(msg);
                    } else {
                        cacheMessage(playerId, msg);
                    }
                }
            }.runTaskLater(plugin, 100);
        }
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

        removeShop(event.getClient(), event.getOwner(), event.getSign(), event.getOwnerInventory(), event.getStock(), event.getPrice());
    }

    @EventHandler
    public void onTrade(final TransactionEvent event) {
        if(!isManaged(event.getSign().getWorld())
                || event.getTransactionType() != TransactionEvent.TransactionType.BUY
                || PriceUtil.hasSellPrice(event.getSign().getLine(2))
                || hasItems(event.getStock(), event.getOwnerInventory())
                ) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> removeShop(event.getClient(), event.getOwner(), event.getSign(), event.getOwnerInventory(), event.getStock(), event.getPrice()));
    }

    private boolean removeShop(Player client, OfflinePlayer owner, Sign sign, Inventory inventory, ItemStack[] stock, double price) {
        // Check if we can safely cleanup this shop. Adminshops don't need cleanup!
        boolean cleanupPossible = !(inventory instanceof AdminInventory)
                && inventory.getHolder() instanceof Chest
                && sign.equals(uBlock.getConnectedSign((Chest) inventory.getHolder()));

        if(!cleanupPossible) {
            return false;
        }

        Chest connectedChest = (Chest) inventory.getHolder();
        ShopDestroyedEvent destroyedEvent = new ShopDestroyedEvent(client, sign, connectedChest);
        ChestShop.callEvent(destroyedEvent);

        for(int i = 0; i < 4; i++) {
            sign.setLine(i, "");
        }
        sign.update(true);

        // We don't care about items which the shop shouldn't sell, remove them!
        inventory.setContents(new ItemStack[inventory.getSize() - 1]);

        ItemStack item = null;
        if(stock.length > 0) {
            item = stock[0];
        }
        Location loc = sign.getBlock().getLocation();
        Map<String, String> replacements = ImmutableMap.of(
                "world", loc.getWorld().getName(),
                "location", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                "price", Double.toString(price),
                "item", item != null ? Utils.humanize(item.getType().toString()) + " ": ""
        );

        if(!messageBuyer.isEmpty()) {
            client.sendMessage(plugin.buildMsg(messageBuyer, replacements));
        }
        if(!messageOwner.isEmpty()) {
            String msg = plugin.buildMsg(messageOwner, replacements);
            if(owner.isOnline()) {
                owner.getPlayer().sendMessage(msg);
            } else {
                cacheMessage(owner.getUniqueId(), msg);
            }
        }
        plugin.getLogger().log(Level.INFO, "Removed empty " + (item != null ? item.getType().toString() : "") + " shop by " + owner.getName() + " in " + loc.getWorld().getName() + " at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
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

    private void cacheMessage(UUID playerId, String msg) {
        if(offlineMessages.containsKey(playerId)) {
            offlineMessages.put(playerId, offlineMessages.get(playerId) + "\n" + msg);
        } else {
            offlineMessages.put(playerId, msg);
        }
    }

    /**
     * Writes a Hashmap to a file
     * @param object The Hashmap to write
     * @param outputFile The file to write to
     */
    public void writeMap(Object object, String outputFile) {
        try {
            File file = new File(plugin.getDataFolder(), outputFile);
            if (!file.isFile()) {
                if(!file.createNewFile()){
                    throw new IOException("Error creating new file: " + file.getPath());
                }
            }
            FileOutputStream fileOut = new FileOutputStream(file.getPath());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(object);
            out.close();
            fileOut.close();
            plugin.getLogger().fine("Serialized data is saved in " + file.getPath());
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Reads a Hashmap from a file
     * @param inputFile The file to read from
     * @return An Object which is a HashMap<Object,Object>
     */
    @SuppressWarnings("unchecked")
    public Object readMap(String inputFile) {
        Map<Object, Object> map = new HashMap<Object,Object>();
        File file = new File(plugin.getDataFolder(), inputFile);
        if (!file.isFile()) {
            plugin.getLogger().log(Level.INFO, "No file found in " + file.getPath());
            try {
                if(!file.createNewFile()) {
                    throw new IOException("Error while creating new file: " + file.getPath());
                } else {
                    writeMap(map, inputFile);
                    plugin.getLogger().log(Level.INFO, "New file created in " + file.getPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileInputStream fileIn = new FileInputStream(file.getPath());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            map = (HashMap<Object, Object>) in.readObject();
            in.close();
            fileIn.close();
            plugin.getLogger().log(Level.INFO, "Sucessfully loaded cooldown.map.");
        } catch(IOException i) {
            plugin.getLogger().log(Level.WARNING, "No saved Map found in " + inputFile);
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }
}
