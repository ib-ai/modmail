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

import com.ibdiscord.utils.objects.Ticket;
import com.ibdiscord.utils.objects.TicketResponse;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.time.format.DateTimeFormatter;

public final class UFormatter {

    /**
     * Format a response for user DMs.
     * @param guild Guild the bot belongs to
     * @param message Message response
     * @return Message Embed
     */
    public static MessageEmbed replyMessage(Guild guild, String message) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(String.format("New Mail from %s", guild.getName()));
        builder.setDescription(message);

        return builder.build();
    }

    /**
     * Formats a ticket as an embed.
     * @param ticket Ticket
     * @return Message Embed
     */
    public static MessageEmbed ticketEmbed(Ticket ticket) {
        EmbedBuilder builder = new EmbedBuilder();

        Member ticketMember = ticket.getMember();

        builder.setTitle(String.format("ModMail Conversation for %s", formatMember(ticketMember.getUser())));
        builder.setDescription(String.format("User %s has **%d** roles\n", ticketMember.getAsMention(), ticketMember.getRoles().size())
                + String.format("Joined Discord: **%s**\n", ticketMember.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
                + String.format("Joined Server: **%s**", ticketMember.getTimeJoined().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
        );

        for (TicketResponse response : ticket.getResponses()) {
            String messenger = formatMember(response.getMember().getUser());
            if (response.getMember().getIdLong() == ticketMember.getIdLong()) {
                messenger = "user";
            }
            builder.addField(String.format("On %s, %s wrote", response.getTimestamp().toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss")), messenger),
                    response.getResponse(),
                    false
            );
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
}
