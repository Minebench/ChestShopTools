package de.themoep.chestshoptools;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class Utils {

    public static String humanize(String string) {
        String newString = string.toLowerCase().replace('_', ' ');
        int i = -1;
        do {
            try {
                newString = newString.substring(0, i + 1) + newString.substring(i + 1, i + 2).toUpperCase() + newString.substring(i + 2);
            } catch (IndexOutOfBoundsException e) {
                System.out.print("[ERROR] Could not humanize " + string + "! Returning " + newString + "! (i == " + i + ")");
                break;
            }
            i = newString.indexOf(' ', i + 2);
        } while (i != -1 && i + 2 < newString.length());
        return newString;
    }
}
