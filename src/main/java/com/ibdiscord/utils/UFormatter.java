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

package com.ibdiscord.utils;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.db.DataContainer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public final class UFormatter {

    /**
     * Format a response for user DMs.
     * @param message Message response
     * @return Message Embed
     */
    public static MessageEmbed replyMessage(String message) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(String.format("New Mail from %s", Modmail.INSTANCE.getGuild().getName()));
        builder.setDescription(message);

        return builder.build();
    }

    /**
     * Formats a timeout message for user DMs.
     * @param timeout Timeout timestamp
     * @return Message Embed
     */
    public static MessageEmbed timeoutMessage(Timestamp timeout) {
        EmbedBuilder builder = new EmbedBuilder();

        String timeoutTime = UFormatter.formatTimestamp(timeout);
        builder.setDescription(String.format("You have been timed out. You will be able to message ModMail again after %s.", timeoutTime));

        return builder.build();
    }

    /**
     * Formats a ticket as an embed.
     * @param ticketId Ticket
     * @return Message Embed
     */
    public static MessageEmbed ticketEmbed(long ticketId) {
        EmbedBuilder builder = new EmbedBuilder();

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"user\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
            pst.setLong(1, ticketId);
            ResultSet result = pst.executeQuery();

            if (result.next()) {
                Guild guild = Modmail.INSTANCE.getGuild();
                Member ticketMember = guild.getMemberById(result.getLong("user"));

                builder.setTitle(String.format("ModMail Conversation for %s", formatMember(ticketMember.getUser())));
                builder.setDescription(String.format("User %s has **%d** roles\n", ticketMember.getAsMention(), ticketMember.getRoles().size())
                        + String.format("Joined Discord: **%s**\n", ticketMember.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
                        + String.format("Joined Server: **%s**", ticketMember.getTimeJoined().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
                );

                pst = con.prepareStatement("SELECT \"user\", \"response\", \"timestamp\" FROM \"mm_ticket_responses\" WHERE \"ticket_id\"=? ORDER BY \"response_id\" ASC");
                pst.setLong(1, ticketId);
                ResultSet results = pst.executeQuery();
                while (results.next()) {
                    Member responseMember = guild.getMemberById(results.getLong("user"));
                    String messenger = formatMember(responseMember.getUser());
                    if (responseMember.getIdLong() == ticketMember.getIdLong()) {
                        messenger = "user";
                    }
                    String timestamp = UFormatter.formatTimestamp(results.getTimestamp("timestamp"));
                    builder.addField(String.format("On %s, %s wrote", timestamp, messenger),
                            results.getString("response"),
                            false
                    );
                }
            } else {
                builder.setTitle("Database Error. Could not find ticket. Message a bot dev.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return builder.build();
    }

    /**
     * Formats a closed ticket as an embed.
     * @param closer The member who closed the ticket.
     * @param ticketMember The member who's ticket was closed.
     * @return Message Embed
     */
    public static MessageEmbed closedTicket(Member closer, Member ticketMember) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setDescription(String.format("**%s** closed the ModMail conversation for **%s**", formatMember(closer.getUser()), formatMember(ticketMember.getUser())));

        return builder.build();
    }

    /**
     * Formats a close confirmation embed.
     * @param ticketMember The member who's ticket is to be closed.
     * @return Close Confirmation Embed
     */
    public static MessageEmbed closeConfirmation(Member ticketMember) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setDescription(String.format("Do you want to close the ModMail conversation for **%s**", formatMember(ticketMember.getUser())));

        return builder.build();
    }

    /**
     * Formats a reply confirmation embed.
     * @param ticketMember The member who's ticket is to be closed.
     * @return Reply Confirmation Embed
     */
    public static MessageEmbed replyConfirmation(Member ticketMember) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setDescription(String.format("Replying to ModMail conversation for **%s**", formatMember(ticketMember.getUser())));

        return builder.build();
    }

    /**
     * Formats a timeout confirmation embed.
     * @param ticketMember The member who is to be timedout.
     * @return Timeout Confirmation Embed
     */
    public static MessageEmbed timeoutConfirmation(Member ticketMember) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setDescription(String.format("Do you want to timeout **%s** for 24 hours?", formatMember(ticketMember.getUser())));

        return builder.build();
    }

    /**
     * Formats a user to a user friendly name#discrim form.
     * @param user The user.
     * @return The user's name as a tag (formatted).
     */
    public static String formatMember(User user) {
        return user.getAsTag();
    }

    /**
     * Formats a Message object as a ticket response.
     * @param message Message Object
     * @return Response String
     */
    public static String formatResponse(Message message) {
        String attachments = message.getAttachments().stream()
                .map(attachment -> {
                    String type = attachment.isImage() ? "Image" : attachment.isVideo() ? "Video" : "Unknown";
                    return String.format("[%s Attachment](%s)", type, attachment.getUrl());
                })
                .collect(Collectors.joining("\n"));

        return String.format("%s\n%s", message.getContentRaw(), attachments).trim();
    }

    /**
     * Format a Timestamp.
     * @param timestamp Timestamp
     * @return Formatted Timestamp
     */
    public static String formatTimestamp(Timestamp timestamp) {
        return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss O"));
    }
}
