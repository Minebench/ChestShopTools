package de.themoep.chestshoptools.manager;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import de.themoep.chestshoptools.ChestShopTools;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class EnchantManager extends AbstractManager {

    public EnchantManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
    }

    @EventHandler
    public void onShopCreation(ShopCreatedEvent event) {
        if(!isManaged(event.getSign().getWorld())) {
            return;
        }

        BlockState above = event.getSign().getBlock().getRelative(BlockFace.UP).getState();
        if(above instanceof Sign) {
            Sign sign = (Sign) above;
            String itemLine = event.getSignLine((short) 3);
            ItemStack item = ChestShop.getItemDatabase().getFromCode(itemLine.substring(itemLine.indexOf('#') + 1));
            if(item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                List<String> lines = new ArrayList<String>();
                if(meta.hasEnchants() && !meta.getEnchants().isEmpty()) {
                    for(Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                        String enchName = getHumanName(entry.getKey());
                        String line = enchName + " " + entry.getValue();
                        if(line.length() > 15) {
                            int nameLenght = 15 - (" " + entry.getValue()).length();
                            line = enchName.substring(0, nameLenght ) + " " + entry.getValue();
                        }
                        lines.add(ChatColor.AQUA + line);
                        if(lines.size() >= 4) {
                            break;
                        }
                    }
                }
                if(meta instanceof EnchantmentStorageMeta) {
                    for(Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
                        String enchName = getHumanName(entry.getKey());
                        String line = enchName + " " + entry.getValue();
                        if(line.length() > 15) {
                            int nameLenght = 15 - (" " + entry.getValue()).length();
                            line = enchName.substring(0, nameLenght ) + " " + entry.getValue();
                        }
                        lines.add(ChatColor.YELLOW + line);
                        if(lines.size() >= 4) {
                            break;
                        }
                    }
                }
                for(int i = 0; i < 4; i++) {
                    sign.setLine(i, "");
                }
                int lineNmbr = lines.size() >= 3 ? 0 : 1;
                for(String line : lines) {
                    sign.setLine(lineNmbr, line);
                    lineNmbr++;
                    if(lineNmbr >= 4) {
                        break;
                    }
                }
                sign.update(true);
            }
        }
    }

    private String getHumanName(Enchantment enchantment) {
        try {
            EnchantName enchName = EnchantName.valueOf(enchantment.getName());
            return enchName.getName();
        } catch(IllegalArgumentException  e) {
            return humanize(enchantment.getName());
        }
    }

    private String humanize(String string) {
        String newString = string.toLowerCase().replace('_', ' ');
        int i = -1;
        do {
            try {
                newString = newString.substring(0, i + 1) + newString.substring(i + 1, i + 2).toUpperCase() + newString.substring(i + 2);
            } catch(IndexOutOfBoundsException e) {
                plugin.getLogger().log(Level.WARNING, "Could not humanize " + string + "! Returning " + newString + "! (i == " + i + ")");
                break;
            }
            i = newString.indexOf(' ', i + 2);
        } while(i != -1 && i + 2 < newString.length());
        return newString;
    }

    private enum EnchantName {
        PROTECTION_ENVIRONMENTAL    ("Protection"),
        PROTECTION_FIRE             ("Fire Prot."),
        PROTECTION_FALL             ("Feather Fall."),
        PROTECTION_EXPLOSIONS       ("Blast Prot."),
        PROTECTION_PROJECTILE       ("Proj. Prot."),
        OXYGEN                      ("Respiration"),
        WATER_WORKER                ("Aqua Affinity"),
        DAMAGE_ALL                  ("Sharpness"),
        DAMAGE_UNDEAD               ("Smite"),
        DAMAGE_ARTHROPODS           ("Bane of Arthr."),
        LOOT_BONUS_MOBS             ("Looting"),
        DIG_SPEED                   ("Efficiency"),
        DURABILITY                  ("Unbreaking"),
        LOOT_BONUS_BLOCKS           ("Fortune"),
        ARROW_DAMAGE                ("Power"),
        ARROW_KNOCKBACK             ("Punch"),
        ARROW_FIRE                  ("Flame"),
        ARROW_INFINITE              ("Infinity");

        private final String name;

        EnchantName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
