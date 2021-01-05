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

/**
 * Wait Handler Interface.
 * Based off of Arraying's Impulse Bot
 */
public interface WaitHandler {

    /**
     * Creation process.
     */
    void onCreate();

    /**
     * Process user's input.
     * @param input The user's input.
     * @return True if process was successful, false otherwise.
     */
    boolean onInput(String input);

    /**
     * Process user's reaction.
     * @param emoji The user's reaction emoji.
     * @return True if process successful, false otherwise
     */
    boolean onReact(String emoji);

    /**
     * Process timeout.
     */
    void onTimeout();

}
