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

package com.ibdiscord;

import com.ibdiscord.data.LocalConfig;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.listeners.MessageListener;
import com.ibdiscord.listeners.ReactionListener;
import com.ibdiscord.listeners.ShutdownListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public enum Modmail {

    /**
     * Singleton instance of bot.
     */
    INSTANCE;

    @Getter private LocalConfig config;
    @Getter private Logger logger = LoggerFactory.getLogger(getClass());

    @Getter private JDA jda;
    @Getter private TextChannel modmailChannel;
    @Getter private Guild guild;

    /**
     * Entry point of the program.
     * @param args The arguments
     */
    public static void main(String[] args) {
        //TODO: Check Java Version

        Thread.currentThread().setName("Main");
        Modmail.INSTANCE.init();
    }

    /**
     * Initialize the bot.
     */
    private void init() {
        config = new LocalConfig();
        DataContainer.INSTANCE.connect();
        try {
            jda = JDABuilder.createLight(config.getBotToken(),
                    GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_MEMBERS
                    )
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .addEventListeners(new ShutdownListener(),
                            new MessageListener(),
                            new ReactionListener()
                    )
                    .build();
            jda.setAutoReconnect(true);
            jda.awaitReady();

            guild = jda.getGuildById(config.getGuildId());
            if (guild == null) {
                logger.error("Failed to get guild from provided ID.");
                jda.shutdownNow();
                return;
            }

            modmailChannel = guild.getTextChannelById(config.getChannelId());
            if (modmailChannel == null) {
                logger.error("Failed to get Modmail text channel from provided ID.");
                jda.shutdownNow();
                return;
            }
        } catch (LoginException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

}
