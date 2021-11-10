/* Copyright 2021 Ray Clark <raynichclark@gmail.com>
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

package com.ibdiscord.utils;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.db.DataContainer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class UTicket {

    /**
     * Method to open a ticket for a user.
     * @param userID The user's ID.
     * @return The ticket ID of the user.
     * @throws SQLException Database Error.
     */
    public static int openTicket(long userID) throws SQLException {
        int ticketId = getTicket(userID);
        if (ticketId > -1) {
            return ticketId;
        }
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            Modmail.INSTANCE.getLogger().info("Creating new ticket for {}.", userID);
            PreparedStatement pst = con.prepareStatement("INSERT INTO \"mm_tickets\" (\"user\")"
                + "VALUES (?)"
                + "RETURNING \"ticket_id\";"
            );
            pst.setLong(1, userID);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                return result.getInt("ticket_id");
            } else {
                Modmail.INSTANCE.getLogger().error("Failed to create new ticket for {}.", userID);
                return -1;
            }
        }
    }

    /**
     * Method to close a ticket.
     * @param ticketId The ticket's ID.
     * @param member The member who closed the ticket.
     * @return True if successful, false otherwise.
     */
    public static boolean closeTicket(long ticketId, Member member) {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            //Close ticket
            PreparedStatement pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"open\"=FALSE WHERE \"ticket_id\"=?");
            pst.setLong(1, ticketId);
            if (pst.executeUpdate() > 0) {
                //Edit ticket message
                pst = con.prepareStatement("SELECT \"user\", \"message_id\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
                pst.setLong(1, ticketId);
                ResultSet result = pst.executeQuery();
                if (result.next()) {
                    long user = result.getLong("user");
                    Member ticketMember = Modmail.INSTANCE.getGuild().getMemberById(user);

                    long messageId = result.getLong("message_id");
                    if (messageId > 0 && ticketMember != null) {
                        Modmail.INSTANCE.getModmailChannel().retrieveMessageById(messageId).queue(
                            message -> {
                                message.editMessage(UFormatter.closedTicket(member, ticketMember)).queue();
                                message.clearReactions().queue();
                            },
                            failure -> {
                                Modmail.INSTANCE.getLogger().error("Failed to retrieve ticket message {}.", messageId);
                            });
                    }
                } else {
                    Modmail.INSTANCE.getLogger().error("Failed to get message id for ticket {}.", ticketId);
                }

                return true;
            } else {
                Modmail.INSTANCE.getLogger().error("Failed to close ticket {}.", ticketId);
                Modmail.INSTANCE.getModmailChannel().sendMessage("Database Error. Failed to close ticket. Message a bot dev.").queue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Method to get an open ticket ID for a given user.
     * @param userId The user's id.
     * @return -1 if no open ticket, else the ticket's id.
     * @throws SQLException Database Error.
     */
    public static int getTicket(long userId) throws SQLException {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"ticket_id\" FROM \"mm_tickets\" WHERE \"user\"=? AND \"open\"=TRUE;");
            pst.setLong(1, userId);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                return result.getInt("ticket_id");
            } else {
                return -1;
            }
        }
    }

    /**
     * Method to send a Ticket Embed to the ModMail Channel.
     * @param ticketId The ticket's ID.
     */
    public static void sendTicketEmbed(long ticketId) {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"message_id\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?;");
            pst.setLong(1, ticketId);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                //If ticket already open, get current message id of ticket
                long messageId = result.getLong("message_id");
                if (messageId > 0) {
                    Modmail.INSTANCE.getModmailChannel().deleteMessageById(messageId).queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        MessageEmbed ticketEmbed = UFormatter.ticketEmbed(ticketId);
        Modmail.INSTANCE.getModmailChannel().sendMessage(ticketEmbed).queue(
            embed -> postProcessTicketEmbed(ticketId, embed),
            failure -> {
                Modmail.INSTANCE.getLogger().error("Failed to send ticket message for ticket {}", ticketId);
                Modmail.INSTANCE.getModmailChannel().sendMessage("An error occurred when sending the ticket. Please try again.").queue();
            });
    }

    /**
     * Method to post process a ticket Embed.
     * @param ticketId The ticket's ID.
     * @param embed The Ticket Embed Message Object.
     */
    public static void postProcessTicketEmbed(long ticketId, Message embed) {
        embed.addReaction(UEmoji.REPLY_TICKET_EMOJI).queue();
        embed.addReaction(UEmoji.CLOSE_TICKET_EMOJI).queue();
        embed.addReaction(UEmoji.TIMEOUT_TICKET_EMOJI).queue();
        try (Connection con1 = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst1 = con1.prepareStatement("UPDATE \"mm_tickets\" SET \"message_id\"=? WHERE \"ticket_id\"=?");
            pst1.setLong(1, embed.getIdLong());
            pst1.setLong(2, ticketId);
            if (pst1.executeUpdate() == 0) {
                Modmail.INSTANCE.getLogger().error("Failed to set new message id for ticket {}", ticketId);
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }

    /**
     * Method to timeout a user.
     * @param userId The user's ID.
     * @return true if successful, false otherwise.
     * @throws SQLException Database Error.
     */
    public static boolean timeoutUser(long userId) throws SQLException {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            //Set timeout timestamp
            Timestamp timeout = Timestamp.valueOf(LocalDateTime.now().plusDays(1));

            //Update ticket timeout time
            PreparedStatement pst = con.prepareStatement("INSERT INTO \"mm_timeouts\" (\"user\", \"timestamp\") "
                    + "VALUES (?, ?) "
                    + "ON CONFLICT ON CONSTRAINT \"mm_timeouts_user\" "
                    + "DO UPDATE SET \"timestamp\"=EXCLUDED.timestamp "
            );
            pst.setLong(1, userId);
            pst.setTimestamp(2, timeout);
            if (pst.executeUpdate() > 0) {
                //TODO: Update ticket log and render new ticket
                Modmail.INSTANCE.getGuild().getMemberById(userId).getUser().openPrivateChannel().queue(
                    success -> {
                        success.sendMessage(UFormatter.timeoutMessage(timeout)).queue(
                            success1 -> {
                                //Do nothing
                            },
                            failure -> {
                                Modmail.INSTANCE.getLogger().error("Failed to send timeout message to user {}.", userId);
                            });
                    },
                    failure -> {
                        Modmail.INSTANCE.getLogger().error("Failed to open private channel with user {}.", userId);
                    });

                return true;
            } else {
                Modmail.INSTANCE.getLogger().error("Failed to update db with new timeout time for user {}.", userId);
                Modmail.INSTANCE.getModmailChannel().sendMessage("Database Error. Failed to set timeout. Message a bot dev.").queue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Method to untimeout a user.
     * @param userId The user's ID.
     * @return true if successful, false otherwise.
     * @throws SQLException Database Error.
     */
    public static boolean untimeoutUser(long userId) throws SQLException {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            //Update ticket timeout time
            PreparedStatement pst = con.prepareStatement("UPDATE \"mm_timeouts\" SET \"timestamp\" = NOW() WHERE \"user\" = ? ");
            pst.setLong(1, userId);
            if (pst.executeUpdate() > 0) {
                //TODO: Update ticket log and render new ticket
                Modmail.INSTANCE.getGuild().getMemberById(userId).getUser().openPrivateChannel().queue(
                    success -> {
                        success.sendMessage(UFormatter.untimeoutMessage()).queue(
                            success1 -> {
                                //Do nothing
                            },
                            failure -> {
                                Modmail.INSTANCE.getLogger().error("Failed to send untimeout message to user {}.", userId);
                            });
                    },
                    failure -> {
                        Modmail.INSTANCE.getLogger().error("Failed to open private channel with user {}.", userId);
                    });

                return true;
            } else {
                Modmail.INSTANCE.getLogger().error("Failed to update db to untimeout user {}.", userId);
                Modmail.INSTANCE.getModmailChannel().sendMessage("Database Error. Failed to set timeout. Message a bot dev.").queue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
