/* Copyright 2020-2021 Ray Clark <raynichclark@gmail.com>
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl("jdbc:postgresql://db/postgre");
        dbConfig.setUsername("postgre");
        dbConfig.setPassword("toor");

        dbConfig.setMaximumPoolSize(30);
        dbConfig.setLeakDetectionThreshold(2500);

        ds = new HikariDataSource(dbConfig);

        initialize();
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

    /**
     * Initialize the Database.
     */
    private void initialize() {
        try (Connection con = getConnection()) {
            //Ticket Table
            PreparedStatement pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS \"mm_tickets\" ( "
                    + "\"ticket_id\" SERIAL PRIMARY KEY,"
                    + "\"user\" bigint NOT NULL,"
                    + "\"open\" boolean DEFAULT TRUE NOT NULL,"
                    + "\"timeout\" timestamp DEFAULT Now(),"
                    + "\"message_id\" bigint"
                    + ");"
            );
            pst.execute();

            pst = con.prepareStatement("CREATE INDEX IF NOT EXISTS \"mm_tickets_user\" ON \"mm_tickets\" (\"user\")");
            pst.execute();

            //Ticket Response Table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS \"mm_ticket_responses\" ("
                    + "  \"response_id\" SERIAL PRIMARY KEY,"
                    + "  \"ticket_id\" int REFERENCES \"mm_tickets\","
                    + "  \"user\" bigint NOT NULL,"
                    + "  \"response\" text NOT NULL,"
                    + "  \"timestamp\" timestamp DEFAULT Now(),"
                    + "  \"as_server\" boolean NOT NULL"
                    + ");"
            );
            pst.execute();

            pst = con.prepareStatement("CREATE INDEX IF NOT EXISTS \"mm_ticket_responses_ticket_id\" ON \"mm_ticket_responses\" (\"ticket_id\")");
            pst.execute();

            pst = con.prepareStatement("CREATE INDEX IF NOT EXISTS \"mm_ticket_responses_user\" ON \"mm_ticket_responses\" (\"user\")");
            pst.execute();

            //Timeout Table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXIST \"mm_timeouts\" ("
                    + "  \"timeout_id\" SERIAL PRIMARY KEY,"
                    + "  \"user\" bigint NOT NULL,"
                    + "  \"timestamp\" timestamp DEFAULT Now()"
                    + ");"
            );
            pst.execute();

            pst = con.prepareStatement("CREATE INDEX IF NOT EXISTS \"mm_timeouts_user\" ON \"mm_timeouts\" (\"user\")");
            pst.execute();

            /*
            //Ticket Log Table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS \"mm_ticket_log\" ("
                    + "  \"log_id\" SERIAL PRIMARY KEY,"
                    + "  \"ticket_id\" int REFERENCES \"mm_tickets\","
                    + "  \"user\" bigint NOT NULL,"
                    + "  \"action\" ENUM('close', 'timeout') NOT NULL,"
                    + "  \"timestamp\" timestamp DEFAULT Now()"
                    + ");"
            );
            pst.execute();

            pst = con.prepareStatement("CREATE INDEX IF NOT EXISTS \"mm_ticket_log_ticket_id\" ON \"mm_ticket_log\" (\"ticket_id\")");
            pst.execute();
             */
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
