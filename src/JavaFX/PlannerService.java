package JavaFX;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.smartcardio.Card;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlannerService {
    private static final String EVENT_FILE = "planner.txt";  // Stores events
    private static final String CLASS_FILE = "classes.txt";  // Stores class names
    private static final String PAST_EVENTS_FILE = "past_events.txt";
    private final List<Runnable> updateListeners = new ArrayList<>();

    public PlannerService() {
        ensureFileExists(EVENT_FILE);
        ensureFileExists(CLASS_FILE);
        ensureFileExists(PAST_EVENTS_FILE);
    }

    public List<TimeSlot> loadEventsForClass(String className) {
        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> classEvents = new ArrayList<>();

        for (TimeSlot event : allEvents) {
            if (event.getClassName().equalsIgnoreCase(className)) {
                classEvents.add(event);
            }
        }

        return classEvents;
    }

    // Ensures the file exists, creates it if missing
    private void ensureFileExists(String fileName) {
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Loads all classes from classes.txt
    public static List<String> loadClasses() {
        List<String> classes = new ArrayList<>();
        try {
            classes = Files.readAllLines(Paths.get(CLASS_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    // Adds a new class to classes.txt, returns the added class name or null if cancelled
    public String addClass() {
        // This will be handled in PlannerApp via a UI callback
        // For now, we'll simulate the logic and return the class name
        // PlannerApp will call this and handle the UI input
        return null; // Placeholder; actual implementation will be in PlannerApp
    }

    public void addNewClass(String className) {
        List<String> classes = loadClasses();
        if (classes.contains(className)) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASS_FILE, true))) {
            writer.write(className);
            writer.newLine();
            notifyUpdateListeners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void movePastEventsToStorage() {
        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> upcomingEvents = new ArrayList<>();
        List<TimeSlot> pastEvents = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (TimeSlot event : allEvents) {
            if (event.getDateTime() == null) {
                System.err.println("movePastEventsToStorage: Event with null dateTime: " + event.getEventName());
                continue;
            }
            if (event.getDateTime().isBefore(now)) {
                pastEvents.add(event);
            } else {
                upcomingEvents.add(event);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PAST_EVENTS_FILE, true))) {
            for (TimeSlot event : pastEvents) {
                writer.write(event.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        saveEvents(upcomingEvents);
        notifyUpdateListeners();
    }

    // Saves the event to planner.txt
    public void saveEvent(TimeSlot event) {
        if (event == null) {
            System.err.println("saveEvent: Event is null");
            return;
        }
        if (event.getDateTime() == null) {
            System.err.println("saveEvent: Event dateTime is null for event: " + event.getEventName());
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE, true))) {
            System.out.println("Saving event: " + event.toString());
            writer.write(event.toString());
            writer.newLine();
            movePastEventsToStorage();
            notifyUpdateListeners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearPastEvents() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PAST_EVENTS_FILE, false))) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        notifyUpdateListeners();
    }

    // Loads all events from planner.txt
    public List<TimeSlot> loadEvents() {
        List<TimeSlot> events = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(EVENT_FILE));
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                try {
                    events.add(TimeSlot.fromString(line));
                } catch (Exception e) {
                    System.err.println("Failed to parse event: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }

    // Returns upcoming events
    public List<TimeSlot> getUpcomingEvents() {
        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> upcomingEvents = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (TimeSlot event : allEvents) {
            if (event.getDateTime() == null) {
                System.err.println("getUpcomingEvents: Event with null dateTime: " + event.getEventName());
                continue;
            }
            if (event.getDateTime().isAfter(now)) {
                upcomingEvents.add(event);
            }
        }

        upcomingEvents.sort(Comparator.comparing(TimeSlot::getDateTime));
        return upcomingEvents;
    }

    // Finds an event by name
    public TimeSlot getEventByName(String eventName) {
        for (TimeSlot event : loadEvents()) {
            if (event.getEventName().equalsIgnoreCase(eventName)) {
                return event;
            }
        }
        return null;
    }

    // Returns an EventDialog for adding an event
    public EventDialog createEventDialog() {
        return new EventDialog(loadClasses());
    }

    public EventDialog createEventDialogForClass(String className) {
        return new EventDialog(loadClasses(), className);
    }

    // Deletes an event
    public void deleteEvent(String eventName, String className) {
        List<TimeSlot> events = loadEvents();
        List<TimeSlot> updatedEvents = new ArrayList<>();

        boolean found = false;
        for (TimeSlot event : events) {
            if (!(event.getEventName().equalsIgnoreCase(eventName) && event.getClassName().equalsIgnoreCase(className))) {
                updatedEvents.add(event);
            } else {
                found = true;
            }
        }

        if (found) {
            saveEvents(updatedEvents);
            movePastEventsToStorage();
            notifyUpdateListeners();
        }
    }

    public TimeSlot getEventByDetails(String className, String eventName, String dateTime) {
        List<TimeSlot> events = loadEvents();
        for (TimeSlot event : events) {
            String formattedEventDate = event.getDateTimeFormatted();
            if (event.getClassName().equalsIgnoreCase(className) &&
                    event.getEventName().equalsIgnoreCase(eventName) &&
                    formattedEventDate.equals(dateTime)) {
                return event;
            }
        }
        return null;
    }

    public VBox showDescriptionDialog(String description) {
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        Separator separator = new Separator();
        Label titleLabel = new Label("Description");
        titleLabel.setAlignment(Pos.CENTER);
        Label descriptionLabel = new Label(description);
        if (Objects.equals(description, "")) {
            descriptionLabel = new Label("Description is empty");
        } else {
            descriptionLabel = new Label(description);
        }
        descriptionLabel.setAlignment(Pos.CENTER);
        descriptionLabel.setWrapText(true);
        vBox.getChildren().addAll(titleLabel, separator, descriptionLabel);
        vBox.setAlignment(Pos.CENTER);

        return vBox;
    }

    public void updateEvent(TimeSlot oldEvent, TimeSlot newEvent) {
        List<TimeSlot> events = loadEvents();
        boolean found = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE, false))) {
            for (TimeSlot event : events) {
                if (event.getClassName().equals(oldEvent.getClassName()) &&
                        event.getEventName().equals(oldEvent.getEventName()) &&
                        event.getDateTimeFormatted().equals(oldEvent.getDateTimeFormatted())) {
                    writer.write(newEvent.toString());
                    found = true;
                } else {
                    writer.write(event.toString());
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (found) {
            notifyUpdateListeners();
        }
    }

    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyUpdateListeners() {
        for (Runnable listener : updateListeners) {
            Platform.runLater(listener::run);
        }
    }

    // Deletes a class
    public void deleteClass(String className) {
        List<String> classes = loadClasses();

        if (!classes.contains(className)) {
            return;
        }

        classes.remove(className);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASS_FILE))) {
            for (String c : classes) {
                writer.write(c);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> updatedEvents = allEvents.stream()
                .filter(event -> !event.getClassName().equalsIgnoreCase(className))
                .collect(Collectors.toList());

        saveEvents(updatedEvents);
        notifyUpdateListeners();
    }

    public boolean classExists(String className) {
        List<String> classes = loadClasses();
        return classes.contains(className);
    }

    public void moveEventToFuture(TimeSlot oldEvent, TimeSlot newEvent) {
        List<TimeSlot> pastEvents = loadPastEvents();
        List<TimeSlot> futureEvents = loadEvents();

        pastEvents.removeIf(e -> e.getEventName().equals(oldEvent.getEventName()) && e.getClassName().equals(oldEvent.getClassName()));
        futureEvents.add(newEvent);

        savePastEvents(pastEvents);
        saveEvents(futureEvents);
        notifyUpdateListeners();
    }

    public void savePastEvents(List<TimeSlot> pastEvents) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PAST_EVENTS_FILE))) {
            for (TimeSlot event : pastEvents) {
                writer.write(event.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Saves the updated list of events
    private void saveEvents(List<TimeSlot> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE))) {
            for (TimeSlot event : events) {
                if (event.getDateTime() == null) {
                    System.err.println("saveEvents: Event with null dateTime: " + event.getEventName());
                    continue;
                }
                writer.write(event.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getEventNames() {
        List<String> eventNames = new ArrayList<>();
        List<TimeSlot> events = loadEvents();

        for (TimeSlot event : events) {
            eventNames.add(event.getEventName());
        }

        return eventNames;
    }

    public List<TimeSlot> loadPastEvents() {
        List<TimeSlot> events = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(PAST_EVENTS_FILE));
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                events.add(TimeSlot.fromString(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }
}