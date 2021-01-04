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
import com.ibdiscord.utils.UChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class TicketHandler implements WaitHandler {

    @Getter private Guild guild;

    @Getter private Member member;

    @Getter private long ticketID;
    @Getter private Member ticketMember;
    @Getter private TextChannel modmailChannel;

    @Getter @Setter(value = AccessLevel.PROTECTED)
    private long messageID;

    /**
     * Ticket Confirmation Handler.
     * @param member Member who called action
     * @param ticketID ID of ticket
     */
    public TicketHandler(Member member, long ticketID) {
        this.guild = member.getGuild();
        this.member = member;
        this.ticketID = ticketID;
        this.modmailChannel = UChannel.getModmailChannel(guild);

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"user\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
            pst.setLong(1, ticketID);
            ResultSet result = pst.executeQuery();
            if  (result.next()) {
                ticketMember = guild.getMemberById(result.getLong("user"));
            } else {
                //TODO:
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTimeout() {
        TextChannel channel = UChannel.getModmailChannel(guild);
        if (channel != null) {
            channel.retrieveMessageById(messageID).queue(message -> message.delete().queue());
        }
    }

}
