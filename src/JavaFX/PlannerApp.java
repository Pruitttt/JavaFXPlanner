package JavaFX;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
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
        setBackground(root);


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

    public static PlannerApp getInstance() {
        return instance;
    }


    public void setBackground(Region root) {
        //String imagePath = getClass().getResource("/JavaFX/pexels-pixabay-531880.jpg").toExternalForm();
        String imagePath = getClass().getResource("/JavaFX/pexels-deepu-b-iyer-40465.jpg").toExternalForm();

        // Load image safely
        Image backgroundImage = new Image(imagePath);
        if (backgroundImage.isError()) {
            System.err.println("Failed to load background image.");
            return;
        }

        // Create BackgroundImage
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
        );

        // Apply background image
        root.setBackground(new Background(background));
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
                            pastEventsTable.getItems().setAll(new PlannerService().loadPastEvents()); // Reload past events
                            return; // Exit after updating the table
                        }
                    }
                }
            }
        });
    }



    private void updateUpcomingEvents() {

        List<TimeSlot> events = plannerService.getUpcomingEvents();
        upcomingEventsList.getItems().clear();

        events.removeIf(event -> !plannerService.classExists(event.getClassName()));

        events.sort(Comparator.comparing(TimeSlot::getDateTime));

        if (events.isEmpty()) {
            upcomingEventsList.getItems().add("No upcoming events.");
        } else {
            for (TimeSlot event : events) {
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
        if (event.getDescription().equals("")) {
            descLabel.setText("Description: Not Applicable");
        }

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> {
            boolean deleted = confirmAndDeleteEvent(event);
            if (deleted) {
                Platform.runLater(() -> {
                    dialog.setResult(ButtonType.CLOSE);
                    dialog.close();
                });
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);

        // Check if the event is in the past
        if (event.getDateTime().isAfter(LocalDateTime.now())) {
            Button modifyButton = new Button("Modify");
            modifyButton.setOnAction(e -> {
                modifyEvent(event);
                dialog.close();
            });

            modifyButton.setPrefWidth(120);
            deleteButton.setPrefWidth(120);

            HBox.setHgrow(modifyButton, Priority.ALWAYS);
            buttonBox.getChildren().addAll(modifyButton, deleteButton);
        } else {
            deleteButton.setPrefWidth(120);
            buttonBox.getChildren().add(deleteButton);
        }

        VBox vbox = new VBox(10, classLabel, dateLabel, descLabel, buttonBox);
        PlannerApp.getInstance().setBackground(vbox);
        vbox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(vbox);

        // **Apply styles.css to the dialog**
        Scene scene = dialog.getDialogPane().getScene();
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

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
        plannerService.movePastEventsToStorage(); // Ensure past events are moved before displaying
        List<TimeSlot> pastEvents = plannerService.loadPastEvents(); // Reload past events

        Stage pastStage = new Stage();
        openWindows.add(pastStage); // Track this window

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        setBackground(vbox);

        // 🎯 ListView to display past events
        ListView<TimeSlot> listView = new ListView<>();
        listView.getItems().setAll(pastEvents);

        // 📆 Custom ListCell to format events as "Event Name - Class - Month Day, Year"
        listView.setCellFactory(lv -> new ListCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

            @Override
            protected void updateItem(TimeSlot event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                } else {
                    String formattedDate = event.getDateTime().format(formatter);
                    setText(event.getEventName() + " - " + event.getClassName() + " - " + formattedDate);
                }
            }
        });

        // ✨ Enable Double-Click to Open Event Details Dialog
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click detection
                TimeSlot selectedEvent = listView.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    showEventDetails(selectedEvent); // Open event details dialog
                }
            }
        });

        // 🗑️ "Clear All Past Events" Button
        Button clearPastEventsBtn = new Button("Clear All Past Events");
        clearPastEventsBtn.setStyle("-fx-background-color: #ff4c4c; -fx-text-fill: white; -fx-font-weight: bold;");

        clearPastEventsBtn.setOnAction(e -> {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Clear Past Events");
            confirmDialog.setHeaderText("Are you sure you want to delete ALL past events?");
            confirmDialog.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                plannerService.clearPastEvents(); // Deletes past_events.txt content
                listView.getItems().clear(); // Clear UI after deletion
            }
        });

        // Layout
        HBox buttonBox = new HBox(clearPastEventsBtn);
        buttonBox.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(new Label("Past Events:"), listView, buttonBox);
        Scene scene = new Scene(vbox, 500, 450);
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        pastStage.setTitle("Past Events");
        pastStage.setScene(scene);

        pastStage.setOnCloseRequest(e -> openWindows.remove(pastStage)); // Remove from tracked windows

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
                plannerService.deleteEvent(eventToDelete.getEventName(), eventToDelete.getClassName()); // Updated to use two parameters
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

        VBox vbox = new VBox(5);// Reduce spacing between elements
        setBackground(vbox);
        vbox.setPadding(new Insets(5)); // Reduce padding to bring border closer

        // ListView for Class Selection
        ListView<String> classListView = new ListView<>();
        classListView.getItems().addAll(classList);

        classListView.setPrefHeight(250);
        classListView.setMaxHeight(Double.MAX_VALUE); // Make it expand fully

        VBox.setVgrow(classListView, Priority.ALWAYS); // Allow ListView to fill space

        // Double Click to Open Class Events
        classListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click required
                String selectedClass = classListView.getSelectionModel().getSelectedItem();
                if (selectedClass != null) {
                    showEventsByClass(selectedClass);
                }
            }
        });

        // Add Class Button (Below ListView)
        Button addClassBtn = new Button("Add Class");
        addClassBtn.setPrefWidth(200);
        addClassBtn.getStyleClass().add("button-primary");
        addClassBtn.setOnAction(e -> {
            plannerService.addClass();
            classListView.getItems().setAll(plannerService.loadClasses()); // Refresh ListView
        });

        // Add Components to Layout
        Label classes = new Label("Classes: ");
        VBox classBtn = new VBox(addClassBtn);
        classBtn.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(classes, classListView, classBtn);
        vbox.setAlignment(Pos.CENTER_LEFT);

        // Reduce window size to match content
        Scene scene = new Scene(vbox);
        classStage.sizeToScene(); // Adjusts window size to fit content

        // Attach styles.css
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        classStage.setTitle("Select Class");
        classStage.setScene(scene);
        classStage.sizeToScene(); // Auto-adjust window to content size

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
        setBackground(vbox);

        // 🟢 Define a reasonable width that fits within the window

        // 🟢 Create headers and center text
        Label nameHeader = new Label("Event Name");
        Label dateHeader = new Label("Date");
        Label descHeader = new Label("Description");

// 🟢 Style headers for better alignment
        nameHeader.setAlignment(Pos.CENTER);
        dateHeader.setAlignment(Pos.CENTER);
        descHeader.setAlignment(Pos.CENTER);

        nameHeader.setMaxWidth(Double.MAX_VALUE);
        dateHeader.setMaxWidth(Double.MAX_VALUE);
        descHeader.setMaxWidth(Double.MAX_VALUE);

        nameHeader.getStyleClass().add("list-header");
        dateHeader.getStyleClass().add("list-header");
        descHeader.getStyleClass().add("list-header");

// 🟢 Create ListViews
        ListView<String> nameList = new ListView<>();
        ListView<String> dateList = new ListView<>();
        ListView<String> descList = new ListView<>();

// 🟢 Ensure all ListViews have the same size
        double listWidth = 180;
        double listHeight = 300;

        nameList.setPrefSize(listWidth, listHeight);
        dateList.setPrefSize(listWidth, listHeight);
        descList.setPrefSize(listWidth, listHeight);

        nameList.setMaxSize(listWidth, listHeight);
        dateList.setMaxSize(listWidth, listHeight);
        descList.setMaxSize(listWidth, listHeight);

// 🟢 Populate ListViews
        for (TimeSlot event : classEvents) {
            nameList.getItems().add(event.getEventName());
            dateList.getItems().add(event.getDateTimeFormatted());
            descList.getItems().add(event.getDescription());
        }

// 🟢 Synchronize Selection & Hover
        synchronizeListViews(nameList, dateList, descList, classEvents);

// 🟢 Wrap ListViews in VBox to ensure headers are centered
        VBox nameColumn = new VBox(5, nameHeader, nameList);
        VBox dateColumn = new VBox(5, dateHeader, dateList);
        VBox descColumn = new VBox(5, descHeader, descList);

        nameColumn.setAlignment(Pos.TOP_CENTER);
        dateColumn.setAlignment(Pos.TOP_CENTER);
        descColumn.setAlignment(Pos.TOP_CENTER);

// 🟢 Put everything inside an HBox
        HBox listContainer = new HBox(1, nameColumn, dateColumn, descColumn);
        listContainer.setAlignment(Pos.CENTER);


        // 🟢 Add Event Button
        Button addEventBtn = new Button("Add Event");
        addEventBtn.setPrefWidth(200);
        addEventBtn.getStyleClass().add("button-primary");
        addEventBtn.setOnAction(e -> {
            plannerService.addEventForClass(className); // Add event
            refreshListViews(nameList, dateList, descList, className);
            updateUpcomingEvents();
        });

        // 🟢 Delete Class Button
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
                classStage.close();
                updateUpcomingEvents();
                refreshClassSelectionWindow();
            }
        });

        HBox buttonRow = new HBox(10, addEventBtn, deleteClassBtn);
        buttonRow.setAlignment(Pos.CENTER);

        // 🟢 Add Components to Layout
        vbox.getChildren().addAll(new Label("Events for " + className + ":"), listContainer, buttonRow);

        Scene scene = new Scene(vbox, 550, 400);

        // Attach styles.css
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(cssFile);

        classStage.setTitle(className + " Events");
        classStage.setScene(scene);
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

    private void synchronizeListViews(ListView<String> nameList, ListView<String> dateList, ListView<String> descList, List<TimeSlot> classEvents) {
        List<ListView<String>> listViews = List.of(nameList, dateList, descList);

        // 🟢 Synchronize Selection Across Lists
        for (ListView<String> listView : listViews) {
            listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.intValue() != -1) { // Ensure valid index
                    for (ListView<String> lv : listViews) {
                        if (lv != listView) {
                            lv.getSelectionModel().clearSelection();
                            lv.getSelectionModel().select(newVal.intValue());
                        }
                    }
                }
            });
        }

        // 🟢 Improved Hover Synchronization
        for (ListView<String> listView : listViews) {
            listView.setCellFactory(lv -> {
                ListCell<String> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle(""); // Reset styling
                        } else {
                            setText(item);
                        }
                    }
                };

                // 🟢 Add real-time hover effect
                cell.setOnMouseEntered(event -> {
                    int index = cell.getIndex();
                    if (index != -1) {
                        for (ListView<String> lz : listViews) {
                            lv.getSelectionModel().clearSelection();
                            lv.getSelectionModel().select(index);
                        }
                    }
                });

                cell.setOnMouseExited(event -> {
                    for (ListView<String> lc : listViews) {
                        lv.getSelectionModel().clearSelection();
                    }
                });

                return cell;
            });
        }

        // 🟢 Restore Double-Click Functionality for Viewing Event Details
        for (ListView<String> listView : listViews) {
            listView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Double-click opens event details
                    int selectedIndex = listView.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0 && selectedIndex < classEvents.size()) {
                        showEventDetails(classEvents.get(selectedIndex)); // Open event details
                    }
                }
            });
        }
    }


    // 🟢 Helper Method: Determine Which Row is Being Hovered Over
    private int getHoveredIndex(ListView<String> listView, double mouseY) {
        int cellHeight = 30; // Adjust based on your ListView row height
        return (int) (mouseY / cellHeight);
    }



    private void refreshListViews(ListView<String> nameList, ListView<String> dateList, ListView<String> descList, String className) {
        List<TimeSlot> updatedEvents = plannerService.loadEventsForClass(className);

        nameList.getItems().clear();
        dateList.getItems().clear();
        descList.getItems().clear();

        for (TimeSlot event : updatedEvents) {
            nameList.getItems().add(event.getEventName());
            dateList.getItems().add(event.getDateTimeFormatted());
            descList.getItems().add(event.getDescription());
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
