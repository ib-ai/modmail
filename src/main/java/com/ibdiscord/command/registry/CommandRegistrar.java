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

package com.ibdiscord.command.registry;

import com.ibdiscord.utils.UTicket;
import net.dv8tion.jda.api.entities.Member;

import java.sql.SQLException;

public enum CommandRegistrar {

    /**
     * Singleton instance of the Registrar.
     */
    INSTANCE;

    private boolean registered;

    CommandRegistrar() {
        registered = false;
    }

    /**
     * Method to register this Registrar with a Register.
     * @param registry Command Registry to register to.
     */
    public void register(CommandRegistry registry) {
        if (registered) {
            return;
        }

        registry.define("open")
                .on(context -> {
                    Member member = context.assertMember(0);
                    long userID = member.getUser().getIdLong();

                    try {
                        int ticketId = UTicket.getTicket(userID);

                        if (ticketId > -1) {
                            //Ticket already open
                            context.replyRaw(String.format("There is already a ticket open for %s.", member.getUser().getAsTag()));
                        } else {
                            //Open new ticket
                            ticketId = UTicket.openTicket(userID);

                            if (ticketId == -1) {
                                context.replyRaw(String.format("An error occurred when creating a ticket for %s.", member.getUser().getAsTag()));
                            } else {
                                UTicket.sendTicketEmbed(ticketId);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        registry.define("close")
                .on(context -> {
                    Member member = context.assertMember(0);
                    try {
                        int ticketId = UTicket.getTicket(member.getUser().getIdLong());
                        boolean success = UTicket.closeTicket(ticketId, context.getMember());
                        if (success) {
                            context.replyRaw("Ticket has been successfully closed.");
                        } else {
                            context.replyRaw("An error occurred while attempting to close ticket.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        registry.define("refresh")
                .on(context -> {
                    Member member = context.assertMember(0);
                    try {
                        int ticketId = UTicket.getTicket(member.getUser().getIdLong());
                        if (ticketId > -1) {
                            UTicket.sendTicketEmbed(ticketId);
                        } else {
                            context.replyRaw("Specified user does not have an open ticket.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        registry.define("send")
                .on(context -> {
                    //TODO: Implement send command.
                });

        registry.define("timeout")
                .on(context -> {
                    Member member = context.assertMember(0);
                    try {
                        boolean success = UTicket.timeoutUser(member.getUser().getIdLong());
                        if (success) {
                            context.replyRaw("The user has been timedout for 24 hours.");
                        } else {
                            context.replyRaw("An error has occurred.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        registry.define("untimeout")
                .on(context -> {
                    Member member = context.assertMember(0);
                    try {
                        boolean success = UTicket.untimeoutUser(member.getUser().getIdLong());
                        if (success) {
                            context.replyRaw("The user's timeout has been removed.");
                        } else {
                            context.replyRaw("An error has occurred.");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        registered = true;
    }

}
