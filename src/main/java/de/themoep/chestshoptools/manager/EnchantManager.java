package de.themoep.chestshoptools.manager;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import de.themoep.chestshoptools.ChestShopTools;
import de.themoep.chestshoptools.Utils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class EnchantManager extends AbstractManager {

    private final ChatColor enchantmentColor;
    private final ChatColor enchantmentStorageColor;
    private final boolean autoCreateSign;

    public EnchantManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
        autoCreateSign = config.getBoolean("auto-create-sign");
        enchantmentColor = getColor("enchant-color", ChatColor.AQUA);
        enchantmentStorageColor = getColor("enchant-storage-color", ChatColor.YELLOW);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onShopCreation(ShopCreatedEvent event) {
        if (!isManaged(event.getSign().getWorld())) {
            return;
        }
        String itemLine = event.getSignLine((short) 3);
        ItemStack item = ChestShop.getItemDatabase().getFromCode(itemLine.substring(itemLine.indexOf('#') + 1));
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasEnchants() || meta instanceof EnchantmentStorageMeta) {
                Block blockAbove = event.getSign().getBlock().getRelative(BlockFace.UP);
                BlockState above = blockAbove.getState();

                // Create sign if there is none if auto creation is enabled
                if (autoCreateSign && !(above instanceof Sign)) {
                    blockAbove.setType(event.getSign().getType());
                    above = blockAbove.getState();
                    BlockData aboveData = above.getBlockData();
                    BlockData signData = event.getSign().getBlockData();
                    if (aboveData instanceof Directional && signData instanceof Directional) {
                        ((Directional) aboveData).setFacing(((Directional) signData).getFacing());
                    } else if (aboveData instanceof Rotatable && signData instanceof Rotatable) {
                        ((Rotatable) aboveData).setRotation(((Rotatable) signData).getRotation());
                    }
                    above.setBlockData(aboveData);
                }

                // Write enchant info on the sign
                if (above instanceof Sign) {
                    Sign sign = (Sign) above;

                    List<String> lines = new ArrayList<String>();
                    if (meta.hasEnchants() && !meta.getEnchants().isEmpty()) {
                        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                            String enchName = getHumanName(entry.getKey());
                            String line = enchName + " " + entry.getValue();
                            if (line.length() > 15) {
                                int nameLenght = 15 - (" " + entry.getValue()).length();
                                line = enchName.substring(0, nameLenght) + " " + entry.getValue();
                            }
                            lines.add(enchantmentColor + line);
                            if (lines.size() >= 4) {
                                break;
                            }
                        }
                    }
                    if (meta instanceof EnchantmentStorageMeta) {
                        for (Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
                            String enchName = getHumanName(entry.getKey());
                            String line = enchName + " " + entry.getValue();
                            if (line.length() > 15) {
                                int nameLenght = 15 - (" " + entry.getValue()).length();
                                line = enchName.substring(0, nameLenght) + " " + entry.getValue();
                            }
                            lines.add(enchantmentStorageColor + line);
                            if (lines.size() >= 4) {
                                break;
                            }
                        }
                    }
                    for (int i = 0; i < 4; i++) {
                        sign.setLine(i, "");
                    }
                    int lineNmbr = lines.size() >= 3 ? 0 : 1;
                    for (String line : lines) {
                        sign.setLine(lineNmbr, line);
                        lineNmbr++;
                        if (lineNmbr >= 4) {
                            break;
                        }
                    }
                    sign.update(true);
                }
            }
        }
    }

    @EventHandler
    public void onShopRemoved(ShopDestroyedEvent event) {
        if (!isManaged(event.getSign().getWorld())) {
            return;
        }

        // Remove enchantment info sign if there is one
        Block above = event.getSign().getBlock().getRelative(BlockFace.UP);
        BlockState state = above.getState();
        if (state instanceof Sign) {
            Sign enchSign = (Sign) state;
            if (isEnchantmentSign(enchSign)) {
                for (int i = 0; i < 4; i++) {
                    enchSign.setLine(i, "");
                }
                enchSign.update(true);
            }
        }
    }

    public boolean isEnchantmentSign(Sign sign) {
        int nonEmptyLines = 0;
        for (String line : sign.getLines()) {
            if (!line.isEmpty()) {
                if (!line.startsWith(enchantmentColor.toString()) && !line.startsWith(enchantmentStorageColor.toString())) {
                    return false;
                }
                nonEmptyLines++;
            }
        }
        return sign.getLines().length > 0 && nonEmptyLines > 0;
    }

    private String getHumanName(Enchantment enchantment) {
        String name = config.getString("aliases." + enchantment.getName().toLowerCase(), null);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return Utils.humanize(enchantment.getName());
    }

    private ChatColor getColor(String key, ChatColor defaultColor) {
        try {
            return ChatColor.valueOf(config.getString(key).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(getName() + ": " + config.getString(key) + " is not a valid color for " + key);
        }
        return defaultColor;
    }
}
