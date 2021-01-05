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
import com.ibdiscord.utils.UChannel;
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.utils.UFormatter;
import com.ibdiscord.utils.objects.Ticket;
import com.ibdiscord.utils.objects.TicketResponse;
import com.ibdiscord.waiter.Waiter;
import net.dv8tion.jda.api.entities.Guild;
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

            pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, FALSE)"
            );
            pst.setInt(1, ticketId);
            pst.setLong(2, userID);
            pst.setString(3, event.getMessage().getContentRaw());
            pst.execute();

            event.getMessage().addReaction("U+1F4E8").queue();

            Guild guild = event.getJDA().getGuildById(Modmail.INSTANCE.getConfig().getGuildID());

            Ticket ticket = new Ticket(guild.getMember(event.getAuthor()));
            pst = con.prepareStatement("SELECT \"user\", \"response\", \"timestamp\" FROM \"mm_ticket_responses\" WHERE \"ticket_id\"=? ORDER BY \"response_id\" ASC");
            pst.setLong(1, ticketId);
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                ticket.addResponse(new TicketResponse(guild.getMemberById(results.getLong("user")),
                        results.getString("response"),
                        results.getTimestamp("timestamp")));
            }
            MessageEmbed ticketEmbed = UFormatter.ticketEmbed(ticket);

            //TODO: remove old message, if exists
            TextChannel modmailChannel = UChannel.getModmailChannel(guild);
            if (modmailChannel != null) {
                Message message = modmailChannel.sendMessage(ticketEmbed).complete();
                if (message != null) {
                    message.addReaction(UEmoji.REPLY_TICKET_EMOJI).queue();
                    message.addReaction(UEmoji.CLOSE_TICKET_EMOJI).queue();
                    message.addReaction(UEmoji.TIMEOUT_TICKET_EMOJI).queue();
                    pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"message_id\"=? WHERE \"ticket_id\"=?");
                    pst.setLong(1, message.getIdLong());
                    pst.setLong(2, ticketId);
                    if (pst.executeUpdate() > 0) {
                        //TODO:
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
