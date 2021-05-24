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

package com.ibdiscord.command;

import com.ibdiscord.Modmail;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class Command {

    @Getter private final String name;
    private Consumer<CommandContext> action;

    /**
     * Method to set the action of a command.
     * @param action Consumer for Command Context
     * @return The Command for Chaining.
     */
    public Command on(Consumer<CommandContext> action) {
        this.action = action;
        return this;
    }

    /**
     * Method to execute the Command with a given context.
     * @param context The Command Context.
     */
    public void execute(CommandContext context) {
        Modmail.INSTANCE.getLogger().info("{} executed the command {} in {}",
                context.getMember().getUser().getId(),
                name,
                context.getGuild().getId()
        );

        try {
            action.accept(context);
        } catch (RuntimeException exception) {
            context.replyRaw(exception.getMessage());
        }
    }
}
