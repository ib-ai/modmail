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
import net.dv8tion.jda.api.entities.Member;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Waiter
 * Based off of Arraying's Impulse Bot.
 */
public enum Waiter {

    /**
     * The waiter instance.
     */
    INSTANCE;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
    private ConcurrentHashMap<Member, WaitTask> tasks = new ConcurrentHashMap<>();

    /**
     * Create a Wait Task for a Member.
     * @param member Member who created task
     * @param timeout Seconds until task timeout
     * @param handler Handler for task
     * @return True if created, false if member already has active task.
     */
    public boolean create(Member member, int timeout, WaitHandler handler) {
        if (hasTask(member)) {
            return false;
        }
        WaitTask task = new WaitTask(handler);
        handler.onCreate();
        executorService.schedule(() -> handleEnd(member, task, true), timeout, TimeUnit.SECONDS);
        tasks.put(member, task);
        return true;
    }

    /**
     * Check if a member has an active task.
     * @param member Member to check
     * @return True if has active task, false otherwise.
     */
    public boolean hasTask(Member member) {
        return tasks.containsKey(member);
    }

    /**
     * Method to supply input to task.
     * @param member Member who inputted
     * @param input Input
     */
    public void input(Member member, String input) {
        WaitTask task = tasks.get(member);
        if (task == null) {
            //TODO: Log null task
            return;
        }
        if (task.getHandler().onInput(input)) {
            handleEnd(member, task, false);
        }
    }

    /**
     * Method to supply reaction to task.
     * @param member Member who reacted
     * @param emoji Emoji
     */
    public void react(Member member, String emoji) {
        WaitTask task = tasks.get(member);
        if (task == null) {
            //TODO: Log null task
            return;
        }
        if (task.getHandler().onReact(emoji)) {
            handleEnd(member, task, false);
        }
    }

    /**
     * Method to cancel a task.
     * @param member Member to cancel task of
     */
    public void cancel(Member member) {
        WaitTask task = tasks.get(member);
        if (task == null)   {
            //TODO: Log null task
            return;
        }
        handleEnd(member, task, true);
    }

    /**
     * Method to handle the end of a task.
     * @param member Member who task belongs to
     * @param task Wait Task
     * @param timeout Timeout Flag
     */
    private void handleEnd(Member member, WaitTask task, boolean timeout) {
        if (!task.isHandled()) {
            task.setHandled();
            if (timeout) {
                task.getHandler().onTimeout();
            }
            tasks.remove(member);
        }
    }

}
