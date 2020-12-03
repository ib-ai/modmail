/* Copyright 2020 Ray Clark <raynichclark@gmail.com>
 *
 * This file is part of Modmail.
 *
 * Modmail is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modmail is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modmail. If not, see http://www.gnu.org/licenses/.
 *
 */

package com.ibdiscord.data;

import lombok.Getter;

public final class LocalConfig {

    @Getter private final String botToken;

    @Getter private final String dbIP;

    @Getter private final String dbUsername;

    @Getter private final String dbPassword;

    @Getter private final String dbName;

    /**
     * Constructor for the local configuration object.
     * Sets all of the class properties to their corresponding environment
     * variable.
     */
    public LocalConfig() {
        this.botToken = getEnvironment("TOKEN", "");

        this.dbIP = getEnvironment("DATABASE_IP", "");
        this.dbName = getEnvironment("DATABASE_NAME", "");
        this.dbUsername = getEnvironment("DATABASE_USERNAME", "");
        this.dbPassword = getEnvironment("DATABASE_PASSWORD", "");
    }

    /**
     * Gets an environment variable.
     * @param key The key.
     * @param fallback The fallback value.
     * @return A non null value.
     */
    private String getEnvironment(String key, String fallback) {
        String value = System.getenv(key);
        return value == null ? fallback : value;
    }
}
