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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TicketTimoutHandler extends TicketHandler {

    /**
     * Ticket Timeout Confirmation Handler.
     * @param member Member who called timeout
     * @param ticketID ID of ticket
     */
    public TicketTimoutHandler(Member member, long ticketID) {
        super(member, ticketID);
    }

    @Override
    public void onCreate() {
        getModmailChannel().sendMessage(UFormatter.timeoutConfirmation(getTicketMember())).queue(
            success -> {
                success.addReaction(UEmoji.YES_CONFIRMATION_EMOJI).queue();
                success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue();
                setMessageID(success.getIdLong());
            },
            failure -> {

            });
    }

    @Override
    public boolean onInput(String input) {
        return false;
    }

    @Override
    public boolean onReact(String emoji) {
        if (emoji.equalsIgnoreCase(UEmoji.YES_CONFIRMATION_EMOJI)) {
            try (Connection con = DataContainer.INSTANCE.getConnection()) {
                PreparedStatement pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"timeout\"=? WHERE \"ticket_id\"=?");
                //TODO: Improve this
                Timestamp timestamp = new Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                pst.setTimestamp(1, timestamp);
                pst.setLong(2, getTicketID());
                if (pst.executeUpdate() > 0) {
                    this.onTimeout();
                    return true;
                }
                //TODO: Send timeout message
                //TODO: Update log
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
