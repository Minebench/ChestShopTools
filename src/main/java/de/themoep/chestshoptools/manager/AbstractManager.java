package de.themoep.chestshoptools.manager;

import de.themoep.chestshoptools.ChestShopTools;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
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
public abstract class AbstractManager implements Listener {

    ChestShopTools plugin;
    private Set<String> worldSet = new HashSet<String>();

    public AbstractManager(ChestShopTools plugin, ConfigurationSection config) {
        this.plugin = plugin;
        if(config != null && config.getBoolean("enabled", false)) {
            List<String> worldList = config.getStringList("worlds");
            if(worldList != null) {
                worldSet.addAll(worldList);
                if(worldList.size() > 0) {
                    plugin.getServer().getPluginManager().registerEvents(this, plugin);
                }
            }
        }
    }

    /**
     * Get wether or not a world is managed by this manager
     * @param world The world to check
     * @return <tt>true</tt> if it is managed; <tt>false</tt> if not
     */
    public boolean isManaged(World world) {
        return isManaged(world.getName());
    }

    /**
     * Get wether or not a world is managed by this manager
     * @param worldname The name of the world to check
     * @return <tt>true</tt> if it is managed; <tt>false</tt> if not
     */
    public boolean isManaged(String worldname) {
        return worldSet.contains(worldname) || worldSet.contains("*") || worldSet.contains(".*") || worldSet.contains("(.*)");
    }

    public abstract void disable();

}
