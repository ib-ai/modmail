package com.ibdiscord;

import com.ibdiscord.data.LocalConfig;
import com.ibdiscord.data.db.DataContainer;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public enum Modmail {

    INSTANCE;

    @Getter private LocalConfig config;
    @Getter private JDA jda;

    public static void main(String[] args) {
        //TODO: Check Java Version

        Thread.currentThread().setName("Main");
        Modmail.INSTANCE.init();
    }

    private void init() {
        config = new LocalConfig();
        DataContainer.INSTANCE.connect();
        try {
            //TODO: Further settings
            jda = JDABuilder.createDefault(config.getBotToken())
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();
            jda.setAutoReconnect(true);
            jda.awaitReady();
        } catch (LoginException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

}
