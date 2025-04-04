package JavaFX;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeSlot {
    private String className;
    private String eventName;
    private LocalDateTime dateTime;
    private String description;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TimeSlot(String className, String eventName, LocalDateTime dateTime, String description) {
        this.className = className;
        this.eventName = eventName;
        this.dateTime = dateTime;
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public String getEventName() {
        return eventName;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getDescription() {
        return description;
    }

    public String getDateTimeFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy"); // 📆 Example: "March 19, 2025"
        return dateTime != null ? dateTime.format(formatter) : "null";
    }

    public String getTimeFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return dateTime != null ? dateTime.format(formatter) : "null";
    }

    @Override
    public String toString() {
        String dateTimeStr = (dateTime != null) ? dateTime.format(FORMATTER) : "null";
        return className + "|" + eventName + "|" + dateTimeStr + "|" + description;
    }

    // Parses a string into a TimeSlot object
    public static TimeSlot fromString(String eventString) {
        String[] parts = eventString.split("\\|", -1); // Split with limit to preserve empty fields

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid event format: " + eventString);
        }

        String className = parts[0].trim();
        String eventName = parts[1].trim();
        String dateTimeStr = parts[2].trim();
        String description = (parts.length > 3) ? parts[3].trim() : ""; // Handle empty descriptions

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new TimeSlot(className, eventName, dateTime, description);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format in event: " + eventString, e);
        }
    }

    public void setClassName(String newClassName) {
        newClassName = newClassName.trim();
        this.className = newClassName;
    }
}