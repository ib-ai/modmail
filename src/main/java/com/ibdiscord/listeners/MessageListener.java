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
import com.ibdiscord.command.Command;
import com.ibdiscord.command.CommandContext;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.utils.UFormatter;
import com.ibdiscord.utils.UTicket;
import com.ibdiscord.waiter.Waiter;
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

        String prefix = Modmail.INSTANCE.getConfig().getBotPrefix();
        String message = event.getMessage().getContentRaw();
        if (message.startsWith(prefix)) {
            String commandName = message.substring(prefix.length(), message.indexOf(" "));
            Command command = Modmail.INSTANCE.getCommandRegistry().query(commandName);
            if (command != null) {
                command.execute(CommandContext.construct(event.getMessage()));
            }
            return;
        }

        String response = UFormatter.formatResponse(event.getMessage());
        if (response.isEmpty()) {
            return;
        }

        Waiter.INSTANCE.input(event.getMember(), response);
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

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

            String response = UFormatter.formatResponse(event.getMessage());
            if (response.isEmpty()) {
                return;
            }

            //TODO: Implement a proper fix
            if (response.length() > 1000) {
                event.getChannel().sendMessage("Your message is too long. Please shorten your message or send in multiple parts.").queue();
                return;
            }

            //Get ticket info
            pst = con.prepareStatement("SELECT \"ticket_id\", \"message_id\" FROM \"mm_tickets\" WHERE \"user\"=? AND \"open\"=TRUE;");
            pst.setLong(1, userID);
            result = pst.executeQuery();

            int ticketId;
            long messageId = -1;

            if (result.next()) {
                //If ticket already open, get current message id of ticket
                ticketId = result.getInt("ticket_id");
                messageId = result.getLong("message_id");
            } else {
                //Otherwise, open new ticket
                ticketId = UTicket.openTicket(userID);
            }

            //Insert new response
            pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, FALSE)"
            );
            pst.setInt(1, ticketId);
            pst.setLong(2, userID);
            pst.setString(3, response);
            if (pst.executeUpdate() == 0) {
                Modmail.INSTANCE.getLogger().error("Failed to insert new response from {}.", userID);
                return;
            }

            //Remove old ticket message and send new one
            TextChannel modmailChannel = Modmail.INSTANCE.getModmailChannel();
            if (messageId != -1) {
                modmailChannel.deleteMessageById(messageId).queue();
            }

            //Format new ticket
            MessageEmbed ticketEmbed = UFormatter.ticketEmbed(ticketId);
            modmailChannel.sendMessage(ticketEmbed).queue(
                embed -> {
                    event.getMessage().addReaction("U+1F4E8").queue();
                    UTicket.postProcessTicketEmbed(ticketId, embed);
                },
                failure -> {
                    Modmail.INSTANCE.getLogger().error("Failed to send ticket message for ticket {}", ticketId);
                    event.getChannel().sendMessage("Sorry, we encountered an error and your message wasn't sent through. Please try again.").queue();
                });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
