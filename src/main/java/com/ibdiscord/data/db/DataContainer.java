package com.ibdiscord.data.db;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.LocalConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public enum DataContainer {

    INSTANCE;

    private HikariDataSource ds;

    public void connect() {
        if (ds != null)
            return;

        LocalConfig config = Modmail.INSTANCE.getConfig();

        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl(String.format("jdbc:postgresql://%s/%s", config.getDbIP(), config.getDbName()));
        dbConfig.setUsername(config.getDbUsername());
        dbConfig.setPassword(config.getDbPassword());

        ds = new HikariDataSource(dbConfig);
    }

    public Connection getConnection() throws SQLException {
        if (ds == null)
            connect();

        return ds.getConnection();
    }

}
