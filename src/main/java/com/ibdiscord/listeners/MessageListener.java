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
import net.dv8tion.jda.api.entities.Member;
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

        String prefix = Modmail.INSTANCE.getConfig().getBotPrefix();
        String message = event.getMessage().getContentRaw();
        if (message.startsWith(prefix)) {
            String[] args = message.substring(1).split(" ", 2);
            if (args.length > 0 && args[0].equalsIgnoreCase("send")) {
                //TODO: Permission checking?
                if (args.length > 1) {
                    Member member = null;
                    if (event.getMessage().getMentionedMembers().size() > 0) {
                        member = event.getMessage().getMentionedMembers().get(0);
                    } else {
                        try {
                            member = event.getGuild().getMemberByTag(args[1]);
                        } catch (IllegalArgumentException e) {
                            //Fuck checkstyle
                        }
                        if (member == null) {
                            try {
                                member = event.getGuild().getMemberById(args[1]);
                            } catch (IllegalArgumentException e) {
                                //Fuck checkstyle
                            }
                        }
                    }

                    if (member != null) {
                        long userID = member.getUser().getIdLong();
                        try (Connection con = DataContainer.INSTANCE.getConnection()) {
                            PreparedStatement pst = con.prepareStatement("SELECT \"ticket_id\" FROM \"mm_tickets\" WHERE \"user\"=? AND \"open\"=TRUE;");
                            pst.setLong(1, member.getUser().getIdLong());
                            ResultSet result = pst.executeQuery();

                            if (result.next()) {
                                //Ticket already open
                                event.getChannel().sendMessage(String.format("There is already a ticket open for %s.", member.getUser().getAsTag())).queue();
                            } else {
                                //Open new ticket
                                pst = con.prepareStatement("INSERT INTO \"mm_tickets\" (\"user\")"
                                        + "VALUES (?)"
                                        + "RETURNING \"ticket_id\";"
                                );
                                pst.setLong(1, member.getUser().getIdLong());
                                result = pst.executeQuery();

                                if (!result.next()) {
                                    Modmail.INSTANCE.getLogger().error("Failed to create new ticket for %d.", userID);
                                    event.getChannel().sendMessage(String.format("An error occurred when creating a ticket for %s.", member.getUser().getAsTag())).queue();
                                } else {
                                    int ticketId = result.getInt("ticket_id");

                                    //Format new ticket
                                    MessageEmbed ticketEmbed = UFormatter.ticketEmbed(ticketId);
                                    Modmail.INSTANCE.getModmailChannel().sendMessage(ticketEmbed).queue(
                                        embed -> postProcessTicketEmbed(ticketId, embed),
                                        failure -> {
                                            Modmail.INSTANCE.getLogger().error("Failed to send ticket message for ticket %d", ticketId);
                                            event.getChannel().sendMessage("An error occurred when sending the ticket. Please try again.").queue();
                                        });
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        event.getChannel().sendMessage("Invalid user.").queue();
                    }
                } else {
                    event.getChannel().sendMessage(String.format("Usage %ssend <User>.", prefix)).queue();
                }
            } else {
                event.getChannel().sendMessage("Command not recognized.").queue();
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
                    Modmail.INSTANCE.getLogger().error("Failed to create new ticket for %d.", userID);
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
            pst.setString(3, response);
            if (pst.executeUpdate() == 0) {
                Modmail.INSTANCE.getLogger().error("Failed to insert new response from %d.", userID);
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
                message -> {
                    event.getMessage().addReaction("U+1F4E8").queue();
                    postProcessTicketEmbed(ticketId, message);
                },
                failure -> {
                    Modmail.INSTANCE.getLogger().error("Failed to send ticket message for ticket %d", ticketId);
                    event.getChannel().sendMessage("Sorry, we encountered an error and your message wasn't sent through. Please try again.").queue();
                });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void postProcessTicketEmbed(int ticketId, Message message) {
        message.addReaction(UEmoji.REPLY_TICKET_EMOJI).queue();
        message.addReaction(UEmoji.CLOSE_TICKET_EMOJI).queue();
        message.addReaction(UEmoji.TIMEOUT_TICKET_EMOJI).queue();
        try (Connection con1 = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst1 = con1.prepareStatement("UPDATE \"mm_tickets\" SET \"message_id\"=? WHERE \"ticket_id\"=?");
            pst1.setLong(1, message.getIdLong());
            pst1.setLong(2, ticketId);
            if (pst1.executeUpdate() == 0) {
                Modmail.INSTANCE.getLogger().error("Failed to set new message id for ticket %d", ticketId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
