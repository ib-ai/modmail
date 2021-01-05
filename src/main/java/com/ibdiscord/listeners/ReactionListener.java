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
            return;
        }

        if (!event.getReactionEmote().isEmoji()) {
            return;
        }

        String emoji = event.getReactionEmote().getAsCodepoints();

        try (Connection con = DataContainer.INSTANCE.getConnection()) {
            long messageId = event.getMessageIdLong();

            //Get ticket info
            PreparedStatement pst = con.prepareStatement("SELECT \"ticket_id\" from mm_tickets WHERE \"message_id\"=? AND \"open\"=TRUE");
            pst.setLong(1, messageId);

            ResultSet result = pst.executeQuery();
            if (result.next()) {
                long ticketID = result.getLong("ticket_id");

                //If the reaction is on a ticket, check which handler is applicable.
                WaitHandler handler;
                if (emoji.equalsIgnoreCase(UEmoji.CLOSE_TICKET_EMOJI)) {
                    handler = new TicketCloseHandler(event.getMember(), ticketID);
                } else if (emoji.equalsIgnoreCase(UEmoji.REPLY_TICKET_EMOJI)) {
                    handler = new TicketReplyHandler(event.getMember(), ticketID);
                } else if (emoji.equalsIgnoreCase(UEmoji.TIMEOUT_TICKET_EMOJI)) {
                    handler = new TicketTimoutHandler(event.getMember(), ticketID);
                } else {
                    //Irrelevant Reaction, ignore
                    return;
                }

                //Try to create wait task
                if (!Waiter.INSTANCE.create(event.getMember(), 5 * 60, handler)) {
                    event.getChannel().sendMessage("Please only select one action at a time.").queue();
                }
            } else {
                //If not ticket, check if member has a active task.
                //TODO: Check if message is part of user's task?
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

        //Remove reaction
        //TODO: This might be a bit overkill
        event.getReaction().removeReaction(event.getUser()).queue();
    }
}
