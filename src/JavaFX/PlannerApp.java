package JavaFX;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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
        // Set fixed preferred width and height for the ListView
        upcomingEventsList.setPrefWidth(400);
        upcomingEventsList.setPrefHeight(500);
        // Set maximum width and height to prevent resizing
        upcomingEventsList.setMaxWidth(400);
        upcomingEventsList.setMaxHeight(500);
        // Set minimum width and height to prevent shrinking
        upcomingEventsList.setMinWidth(400);
        upcomingEventsList.setMinHeight(500);
        plannerService.addUpdateListener(() -> Platform.runLater(this::updateUpcomingEvents));
        plannerService.movePastEventsToStorage();
        updateUpcomingEvents();

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

        // Create a VBox to hold the ListView and buttons together
        VBox listWithButtons = new VBox(15, upcomingEventsList, buttonRow);
        listWithButtons.setAlignment(Pos.CENTER);

        StackPane listPane = new StackPane(listWithButtons);
        listPane.getStyleClass().add("glass-panel"); // Apply glass-panel style to the entire container
        listPane.setMaxWidth(400);
        listPane.setMaxHeight(560); // Height for ListView (500) + buttons (35) + spacing (15) + padding

        uiLayout.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        // Add a top margin to move the title down
        BorderPane.setMargin(title, new Insets(40, 0, 0, 0)); // 20 pixels top margin
        uiLayout.setCenter(listPane);
        BorderPane.setAlignment(listPane, Pos.CENTER);

        // Create a new Region to act as the additional translucent box behind the content
        Region translucentBox = new Region();
        // Use a distinct style to differentiate it from the ListView's background
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        // Size to wrap around the title, ListView, and buttons with padding
        translucentBox.setPrefWidth(500);
        translucentBox.setPrefHeight(700);
        translucentBox.setMaxWidth(500);
        translucentBox.setMaxHeight(700);
        translucentBox.setMinWidth(500);
        translucentBox.setMinHeight(700);

        // Create a StackPane to layer the translucent box behind the uiLayout
        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, uiLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        // Add the contentWithBackdrop to the root StackPane
        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER); // Center the content in the window
        previousView = uiLayout; // Store the uiLayout as the previous view

        // Ensure the ListView's onMouseClicked handler works
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

        EventDialog dialog = new EventDialog(plannerService.loadClasses(), preselectedClass);
        dialog.root.getStyleClass().add("glass-panel");

        ScrollPane scrollPane = (ScrollPane) dialog.root.getChildren().get(0);
        scrollPane.setStyle("-fx-background-color: rgba(150, 150, 150, 0.5)");
        VBox content = (VBox) scrollPane.getContent();
        content.getStyleClass().add("glass-panel");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(120);
        backButton.setOnAction(e -> {
            if (preselectedClass != null) {
                showEventsByClassView(preselectedClass);
            } else {
                showMainView();
            }
        });

        HBox buttonBox = (HBox) content.getChildren().get(1);
        buttonBox.getChildren().add(0, backButton);
        buttonBox.setSpacing(15);

        Button okButton = (Button) buttonBox.getChildren().get(1);

        dialog.resultHandler = event -> {
            if (event != null) {
                plannerService.saveEvent(event);
                plannerService.movePastEventsToStorage();
                if (preselectedClass != null) {
                    showEventsByClassView(preselectedClass);
                } else {
                    showMainView();
                }
            }
        };

        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(titleLabel, dialog.root);

        Region translucentBox = new Region();
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.4);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        translucentBox.setPrefWidth(450);
        translucentBox.setPrefHeight(680);
        translucentBox.setMaxWidth(450);
        translucentBox.setMaxHeight(680);
        translucentBox.setMinWidth(450);
        translucentBox.setMinHeight(680);

        StackPane contentWithBackdrop = new StackPane();
        contentWithBackdrop.getChildren().addAll(translucentBox, mainLayout);
        StackPane.setAlignment(contentWithBackdrop, Pos.CENTER);

        root.getChildren().add(contentWithBackdrop);
        root.setAlignment(Pos.CENTER);
        previousView = contentWithBackdrop;

        // Add key event handler to capture Enter key press
        contentWithBackdrop.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                okButton.fire();
                event.consume();
            }
        });

        // Ensure the newClassField is enabled and editable if "Add New Class..." is selected
        if (preselectedClass == null) {
            Platform.runLater(() -> {
                dialog.newClassField.setDisable(false);
                dialog.newClassField.requestFocus();
            });
        }

        // Ensure the window is focused
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

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showEventsByClassView(String className) {
        // Clear the content except for the background layers
        List<Node> childrenToKeep = List.of(backLayer, frontLayer);
        root.getChildren().removeIf(node -> !childrenToKeep.contains(node));

        // Create and style the title label
        Label title = new Label("Events for " + className);
        title.getStyleClass().add("dialog-label");
        title.setStyle("-fx-font-size: 40");

        VBox content = new VBox(15);
        content.getStyleClass().add("glass-panel");
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);

        ListView<TimeSlot> eventList = new ListView<>();
        eventList.getItems().addAll(plannerService.loadEventsForClass(className));
        eventList.setPrefHeight(300);
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
        addEventBtn.setOnAction(e -> showAddEventView(className));

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
                plannerService.deleteClass(className);
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

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("button");
        backButton.setPrefWidth(120);
        backButton.setOnAction(e -> showClassSelectionView());

        HBox buttonBox = new HBox(15, backButton, addEventBtn, deleteClassBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(400);

        content.getChildren().addAll(eventList, buttonBox);

        // Create a new VBox to hold the title and the content
        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(title, content);

        // Create a new Region to act as the additional translucent box behind the content
        Region translucentBox = new Region();
        // Use the same distinct style as in the other views
        translucentBox.setStyle("-fx-background-color: rgba(150, 150, 150, 0.5);" +
                "-fx-background-radius: 30;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 20, 0.3, 0, 5);");
        // Set a fixed size to wrap around the title, ListView, and buttons with padding
        translucentBox.setPrefWidth(450);
        translucentBox.setPrefHeight(500);
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
        root.setAlignment(Pos.CENTER);
        previousView = contentWithBackdrop;

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentWithBackdrop);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showEventDetailsView(TimeSlot event) {
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

        // Create buttons without setting actions yet
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
        cardWithBackdrop.getChildren().addAll(cardContent);
        StackPane.setMargin(cardWithBackdrop, new Insets(10, 0, 10, 0));
        root.getChildren().add(cardWithBackdrop);

        // Now set the button actions after cardWithBackdrop is defined
        modifyButton.setOnAction(e -> {
            root.getChildren().remove(cardWithBackdrop);
            showModifyEventView(event);
        });

        deleteButton.setOnAction(e -> {
            // Create a confirmation card
            VBox confirmationCard = new VBox(10);
            confirmationCard.getStyleClass().add("event-card");
            confirmationCard.setPadding(new Insets(15));
            confirmationCard.setMaxWidth(320);
            confirmationCard.setMaxHeight(200);
            confirmationCard.setAlignment(Pos.CENTER);

            Label cardTitle = new Label("Delete Event");
            cardTitle.getStyleClass().add("card-title");

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
                root.getChildren().remove(cardWithBackdrop); // Now accessible
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

            // Animation for confirmation card
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