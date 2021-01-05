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
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketCloseHandler extends TicketHandler {

    /**
     * Ticket Close Confirmation Handler.
     * @param member Member who is closing ticket
     * @param ticketID ID of ticket
     */
    public TicketCloseHandler(Member member, long ticketID) {
        super(member, ticketID);
    }

    @Override
    public void onCreate() {
        getModmailChannel().sendMessage(UFormatter.closeConfirmation(getTicketMember())).queue(
            success -> {
                success.addReaction(UEmoji.YES_CONFIRMATION_EMOJI).queue();
                success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue();
            }, failure -> {
            });
    }

    @Override
    public boolean onInput(String input) {
        return false;
    }

    @Override
    public boolean onReact(String emoji) {
        if (emoji == UEmoji.YES_CONFIRMATION_EMOJI) {
            try (Connection con = DataContainer.INSTANCE.getConnection()) {
                PreparedStatement pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"open\"=FALSE WHERE \"ticket_id\"=?");
                pst.setLong(1, getTicketID());
                if (pst.executeUpdate() == 1) {
                    //Edit ticket
                    pst = con.prepareStatement("SELECT \"message_id\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
                    pst.setLong(1, getTicketID());
                    ResultSet result = pst.executeQuery();
                    if (result.next()) {
                        getModmailChannel().retrieveMessageById(result.getLong("message_id")).queue(
                            message -> message.editMessage(UFormatter.closedTicket(getMember(), getTicketMember())).queue()
                        );
                    }

                    //Remove Confirmation Message
                    this.onTimeout();

                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

}