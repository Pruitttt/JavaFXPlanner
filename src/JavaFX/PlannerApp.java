package JavaFX;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
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
        // Set up the background once at startup and persist it across views
        initializeBackground();

        showMainView();

        Scene scene = new Scene(root, 600, 700);
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
                "/JavaFX/pexels-eberhardgross-443446.jpg",
                "/JavaFX/pexels-eberhardgross-568236.jpg",
                "/JavaFX/pexels-eberhardgross-1670187.jpg",
                "/JavaFX/pexels-katja-79053-592077.jpg",
                "/JavaFX/pexels-mattdvphotography-3082313.jpg",
                "/JavaFX/pexels-pixabay-33109.jpg",
                "/JavaFX/pexels-rickyrecap-1586298.jpg",
                "/JavaFX/pexels-todd-trapani-488382-2754200.jpg",
                "/JavaFX/pexels-tomverdoot-3181458.jpg",
                "/JavaFX/pexels-tracehudson-2365457.jpg"
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
        GaussianBlur blur = new GaussianBlur(15); // Adjust blur radius as needed
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
        backgroundTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(120), e -> {
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
        uiLayout.getStyleClass().add("glass-panel"); // Use CSS for transparency

        Label title = new Label("Upcoming Events");
        title.setStyle("-fx-font-size: 25px;");
        title.getStyleClass().add("dialog-label");

        upcomingEventsList = new ListView<>();
        upcomingEventsList.setPrefHeight(500);
        plannerService.addUpdateListener(() -> Platform.runLater(this::updateUpcomingEvents));
        plannerService.movePastEventsToStorage();
        updateUpcomingEvents();

        StackPane listPane = new StackPane(upcomingEventsList);
        listPane.getStyleClass().add("glass-panel");

        Button addEventBtn = new Button("Add Event");
        addEventBtn.getStyleClass().add("button");
        addEventBtn.setPrefWidth(200);
        addEventBtn.setOnAction(e -> showAddEventView(null));

        Button viewByClassBtn = new Button("Class View");
        viewByClassBtn.getStyleClass().add("button");
        viewByClassBtn.setPrefWidth(200);
        viewByClassBtn.setOnAction(e -> showClassSelectionView());

        Button recentEventsBtn = new Button("Past Events");
        recentEventsBtn.getStyleClass().add("button");
        recentEventsBtn.setPrefWidth(200);
        recentEventsBtn.setOnAction(e -> showPastEventsView());

        HBox buttonRow = new HBox(15, addEventBtn, viewByClassBtn, recentEventsBtn);
        buttonRow.setAlignment(Pos.CENTER);

        uiLayout.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        uiLayout.setCenter(listPane);
        uiLayout.setBottom(buttonRow);
        BorderPane.setMargin(buttonRow, new Insets(15, 0, 0, 0));

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
                        showEventDetailsView(eventDetails);
                    }
                }
            }
        });

        root.getChildren().add(uiLayout);
        previousView = uiLayout; // Store the main view as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), uiLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showAddEventView(String preselectedClass) {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        EventDialog dialog = new EventDialog(plannerService.loadClasses(), preselectedClass);
        dialog.root.getStyleClass().add("glass-panel"); // Apply glass-panel to the dialog's root StackPane
        // Ensure the dialog's content is on top
        ScrollPane scrollPane = (ScrollPane) dialog.root.getChildren().get(0);
        scrollPane.setStyle("-fx-background-color: rgba(150, 150, 150, 0.5)");
        VBox content = (VBox) scrollPane.getContent();
        content.getStyleClass().add("glass-panel"); // Ensure the content VBox has glass-panel style

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(120);
        backButton.setOnAction(e -> showMainView());

        HBox buttonBox = (HBox) content.getChildren().get(2);
        buttonBox.getChildren().add(0, backButton);
        buttonBox.setSpacing(15);

        dialog.resultHandler = event -> {
            if (event != null) {
                plannerService.saveEvent(event);
                plannerService.movePastEventsToStorage();
                showMainView();
            }
        };

        root.getChildren().add(dialog.root);
        previousView = dialog.root; // Store the dialog as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), dialog.root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showPastEventsView() {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(450);

        Label title = new Label("Past Events");
        title.getStyleClass().add("dialog-label");

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
                    new KeyFrame(Duration.millis(300),
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

        content.getChildren().addAll(title, pastEventsList, buttonBox);
        root.getChildren().add(content);
        previousView = content; // Store the past events view as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showClassSelectionView() {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);

        Label title = new Label("Select Class");
        title.getStyleClass().add("dialog-label");

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
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Class");
            dialog.setHeaderText("Enter New Class Name:");
            dialog.setContentText("Class:");
            dialog.showAndWait().ifPresent(className -> {
                plannerService.addNewClass(className);
                classListView.getItems().setAll(plannerService.loadClasses());
            });
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> showMainView());

        HBox buttonBox = new HBox(15, backButton, addClassBtn);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(title, classListView, buttonBox);
        root.getChildren().add(content);
        root.setAlignment(Pos.CENTER);
        previousView = content; // Store the class selection view as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showEventsByClassView(String className) {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(500);

        Label title = new Label("Events for " + className);
        title.getStyleClass().add("dialog-label");

        ListView<TimeSlot> eventList = new ListView<>();
        eventList.getItems().addAll(plannerService.loadEventsForClass(className));
        eventList.setPrefHeight(300);
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
        addEventBtn.setPrefWidth(200);
        addEventBtn.setOnAction(e -> showAddEventView(className));

        Button deleteClassBtn = new Button("Delete Class");
        deleteClassBtn.getStyleClass().add("button");
        deleteClassBtn.setPrefWidth(200);
        deleteClassBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Are you sure you want to delete this class?");
            alert.setContentText("All events associated with this class will also be deleted.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    plannerService.deleteClass(className);
                    showClassSelectionView();
                }
            });
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> showClassSelectionView());

        HBox buttonBox = new HBox(15, backButton, addEventBtn, deleteClassBtn);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(title, eventList, buttonBox);
        root.getChildren().add(content);
        previousView = content; // Store the events by class view as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showEventDetailsView(TimeSlot event) {
        // Do not remove the previous view; overlay the EventDetails card on top
        // Create the EventDetails card content
        VBox cardContent = new VBox(10);
        cardContent.getStyleClass().add("event-card");
        cardContent.setPadding(new Insets(15));
        cardContent.setMaxWidth(320);
        cardContent.setMaxHeight(200);
        cardContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Event Details");
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

        Label dateKey = new Label("Date:");
        dateKey.getStyleClass().add("card-label-key");
        Label dateValue = new Label(event.getDateTimeFormatted());
        dateValue.getStyleClass().add("card-label-value");

        Label descKey = new Label("Description:");
        descKey.getStyleClass().add("card-label-key");
        Label descValue = new Label(event.getDescription().isEmpty() ? "N/A" : event.getDescription());
        descValue.getStyleClass().add("card-label-value");
        descValue.setWrapText(true);
        descValue.setMaxWidth(200);

        grid.add(classKey, 0, 0);
        grid.add(classValue, 1, 0);
        grid.add(dateKey, 0, 1);
        grid.add(dateValue, 1, 1);
        grid.add(descKey, 0, 2);
        grid.add(descValue, 1, 2);

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

        // Create a translucent box as the background of the card
        Region translucentBox = new Region();
        translucentBox.setStyle(".event-card");
        translucentBox.setMaxWidth(360);
        translucentBox.setMaxHeight(240);

        // Create a StackPane to hold the translucent box and the card content
        StackPane cardWithBackdrop = new StackPane();
        cardWithBackdrop.getChildren().addAll(translucentBox, cardContent);
        StackPane.setAlignment(cardWithBackdrop, Pos.CENTER);
        StackPane.setMargin(cardWithBackdrop, new Insets(10, 0, 10, 0));

        // Add the card with backdrop to the root StackPane
        root.getChildren().add(cardWithBackdrop);

        // Set up button actions to remove the entire cardWithBackdrop
        modifyButton.setOnAction(e -> {
            root.getChildren().remove(cardWithBackdrop); // Remove the card with backdrop
            showModifyEventView(event);
        });

        deleteButton.setOnAction(e -> {
            if (confirmAndDeleteEvent(event)) {
                root.getChildren().remove(cardWithBackdrop); // Remove the card with backdrop
                showMainView();
            }
        });

        backButton.setOnAction(e -> {
            root.getChildren().remove(cardWithBackdrop); // Remove the card with backdrop
            // No need to re-add previousView since it was never removed
        });

        // Animate the card with backdrop
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

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);

        Label titleLabel = new Label("Modify Event");
        titleLabel.getStyleClass().add("dialog-label");

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

        content.getChildren().addAll(titleLabel, grid, buttonBox);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxWidth(400);
        StackPane.setMargin(scrollPane, new Insets(20));

        root.getChildren().add(scrollPane);
        previousView = scrollPane; // Store the modify view as the previous view

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), scrollPane);
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

    private boolean confirmAndDeleteEvent(TimeSlot event) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Event");
        confirmDialog.setHeaderText("Are you sure you want to delete this event?");
        confirmDialog.setContentText(event.getEventName() + " (" + event.getClassName() + ")");
        return confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).map(response -> {
            plannerService.deleteEvent(event.getEventName(), event.getClassName());
            updateUpcomingEvents();
            return true;
        }).orElse(false);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        StackPane newRoot = new StackPane();
        newRoot.getChildren().add(dialogPane);

        // Ensure the alert dialog also preserves the background
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        newRoot.getChildren().removeIf(node -> !childrenToKeep.contains(node));
        newRoot.getChildren().addAll( backLayer, frontLayer);

        dialogPane.getStyleClass().add("custom-alert");
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        dialogPane.getStylesheets().add(cssFile);

        Scene scene = new Scene(newRoot);
        scene.setFill(Color.TRANSPARENT);
        alert.setOnShown(event -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setScene(scene);
        });
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}