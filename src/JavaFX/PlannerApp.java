package JavaFX;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PlannerApp extends Application {
    private static PlannerApp instance;
    private PlannerService plannerService;
    private ListView<String> upcomingEventsList;
    private StackPane root; // Single root container for all views
    private Stage primaryStage; // Single stage reference
    private Region backLayer; // Background layer 1
    private Region frontLayer; // Background layer 2
    private Timeline backgroundTimeline; // Timeline for background fading
    private List<BackgroundImage> backgrounds; // Preloaded background images
    private Node previousView; // Store the previous view to restore it

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;
        plannerService = new PlannerService();

        // Preload background images synchronously
        preloadBackgroundImages();

        root = new StackPane();
        root.setStyle("-fx-background-color: #474747 ;");
        // Set up the background once at startup and persist it across views
        initializeBackground();

        showMainView();

        Scene scene = new Scene(root, 700, 750);
        scene.setFill(Color.TRANSPARENT);
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        if (cssFile == null) {
            System.err.println("CSS file not found!");
        } else {
            System.out.println("CSS file loaded: " + cssFile);
            scene.getStylesheets().add(cssFile);
        }

        primaryStage.setTitle("Planner App");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static PlannerApp getInstance() {
        return instance;
    }

    private void preloadBackgroundImages() {
        String[] imagePaths = {
                "/JavaFX/pexels-eberhardgross-1670187.jpg",
                "/JavaFX/pexels-katja-79053-592077.jpg",
                "/JavaFX/pexels-mattdvphotography-3082313.jpg",
                "/JavaFX/pexels-pixabay-33109.jpg",
                "/JavaFX/pexels-todd-trapani-488382-2754200.jpg",
        };

        backgrounds = new ArrayList<>();
        for (String path : imagePaths) {
            try {
                System.out.println("Loading image: " + path);
                Image image = new Image(getClass().getResource(path).toExternalForm(), true);
                if (!image.isError()) {
                    backgrounds.add(new BackgroundImage(
                            image,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
                    ));
                }
            } catch (NullPointerException e) {
                System.err.println("Skipping missing resource: " + path);
            }
        }

        if (backgrounds.isEmpty()) {
            System.err.println("No background images loaded. Using fallback.");
        } else {
            System.out.println("Successfully preloaded " + backgrounds.size() + " background images.");
        }
    }

    private void initializeBackground() {
        if (backgrounds == null || backgrounds.isEmpty()) {
            root.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, CornerRadii.EMPTY, Insets.EMPTY)));
            return;
        }

        // Shuffle for variety
        Collections.shuffle(backgrounds);

        // Create background layers
        backLayer = new Region();
        frontLayer = new Region();

        backLayer.setBackground(new Background(backgrounds.get(0))); // Start with first image
        frontLayer.setBackground(new Background(backgrounds.get(1 % backgrounds.size()))); // Next image
        frontLayer.setOpacity(0); // Front layer starts invisible

        // Apply GaussianBlur to the background layers
        GaussianBlur blur = new GaussianBlur(20); // Adjust blur radius as needed
        backLayer.setEffect(blur);
        frontLayer.setEffect(blur);

        // Ensure layers fill the root
        backLayer.prefWidthProperty().bind(root.widthProperty());
        backLayer.prefHeightProperty().bind(root.heightProperty());
        frontLayer.prefWidthProperty().bind(root.widthProperty());
        frontLayer.prefHeightProperty().bind(root.heightProperty());

        // Add layers to the StackPane (root) at the bottom
        root.getChildren().addAll(backLayer, frontLayer);

        // Smooth background cycling with cross-fade
        AtomicInteger currentIndex = new AtomicInteger(1); // Start at second image
        backgroundTimeline = new Timeline();
        backgroundTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(5), e -> {
            int nextIndex = (currentIndex.get() + 1) % backgrounds.size(); // Move to next image, loop back

            // Prepare the next image in the front layer
            frontLayer.setBackground(new Background(backgrounds.get(nextIndex)));

            // Cross-fade: fade out backLayer while fading in frontLayer
            FadeTransition fadeOutBack = new FadeTransition(Duration.seconds(2), backLayer);
            fadeOutBack.setFromValue(1);
            fadeOutBack.setToValue(0);

            FadeTransition fadeInFront = new FadeTransition(Duration.seconds(2), frontLayer);
            fadeInFront.setFromValue(0);
            fadeInFront.setToValue(1);

            // On completion, swap roles and prepare for next transition
            fadeInFront.setOnFinished(event -> {
                backLayer.setBackground(frontLayer.getBackground());
                backLayer.setOpacity(1); // Reset backLayer to full opacity
                frontLayer.setOpacity(0); // Reset frontLayer to invisible
                currentIndex.set(nextIndex);
            });

            // Play both transitions simultaneously for a smoother cross-fade
            fadeOutBack.play();
            fadeInFront.play();
        }));
        backgroundTimeline.setCycleCount(Timeline.INDEFINITE);
        backgroundTimeline.play();
    }

    private void showMainView() {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        BorderPane uiLayout = new BorderPane();
        uiLayout.setPadding(new Insets(20));

        Label title = new Label("Upcoming Events");
        title.setStyle("-fx-font-size: 40px;" +
                "-fx-font-family: Oswald ");
        title.setPadding(new Insets(10));
        title.getStyleClass().add("dialog-label");

        upcomingEventsList = new ListView<>();
        upcomingEventsList.setPrefWidth(400);
        upcomingEventsList.setPrefHeight(500);
        upcomingEventsList.setMaxWidth(400);
        upcomingEventsList.setMaxHeight(500);
        upcomingEventsList.setMinWidth(400);
        upcomingEventsList.setMinHeight(500);
        plannerService.addUpdateListener(() -> Platform.runLater(this::updateUpcomingEvents));
        plannerService.movePastEventsToStorage();
        updateUpcomingEvents();

        // Customize the cell factory to disable hover and selection effects
        upcomingEventsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                }
                // Disable hover and selection background changes
                setOnMouseEntered(e -> setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));
                setOnMouseExited(e -> setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));
                setOnMouseClicked(e -> setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));
            }
        });

        Button addEventBtn = new Button("Add Event");
        addEventBtn.getStyleClass().add("button");
        addEventBtn.setPrefWidth(120);
        addEventBtn.setOnAction(e -> showAddEventView(null));

        Button viewByClassBtn = new Button("Class View");
        viewByClassBtn.getStyleClass().add("button");
        viewByClassBtn.setPrefWidth(120);
        viewByClassBtn.setOnAction(e -> showClassSelectionView());

        Button recentEventsBtn = new Button("Past Events");
        recentEventsBtn.getStyleClass().add("button");
        recentEventsBtn.setPrefWidth(120);
        recentEventsBtn.setOnAction(e -> showPastEventsView());

        HBox buttonRow = new HBox(15, addEventBtn, viewByClassBtn, recentEventsBtn);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setMaxWidth(400);

        VBox listWithButtons = new VBox(15, upcomingEventsList, buttonRow);
        listWithButtons.setAlignment(Pos.CENTER);

        StackPane listPane = new StackPane(listWithButtons);
        listPane.getStyleClass().add("glass-panel");
        listPane.setMaxWidth(400);
        listPane.setMaxHeight(560);

        uiLayout.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(40, 0, 0, 0));
        uiLayout.setCenter(listPane);
        BorderPane.setAlignment(listPane, Pos.CENTER);

        Region translucentBox = new Region();
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        translucentBox.setPrefWidth(500);
        translucentBox.setPrefHeight(700);
        translucentBox.setMaxWidth(500);
        translucentBox.setMaxHeight(700);
        translucentBox.setMinWidth(500);
        translucentBox.setMinHeight(700);

        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, uiLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER);
        previousView = uiLayout;

        // Updated handler to ignore "No upcoming events" message
        upcomingEventsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedEvent = upcomingEventsList.getSelectionModel().getSelectedItem();
                if (selectedEvent != null && !selectedEvent.equals("No upcoming events.")) {
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
                        showEventDetailsView(eventDetails);
                    }
                }
            }
        });

        // Track whether a confirmation card is open
        final boolean[] isConfirmationCardOpen = {false};

        // Key event handler for the main view
        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE && !isConfirmationCardOpen[0]) {
                // Prevent multiple cards from opening
                isConfirmationCardOpen[0] = true;

                // Create a confirmation card for exiting the application
                VBox confirmationCard = new VBox(10);
                confirmationCard.getStyleClass().add("event-card");
                confirmationCard.setPadding(new Insets(15));
                confirmationCard.setMaxWidth(320);
                confirmationCard.setMaxHeight(200);
                confirmationCard.setAlignment(Pos.CENTER);

                Label cardTitle = new Label("Exit Application");
                cardTitle.getStyleClass().add("card-title");

                Separator separator = new Separator();
                separator.getStyleClass().add("card-separator");
                separator.setPrefWidth(280);

                Label message = new Label("Are you sure you want to exit the application?");
                message.setWrapText(true);
                message.setMaxWidth(280);
                message.setAlignment(Pos.CENTER);
                message.getStyleClass().add("card-label-value");

                Button confirmButton = new Button("Confirm");
                confirmButton.getStyleClass().add("card-button-delete");
                confirmButton.setPrefWidth(100);
                confirmButton.setOnAction(evt -> {
                    Platform.exit(); // Exit the application if confirmed
                });

                Button cancelButton = new Button("Cancel");
                cancelButton.getStyleClass().add("card-button-close");
                cancelButton.setPrefWidth(100);
                cancelButton.setOnAction(evt -> {
                    root.getChildren().remove(confirmationCard);
                    isConfirmationCardOpen[0] = false; // Reset the flag when the card is closed
                });

                HBox buttonBox = new HBox(8, cancelButton, confirmButton);
                buttonBox.setAlignment(Pos.CENTER);

                confirmationCard.getChildren().addAll(cardTitle, separator, message, buttonBox);
                StackPane.setAlignment(confirmationCard, Pos.CENTER);
                StackPane.setMargin(confirmationCard, new Insets(10, 0, 10, 0));

                root.getChildren().add(confirmationCard);

                // Animation for card entrance
                confirmationCard.setScaleX(0.8);
                confirmationCard.setScaleY(0.8);
                confirmationCard.setOpacity(0);
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(150),
                                new KeyValue(confirmationCard.scaleXProperty(), 1, Interpolator.EASE_OUT),
                                new KeyValue(confirmationCard.scaleYProperty(), 1, Interpolator.EASE_OUT),
                                new KeyValue(confirmationCard.opacityProperty(), 1, Interpolator.EASE_OUT)
                        )
                );
                timeline.play();

                // Ensure the confirmation card receives focus
                Platform.runLater(() -> {
                    confirmationCard.requestFocus();
                });

                // Add key event handlers for the confirmation card
                confirmationCard.setOnKeyPressed(cardKeyEvent -> {
                    if (cardKeyEvent.getCode() == KeyCode.ENTER) {
                        confirmButton.fire(); // Trigger the Confirm button action
                        cardKeyEvent.consume();
                    } else if (cardKeyEvent.getCode() == KeyCode.ESCAPE) {
                        cancelButton.fire(); // Trigger the Cancel button action
                        cardKeyEvent.consume();
                    }
                });
                confirmationCard.setFocusTraversable(true);

                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showAddEventView(String preselectedClass) {
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        Label titleLabel = new Label("Add Event");
        titleLabel.getStyleClass().add("dialog-label");
        titleLabel.setStyle("-fx-font-size: 40px;");

        VBox content = new VBox(10);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        Label classLabel = new Label("Class:");
        classLabel.getStyleClass().add("card-label-key");
        ComboBox<String> classDropdown = new ComboBox<>();
        classDropdown.getItems().addAll(plannerService.loadClasses());
        classDropdown.getItems().add("Add New Class...");
        classDropdown.setValue(preselectedClass != null ? preselectedClass : "Add New Class...");
        classDropdown.setPrefWidth(320);
        classDropdown.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #cccccc;");

        Label newClassLabel = new Label("New Class:");
        newClassLabel.getStyleClass().add("card-label-key");
        TextField newClassField = new TextField();
        newClassField.setPromptText("New Class Name");
        newClassField.getStyleClass().add("text-field-custom");
        newClassField.setPrefWidth(320);
        newClassField.setStyle("-fx-prompt-text-fill: #ffffff;");

        // Fix: Disable class selection when preselectedClass is not null
        if (preselectedClass != null) {
            classDropdown.setDisable(true);
            newClassField.setDisable(true);
        } else {
            newClassField.setDisable(classDropdown.getValue() != "Add New Class...");
            classDropdown.setOnAction(e -> {
                newClassField.setDisable(!classDropdown.getValue().equals("Add New Class..."));
                if (classDropdown.getValue().equals("Add New Class...")) {
                    Platform.runLater(newClassField::requestFocus);
                }
            });
        }

        Label eventNameLabel = new Label("Event Name:");
        eventNameLabel.getStyleClass().add("card-label-key");
        TextField eventNameField = new TextField();
        eventNameField.setPromptText("Event Name");
        eventNameField.getStyleClass().add("text-field-custom");
        eventNameField.setPrefWidth(320);
        eventNameField.setStyle("-fx-prompt-text-fill: #ffffff;");

        Label dateLabel = new Label("Date:");
        dateLabel.getStyleClass().add("card-label-key");
        TextField monthField = new TextField();
        monthField.setPromptText("MM");
        monthField.getStyleClass().add("text-field-custom");
        monthField.setPrefWidth(60);
        monthField.setStyle("-fx-prompt-text-fill: #ffffff;");
        TextField dayField = new TextField();
        dayField.setPromptText("DD");
        dayField.getStyleClass().add("text-field-custom");
        dayField.setPrefWidth(60);
        dayField.setStyle("-fx-prompt-text-fill: #ffffff;");
        TextField yearField = new TextField();
        yearField.setPromptText("YYYY");
        yearField.getStyleClass().add("text-field-custom");
        yearField.setPrefWidth(170);
        yearField.setStyle("-fx-prompt-text-fill: #ffffff;");
        HBox dateBox = new HBox(5, monthField, new Label("/"), dayField, new Label("/"), yearField);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setPrefWidth(320);

        Label timeLabel = new Label("Time:");
        timeLabel.getStyleClass().add("card-label-key");
        TextField hourField = new TextField();
        hourField.setPromptText("HH");
        hourField.getStyleClass().add("text-field-custom");
        hourField.setPrefWidth(60);
        hourField.setStyle("-fx-prompt-text-fill: #ffffff;");
        TextField minuteField = new TextField();
        minuteField.setPromptText("MM");
        minuteField.getStyleClass().add("text-field-custom");
        minuteField.setPrefWidth(60);
        minuteField.setStyle("-fx-prompt-text-fill: #ffffff;");
        ComboBox<String> amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setValue("AM");
        amPmDropdown.setPrefWidth(170);
        amPmDropdown.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-prompt-text-fill: #ffffff;");
        amPmDropdown.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                }
            }
        });
        HBox timeBox = new HBox(10, hourField, new Label(" : "), minuteField, amPmDropdown);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        timeBox.setPrefWidth(320);

        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("card-label-key");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setWrapText(true);
        descField.setPrefRowCount(5);
        descField.setPrefWidth(320);
        descField.getStyleClass().add("text-field-custom");
        descField.setStyle("-fx-prompt-text-fill: #ffffff;");

        grid.add(classLabel, 0, 0);
        grid.add(classDropdown, 1, 0);
        grid.add(newClassLabel, 0, 1);
        grid.add(newClassField, 1, 1);
        grid.add(eventNameLabel, 0, 2);
        grid.add(eventNameField, 1, 2);
        grid.add(dateLabel, 0, 3);
        grid.add(dateBox, 1, 3);
        grid.add(timeLabel, 0, 4);
        grid.add(timeBox, 1, 4);
        grid.add(descLabel, 0, 5);
        grid.add(descField, 1, 5);

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> {
            if (preselectedClass != null) {
                showEventsByClassView(preselectedClass);
            } else {
                showMainView();
            }
        });

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("card-button-modify");
        okButton.setPrefWidth(200);
        okButton.setOnAction(e -> {
            try {
                String className = classDropdown.getValue().equals("Add New Class...") ? newClassField.getText().trim() : classDropdown.getValue();
                String eventName = eventNameField.getText().trim();
                String description = descField.getText().trim();

                // Validate class and event name
                if (className == null || className.isEmpty() || (className.equals("Add New Class...") && newClassField.getText().trim().isEmpty())) {
                    showAlert("Invalid Input", "Class name cannot be empty.");
                    return;
                }
                if (eventName.isEmpty()) {
                    showAlert("Invalid Input", "Event Name cannot be empty.");
                    return;
                }

                // Validate date fields
                String monthText = monthField.getText().trim();
                String dayText = dayField.getText().trim();
                String yearText = yearField.getText().trim();
                if (monthText.isEmpty() || dayText.isEmpty()) {
                    showAlert("Invalid Input", "Month and Day fields cannot be empty.");
                    return;
                }
                int month = Integer.parseInt(monthText);
                int day = Integer.parseInt(dayText);
                int year = yearText.isEmpty() ? LocalDateTime.now().getYear() : Integer.parseInt(yearText);

                // Validate time fields
                String hourText = hourField.getText().trim();
                String minuteText = minuteField.getText().trim();
                int hour = hourText.isEmpty() ? 12 : Integer.parseInt(hourText);
                int minute = minuteText.isEmpty() ? 0 : Integer.parseInt(minuteText);
                String amPmValue = amPmDropdown.getValue() != null ? amPmDropdown.getValue() : "AM";

                if ("PM".equals(amPmValue) && hour < 12) hour += 12;
                if ("AM".equals(amPmValue) && hour == 12) hour = 0;

                LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);
                TimeSlot event = new TimeSlot(className, eventName, dateTime, description);

                // Fix: Add the new class if necessary and save the event using the main plannerService
                if (classDropdown.getValue().equals("Add New Class...") && !newClassField.getText().trim().isEmpty()) {
                    String newClassName = newClassField.getText().trim();
                    if (!plannerService.classExists(newClassName)) {
                        plannerService.addNewClass(newClassName);
                        System.out.println("Added new class: " + newClassName);
                    }
                }

                plannerService.saveEvent(event);
                System.out.println("Event saved: " + event.toString());
                plannerService.movePastEventsToStorage();

                if (preselectedClass != null) {
                    showEventsByClassView(preselectedClass);
                } else {
                    showMainView();
                }
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Ensure all date/time fields contain valid numbers.");
            } catch (DateTimeException ex) {
                showAlert("Invalid Date", "The date is invalid (e.g., February 30 does not exist).");
            }
        });

        HBox buttonBox = new HBox(20, backButton, okButton);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(grid, buttonBox);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxWidth(500);
        StackPane.setMargin(scrollPane, new Insets(20));

        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(titleLabel, scrollPane);

        Region translucentBox = new Region();
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        translucentBox.prefWidthProperty().bind(content.widthProperty().add(40));
        translucentBox.prefHeightProperty().bind(content.heightProperty().add(40));
        translucentBox.setMaxWidth(540);
        translucentBox.setMaxHeight(600);
        translucentBox.setMinWidth(540);
        translucentBox.setMinHeight(600);

        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER);
        previousView = contentWithBackdrop;

        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                okButton.fire();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire();
                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        if (preselectedClass == null) {
            Platform.runLater(() -> {
                newClassField.setDisable(false);
                newClassField.requestFocus();
            });
        }

        Platform.runLater(() -> root.getScene().getWindow().requestFocus());

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showPastEventsView() {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        // Create and style the title label
        Label title = new Label("Past Events");
        title.setStyle("-fx-font-size: 40px;");
        title.setAlignment(Pos.CENTER);
        title.getStyleClass().add("dialog-label");

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(450);

        ListView<TimeSlot> pastEventsList = new ListView<>();
        pastEventsList.getItems().setAll(plannerService.loadPastEvents());
        pastEventsList.setPrefHeight(300);
        pastEventsList.setCellFactory(lv -> new ListCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            @Override
            protected void updateItem(TimeSlot event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                } else {
                    setText(event.getEventName() + " - " + event.getClassName() + " - " + event.getDateTime().format(formatter));
                }
            }
        });
        pastEventsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TimeSlot selectedEvent = pastEventsList.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    showEventDetailsView(selectedEvent);
                }
            }
        });

        Button clearPastEventsBtn = new Button("Clear All Past Events");
        clearPastEventsBtn.getStyleClass().add("button");
        clearPastEventsBtn.setPrefWidth(200);
        clearPastEventsBtn.setOnAction(e -> {
            // Create a confirmation card
            VBox confirmationCard = new VBox(10);
            confirmationCard.getStyleClass().add("event-card");
            confirmationCard.setPadding(new Insets(15));
            confirmationCard.setMaxWidth(320);
            confirmationCard.setMaxHeight(200);
            confirmationCard.setAlignment(Pos.CENTER);

            Label cardTitle = new Label("Clear Past Events");
            cardTitle.getStyleClass().add("card-title");
            cardTitle.setStyle("-fx-font-size: 20px;");

            Separator separator = new Separator();
            separator.getStyleClass().add("card-separator");
            separator.setPrefWidth(280);

            Label message = new Label("Are you sure you want to delete ALL past events? This action cannot be undone.");
            message.setWrapText(true);
            message.setMaxWidth(280);
            message.setAlignment(Pos.CENTER);
            message.getStyleClass().add("card-label-value");

            Button confirmButton = new Button("Confirm");
            confirmButton.getStyleClass().add("card-button-delete"); // Reuse delete style for consistency
            confirmButton.setPrefWidth(100);
            confirmButton.setOnAction(evt -> {
                plannerService.clearPastEvents();
                pastEventsList.getItems().clear();
                root.getChildren().remove(confirmationCard); // Remove card after confirmation
            });

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("card-button-close");
            cancelButton.setPrefWidth(100);
            cancelButton.setOnAction(evt -> root.getChildren().remove(confirmationCard)); // Remove card on cancel

            HBox buttonBox = new HBox(8, cancelButton, confirmButton);
            buttonBox.setAlignment(Pos.CENTER);

            confirmationCard.getChildren().addAll(cardTitle, separator, message, buttonBox);
            StackPane.setAlignment(confirmationCard, Pos.CENTER);
            StackPane.setMargin(confirmationCard, new Insets(10, 0, 10, 0));

            root.getChildren().add(confirmationCard);

            // Animation for card entrance
            confirmationCard.setScaleX(0.8);
            confirmationCard.setScaleY(0.8);
            confirmationCard.setOpacity(0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(confirmationCard.scaleXProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.scaleYProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> showMainView());

        HBox buttonBox = new HBox(15, backButton, clearPastEventsBtn);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(pastEventsList, buttonBox);

        // Create a new VBox to hold the title and the content
        VBox mainLayout = new VBox(10); // 10 pixels spacing between title and content
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(title, content);

        // Create a new Region to act as the additional translucent box behind the content
        Region translucentBox = new Region();
        // Use the same distinct style as in showMainView, showAddEventView, and showClassSelectionView
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        // Set a fixed size to wrap around the title, ListView, and buttons with padding
        translucentBox.setPrefWidth(500);
        translucentBox.setPrefHeight(500); // Match the height from showClassSelectionView
        translucentBox.setMaxWidth(500);
        translucentBox.setMaxHeight(500);
        translucentBox.setMinWidth(500);
        translucentBox.setMinHeight(500);

        // Create a StackPane to layer the translucent box behind the mainLayout
        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        // Add the contentWithBackdrop to the root StackPane
        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER); // Center the content in the window
        previousView = contentWithBackdrop; // Store the contentWithBackdrop as the previous view

        // Add key event handlers for Enter and Escape
        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                // No specific OK action here, as past events view has no OK button
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire(); // Trigger the Back button action
                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showClassSelectionView() {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        // Create and style the title label
        Label title = new Label("Select Class");
        title.setStyle("-fx-font-size: 40");
        title.getStyleClass().add("dialog-label");

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);

        ListView<String> classListView = new ListView<>();
        classListView.getItems().addAll(plannerService.loadClasses());
        classListView.setPrefHeight(300);
        classListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedClass = classListView.getSelectionModel().getSelectedItem();
                if (selectedClass != null) {
                    showEventsByClassView(selectedClass);
                }
            }
        });

        Button addClassBtn = new Button("Add Class");
        addClassBtn.getStyleClass().add("button");
        addClassBtn.setPrefWidth(200);
        addClassBtn.setOnAction(e -> {
            // Create the Add Class card content
            VBox cardContent = new VBox(10);
            cardContent.setStyle("-fx-background-color: rgba(150, 150, 150, 1); -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
            cardContent.setPadding(new Insets(15));
            cardContent.setMaxWidth(320); // Match the card width
            cardContent.setMaxHeight(200); // Constrain height
            cardContent.setAlignment(Pos.TOP_LEFT); // Left-aligned content

            // Enhanced Title
            Label titleLabel = new Label("Add Class");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 10 0;");

            // Input section
            Label promptLabel = new Label("Enter New Class Name:");
            promptLabel.getStyleClass().add("card-label-key");

            TextField classNameField = new TextField();
            classNameField.setPromptText("Class:");
            classNameField.getStyleClass().add("text-field-custom"); // Custom style for text field
            classNameField.setMaxWidth(280); // Fit within the card

            // Buttons (initially created without actions)
            Button okButton = new Button("OK");
            okButton.getStyleClass().add("card-button-modify"); // Match button style
            okButton.setPrefWidth(100);

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("card-button-close"); // Match button style
            cancelButton.setPrefWidth(100);

            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_LEFT); // Left-aligned buttons
            buttonBox.getChildren().addAll(okButton, cancelButton);

            // Add components to cardContent
            cardContent.getChildren().addAll(titleLabel, promptLabel, classNameField, buttonBox);

            // Create the card without a separate translucent box
            StackPane cardWithBackdrop = new StackPane();
            cardWithBackdrop.getChildren().add(cardContent); // Only cardContent, no translucentBox
            StackPane.setAlignment(cardWithBackdrop, Pos.CENTER); // Keep centered in root
            root.getChildren().add(cardWithBackdrop);

            // Define the action for OK (shared between button and Enter key)
            EventHandler<ActionEvent> okAction = okEvent -> {
                String className = classNameField.getText().trim();
                if (!className.isEmpty()) {
                    plannerService.addNewClass(className);
                    classListView.getItems().setAll(plannerService.loadClasses());
                    root.getChildren().remove(cardWithBackdrop); // Close card
                }
            };

            // Set OK button action
            okButton.setOnAction(okAction);

            // Add Enter key handler to the TextField
            classNameField.setOnAction(event -> {
                okAction.handle(new ActionEvent()); // Trigger the same action as OK button
            });

            // Set Cancel button action
            cancelButton.setOnAction(cancelEvent -> {
                root.getChildren().remove(cardWithBackdrop); // Close card
            });

            // Add Escape key handler to close the card
            cardWithBackdrop.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    okAction.handle(new ActionEvent()); // Trigger OK action on Enter
                    keyEvent.consume();
                } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    cancelButton.fire(); // Trigger the Cancel button action
                    keyEvent.consume();
                }
            });

            // Focus the text field initially
            classNameField.requestFocus();

            // Animate the card entry
            cardWithBackdrop.setScaleX(0.8);
            cardWithBackdrop.setScaleY(0.8);
            cardWithBackdrop.setOpacity(0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(cardWithBackdrop.scaleXProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(cardWithBackdrop.scaleYProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(cardWithBackdrop.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();

            // Ensure the card can receive key events
            cardWithBackdrop.setFocusTraversable(true);
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> showMainView());

        HBox buttonBox = new HBox(15, backButton, addClassBtn);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(classListView, buttonBox);

        // Create a new VBox to hold the title and the content
        VBox mainLayout = new VBox(10); // 10 pixels spacing between title and content
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(title, content);

        // Create a new Region to act as the additional translucent box behind the content
        Region translucentBox = new Region();
        // Use the same distinct style as in showMainView and showAddEventView
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        // Set a fixed size to wrap around the title, ListView, and buttons with less padding at top and bottom
        translucentBox.setPrefWidth(500);
        translucentBox.setPrefHeight(500); // Reduced height to decrease space at top and bottom
        translucentBox.setMaxWidth(500);
        translucentBox.setMaxHeight(500);
        translucentBox.setMinWidth(500);
        translucentBox.setMinHeight(500);

        // Create a StackPane to layer the translucent box behind the mainLayout
        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        // Add the contentWithBackdrop to the root StackPane
        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER); // Center the content in the window
        previousView = contentWithBackdrop; // Store the contentWithBackdrop as the previous view

        // Add key event handlers for Enter and Escape
        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                // No specific OK action here, as class selection view has no OK button
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire(); // Trigger the Back button action
                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    // Helper method to split the class name into two parts based on word order
    private String[] splitClassName(String className) {
        // Split the class name into words
        String[] words = className.trim().split("\\s+");
        if (words.length <= 1) {
            // If there's only one word, return it as the second part (bottom) with an empty first part
            return new String[]{"", className};
        }

        // Split into two parts based on word order
        StringBuilder firstPart = new StringBuilder();
        StringBuilder secondPart = new StringBuilder();
        int splitIndex = words.length / 2; // Split roughly in the middle of the word count

        // Build the first part (top)
        for (int i = 0; i < splitIndex; i++) {
            if (i > 0) firstPart.append(" ");
            firstPart.append(words[i]);
        }

        // Build the second part (bottom)
        for (int i = splitIndex; i < words.length; i++) {
            if (i > splitIndex) secondPart.append(" ");
            secondPart.append(words[i]);
        }

        return new String[]{firstPart.toString(), secondPart.toString()};
    }

    private void showEventsByClassView(String className) {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        // Use a single-element array to hold the className, allowing updates in lambdas
        final String[] classNameHolder = {className};

        // Define eventList and addEventBtn before the lambda expressions
        ListView<TimeSlot> eventList = new ListView<>();
        eventList.getItems().addAll(plannerService.loadEventsForClass(classNameHolder[0]));
        eventList.setMinHeight(50); // Minimum height when empty
        eventList.setMaxHeight(300); // Maximum height to prevent excessive growth
        eventList.setPrefWidth(400);
        eventList.setMaxWidth(400);
        eventList.setMinWidth(400);
        eventList.setCellFactory(lv -> new ListCell<TimeSlot>() {
            @Override
            protected void updateItem(TimeSlot event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                } else {
                    setText(event.getEventName() + " - " + event.getDateTimeFormatted() + " - " +
                            (event.getDescription().isEmpty() ? "N/A" : event.getDescription()));
                }
            }
        });
        eventList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TimeSlot selectedEvent = eventList.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    showEventDetailsView(selectedEvent);
                }
            }
        });

        Button addEventBtn = new Button("Add Event");
        addEventBtn.getStyleClass().add("button");
        addEventBtn.setPrefWidth(120);
        addEventBtn.setOnAction(e -> showAddEventView(classNameHolder[0]));

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(120);
        backButton.setOnAction(e -> showClassSelectionView());

        Button deleteClassBtn = new Button("Delete Class");
        deleteClassBtn.getStyleClass().add("button");
        deleteClassBtn.setPrefWidth(120);
        deleteClassBtn.setOnAction(e -> {
            // Create a confirmation card
            VBox confirmationCard = new VBox(10);
            confirmationCard.getStyleClass().add("event-card");
            confirmationCard.setPadding(new Insets(15));
            confirmationCard.setMaxWidth(320);
            confirmationCard.setMaxHeight(200);
            confirmationCard.setAlignment(Pos.CENTER);

            Label cardTitle = new Label("Delete Class");
            cardTitle.getStyleClass().add("card-title");
            cardTitle.setStyle("-fx-font-size: 20");

            Separator separator = new Separator();
            separator.getStyleClass().add("card-separator");
            separator.setPrefWidth(280);

            Label message = new Label("Are you sure you want to delete this class? All events associated with this class will also be deleted.");
            message.setWrapText(true);
            message.setMaxWidth(280);
            message.setAlignment(Pos.CENTER);
            message.getStyleClass().add("card-label-value");

            Button confirmButton = new Button("Confirm");
            confirmButton.getStyleClass().add("card-button-delete");
            confirmButton.setPrefWidth(100);
            confirmButton.setOnAction(evt -> {
                plannerService.deleteClass(classNameHolder[0]);
                root.getChildren().remove(confirmationCard); // Remove card after confirmation
                showClassSelectionView();
            });

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("card-button-close");
            cancelButton.setPrefWidth(100);
            cancelButton.setOnAction(evt -> root.getChildren().remove(confirmationCard)); // Remove card on cancel

            HBox buttonBox = new HBox(8, cancelButton, confirmButton);
            buttonBox.setAlignment(Pos.CENTER);

            confirmationCard.getChildren().addAll(cardTitle, separator, message, buttonBox);
            StackPane.setAlignment(confirmationCard, Pos.CENTER);
            StackPane.setMargin(confirmationCard, new Insets(10, 0, 10, 0));

            root.getChildren().add(confirmationCard);

            // Animation for card entrance
            confirmationCard.setScaleX(0.8);
            confirmationCard.setScaleY(0.8);
            confirmationCard.setOpacity(0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(confirmationCard.scaleXProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.scaleYProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();
        });

        HBox buttonBox = new HBox(15, backButton, addEventBtn, deleteClassBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(400);

        // Create a single Label for the class name (no splitting)
        Label titleLabel = new Label(classNameHolder[0]);
        titleLabel.getStyleClass().add("dialog-label");
        titleLabel.setStyle("-fx-font-size: 40; -fx-background-color: transparent;");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMouseTransparent(true); // Allow mouse events to pass through to hoverBackground

        // Measure the natural width of the class name text
        Label tempLabel = new Label(classNameHolder[0]);
        tempLabel.getStyleClass().add("dialog-label");
        tempLabel.setStyle("-fx-font-size: 40; -fx-background-color: transparent;");
        tempLabel.setWrapText(false); // Do not wrap for measurement

        // Add the tempLabel to a temporary scene to force layout calculation
        StackPane tempPane = new StackPane(tempLabel);
        Scene tempScene = new Scene(tempPane);
        tempLabel.applyCss();
        tempLabel.layout();

        double naturalTextWidth = tempLabel.getBoundsInLocal().getWidth();
        double hoverPaddingX = 20; // 10px padding on each side for the hover effect
        double hoverBoxWidth = naturalTextWidth + (hoverPaddingX * 2); // Add padding to the hover effect box
        titleLabel.setMaxWidth(Double.MAX_VALUE); // Allow the label to expand as needed
        titleLabel.setPrefWidth(naturalTextWidth); // Set preferred width to the natural width

        // Create a StackPane for the hover effect background
        StackPane hoverBackground = new StackPane();
        hoverBackground.setStyle("-fx-background-color: transparent;");
        hoverBackground.setPrefWidth(hoverBoxWidth); // Set width to text width plus padding
        hoverBackground.setPrefHeight(titleLabel.prefHeight(naturalTextWidth)); // Set height based on the label

        // Add hover effect to the StackPane
        hoverBackground.setOnMouseEntered(e -> {
            hoverBackground.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3); -fx-background-radius: 18;");
        });
        hoverBackground.setOnMouseExited(e -> {
            hoverBackground.setStyle("-fx-background-color: transparent;");
        });

        // Create the TextField for editing the class name (initially not visible)
        TextField titleField = new TextField(classNameHolder[0]);
        titleField.getStyleClass().add("dialog-label");
        titleField.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-background-color: transparent; -fx-alignment: center; " +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-highlight-fill: transparent;");
        titleField.setMaxWidth(Double.MAX_VALUE); // Allow the text field to expand as needed
        titleField.setPrefWidth(naturalTextWidth); // Set preferred width to the natural width
        titleField.setPrefHeight(titleLabel.prefHeight(naturalTextWidth)); // Match the label height
        titleField.setAlignment(Pos.CENTER);
        titleField.setVisible(false);

        // Wrap the label, hover background, and TextField in a StackPane to overlay them
        StackPane titlePane = new StackPane();
        titlePane.getChildren().addAll(hoverBackground, titleLabel, titleField);
        titlePane.setStyle("-fx-background-color: transparent;"); // Ensure the StackPane is transparent
        titlePane.setPrefWidth(hoverBoxWidth); // Set the titlePane width to match the hover box

        // Update the width when the class name changes
        titleField.textProperty().addListener((obs, oldValue, newValue) -> {
            tempLabel.setText(newValue);
            tempLabel.applyCss();
            tempLabel.layout();
            double updatedTextWidth = tempLabel.getBoundsInLocal().getWidth();
            titleLabel.setPrefWidth(updatedTextWidth); // Update prefWidth dynamically
            double updatedHoverBoxWidth = updatedTextWidth + (hoverPaddingX * 2); // Add padding to the hover effect box
            hoverBackground.setPrefWidth(updatedHoverBoxWidth);
            titleField.setPrefWidth(updatedTextWidth);
            titlePane.setPrefWidth(updatedHoverBoxWidth);
            // Update the height based on the new width
            double newTitleHeight = titleLabel.prefHeight(updatedTextWidth);
            hoverBackground.setPrefHeight(newTitleHeight);
            titleField.setPrefHeight(newTitleHeight);
        });

        // Switch to TextField on click
        titleLabel.setOnMouseClicked(e -> {
            titleLabel.setVisible(false);
            hoverBackground.setVisible(false);
            titleField.setVisible(true);
            titleField.requestFocus();
            titleField.selectAll();
        });

        // Also allow clicking the hover background to trigger editing
        hoverBackground.setOnMouseClicked(e -> {
            titleLabel.setVisible(false);
            hoverBackground.setVisible(false);
            titleField.setVisible(true);
            titleField.requestFocus();
            titleField.selectAll();
        });

        // Handle finishing the edit (Enter)
        titleField.setOnAction(e -> {
            String newClassName = titleField.getText().trim();
            try {
                if (newClassName.isEmpty()) {
                    showAlert("Invalid Input", "Class name cannot be empty.");
                    titleField.setText(classNameHolder[0]); // Revert to original
                } else if (plannerService.classExists(newClassName) && !newClassName.equalsIgnoreCase(classNameHolder[0])) {
                    showAlert("Invalid Input", "Class name '" + newClassName + "' already exists.");
                    titleField.setText(classNameHolder[0]); // Revert to original
                } else if (!newClassName.equals(classNameHolder[0])) {
                    // Rename the class
                    plannerService.renameClass(classNameHolder[0], newClassName);
                    classNameHolder[0] = newClassName; // Update the class name
                    // Update the events list
                    eventList.getItems().setAll(plannerService.loadEventsForClass(classNameHolder[0]));
                    // Update the Add Event button's action
                    addEventBtn.setOnAction(evt -> showAddEventView(classNameHolder[0]));
                    // Update the label text
                    titleLabel.setText(newClassName);
                }
                titleField.setVisible(false);
                titleLabel.setVisible(true);
                hoverBackground.setVisible(true);
            } catch (IllegalArgumentException ex) {
                showAlert("Error", ex.getMessage());
                titleField.setText(classNameHolder[0]); // Revert to original
                titleField.setVisible(false);
                titleLabel.setVisible(true);
                hoverBackground.setVisible(true);
            }
        });

        // Revert to Label when focus is lost
        titleField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String newClassName = titleField.getText().trim();
                try {
                    if (newClassName.isEmpty()) {
                        showAlert("Invalid Input", "Class name cannot be empty.");
                        titleField.setText(classNameHolder[0]); // Revert to original
                    } else if (plannerService.classExists(newClassName) && !newClassName.equalsIgnoreCase(classNameHolder[0])) {
                        showAlert("Invalid Input", "Class name '" + newClassName + "' already exists.");
                        titleField.setText(classNameHolder[0]); // Revert to original
                    } else if (!newClassName.equals(classNameHolder[0])) {
                        // Rename the class
                        plannerService.renameClass(classNameHolder[0], newClassName);
                        classNameHolder[0] = newClassName; // Update the class name
                        // Update the events list
                        eventList.getItems().setAll(plannerService.loadEventsForClass(classNameHolder[0]));
                        // Update the Add Event button's action
                        addEventBtn.setOnAction(evt -> showAddEventView(classNameHolder[0]));
                        // Update the label text
                        titleLabel.setText(newClassName);
                    }
                    titleField.setVisible(false);
                    titleLabel.setVisible(true);
                    hoverBackground.setVisible(true);
                } catch (IllegalArgumentException ex) {
                    showAlert("Error", ex.getMessage());
                    titleField.setText(classNameHolder[0]); // Revert to original
                    titleField.setVisible(false);
                    titleLabel.setVisible(true);
                    hoverBackground.setVisible(true);
                }
            }
        });

        // Create the content VBox to hold the title, event list, and buttons
        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: transparent;"); // Ensure the content VBox is transparent

        // Add the titlePane, eventList, and buttonBox to the content VBox
        content.getChildren().addAll(titlePane, eventList, buttonBox);

        // Create the main layout
        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(content);
        mainLayout.setStyle("-fx-background-color: transparent;"); // Ensure the mainLayout is transparent

        // Create the translucent box
        Region translucentBox = new Region();
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.5);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");

        // Layer the translucent box and content in a StackPane
        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        contentWithBackdrop.setStyle("-fx-background-color: transparent;"); // Ensure the StackPane is transparent
        StackPane.setAlignment(mainLayout, Pos.CENTER);
        StackPane.setAlignment(translucentBox, Pos.CENTER);

        // Dynamically set the width and height of translucentBox and contentWithBackdrop
        content.boundsInLocalProperty().addListener((obs, oldBounds, newBounds) -> {
            double maxWidth = Math.max(hoverBoxWidth, 400); // Ensure at least the width of the event list/buttons
            double paddingX = 40; // 20px padding on each side
            translucentBox.setPrefWidth(maxWidth + paddingX);
            translucentBox.setMaxWidth(maxWidth + paddingX);
            content.setMaxWidth(maxWidth);
            contentWithBackdrop.setPrefWidth(maxWidth + paddingX);

            double paddingY = 20; // 10px padding on top and bottom
            double mainLayoutSpacing = 10; // 10px spacing in mainLayout
            double totalHeight = newBounds.getHeight() + mainLayoutSpacing + paddingY;
            translucentBox.setPrefHeight(totalHeight);
            translucentBox.setMaxHeight(totalHeight);
            contentWithBackdrop.setPrefHeight(totalHeight); // Use prefHeight for better layout control
        });

        // Add to the root
        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER);
        previousView = contentWithBackdrop;

        // Add key event handlers for Enter and Escape
        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                // No specific OK action here, as events view has no OK button
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire(); // Trigger the Back button action
                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showEventDetailsView(TimeSlot event) {
        // Create Planner Service
        PlannerService plannerService = new PlannerService();

        // Create the EventDetails card content
        VBox cardContent = new VBox(10);
        cardContent.getStyleClass().add("event-card");
        cardContent.setPadding(new Insets(15));
        cardContent.setMaxWidth(320);
        cardContent.setMaxHeight(225);
        cardContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Event Details for " + event.getEventName());
        titleLabel.getStyleClass().add("card-title");

        Separator separator = new Separator();
        separator.getStyleClass().add("card-separator");
        separator.setPrefWidth(280);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label classKey = new Label("Class:");
        classKey.getStyleClass().add("card-label-key");
        Label classValue = new Label(event.getClassName());
        classValue.getStyleClass().add("card-label-value");
        classKey.setPadding(new Insets(5));

        Label dateKey = new Label("Date:");
        dateKey.getStyleClass().add("card-label-key");
        Label dateValue = new Label(event.getDateTimeFormatted());
        dateValue.getStyleClass().add("card-label-value");
        dateKey.setPadding(new Insets(5));

        Label timeKey = new Label("Time:");
        timeKey.getStyleClass().add("card-label-key");
        Label timeValue = new Label(event.getTimeFormatted());
        timeValue.getStyleClass().add("card-label-value");
        timeKey.setPadding(new Insets(5));

        Label descKey = new Label("Description:");
        descKey.getStyleClass().add("card-label-key");
        Label descValue = new Label(event.getDescription().isEmpty() ? "N/A" : event.getDescription());
        descValue.setPadding(new Insets(5));
        descValue.setOnMouseClicked(mouseEvent -> {
            List<Node> originalContent = new ArrayList<>(cardContent.getChildren());
            VBox descriptionVbox = plannerService.showDescriptionDialog(event.getDescription());
            Button backButton = new Button("Back");
            backButton.getStyleClass().add("button");
            backButton.setPrefWidth(120);
            backButton.setOnAction(e -> {
                cardContent.getChildren().clear();
                cardContent.getChildren().addAll(originalContent);
            });
            descriptionVbox.getChildren().add(backButton);
            cardContent.getChildren().clear();
            cardContent.getChildren().add(descriptionVbox);
        });
        // Apply hover effect with rounded edges
        descValue.setOnMouseEntered(mouseEvent -> {
            descValue.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 18;"); // Rounded background
        });
        descValue.setOnMouseExited(mouseEvent -> {
            descValue.setStyle(""); // Reset to default
        });
        descValue.getStyleClass().add("card-label-value");
        descValue.setWrapText(true);
        descValue.setMaxWidth(200);

        grid.add(classKey, 0, 0);
        grid.add(classValue, 1, 0);
        grid.add(dateKey, 0, 1);
        grid.add(dateValue, 1, 1);
        grid.add(timeKey, 0, 2);
        grid.add(timeValue, 1, 2);
        grid.add(descKey, 0, 3);
        grid.add(descValue, 1, 3);

        // Create buttons
        Button modifyButton = new Button("Modify");
        modifyButton.getStyleClass().add("card-button-modify");
        modifyButton.setPrefWidth(80);

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("card-button-delete");
        deleteButton.setPrefWidth(80);

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("card-button-close");
        backButton.setPrefWidth(80);

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        if (event.getDateTime().isAfter(LocalDateTime.now())) {
            buttonBox.getChildren().addAll(backButton, modifyButton, deleteButton);
        } else {
            buttonBox.getChildren().addAll(backButton, deleteButton);
        }

        cardContent.getChildren().addAll(titleLabel, separator, grid, buttonBox);

        // Create and add cardWithBackdrop
        StackPane cardWithBackdrop = new StackPane();
        cardWithBackdrop.getChildren().add(cardContent);
        StackPane.setAlignment(cardWithBackdrop, Pos.CENTER); // Center the card
        StackPane.setMargin(cardWithBackdrop, new Insets(10, 0, 10, 0));
        root.getChildren().add(cardWithBackdrop);

        // Set button actions
        modifyButton.setOnAction(e -> {
            root.getChildren().remove(cardWithBackdrop);
            showModifyEventView(event);
        });

        deleteButton.setOnAction(e -> {
            VBox confirmationCard = new VBox(10);
            confirmationCard.getStyleClass().add("event-card");
            confirmationCard.setPadding(new Insets(15));
            confirmationCard.setMaxWidth(320);
            confirmationCard.setMaxHeight(200);
            confirmationCard.setAlignment(Pos.CENTER);

            Label cardTitle = new Label("Delete Event");
            cardTitle.getStyleClass().add("card-title");
            cardTitle.setStyle("-fx-font-size: 20;");

            Separator separator2 = new Separator();
            separator2.getStyleClass().add("card-separator");
            separator2.setPrefWidth(280);

            Label message = new Label("Are you sure you want to delete this event? " + event.getEventName() + " (" + event.getClassName() + ")");
            message.setWrapText(true);
            message.setMaxWidth(280);
            message.setAlignment(Pos.CENTER);
            message.getStyleClass().add("card-label-value");

            Button confirmButton = new Button("Confirm");
            confirmButton.getStyleClass().add("card-button-delete");
            confirmButton.setPrefWidth(100);
            confirmButton.setOnAction(evt -> {
                plannerService.deleteEvent(event.getEventName(), event.getClassName());
                updateUpcomingEvents();
                root.getChildren().remove(confirmationCard);
                root.getChildren().remove(cardWithBackdrop);
                showMainView();
            });

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("card-button-close");
            cancelButton.setPrefWidth(100);
            cancelButton.setOnAction(evt -> root.getChildren().remove(confirmationCard));

            HBox confirmButtonBox = new HBox(8, cancelButton, confirmButton);
            confirmButtonBox.setAlignment(Pos.CENTER);

            confirmationCard.getChildren().addAll(cardTitle, separator2, message, confirmButtonBox);
            StackPane.setAlignment(confirmationCard, Pos.CENTER);
            StackPane.setMargin(confirmationCard, new Insets(10, 0, 10, 0));

            root.getChildren().add(confirmationCard);

            confirmationCard.setScaleX(0.8);
            confirmationCard.setScaleY(0.8);
            confirmationCard.setOpacity(0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(confirmationCard.scaleXProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.scaleYProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(confirmationCard.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();
        });

        backButton.setOnAction(e -> {
            root.getChildren().remove(cardWithBackdrop);
        });

        // Add key event handlers for Enter and Escape
        cardWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                // No specific OK action here, as event details view has no OK button
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire(); // Trigger the Back button action
                keyEvent.consume();
            }
        });
        cardWithBackdrop.setFocusTraversable(true);

        // Animate the card with backdrop (initial entry only)
        cardWithBackdrop.setScaleX(0.8);
        cardWithBackdrop.setScaleY(0.8);
        cardWithBackdrop.setOpacity(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(150),
                        new KeyValue(cardWithBackdrop.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(cardWithBackdrop.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(cardWithBackdrop.opacityProperty(), 1, Interpolator.EASE_OUT)
                )
        );
        timeline.play();
    }

    private void showModifyEventView(TimeSlot event) {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        // Create and style the title label
        Label titleLabel = new Label("Modify Event");
        titleLabel.getStyleClass().add("dialog-label");
        titleLabel.setStyle("-fx-font-size: 40");

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField classField = new TextField(event.getClassName());
        classField.setPrefWidth(200);
        TextField nameField = new TextField(event.getEventName());
        nameField.setPrefWidth(200);
        TextField monthField = new TextField(String.valueOf(event.getDateTime().getMonthValue()));
        monthField.setPromptText("MM");
        monthField.setPrefWidth(50);
        TextField dayField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("dd")));
        dayField.setPromptText("DD");
        dayField.setPrefWidth(50);
        TextField yearField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("yyyy")));
        yearField.setPromptText("YYYY");
        yearField.setPrefWidth(70);
        HBox dateBox = new HBox(5, monthField, new Label("/"), dayField, new Label("/"), yearField);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        TextField hourField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("hh")));
        hourField.setPromptText("HH");
        hourField.setPrefWidth(50);
        TextField minuteField = new TextField(event.getDateTime().format(DateTimeFormatter.ofPattern("mm")));
        minuteField.setPromptText("MM");
        minuteField.setPrefWidth(50);
        ComboBox<String> amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setValue(event.getDateTime().getHour() < 12 ? "AM" : "PM");
        amPmDropdown.setPrefWidth(70);
        HBox timeBox = new HBox(5, hourField, new Label(":"), minuteField, amPmDropdown);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        TextArea descField = new TextArea(event.getDescription());
        descField.setWrapText(true);
        descField.setPrefRowCount(3);
        descField.setPrefWidth(200);

        grid.add(new Label("Class:"), 0, 0);
        grid.add(classField, 1, 0);
        grid.add(new Label("Event Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(dateBox, 1, 2);
        grid.add(new Label("Time:"), 0, 3);
        grid.add(timeBox, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descField, 1, 4);

        Button saveButton = new Button("Save");
        saveButton.getStyleClass().add("button");
        saveButton.setPrefWidth(120);
        saveButton.setOnAction(e -> {
            try {
                String className = classField.getText().trim();
                String eventName = nameField.getText().trim();
                String description = descField.getText().trim();

                if (className.isEmpty() || eventName.isEmpty()) {
                    showAlert("Invalid Input", "Class and Event Name cannot be empty.");
                    return;
                }

                int month = Integer.parseInt(monthField.getText().trim());
                int day = Integer.parseInt(dayField.getText().trim());
                int year = yearField.getText().trim().isEmpty() ? LocalDateTime.now().getYear() : Integer.parseInt(yearField.getText().trim());
                int hour = hourField.getText().trim().isEmpty() ? 12 : Integer.parseInt(hourField.getText().trim());
                int minute = minuteField.getText().trim().isEmpty() ? 0 : Integer.parseInt(minuteField.getText().trim());
                String amPmValue = amPmDropdown.getValue() != null ? amPmDropdown.getValue() : "AM";

                if ("PM".equals(amPmValue) && hour < 12) hour += 12;
                if ("AM".equals(amPmValue) && hour == 12) hour = 0;

                LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);
                TimeSlot updatedEvent = new TimeSlot(className, eventName, dateTime, description);
                plannerService.updateEvent(event, updatedEvent);
                showMainView();
            } catch (Exception ex) {
                showAlert("Invalid Input", "Ensure all date/time fields contain valid numbers.");
            }
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(120);
        backButton.setOnAction(e -> {
            root.getChildren().remove(content); // Remove the modify view
            if (previousView != null && !root.getChildren().contains(previousView)) {
                root.getChildren().add(previousView); // Restore the previous view only if not already present
            } else {
                showMainView(); // Fallback to main view if previous view is null or already added
            }
        });

        HBox buttonBox = new HBox(15, backButton, saveButton);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(grid, buttonBox);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxWidth(400);
        StackPane.setMargin(scrollPane, new Insets(20));

        // Create a new VBox to hold the title and the content
        VBox mainLayout = new VBox(10); // 10 pixels spacing between title and content
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(titleLabel, scrollPane);

        // Create a new Region to act as the additional translucent box behind the content
        Region translucentBox = new Region();
        // Use the same distinct style as in the other views
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.5);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        // Set a fixed size to wrap around the title, form fields, and buttons with padding
        translucentBox.setPrefWidth(450); // Match the width from showEventsByClassView
        translucentBox.setPrefHeight(500); // Match the height from showClassSelectionView and showPastEventsView
        translucentBox.setMaxWidth(450);
        translucentBox.setMaxHeight(500);
        translucentBox.setMinWidth(450);
        translucentBox.setMinHeight(500);

        // Create a StackPane to layer the translucent box behind the mainLayout
        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        // Add the contentWithBackdrop to the root StackPane
        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER); // Center the content in the window
        previousView = contentWithBackdrop; // Store the contentWithBackdrop as the previous view

        // Add key event handlers for Enter and Escape
        contentWithBackdrop.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                saveButton.fire(); // Trigger the Save button action
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                backButton.fire(); // Trigger the Back button action
                keyEvent.consume();
            }
        });
        contentWithBackdrop.setFocusTraversable(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
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

    private void showAlert(String title, String content) {
        // Create a custom card for the error message
        VBox errorCard = new VBox(10);
        errorCard.getStyleClass().add("event-card");
        errorCard.setPadding(new Insets(15));
        errorCard.setMaxWidth(320);
        errorCard.setMaxHeight(200);
        errorCard.setAlignment(Pos.CENTER);

        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-title");
        cardTitle.setStyle("-fx-font-size: 20px;");

        Separator separator = new Separator();
        separator.getStyleClass().add("card-separator");
        separator.setPrefWidth(280);

        Label message = new Label(content);
        message.setWrapText(true);
        message.setMaxWidth(280);
        message.setAlignment(Pos.CENTER);
        message.getStyleClass().add("card-label-value");

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("card-button-close");
        okButton.setPrefWidth(100);
        okButton.setOnAction(evt -> root.getChildren().remove(errorCard)); // Remove the card on OK

        HBox buttonBox = new HBox(8, okButton);
        buttonBox.setAlignment(Pos.CENTER);

        errorCard.getChildren().addAll(cardTitle, separator, message, buttonBox);
        StackPane.setAlignment(errorCard, Pos.CENTER);
        StackPane.setMargin(errorCard, new Insets(10, 0, 10, 0));

        root.getChildren().add(errorCard);

        // Animation for card entrance
        errorCard.setScaleX(0.8);
        errorCard.setScaleY(0.8);
        errorCard.setOpacity(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(150),
                        new KeyValue(errorCard.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(errorCard.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(errorCard.opacityProperty(), 1, Interpolator.EASE_OUT)
                )
        );
        timeline.play();

        // Ensure the error card receives focus
        Platform.runLater(() -> errorCard.requestFocus());

        // Add key event handlers for the error card
        errorCard.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER || keyEvent.getCode() == KeyCode.ESCAPE) {
                okButton.fire(); // Trigger the OK button action
                keyEvent.consume();
            }
        });
        errorCard.setFocusTraversable(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}