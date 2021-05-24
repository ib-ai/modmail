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

import com.ibdiscord.command.Command;

import java.util.Comparator;
import java.util.TreeSet;

public final class CommandRegistry {

    private final TreeSet<Command> commands = new TreeSet<>((Comparator.comparing(Command::getName)));

    /**
     * Defines a new command.
     * @param name The name of the command.
     * @return A command object
     */
    public Command define(String name) {
        Command command = new Command(name);
        commands.add(command);
        return command;
    }

    /**
     * Queries a command by name.
     * @param name The name of the command.
     * @return A command, or null if not found.
     */
    public Command query(String name) {
        return commands.stream()
                .filter(command -> command.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

}
