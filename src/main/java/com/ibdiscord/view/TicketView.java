/* Copyright 2021 Arraying
 *
 * This file is part of IB.ai.
 *
 * IB.ai is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IB.ai is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IB.ai. If not, see http://www.gnu.org/licenses/.
 */

package com.ibdiscord.view;

import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.utils.UFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class TicketView {

    private final Guild guild;
    private final long id;
    private final long user;
    private final Pagination pagination;
    int page = 1;

    /**
     * Constructor for Ticket View.
     * @param guild The Guild
     * @param id The ticket ID
     */
    public TicketView(Guild guild, long id) {
        this.guild = guild;
        this.id = id;
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT \"user\" FROM \"mm_tickets\" WHERE \"ticket_id\"=?");
            pst.setLong(1, id);
            ResultSet result = pst.executeQuery();
            if (result.next()) {
                this.user = result.getLong("user");
                this.pagination = new Pagination(guild, 10, user);
            } else {
                throw new IllegalArgumentException("could not find ticket");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Method to update the responses.
     */
    public void updateResponses() {
        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            pagination.clear(); // Maybe a better way to incrementally add responses.
            PreparedStatement pst = con.prepareStatement("SELECT \"user\", \"response\", \"timestamp\" FROM \"mm_ticket_responses\" WHERE \"ticket_id\"=? ORDER BY \"response_id\" ASC");
            pst.setLong(1, id);
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                pagination.add(results.getLong("user"),
                    results.getTimestamp("timestamp"),
                    results.getString("response")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method to render the ticket at the current page.
     * @return Message Embed of the ticket
     */
    public MessageEmbed render() {
        Member ticketMember = guild.getMemberById(user);
        assert ticketMember != null;
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(String.format("ModMail Conversation for %s", UFormatter.formatMember(ticketMember.getUser())))
            .setDescription(String.format("User %s has **%d** roles\n", ticketMember.getAsMention(), ticketMember.getRoles().size())
                + String.format("Joined Discord: **%s**\n", ticketMember.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
                + String.format("Joined Server: **%s**", ticketMember.getTimeJoined().format(DateTimeFormatter.ofPattern("MMM dd yyyy")))
            );
        for (MessageEmbed.Field field : pagination.getResponses(page)) {
            builder.addField(field);
        }
        builder.setFooter(String.format("Page %d/%d", page, pagination.pages()));
        return builder.build();
    }

    /**
     * Method to move to the previous page.
     */
    public void pagePrevious() {
        if (page > 1) {
            page--;
        }
    }

    /**
     * Method to move to the next page.
     */
    public void pageNext() {
        if (page < pagination.pages()) {
            page++;
        }
    }

}
