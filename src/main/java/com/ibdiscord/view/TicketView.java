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

    public void pagePrevious() {
        if (page > 1) {
            page--;
        }
    }

    public void pageNext() {
        if (page < pagination.pages()) {
            page++;
        }
    }

}
