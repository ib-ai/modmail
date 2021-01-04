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

import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.utils.UFormatter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
        getModmailChannel().sendMessage(UFormatter.replyConfirmation(getTicketMember())).queue(
            success -> success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue(),
            failure -> {

            });
    }

    @Override
    public boolean onInput(String input) {
        input = input.trim();
        if (input.isEmpty()) {
            return false;
        }

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("INSERT INTO \"mm_ticket_responses\" (\"ticket_id\", \"user\", \"response\", \"as_server\")"
                    + "VALUES (?, ?, ?, TRUE)"
            );
            pst.setLong(1, getTicketID());
            pst.setLong(2, getMember().getUser().getIdLong());
            pst.setString(3, input);

            if (!pst.execute()) {
                return false;
            }

            MessageEmbed replyMessage = UFormatter.replyMessage(getGuild(), input);
            getTicketMember().getUser().openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage(replyMessage)
            );

            this.onTimeout();

            //TODO: Remove old ticket, send new one

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
