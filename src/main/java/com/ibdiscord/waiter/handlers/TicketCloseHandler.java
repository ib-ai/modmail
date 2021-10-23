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
import com.ibdiscord.utils.UEmoji;
import com.ibdiscord.utils.UFormatter;
import com.ibdiscord.utils.UTicket;
import com.ibdiscord.waiter.Waiter;
import net.dv8tion.jda.api.entities.Member;

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
        Modmail.INSTANCE.getModmailChannel().sendMessage(UFormatter.closeConfirmation(getTicketMember())).queue(
            success -> {
                success.addReaction(UEmoji.YES_CONFIRMATION_EMOJI).queue();
                success.addReaction(UEmoji.NO_CONFIRMATION_EMOJI).queue();
                setMessageID(success.getIdLong());
            }, failure -> {
                Modmail.INSTANCE.getLogger().error("Failed to send ticket close confirmation for user {} on ticket {}.", getMember().getIdLong(), getTicketID());
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
            boolean success = UTicket.closeTicket(getTicketID(), getMember());
            if (success) {
                this.deleteConfirmation();
                return true;
            }
        }

        return false;
    }

}
