package JavaFX;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlannerService {
    private static final String EVENT_FILE = "planner.txt";  // Stores events
    private static final String CLASS_FILE = "classes.txt";  // Stores class names
    private static final String PAST_EVENTS_FILE = "past_events.txt";

    public PlannerService() {
        ensureFileExists(EVENT_FILE);
        ensureFileExists(CLASS_FILE);
    }

    public List<TimeSlot> loadEventsForClass(String className) {
        List<TimeSlot> allEvents = loadEvents(); // Load all events from planner.txt
        List<TimeSlot> classEvents = new ArrayList<>();

        for (TimeSlot event : allEvents) {
            if (event.getClassName().equalsIgnoreCase(className)) {
                classEvents.add(event);
            }
        }

        return classEvents;
    }

    public void addEventForClass(String className) {
        EventDialog dialog = new EventDialog(PlannerService.loadClasses(), className);
        // Open event dialog for the specific class
        dialog.showAndWait().ifPresent(event -> {
            saveEvent(event); // Save the event
        });
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

    // Adds a new class to classes.txt
    public void addClass() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Class");
        dialog.setHeaderText("Enter New Class Name:");
        dialog.setContentText("Class:");

        dialog.showAndWait().ifPresent(className -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASS_FILE, true))) {
                writer.write(className);
                writer.newLine();
                showAlert("Success", "Class '" + className + "' added.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Adds a new event
    public void addEvent() {
        EventDialog dialog = new EventDialog(loadClasses());
        dialog.showAndWait().ifPresent(event -> {
            saveEvent(event);
        });
    }

    public void movePastEventsToStorage() {
        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> upcomingEvents = new ArrayList<>();
        List<TimeSlot> pastEvents = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (TimeSlot event : allEvents) {
            if (event.getDateTime().isBefore(now)) {
                pastEvents.add(event); // Move past events to past_events.txt
            } else {
                upcomingEvents.add(event); // Keep future events
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

        saveEvents(upcomingEvents); // Keep only future events in planner.txt
    }


    // Saves the event to planner.txt
    private void saveEvent(TimeSlot event) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE, true))) {
            writer.write(event.toString());
            writer.newLine();
            showAlert("Success", "Event '" + event.getEventName() + "' added.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads all events from planner.txt
    public List<TimeSlot> loadEvents() {
        List<TimeSlot> events = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(EVENT_FILE));
            for (String line : lines) {
                try {
                    events.add(TimeSlot.fromString(line));
                } catch (Exception e) {
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
            if (event.getDateTime().isAfter(now)) {
                upcomingEvents.add(event);
            }
        }

        //  Sort the upcoming events by date (soonest first)
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

    // Deletes an event
    public void deleteEvent(String eventName) {
        List<TimeSlot> events = loadEvents();
        List<TimeSlot> updatedEvents = new ArrayList<>();

        for (TimeSlot event : events) {
            if (!event.getEventName().equalsIgnoreCase(eventName)) {
                updatedEvents.add(event);
            }
        }

        saveEvents(updatedEvents);
    }

    public TimeSlot getEventByDetails(String className, String eventName, String dateTime) {

        List<TimeSlot> events = loadEvents(); // Load all events

        for (TimeSlot event : events) {
            if (event.getClassName().equalsIgnoreCase(className) &&
                    event.getEventName().equalsIgnoreCase(eventName) &&
                    event.getDateTimeFormatted().equals(dateTime)) {
                return event;
            }
        }

        return null;
    }

    public void updateEvent(TimeSlot oldEvent, TimeSlot newEvent) {
        List<TimeSlot> events = loadEvents();
        boolean found = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE, false))) { // Overwrites file
            for (TimeSlot event : events) {
                if (event.getClassName().equals(oldEvent.getClassName()) &&
                        event.getEventName().equals(oldEvent.getEventName()) &&
                        event.getDateTimeFormatted().equals(oldEvent.getDateTimeFormatted())) {

                    writer.write(newEvent.toString()); // Replace the old event with the new one
                    found = true;
                } else {
                    writer.write(event.toString()); // Keep other events unchanged
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!found) {
            System.out.println("DEBUG: Event not found while updating.");
        }
    }



    // Deletes a class
    public void deleteClass(String className) {
        List<String> classes = loadClasses();

        if (!classes.contains(className)) {
            System.out.println("No such class to delete.");
            return;
        }

        // Remove the class from the class list
        classes.remove(className);

        // Save updated class list
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASS_FILE))) {
            for (String c : classes) {
                writer.write(c);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove all events associated with this class
        List<TimeSlot> allEvents = loadEvents();
        List<TimeSlot> updatedEvents = allEvents.stream()
                .filter(event -> !event.getClassName().equalsIgnoreCase(className))
                .collect(Collectors.toList());

        for (TimeSlot event : allEvents) {
            if (!event.getClassName().equalsIgnoreCase(className)) {
                updatedEvents.add(event); // Keep only events that do not belong to the deleted class
            }
        }

        // Save only the remaining events back to planner.txt
        saveEvents(updatedEvents);

        System.out.println("Deleted class: " + className + " and all its events.");
    }

    public boolean classExists(String className) {
        List<String> classes = loadClasses(); // Load existing classes
        return classes.contains(className); // Check if the class is still present
    }


    public void addNewClass(String className) {

        List<String> classes = loadClasses();

        //  Ensure the class isn't already in the list
        if (classes.contains(className)) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASS_FILE, true))) {
            writer.write(className);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Saves the updated list of events
    private void saveEvents(List<TimeSlot> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENT_FILE))) {
            for (TimeSlot event : events) {
                writer.write(event.toString());
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getEventNames() {

        List<String> eventNames = new ArrayList<>();
        List<TimeSlot> events = loadEvents(); // Load all events

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
                if (line.trim().isEmpty()) continue; // Skip empty lines
                events.add(TimeSlot.fromString(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }




    // Shows a popup alert
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
