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
import com.ibdiscord.utils.UInput;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.apache.commons.lang3.ArrayUtils;

@Getter
public final class CommandContext {

    private final JDA jda;
    private final Guild guild;
    private final MessageChannel channel;
    private final Message message;
    private final Member member;
    private final String[] arguments;

    private CommandContext(Message message, String[] arguments) {
        this.jda = message.getJDA();
        this.guild = message.getGuild();
        this.channel = message.getChannel();
        this.message = message;
        this.member = message.getMember();
        this.arguments = arguments;
    }

    /**
     * Method to construct a Command Context from a Message Object.
     * @param message The Message Object.
     * @return A Command Context.
     */
    public static CommandContext construct(Message message) {
        String content = message.getContentRaw();
        String prefix = Modmail.INSTANCE.getConfig().getBotPrefix();
        String[] arguments = content.substring(prefix.length()).split(" ");

        return new CommandContext(message, ArrayUtils.remove(arguments, 0));
    }

    /**
     * Method to assert the minimum number of arguments of the Command Context.
     * @param length Minimum number of arguments.
     * @param error Error message to return.
     */
    public void assertArgument(int length, String error) {
        if (arguments.length < length) {
            throw new RuntimeException(error);
        }
    }

    /**
     * Method to assert that a certain argument is a valid member reference, with a default error message.
     * @param position The position of the argument to check.
     * @return The referenced member.
     */
    public Member assertMember(int position) {
        return assertMember(position, "Please specify a user.");
    }

    /**
     * Method to assert that a certain argument is a valid member reference.
     * @param position The position of the argument to check.
     * @param error The error message.
     * @return The referenced member.
     */
    public Member assertMember(int position, String error) {
        if (arguments.length < position) {
            throw new RuntimeException(error);
        }

        Member member = UInput.getMember(this.guild, arguments[position]);

        if (member == null) {
            throw new RuntimeException(error);
        }

        return member;
    }

    /**
     * Method to send a message to the same channel as the command was given.
     * @param message The message to reply.
     */
    public void replyRaw(String message) {
        channel.sendMessage(message).queue(null, Throwable::printStackTrace);
    }

}
