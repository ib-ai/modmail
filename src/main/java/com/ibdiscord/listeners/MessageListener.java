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
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.utils.UFormatter;
import com.ibdiscord.waiter.Waiter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (!Objects.equals(event.getChannel().getId(), Modmail.INSTANCE.getConfig().getChannelId())) {
            return;
        }

        Waiter.INSTANCE.input(event.getMember(), event.getMessage().getContentRaw());
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        //Check if user is in guild?

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            long userID = event.getAuthor().getIdLong();

            //Check for timeout
            PreparedStatement pst = con.prepareStatement("SELECT \"timeout\" FROM \"mm_tickets\" WHERE \"user\"=? ORDER BY \"timeout\" DESC;");
            pst.setLong(1, userID);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                //Get latest timeout
                Timestamp timeout = result.getTimestamp("timeout");
                //Check if latest timeout is still active
                if (LocalDateTime.now().isBefore(timeout.toLocalDateTime())) {
                    //Send message about being timed out.
                    event.getChannel().sendMessage(UFormatter.timeoutMessage(timeout)).queue();
                    return;
                }
            }

            //Get ticket info
            pst = con.prepareStatement("SELECT \"ticket_id\", \"message_id\" FROM \"mm_tickets\" WHERE \"user\"=? AND \"open\"=TRUE;");
            pst.setLong(1, userID);
            result = pst.executeQuery();

            long messageId = -1;

            if (result.next()) {
                //If ticket already open, get current message id of ticket
                messageId = result.getLong("message_id");
            } else {
                //Otherwise, open new ticket
                pst = con.prepareStatement("INSERT INTO \"mm_tickets\" (\"user\")"
                        + "VALUES (?)"
                        + "RETURNING \"ticket_id\";"
                );
                pst.setLong(1, userID);
                result = pst.executeQuery();

                if (!result.next()) {
                    //TODO: Log failure to create new ticket
                    return;
                }
            }

            //Get ticket ID
            int ticketId = result.getInt("ticket_id");

            //Insert new response
            pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, FALSE)"
            );
            pst.setInt(1, ticketId);
            pst.setLong(2, userID);
            pst.setString(3, event.getMessage().getContentRaw());
            if (pst.executeUpdate() == 0) {
                //TODO: Log failure to insert new response
            }

            //Remove old ticket message and send new one
            TextChannel modmailChannel = Modmail.INSTANCE.getModmailChannel();
            if (messageId != -1) {
                modmailChannel.deleteMessageById(messageId).queue();
            }

            //Format new ticket
            MessageEmbed ticketEmbed = UFormatter.ticketEmbed(ticketId);
            Message message = modmailChannel.sendMessage(ticketEmbed).complete();
            if (message != null) {
                event.getMessage().addReaction("U+1F4E8").queue();

                message.addReaction(UEmoji.REPLY_TICKET_EMOJI).queue();
                message.addReaction(UEmoji.CLOSE_TICKET_EMOJI).queue();
                message.addReaction(UEmoji.TIMEOUT_TICKET_EMOJI).queue();
                pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"message_id\"=? WHERE \"ticket_id\"=?");
                pst.setLong(1, message.getIdLong());
                pst.setLong(2, ticketId);
                if (pst.executeUpdate() == 0) {
                    //TODO: Log failure to update ticket's message id
                }
            } else {
                //TODO: Log failure to send new ticket.
                event.getChannel().sendMessage("Sorry, we encountered an error and your message wasn't sent through. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
