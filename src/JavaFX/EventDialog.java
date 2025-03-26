package JavaFX;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class EventDialog {
    private ComboBox<String> classDropdown;
    private TextField newClassField, eventNameField, monthField, dayField, yearField, hourField, minuteField;
    private ComboBox<String> amPmDropdown;
    private TextArea descField;
    public StackPane root; // Public for external access
    public Consumer<TimeSlot> resultHandler;

    public EventDialog(List<String> classList) {
        this(classList, null);
    }

    public EventDialog(List<String> classList, String preselectedClass) {
        root = new StackPane();

        Label titleLabel = new Label("Add New Event");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        titleLabel.getStyleClass().add("dialog-label");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(15));
        formGrid.setAlignment(Pos.CENTER);

        classDropdown = new ComboBox<>();
        classDropdown.getItems().add("Add New Class...");
        classDropdown.getItems().addAll(classList);
        classDropdown.setPromptText("Select or Add Class");
        classDropdown.setPrefWidth(200);

        newClassField = new TextField();
        newClassField.setPromptText("New Class Name");
        newClassField.setPrefWidth(200);
        newClassField.setDisable(true);

        classDropdown.setOnAction(e -> {
            String selected = classDropdown.getValue();
            if ("Add New Class...".equals(selected) || selected == null) {
                newClassField.setDisable(false);
                newClassField.requestFocus();
            } else {
                newClassField.setDisable(true);
                newClassField.clear();
            }
        });

        if (preselectedClass != null) {
            classDropdown.setValue(preselectedClass);
            classDropdown.setDisable(true);
            newClassField.setDisable(true);
        }

        eventNameField = new TextField();
        eventNameField.setPromptText("Event Name");
        eventNameField.setPrefWidth(200);

        monthField = new TextField();
        monthField.setPromptText("MM");
        monthField.setPrefWidth(50);
        dayField = new TextField();
        dayField.setPromptText("DD");
        dayField.setPrefWidth(50);
        yearField = new TextField();
        yearField.setPromptText("YYYY");
        yearField.setPrefWidth(70);
        HBox dateBox = new HBox(5, monthField, new Label("/"), dayField, new Label("/"), yearField);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        hourField = new TextField();
        hourField.setPromptText("HH");
        hourField.setPrefWidth(50);
        minuteField = new TextField();
        minuteField.setPromptText("MM");
        minuteField.setPrefWidth(50);
        amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setValue("AM");
        amPmDropdown.setPrefWidth(70);
        HBox timeBox = new HBox(5, hourField, new Label(":"), minuteField, amPmDropdown);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        descField = new TextArea();
        descField.setPromptText("Description (Optional)");
        descField.setWrapText(true);
        descField.setPrefRowCount(3);
        descField.setPrefWidth(200);

        int row = 0;
        formGrid.add(new Label("Class:"), 0, row);
        formGrid.add(classDropdown, 1, row++);
        formGrid.add(new Label("New Class:"), 0, row);
        formGrid.add(newClassField, 1, row++);
        formGrid.add(new Label("Event Name:"), 0, row);
        formGrid.add(eventNameField, 1, row++);
        formGrid.add(new Label("Date:"), 0, row);
        formGrid.add(dateBox, 1, row++);
        formGrid.add(new Label("Time:"), 0, row);
        formGrid.add(timeBox, 1, row++);
        formGrid.add(new Label("Description:"), 0, row);
        formGrid.add(descField, 1, row++);

        Button okButton = new Button("OK");
        okButton.setPrefWidth(120);

        okButton.setOnAction(e -> {
            if (validateFields()) {
                TimeSlot event = createTimeSlot();
                if (resultHandler != null) {
                    resultHandler.accept(event);
                }
            }
        });


        HBox buttonBox = new HBox(15, okButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox content = new VBox(15, titleLabel, formGrid, buttonBox);
        content.setPadding(new Insets(20));
        content.setMaxWidth(400);
        content.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setMaxWidth(400);
        StackPane.setMargin(scrollPane, new Insets(20));

        root.getChildren().add(scrollPane);
    }

    private boolean validateFields() {
        // Same as original
        try {
            String selectedClass = classDropdown.getValue();
            String newClass = newClassField.getText().trim();
            String eventName = eventNameField.getText().trim();

            if ((selectedClass == null || selectedClass.isEmpty() || "Add New Class...".equals(selectedClass)) && newClass.isEmpty()) {
                showAlert("Invalid Input", "Please select or enter a class name.");
                return false;
            }

            if (eventName.isEmpty()) {
                showAlert("Invalid Input", "Event Name cannot be empty.");
                return false;
            }

            int month = Integer.parseInt(monthField.getText().trim());
            int day = Integer.parseInt(dayField.getText().trim());
            int year = Integer.parseInt(yearField.getText().trim().isEmpty()
                    ? String.valueOf(LocalDateTime.now().getYear())
                    : yearField.getText().trim());
            int hour = hourField.getText().trim().isEmpty()
                    ? 12 : Integer.parseInt(hourField.getText().trim());
            int minute = minuteField.getText().trim().isEmpty()
                    ? 0 : Integer.parseInt(minuteField.getText().trim());

            String amPm = amPmDropdown.getValue();
            if (amPm == null || amPm.trim().isEmpty()) amPm = "AM";
            if ("PM".equals(amPm) && hour < 12) hour += 12;
            if ("AM".equals(amPm) && hour == 12) hour = 0;

            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

            return true;
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Ensure numeric fields contain valid numbers.");
            return false;
        } catch (DateTimeException e) {
            showAlert("Invalid Date", "The date is invalid (e.g., February 30 does not exist).");
            return false;
        }
    }

    private TimeSlot createTimeSlot() {
        // Same as original
        try {
            String finalClass = newClassField.getText().isEmpty()
                    ? classDropdown.getValue()
                    : newClassField.getText().trim();

            String eventName = eventNameField.getText().trim();
            int month = Integer.parseInt(monthField.getText().trim());
            int day = Integer.parseInt(dayField.getText().trim());
            int year = Integer.parseInt(yearField.getText().trim().isEmpty()
                    ? String.valueOf(LocalDateTime.now().getYear())
                    : yearField.getText().trim());
            int hour = hourField.getText().trim().isEmpty()
                    ? 12 : Integer.parseInt(hourField.getText().trim());
            int minute = minuteField.getText().trim().isEmpty()
                    ? 0 : Integer.parseInt(minuteField.getText().trim());

            String amPm = amPmDropdown.getValue();
            if (amPm == null || amPm.isEmpty()) amPm = "AM";
            if ("PM".equals(amPm) && hour < 12) hour += 12;
            if ("AM".equals(amPm) && hour == 12) hour = 0;

            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

            if (!newClassField.getText().trim().isEmpty()) {
                PlannerService plannerService = new PlannerService(); // No stage needed
                if (!plannerService.classExists(finalClass)) {
                    plannerService.addNewClass(finalClass);
                }
            }

            return new TimeSlot(finalClass, eventName, dateTime, descField.getText().trim());
        } catch (Exception e) {
            showAlert("Invalid Input", "Failed to create event: " + e.getMessage());
            return null;
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
}