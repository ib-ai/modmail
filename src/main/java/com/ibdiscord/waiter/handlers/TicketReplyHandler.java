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

package com.ibdiscord.waiter.handlers;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.utils.UFormatter;
import com.ibdiscord.waiter.Waiter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketReplyHandler extends TicketHandler {

    /**
     * Ticket Reply Handler.
     * @param member Member who is replying
     * @param ticketID ID of ticket
     */
    public TicketReplyHandler(Member member, long ticketID) {
        super(member, ticketID);
    }

    @Override
    public void onCreate() {
        Modmail.INSTANCE.getModmailChannel().sendMessage(UFormatter.replyConfirmation(getTicketMember())).queue(
            success -> {
                success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue();
                setMessageID(success.getIdLong());
            },
            failure -> {
                Modmail.INSTANCE.getLogger().error("Failed to send ticket reply confirmation for user {} on ticket {}.", getMember().getIdLong(), getTicketID());
                Waiter.INSTANCE.cancel(getMember());
            });
    }

    @Override
    public boolean onInput(String input) {
        //Trim and check input
        input = input.trim();
        if (input.isEmpty()) {
            return false;
        }

        if (input.length() > MessageEmbed.VALUE_MAX_LENGTH) {
            Modmail.INSTANCE.getModmailChannel().sendMessage(String.format(
                    "The message you send is too large, please make it shorter (no more than %d characters).",
                    MessageEmbed.VALUE_MAX_LENGTH
                )).queue();
            this.deleteConfirmation();
            return true;
        }

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            //Insert new response
            PreparedStatement pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, TRUE)"
            );
            pst.setLong(1, getTicketID());
            pst.setLong(2, getMember().getIdLong());
            pst.setString(3, input);

            if (pst.executeUpdate() == 0) {
                Modmail.INSTANCE.getLogger().error("Failed to insert new response by user {} on ticket {}.", getMember().getIdLong(), getTicketID());
                return false;
            }

            //Send response to user
            MessageEmbed replyMessage = UFormatter.replyMessage(input);
            getTicketMember().getUser().openPrivateChannel().queue(
                privateChannel -> privateChannel.sendMessage(replyMessage).queue(
                    success -> {
                        //Do nothing.
                    },
                    failure -> Modmail.INSTANCE.getLogger().error("Failed to send response to user {}.", getTicketMember().getIdLong())
                ),
                failure -> Modmail.INSTANCE.getLogger().error("Failed to open private channel for user {}.", getTicketMember().getIdLong())
            );

            this.deleteConfirmation();

            //Update ticket message
            pst = con.prepareStatement("SELECT \"message_id\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
            pst.setLong(1, getTicketID());
            ResultSet result = pst.executeQuery();
            if (result.next()) {
                //Update ticket message
                MessageEmbed ticketEmbed = UFormatter.ticketEmbed(getTicketID());
                Modmail.INSTANCE.getModmailChannel().retrieveMessageById(result.getLong("message_id")).queue(
                    success -> success.editMessage(ticketEmbed).queue(),
                    failure -> {
                        Modmail.INSTANCE.getLogger().error("Failed to retrieve message for ticket {}.", getTicketID());
                    });
            } else {
                Modmail.INSTANCE.getLogger().error("Failed ot get message id for ticket {}.", getTicketID());
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean onReact(String emoji) {
        return false;
    }
}
