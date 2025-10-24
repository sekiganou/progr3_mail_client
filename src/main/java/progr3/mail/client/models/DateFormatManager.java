package progr3.mail.client.models;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateFormatManager {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String formatTimestamp(DateTimeFormatter formatter, String timestamp) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp,
                    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH));
            LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
            var formatted = localDateTime.format(formatter);

            System.out.println("Original timestamp: " + timestamp);
            System.out.println("Formatted timestamp: " + formatted);
            return formatted;
        } catch (Exception e) {
            System.err.println("Error formatting timestamp: " + e.getMessage());

            return timestamp;
        }
    }

}
