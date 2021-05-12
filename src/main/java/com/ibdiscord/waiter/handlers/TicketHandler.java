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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public abstract class TicketHandler implements WaitHandler {

    private Member member;

    private long ticketID;
    private Member ticketMember;

    @Setter(value = AccessLevel.PROTECTED)
    private long messageID;

    /**
     * Ticket Confirmation Handler.
     * @param member Member who called action
     * @param ticketID ID of ticket
     */
    public TicketHandler(Member member, long ticketID) {
        this.member = member;
        this.ticketID = ticketID;

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"user\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
            pst.setLong(1, ticketID);
            ResultSet result = pst.executeQuery();
            if  (result.next()) {
                ticketMember = Modmail.INSTANCE.getGuild().getMemberById(result.getLong("user"));
            } else {
                Modmail.INSTANCE.getLogger().error("Could not retrieve user id from ticket {}", ticketID);
                //TODO: Throw error?
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTimeout() {
        Modmail.INSTANCE.getModmailChannel().retrieveMessageById(messageID).queue(
            message -> message.delete().queue(
                success -> {
                    //Do nothing
                },
                failure -> Modmail.INSTANCE.getLogger().error("Failed to delete confirmation message {}.", messageID)
            ),
            failure -> Modmail.INSTANCE.getLogger().error("Failed to find confirmation message {}.", messageID));
    }

}
