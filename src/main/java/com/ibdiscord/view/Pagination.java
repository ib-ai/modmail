package com.ibdiscord.view;

import com.ibdiscord.utils.UFormatter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class Pagination {

    @RequiredArgsConstructor
    private static final class Paginator implements Iterator<MessageEmbed.Field> {

        private final List<MessageEmbed.Field> entries;
        private final int start;
        private final int length;
        private int counter = 0;

        @Override
        public boolean hasNext() {
            return counter < length;
        }

        @Override
        public MessageEmbed.Field next() {
            System.out.println(length);
            MessageEmbed.Field field = entries.get(start + counter);
            counter++;
            return field;
        }

    }

    // Use ArrayList for O(1) access.
    private final List<MessageEmbed.Field> entries = new ArrayList<>();
    private final Guild guild;
    private final int pageSize;
    private final long owner;

    public synchronized int pages() {
        int size = entries.size();
        return ((size - 1) / pageSize) + 1;
    }

    public synchronized void add(long user, Timestamp sqlStamp, String response) {
        String messenger;
        if (guild == null || guild.getMemberById(user) == null) {
            messenger = "unknown";
        } else {
            Member responseMember = guild.getMemberById(user);
            assert responseMember != null;
            messenger = UFormatter.formatMember(responseMember.getUser());
            if (responseMember.getIdLong() == owner) {
                messenger = "user";
            }
        }
        String timestamp = UFormatter.formatTimestamp(sqlStamp);
        MessageEmbed.Field field = new MessageEmbed.Field(String.format("On %s, %s wrote", timestamp, messenger),
            response,
            false,
            true
        );
        entries.add(field);
    }

    public synchronized Iterable<MessageEmbed.Field> getResponses(int page) {
        if (page < 1 || page > pages()) {
            throw new IllegalStateException("invalid page index");
        }
        int start = pageSize * (page - 1);
        int modulo = entries.size() % pageSize;
        int length = page == pages() ? (modulo == 0 ? pageSize : modulo) : pageSize;
        return () -> new Paginator(entries, start, length);
    }

    public synchronized void clear() {
        entries.clear();
    }

}
