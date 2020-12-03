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

package com.ibdiscord.data.db;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.LocalConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public enum DataContainer {

    /**
     * Singleton instance of container.
     */
    INSTANCE;

    private HikariDataSource ds;

    /**
     * Connect to the database.
     */
    public void connect() {
        if (ds != null) {
            return;
        }

        LocalConfig config = Modmail.INSTANCE.getConfig();

        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl(String.format("jdbc:postgresql://%s/%s", config.getDbIP(), config.getDbName()));
        dbConfig.setUsername(config.getDbUsername());
        dbConfig.setPassword(config.getDbPassword());

        dbConfig.setMaximumPoolSize(30);
        dbConfig.setLeakDetectionThreshold(2500);

        ds = new HikariDataSource(dbConfig);
    }

    /**
     * Close the database connection pool.
     */
    public void close() {
        if (ds != null) {
            ds.close();
        }
    }

    /**
     * Get a Database Connection.
     * @return Connection
     * @throws SQLException SQLException
     */
    public Connection getConnection() throws SQLException {
        if (ds == null) {
            connect();
        }

        return ds.getConnection();
    }

}
