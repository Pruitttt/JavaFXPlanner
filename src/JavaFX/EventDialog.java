package JavaFX;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventDialog extends Dialog<TimeSlot> {
    private ComboBox<String> classDropdown;
    private TextField newClassField, eventNameField, monthField, dayField, yearField, hourField, minuteField;
    private ComboBox<String> amPmDropdown;
    private TextArea descField;

    /** Constructor for Main Menu - Allows new or existing class */
    public EventDialog(List<String> classList) {
        this(classList, null); // Calls the second constructor with null preselectedClass
    }

    /** Constructor for Adding Events Inside a Class (Preselects class) */
    public EventDialog(List<String> classList, String preselectedClass) {
        setTitle("Add Event");

        // Class selection dropdown
        classDropdown = new ComboBox<>();
        classDropdown.getItems().add("Add New Class...");
        classDropdown.getItems().addAll(classList);
        classDropdown.setPromptText("Select Existing Class or Add New");

        // New class name input field (STARTS ENABLED)
        newClassField = new TextField();
        newClassField.setPromptText("Enter New Class Name");

        // Handle class selection logic
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

        // If class is preselected (from inside a class), disable text field
        if (preselectedClass != null) {
            classDropdown.setValue(preselectedClass);
            classDropdown.setDisable(true);
            newClassField.setDisable(true);
        }

        // Event details
        eventNameField = new TextField();
        eventNameField.setPromptText("Event Name");

        monthField = new TextField();
        monthField.setPromptText("MM");

        dayField = new TextField();
        dayField.setPromptText("DD");

        yearField = new TextField();
        yearField.setPromptText("YYYY");

        hourField = new TextField();
        hourField.setPromptText("HH");

        minuteField = new TextField();
        minuteField.setPromptText("MM");

        amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setPromptText("AM/PM");

        descField = new TextArea();
        descField.setPromptText("Event Description (Optional)");
        descField.setWrapText(true);
        descField.setPrefRowCount(3);

        // Layout setup
        VBox layout = new VBox(10);
        layout.getChildren().addAll(
                new Label("Select Class or Add New:"), classDropdown, newClassField,
                new Label("Event Name:"), eventNameField,
                new Label("Date:"), monthField, dayField, yearField,
                new Label("Time:"), hourField, minuteField, amPmDropdown,
                new Label("Description:"), descField
        );
        layout.setPadding(new Insets(5, 10, 5, 10));

        // Apply CSS globally to all EventDialogs
        getDialogPane().setContent(layout);
        getDialogPane().setStyle("-fx-border-color: transparent; -fx-border-width: 0; -fx-background-color: transparent;");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 🔹 Apply styles.css to all EventDialogs
        applyStyles();

        // Validation before submission
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateFields()) {
                event.consume(); // Prevents dialog from closing on invalid input
            }
        });

        // Handle result conversion (ensure a TimeSlot is created only on valid input)
        setResultConverter(button -> button == ButtonType.OK ? createTimeSlot() : null);
    }

    /** 🔹 Method to Apply CSS Styles Globally to Dialogs */
    private void applyStyles() {
        Scene scene = getDialogPane().getScene();
        if (scene != null) {
            String cssFile = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(cssFile);
        }
    }

    /** 🔹 Validates input fields before submitting */
    private boolean validateFields() {
        try {
            String selectedClass = classDropdown.getValue();
            String newClass = newClassField.getText().trim();
            String eventName = eventNameField.getText().trim();

            if ((selectedClass == null || selectedClass.isEmpty()) && newClass.isEmpty()) {
                showAlert("Invalid Input", "Please select or enter a class name.");
                return false;
            }

            if (eventName.isEmpty()) {
                showAlert("Invalid Input", "Event Name cannot be empty.");
                return false;
            }

            // Normalize & Validate Date Fields
            int month = Integer.parseInt(monthField.getText().trim());
            int day = Integer.parseInt(dayField.getText().trim());
            int year = Integer.parseInt(yearField.getText().trim().isEmpty()
                    ? String.valueOf(LocalDateTime.now().getYear())
                    : yearField.getText().trim());

            int hour = hourField.getText().trim().isEmpty()
                    ? 12 : Integer.parseInt(hourField.getText().trim());

            int minute = minuteField.getText().trim().isEmpty()
                    ? 0 : Integer.parseInt(minuteField.getText().trim());

            String amPmValue = amPmDropdown.getValue();
            if (amPmValue == null || amPmValue.trim().isEmpty()) amPmValue = "AM";

            // Validate numeric ranges
            if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                showAlert("Invalid Input", "Ensure month, day, hour, and minute are within valid ranges.");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Ensure numeric fields contain valid numbers.");
            return false;
        }
    }

    /** 🔹 Creates a new TimeSlot from validated inputs */
    private TimeSlot createTimeSlot() {
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

            // Convert to 24-hour format
            if ("PM".equals(amPm) && hour < 12) hour += 12;
            if ("AM".equals(amPm) && hour == 12) hour = 0;

            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

            return new TimeSlot(finalClass, eventName, dateTime, descField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Ensure numeric fields contain valid numbers.");
            return null;
        }
    }

    /** 🔹 Show Alert */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
