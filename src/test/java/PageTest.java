import com.ibdiscord.view.Pagination;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class PageTest {

    @Test
    public void size0() {
        Pagination pagination = p(0);
        assertEquals(1, pagination.pages());
    }

    @Test
    public void size1() {
        Pagination pagination = p(1);
        assertEquals(1, pagination.pages());
    }

    @Test
    public void size10() {
        Pagination pagination = p(10);
        assertEquals(1, pagination.pages());
    }

    @Test
    public void size11() {
        Pagination pagination = p(11);
        assertEquals(2, pagination.pages());
    }

    @Test
    public void page1() {
        Pagination pagination = p(1);
        Iterable<MessageEmbed.Field> iterable = pagination.getResponses(1);
        int i = 0;
        for (MessageEmbed.Field field : iterable) {
            assertEquals(String.valueOf(i), field.getValue());
            i++;
        }
    }

    @Test
    public void page2_1() {
        Pagination pagination = p(14);
        Iterable<MessageEmbed.Field> iterable = pagination.getResponses(1);
        int i = 0;
        for (MessageEmbed.Field field : iterable) {
            assertEquals(String.valueOf(i), field.getValue());
            i++;
        }
    }

    @Test
    public void page2_2() {
        Pagination pagination = p(14);
        Iterable<MessageEmbed.Field> iterable = pagination.getResponses(2);
        int i = 10;
        for (MessageEmbed.Field field : iterable) {
            assertEquals(String.valueOf(i), field.getValue());
            i++;
        }
    }

    private Pagination p(int dummy) {
        Pagination pagination = new Pagination(null, 10, 123);
        for (int i = 0; i < dummy; i++) {
            pagination.add(456, new Timestamp(System.currentTimeMillis()), String.valueOf(i));
        }
        return pagination;
    }

}
