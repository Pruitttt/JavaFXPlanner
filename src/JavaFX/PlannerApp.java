package JavaFX;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.stage.Window;


public class PlannerApp extends Application {
    private static PlannerApp instance;
    private PlannerService plannerService = new PlannerService();
    private ListView<String> upcomingEventsList;
    private static final Set<Stage> openWindows = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        instance = this;

        // Initialize UI first to avoid NullPointerException
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Initialize the list BEFORE updating it
        upcomingEventsList = new ListView<>();

        // Register a listener for updates
        plannerService.addUpdateListener(() -> Platform.runLater(this::updateUpcomingEvents));

        // Ensure past events are handled before populating UI
        plannerService.movePastEventsToStorage();
        updateUpcomingEvents(); // This is safe now

        // Buttons for adding events and viewing by class
        Button addEventBtn = new Button("Add Event");
        addEventBtn.setOnAction(e -> {
            plannerService.addEvent();
        });

        Button viewByClassBtn = new Button("Class View");
        viewByClassBtn.setOnAction(e -> selectClassWindow());

        Button recentEventsBtn = new Button("Past Events");
        recentEventsBtn.setOnAction(e -> showPastEvents());

        // Set button widths for symmetry
        double buttonWidth = 225;
        addEventBtn.setPrefWidth(buttonWidth);
        viewByClassBtn.setPrefWidth(buttonWidth);
        recentEventsBtn.setPrefWidth(buttonWidth);

        // Clicking on an event shows details
        upcomingEventsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedEvent = upcomingEventsList.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    String[] parts = selectedEvent.split(" - ");
                    if (parts.length < 3) {
                        showAlert("Error", "Invalid event format.");
                        return;
                    }

                    String className = parts[0].trim();
                    String eventName = parts[1].trim();
                    String dateTime = parts[2].trim();

                    TimeSlot eventDetails = plannerService.getEventByDetails(className, eventName, dateTime);
                    if (eventDetails != null) {
                        showEventDetails(eventDetails);
                    } else {
                        showAlert("Error", "Event not found.");
                    }
                }
            }
        });

        // Arrange buttons in a row
        HBox buttonRow = new HBox(10, addEventBtn, viewByClassBtn, recentEventsBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // Add components to the root layout
        root.getChildren().addAll(new Label("Upcoming Events:"), upcomingEventsList, buttonRow);

        // Create scene and apply stylesheet
        Scene scene = new Scene(root, 450, 500);
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        // Configure and show primary stage
        primaryStage.setTitle("Planner App");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            for (Stage stage : new HashSet<>(openWindows)) {
                stage.close();
            }
        });
        primaryStage.show();
    }


    public static void refreshPastEventsWindow() {
        Platform.runLater(() -> {
            for (Stage stage : openWindows) {
                if (stage.getTitle().equals("Past Events")) {
                    VBox vbox = (VBox) stage.getScene().getRoot();

                    // Find the TableView inside the VBox
                    for (Node node : vbox.getChildren()) {
                        if (node instanceof TableView) {
                            TableView<TimeSlot> pastEventsTable = (TableView<TimeSlot>) node;
                            System.out.println("🔄 Refreshing Past Events window...");
                            pastEventsTable.getItems().setAll(new PlannerService().loadPastEvents()); // ✅ Reload past events
                            return; // Exit after updating the table
                        }
                    }
                }
            }
        });
    }



    private void updateUpcomingEvents() {
        System.out.println("🔄 Updating upcoming events..."); // Debugging

        List<TimeSlot> events = plannerService.getUpcomingEvents();
        upcomingEventsList.getItems().clear();

        events.removeIf(event -> !plannerService.classExists(event.getClassName()));

        events.sort(Comparator.comparing(TimeSlot::getDateTime));

        if (events.isEmpty()) {
            upcomingEventsList.getItems().add("No upcoming events.");
        } else {
            for (TimeSlot event : events) {
                System.out.println("✅ Adding event to UI: " + event); // Debugging
                upcomingEventsList.getItems().add(event.getClassName() + " - " + event.getEventName() + " - " + event.getDateTimeFormatted());
            }
        }
    }








    /** Shows an event’s details in a new window */
    private void showEventDetails(TimeSlot event) {
        if (event == null) {
            showAlert("Error", "Event not found.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Event Details");
        dialog.setHeaderText("Details for " + event.getEventName());

        Label classLabel = new Label("Class: " + event.getClassName());
        Label dateLabel = new Label("Date: " + event.getDateTimeFormatted());
        Label descLabel = new Label("Description: " + event.getDescription());

        Button modifyButton = new Button("Modify");
        Button deleteButton = new Button("Delete");

        modifyButton.setOnAction(e -> {
            modifyEvent(event);
            dialog.close();
        });

        deleteButton.setOnAction(e -> {
            boolean deleted = confirmAndDeleteEvent(event);
            System.out.println("Delete button pressed. Deleted: " + deleted); // Debugging

            if (deleted) {
                System.out.println("Closing event details dialog..."); // Debugging
                Platform.runLater(() -> {
                    dialog.setResult(ButtonType.CLOSE); // ✅ Ensure dialog properly closes
                    dialog.close();
                });
            }
        });



        double buttonWidth = 120;
        modifyButton.setPrefWidth(buttonWidth);
        deleteButton.setPrefWidth(buttonWidth);

        HBox buttonBox = new HBox(10, modifyButton, deleteButton);
        buttonBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(modifyButton, Priority.ALWAYS);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);

        VBox vbox = new VBox(10, classLabel, dateLabel, descLabel, buttonBox);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        // Remove Close button from the UI
        dialog.getDialogPane().getButtonTypes().clear();

        // Handle "X" button (window close request)
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(event1 -> dialog.close());

        dialog.showAndWait();
    }




    private boolean confirmAndDeleteEvent(TimeSlot event) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Event");
        confirmDialog.setHeaderText("Are you sure you want to delete this event?");
        confirmDialog.setContentText(event.getEventName() + " (" + event.getClassName() + ")");

        return confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).map(response -> {
            plannerService.deleteEvent(event.getEventName(), event.getClassName()); // Delete event
            updateUpcomingEvents();

            boolean stillExists = plannerService.getEventByDetails(event.getClassName(), event.getEventName(), event.getDateTimeFormatted()) != null;
            System.out.println("Event deleted successfully: " + !stillExists); // Debugging

            return !stillExists; // Return true only if event was deleted
        }).orElse(false);
    }







    private void modifyEvent(TimeSlot event) {
        Stage modifyStage = new Stage();
        openWindows.add(modifyStage); // Track this window

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        Label titleLabel = new Label("Modify Event Details");

        // Editable fields
        TextField classField = new TextField(event.getClassName());
        TextField nameField = new TextField(event.getEventName());
        TextArea descField = new TextArea(event.getDescription());
        descField.setWrapText(true);

        // Separate fields for month, day, year, hour, minute, and AM/PM
        TextField monthField = new TextField(String.valueOf(event.getDateTime().getMonthValue()));
        monthField.setPromptText("MM");

        TextField dayField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("dd")));
        dayField.setPromptText("DD");

        TextField yearField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("yyyy")));
        yearField.setPromptText("YYYY");

        TextField hourField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("hh")));
        hourField.setPromptText("HH");

        TextField minuteField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("mm")));
        minuteField.setPromptText("MM");

        ComboBox<String> amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setValue(event.getDateTime().getHour() < 12 ? "AM" : "PM");

        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> {
            try {
                String className = classField.getText().trim();
                String eventName = nameField.getText().trim();
                String description = descField.getText().trim();

                if (className.isEmpty() || eventName.isEmpty()) {
                    showAlert("Invalid Input", "Class and Event Name cannot be empty.");
                    return; // Keeps window open
                }

                // Parse all required fields safely with defaults
                int month = Integer.parseInt(monthField.getText().trim());
                int day = Integer.parseInt(dayField.getText().trim());

                int year = yearField.getText().trim().isEmpty()
                        ? LocalDateTime.now().getYear() // default to current year
                        : Integer.parseInt(yearField.getText().trim());

                int hour = hourField.getText().trim().isEmpty()
                        ? 12 : Integer.parseInt(hourField.getText().trim());

                int minute = minuteField.getText().trim().isEmpty()
                        ? 0 : Integer.parseInt(minuteField.getText().trim());

                String amPmValue = amPmDropdown.getValue();
                if (amPmValue == null || amPmValue.trim().isEmpty()) {
                    amPmValue = "AM"; // default to AM
                }

                if (month < 1 || month > 12 || day < 1 || day > 31 ||
                        hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    showAlert("Invalid Input", "Ensure month, day, hour, and minute are valid.");
                    return;
                }

                // Convert to 24-hour time
                if ("PM".equals(amPmValue) && hour < 12) hour += 12;
                if ("AM".equals(amPmValue) && hour == 12) hour = 0;

                LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

                TimeSlot updatedEvent = new TimeSlot(className, eventName, dateTime, description);

                plannerService.updateEvent(event, updatedEvent);

                modifyStage.close();
                updateUpcomingEvents();

            } catch (Exception ex) {
                showAlert("Invalid Input", "Ensure all date/time fields contain valid numbers.");
            }
        });


        // Trigger saveButton on pressing Enter
        classField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });
        nameField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });
        monthField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });
        dayField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });
        yearField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });
        descField.setOnKeyPressed(e -> { if (e.getCode().equals(KeyCode.ENTER)) saveButton.fire(); });

        // Scrollable container for neatness
        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(10, titleLabel,
                new Label("Class:"), classField,
                new Label("Event Name:"), nameField,
                new Label("Month (MM):"), monthField,
                new Label("Day:"), dayField,
                new Label("Year:"), yearField,
                new Label("Hour:"), hourField,
                new Label("Minute:"), minuteField,
                new Label("AM/PM:"), amPmDropdown,
                new Label("Description:"), descField
        );
        content.setPadding(new Insets(10));
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);

        vbox.getChildren().addAll(scrollPane, saveButton);

        Scene scene = new Scene(vbox, 450, 500);
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        modifyStage.setTitle("Modify Event");
        modifyStage.setScene(scene);

        modifyStage.setOnCloseRequest(e -> openWindows.remove(modifyStage));

        modifyStage.show();
    }



    /**  Handles event modification and saving */
    private void saveModifiedEvent(TimeSlot oldEvent, TextField classField, TextField nameField, TextField dateField, TextArea descField, Stage modifyStage) {
        try {
            TimeSlot newEvent = new TimeSlot(
                    classField.getText(),
                    nameField.getText(),
                    LocalDateTime.parse(dateField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    descField.getText()
            );

            plannerService.updateEvent(oldEvent, newEvent); // Save changes
            modifyStage.close();
            updateUpcomingEvents(); // Refresh UI
        } catch (Exception e) {
            showAlert("Error", "Invalid date format. Use YYYY-MM-DD HH:MM.");
        }
    }


    private void showPastEvents() {
        plannerService.movePastEventsToStorage(); // ✅ Ensure past events are moved before displaying

        List<TimeSlot> pastEvents = plannerService.loadPastEvents(); // Reload past events

        Stage pastStage = new Stage();
        openWindows.add(pastStage); // ✅ Track this window

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        TableView<TimeSlot> tableView = new TableView<>();

        TableColumn<TimeSlot, String> nameCol = new TableColumn<>("Event Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventName()));

        TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateTimeFormatted()));

        TableColumn<TimeSlot, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClassName()));

        tableView.getColumns().addAll(nameCol, dateCol, classCol);
        tableView.getItems().setAll(pastEvents); // ✅ Ensures fresh data is loaded

        // 🗑️ Add "Clear All Past Events" Button
        Button clearPastEventsBtn = new Button("Clear All Past Events");
        clearPastEventsBtn.setStyle("-fx-background-color: #ff4c4c; -fx-text-fill: white; -fx-font-weight: bold;");

        clearPastEventsBtn.setOnAction(e -> {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Clear Past Events");
            confirmDialog.setHeaderText("Are you sure you want to delete ALL past events?");
            confirmDialog.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                plannerService.clearPastEvents(); // ✅ Deletes past_events.txt content
                tableView.getItems().clear(); // ✅ Clear UI after deletion
            }
        });

        // Layout
        HBox buttonBox = new HBox(clearPastEventsBtn);
        buttonBox.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(new Label("Past Events:"), tableView, buttonBox);
        Scene scene = new Scene(vbox, 500, 450);
        pastStage.setTitle("Past Events");
        pastStage.setScene(scene);

        pastStage.setOnCloseRequest(e -> openWindows.remove(pastStage)); // ✅ Remove from tracked windows

        pastStage.show();
    }





    /** Deletes an event */
    private void deleteEvent() {
        List<TimeSlot> events = plannerService.loadEvents(); // Load full event objects

        if (events.isEmpty()) {
            showAlert("No Events", "There are no events to delete.");
            return;
        }

        // Format events as "Class - Event - Date"
        List<String> eventOptions = new ArrayList<>();
        for (TimeSlot event : events) {
            eventOptions.add(event.getClassName() + " - " + event.getEventName() + " - " + event.getDateTimeFormatted());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(eventOptions.get(0), eventOptions);
        dialog.setTitle("Delete Event");
        dialog.setHeaderText("Select an event to delete:");
        dialog.setContentText("Event:");

        dialog.showAndWait().ifPresent(selectedString -> {
            String[] parts = selectedString.split(" - ");
            if (parts.length < 3) {
                showAlert("Error", "Invalid event format.");
                return;
            }

            String className = parts[0].trim();
            String eventName = parts[1].trim();
            String dateTime = parts[2].trim();

            TimeSlot eventToDelete = plannerService.getEventByDetails(className, eventName, dateTime);
            if (eventToDelete != null) {
                plannerService.deleteEvent(eventToDelete.getEventName(), eventToDelete.getClassName()); // ✅ Updated to use two parameters
                updateUpcomingEvents();
            } else {
                showAlert("Error", "Event not found.");
            }
        });
    }


    /** Deletes a class */
    private void deleteClass() {

        List<String> classes = plannerService.loadClasses();
        if (classes.isEmpty()) {
            showAlert("No Classes", "There are no classes to delete.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(classes.get(0), classes);
        dialog.setTitle("Delete Class");
        dialog.setHeaderText("Select a class to delete:");
        dialog.setContentText("Class:");

        dialog.showAndWait().ifPresent(className -> {
            plannerService.deleteClass(className); //  Calls the service method
            updateUpcomingEvents(); //  Refresh event list after deletion
        });
    }

    public static void updateUI() { //
        if (instance != null) {
            Platform.runLater(() -> instance.updateUpcomingEvents());
        }
    }

    /** Displays the window to choose a class */
    private void selectClassWindow() {

        List<String> classList = plannerService.loadClasses(); // Load classes

        Stage classStage = new Stage();
        openWindows.add(classStage); // Track this window

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Create Class TableView
        TableView<String> classTable = new TableView<>();

        TableColumn<String, String> classColumn = new TableColumn<>("Class Name");
        classColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        classColumn.setPrefWidth(250);
        classTable.getColumns().add(classColumn);

        classTable.setPrefHeight(250);
        classTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        classTable.getItems().addAll(classList); // Populate table

        // Double Click to Open Class Events (But Keep This Window Open)
        classTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click required
                String selectedClass = classTable.getSelectionModel().getSelectedItem();
                if (selectedClass != null) {
                    showEventsByClass(selectedClass);
                    // Do not close classStage to keep it open
                }
            }
        });

        // Add Class Button (Below Table)
        Button addClassBtn = new Button("Add Class");
        addClassBtn.setPrefWidth(200);
        addClassBtn.getStyleClass().add("button-primary");
        addClassBtn.setOnAction(e -> {
            plannerService.addClass();
            classTable.getItems().setAll(plannerService.loadClasses()); // Refresh table
        });

        // Apply CSS Style
        vbox.getStyleClass().add("window-container");

        // Add Components to Layout
        vbox.getChildren().addAll(classTable, addClassBtn);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, 350, 400);

        // Attach styles.css
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        classStage.setTitle("Select Class");
        classStage.setScene(scene);

        // Ensure the window is tracked and removed when closed
        classStage.setOnCloseRequest(e -> openWindows.remove(classStage));

        classStage.show();
    }


    /** Shows events filtered by class */
    private void showEventsByClass(String className) {

        List<TimeSlot> allEvents = plannerService.loadEvents();
        List<TimeSlot> classEvents = new ArrayList<>();

        for (TimeSlot event : allEvents) {
            if (event.getClassName().equalsIgnoreCase(className)) {
                classEvents.add(event);
            }
        }

        Stage classStage = new Stage();
        openWindows.add(classStage); // Track this window

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Create Event TableView
        TableView<TimeSlot> tableView = new TableView<>();

        TableColumn<TimeSlot, String> nameCol = new TableColumn<>("Event Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventName()));

        TableColumn<TimeSlot, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateTimeFormatted()));
        dateCol.setComparator((date1, date2) -> {
            LocalDateTime dt1 = LocalDateTime.parse(date1, DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a"));
            LocalDateTime dt2 = LocalDateTime.parse(date2, DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a"));
            return dt1.compareTo(dt2); // Sorts from soonest to latest
        });

        TableColumn<TimeSlot, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        // Set the column to sort in ascending order by default
        tableView.getSortOrder().add(dateCol);
        dateCol.setSortable(true);

        tableView.getColumns().addAll(nameCol, dateCol, descCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getItems().addAll(classEvents); // Populate table

        // Double-click to View Event Details
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TimeSlot selectedEvent = tableView.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    showEventDetails(selectedEvent);
                }
            }
        });

        // Delete Class Button (Small, Right-Aligned)
        Button deleteClassBtn = new Button("Delete Class");
        deleteClassBtn.setPrefWidth(200);
        deleteClassBtn.getStyleClass().add("button-danger");
        deleteClassBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Are you sure you want to delete this class?");
            alert.setContentText("All events associated with this class will also be deleted.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                plannerService.deleteClass(className);

                // Close the current class window
                classStage.close();

                // Refresh the upcoming events list in the main menu
                updateUpcomingEvents();

                // Refresh the class selection window if it's open
                refreshClassSelectionWindow();
            }
        });



        // Title + Delete Button (in HBox)
        Label titleLabel = new Label("Events for " + className);
        HBox titleRow = new HBox(10, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Add Event Button (Below Table)
        Button addEventBtn = new Button("Add Event");
        addEventBtn.setPrefWidth(200);
        addEventBtn.getStyleClass().add("button-primary");
        addEventBtn.setOnAction(e -> {
            plannerService.addEventForClass(className); // Add event to the class

            // Refresh the class-specific events list
            tableView.getItems().setAll(plannerService.loadEventsForClass(className));

            // Refresh the upcoming events list in the main menu
            updateUpcomingEvents();
        });


        HBox buttonRow = new HBox(10, addEventBtn, deleteClassBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // Apply CSS Style
        vbox.getStyleClass().add("window-container");

        // Add Components to Layout
        vbox.getChildren().addAll(titleRow, tableView, buttonRow);

        Scene scene = new Scene(vbox, 500, 400);

        // Attach styles.css
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        classStage.setTitle(className + " Events");
        classStage.setScene(scene);

        // Ensure the window is tracked and removed when closed
        classStage.setOnCloseRequest(e -> openWindows.remove(classStage));

        classStage.show();
    }

    private void refreshClassSelectionWindow() {
        for (Stage stage : openWindows) {
            if (stage.getTitle().equals("Select Class")) {
                VBox vbox = (VBox) stage.getScene().getRoot();

                // Find the TableView inside the VBox
                for (Node node : vbox.getChildren()) {
                    if (node instanceof TableView) {
                        TableView<String> classTable = (TableView<String>) node;
                        classTable.getItems().setAll(plannerService.loadClasses()); // Refresh class list
                        return; // Exit after updating the table
                    }
                }
            }
        }
    }


    /** Shows a popup message */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
