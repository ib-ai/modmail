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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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
        Modmail.INSTANCE.getModmailChannel().sendMessage(UFormatter.timeoutConfirmation(getTicketMember())).queue(
            success -> {
                success.addReaction(UEmoji.YES_CONFIRMATION_EMOJI).queue();
                success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue();
                setMessageID(success.getIdLong());
            },
            failure -> {
                //TODO: Log failure to create confirmation message
                Waiter.INSTANCE.cancel(getMember());
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
                //Set timeout timestamp
                Timestamp timeout = Timestamp.valueOf(LocalDateTime.now().plusDays(1));

                //Update ticket timeout time
                PreparedStatement pst = con.prepareStatement("UPDATE \"mm_tickets\" SET \"timeout\"=? WHERE \"ticket_id\"=?");
                pst.setTimestamp(1, timeout);
                pst.setLong(2, getTicketID());
                if (pst.executeUpdate() > 0) {
                    //TODO: Update ticket log and render new ticket
                    getTicketMember().getUser().openPrivateChannel().queue(
                        success -> {
                            success.sendMessage(UFormatter.timeoutMessage(timeout)).queue(
                                success1 -> {
                                    //Do nothing
                                },
                                failure -> {
                                    //TODO: Log failure to send timeout message
                                });
                        },
                        failure -> {
                            //TODO: Log failure to open private channel
                        });

                    this.onTimeout();
                    return true;
                } else {
                    //TODO: Log failure to update db with new timeout time
                    Modmail.INSTANCE.getModmailChannel().sendMessage("Database Error. Failed to set timeout time. Message a bot dev.").queue();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
