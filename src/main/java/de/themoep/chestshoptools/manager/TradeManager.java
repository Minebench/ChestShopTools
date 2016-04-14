package de.themoep.chestshoptools.manager;

import de.themoep.chestshoptools.ChestShopTools;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

import java.util.List;

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
public class TradeManager extends AbstractManager {
    public TradeManager(ChestShopTools plugin, ConfigurationSection config) {
        super(plugin, config);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }
}
