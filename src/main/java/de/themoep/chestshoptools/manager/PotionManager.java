package de.themoep.chestshoptools.manager;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.StringUtil;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
public class PotionManager extends AbstractManager {

    private final ChatColor goodColor;
    private final ChatColor neutralColor;
    private final ChatColor badColor;
    private final boolean autoCreateSign;
    private final boolean showExtended;
    private final boolean showUpgraded;
    private Set<PotionEffectType> badEffects = new HashSet<>();

    public PotionManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
        autoCreateSign = config.getBoolean("auto-create-sign");
        goodColor = getColor("good-color", ChatColor.BLUE);
        neutralColor = getColor("neutral-color", ChatColor.DARK_AQUA);
        badColor = getColor("bad-color", ChatColor.RED);
        showExtended = config.getBoolean("show-extended", false);
        showUpgraded = config.getBoolean("show-upgraded", false);

        for (String effect : config.getStringList("bad-effects")) {

        }
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
            if (meta instanceof PotionMeta potionMeta) {
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

                    List<String> lines = new ArrayList<>();
                    if (potionMeta.getBasePotionData().getType().getEffectType() != null) {
                        lines.add(getPotionInfo(potionMeta.getBasePotionData()));
                    }
                    for (PotionEffect effect : potionMeta.getCustomEffects()) {
                        lines.add(getPotionInfo(effect));
                        if (lines.size() >= 4) {
                            break;
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

    private String getPotionInfo(PotionData potionData) {
        String potionName = getHumanName(potionData.getType().getEffectType());
        String extra = "";
        if (showUpgraded && potionData.isUpgraded()) {
            extra += " II";
        }
        if (showExtended && potionData.isExtended()) {
            extra += " +";
        }
        return getColor(potionData.getType().getEffectType()) + combineSignLine(potionName, extra);
    }

    private String getPotionInfo(PotionEffect potionEffect) {
        String potionName = getHumanName(potionEffect.getType());
        String extra = "";
        if (showUpgraded && potionEffect.getAmplifier() > 0) {
            extra += " " + potionEffect.getAmplifier();
        }
        if (showExtended && potionEffect.getDuration() > 0) {
            int seconds = potionEffect.getDuration() / 20;
            extra += " " + String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60);
        }
        return getColor(potionEffect.getType()) + combineSignLine(potionName, extra);
    }

    private String combineSignLine(String potionName, String extra) {
        String line = potionName + extra;
        for (int i = potionName.length() - 1; i > 0 && StringUtil.getMinecraftStringWidth(line) > MaterialUtil.MAXIMUM_SIGN_WIDTH; i--) {
            line = potionName.substring(0, i) + extra;
        }
        return line;
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
            if (isPotionSign(enchSign)) {
                for (int i = 0; i < 4; i++) {
                    enchSign.setLine(i, "");
                }
                enchSign.update(true);
            }
        }
    }

    public boolean isPotionSign(Sign sign) {
        int nonEmptyLines = 0;
        for (String line : sign.getLines()) {
            if (!line.isEmpty()) {
                if (!line.startsWith(goodColor.toString()) && !line.startsWith(badColor.toString())) {
                    return false;
                }
                nonEmptyLines++;
            }
        }
        return sign.getLines().length > 0 && nonEmptyLines > 0;
    }

    private ChatColor getColor(PotionEffectType type) {
        return switch (type.getEffectCategory()) {
            case HARMFUL -> badColor;
            case NEUTRAL -> neutralColor;
            case BENEFICIAL -> goodColor;
        };
    }

    private String getHumanName(PotionEffectType potionType) {
        String name = config.getString("aliases." + potionType.getName().toLowerCase(), null);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return Utils.humanize(potionType.getName());
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
