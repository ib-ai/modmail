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

package com.ibdiscord.utils.objects;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Timestamp;

public class TicketResponse {

    @Getter private Member member;
    @Getter private String response;
    @Getter private Timestamp timestamp;

    /**
     * Create a Ticket Response.
     * @param member Response Member
     * @param response Response String
     * @param timestamp Response Timestamp
     */
    public TicketResponse(Member member, String response, Timestamp timestamp) {
        this.member = member;
        this.response = response;
        this.timestamp = timestamp;
    }

}