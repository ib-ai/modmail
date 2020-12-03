package com.ibdiscord.data;

import lombok.Getter;

public final class LocalConfig {

    @Getter private final String botToken;

    @Getter private final String dbIP;

    @Getter private final String dbUsername;

    @Getter private final String dbPassword;

    @Getter private final String dbName;

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
