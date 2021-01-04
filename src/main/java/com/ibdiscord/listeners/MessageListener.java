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

package com.ibdiscord.listeners;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.waiter.Waiter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDateTime;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getChannel().getId() != Modmail.INSTANCE.getConfig().getChannelId()) {
            return;
        }

        Waiter.INSTANCE.input(event.getMember(), event.getMessage().getContentRaw());
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            long userID = event.getAuthor().getIdLong();
            PreparedStatement pst = con.prepareStatement("SELECT \"ticket_id\", \"timeout\" FROM \"mm_tickets\" WHERE \"user\"=? AND \"open\"=TRUE;");
            pst.setLong(1, userID);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                //TODO: Make timeouts persist if ticket is closed?
                Timestamp timeout = result.getTimestamp("timeout");
                if (LocalDateTime.now().isBefore(timeout.toLocalDateTime())) {
                    return;
                }
            } else {
                System.out.println("No open ticket, creating ticket");
                pst = con.prepareStatement("INSERT INTO \"mm_tickets\" (\"user\")"
                        + "VALUES (?)"
                        + "RETURNING \"ticket_id\";"
                );
                pst.setLong(1, userID);
                result = pst.executeQuery();

                if (!result.next()) {
                    System.out.println("Could not create new ticket");
                    return;
                }
            }

            int ticketId = result.getInt("ticket_id");
            System.out.println(ticketId);

            pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, FALSE)"
            );
            pst.setInt(1, ticketId);
            pst.setLong(2, userID);
            pst.setString(3, event.getMessage().getContentRaw());
            pst.execute();

            event.getMessage().addReaction("U+1F4E8").queue();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
