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

package com.ibdiscord.listeners;

import com.ibdiscord.Modmail;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.waiter.Waiter;
import com.ibdiscord.waiter.handlers.TicketCloseHandler;
import com.ibdiscord.waiter.handlers.TicketReplyHandler;
import com.ibdiscord.waiter.handlers.TicketTimoutHandler;
import com.ibdiscord.waiter.handlers.WaitHandler;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class ReactionListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        if (!Objects.equals(event.getChannel().getId(), Modmail.INSTANCE.getConfig().getChannelId())) {
            System.out.println("Not in channel");
            return;
        }

        if (!event.getReactionEmote().isEmoji()) {
            System.out.println("Not Emoji");
            return;
        }

        String emoji = event.getReactionEmote().getAsCodepoints().toUpperCase();
        /*
        if (emoji != UEmoji.CLOSE_TICKET_EMOJI && emoji != UEmoji.REPLY_TICKET_EMOJI && emoji != UEmoji.TIMEOUT_TICKET_EMOJI) {
            System.out.println("Not Correct Emoji");
            return;
        }
         */

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            long messageId = event.getMessageIdLong();
            PreparedStatement pst = con.prepareStatement("SELECT \"ticket_id\" from mm_tickets WHERE \"message_id\"=? AND \"open\"=TRUE");
            pst.setLong(1, messageId);
            ResultSet result = pst.executeQuery();
            if (result.next()) {
                long ticketID = result.getLong("ticket_id");
                System.out.println(ticketID);
                WaitHandler handler;
                System.out.println(emoji);
                System.out.println(UEmoji.REPLY_TICKET_EMOJI);
                switch (emoji) {
                    case UEmoji.CLOSE_TICKET_EMOJI:
                        handler = new TicketCloseHandler(event.getMember(), ticketID);
                        break;

                    case UEmoji.REPLY_TICKET_EMOJI:
                        handler = new TicketReplyHandler(event.getMember(), ticketID);
                        break;

                    case UEmoji.TIMEOUT_TICKET_EMOJI:
                        handler = new TicketTimoutHandler(event.getMember(), ticketID);
                        break;

                    default:
                        System.out.println("No handler");
                        return;
                }

                if (!Waiter.INSTANCE.create(event.getMember(), 5 * 60, handler)) {
                    //TODO: Failed to create message
                }
            } else {
                if (Waiter.INSTANCE.hasTask(event.getMember())) {
                    if (emoji.equalsIgnoreCase(UEmoji.NO_CONFIRMATION_EMOJI)) {
                        Waiter.INSTANCE.cancel(event.getMember());
                    } else {
                        Waiter.INSTANCE.react(event.getMember(), emoji);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        event.getReaction().removeReaction(event.getUser()).queue();
    }
}
