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

package com.ibdiscord.waiter;

import com.ibdiscord.waiter.handlers.WaitHandler;
import lombok.Getter;

/**
 * Wait Task
 * Based off of Arraying's Impulse Bot.
 */
final class WaitTask {

    @Getter private boolean handled;

    @Getter private final WaitHandler handler;

    /**
     * Create a new wait task.
     * @param handler The handler to use.
     */
    WaitTask(WaitHandler handler) {
        this.handled = false;
        this.handler = handler;
    }

    synchronized void setHandled() {
        this.handled = true;
    }

}
