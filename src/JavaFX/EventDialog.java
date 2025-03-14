package JavaFX;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventDialog extends Dialog<TimeSlot> {
    private ComboBox<String> classDropdown;
    private TextField newClassField;
    private TextField eventNameField, monthField, dayField, yearField, hourField, minuteField;
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

        // Add "Add New Class..." option at the top
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
                newClassField.setDisable(false); // Enable text box if adding a new class or selecting nothing
                newClassField.requestFocus(); // Focus on text box
            } else {
                newClassField.setDisable(true); // Disable text box if an existing class is selected
                newClassField.clear(); // Clear any manually entered text
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
                new Label("Select Class or Add New:"),
                classDropdown, newClassField,
                new Label("Event Name:"), eventNameField,
                new Label("Date:"), monthField, dayField, yearField,
                new Label("Time:"), hourField, minuteField, amPmDropdown,
                new Label("Description:"), descField
        );

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation before submission
        // Retrieve the OK button from the dialog pane
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                // Determine the final class name
                String selectedClass = classDropdown.getValue();
                String newClass = newClassField.getText().trim();
                String eventName = eventNameField.getText().trim();

                // Validate class name and event name
                if ((selectedClass == null || selectedClass.isEmpty()) && newClass.isEmpty()) {
                    showAlert("Invalid Input", "Please select or enter a class name.");
                    event.consume(); // Prevent dialog from closing
                    return;
                }

                if (eventName.isEmpty()) {
                    showAlert("Invalid Input", "Event Name cannot be empty.");
                    event.consume();
                    return;
                }

                // Validate date and time
                int month = Integer.parseInt(monthField.getText());
                int day = Integer.parseInt(dayField.getText());
                int year = Integer.parseInt(yearField.getText());
                int hour = Integer.parseInt(hourField.getText());
                int minute = Integer.parseInt(minuteField.getText());

                if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    showAlert("Invalid Input", "Ensure month, day, hour, and minute are in valid ranges.");
                    event.consume();
                    return;
                }

                // Validate AM/PM selected
                if (amPmDropdown.getValue() == null) {
                    showAlert("Invalid Input", "Please select AM or PM.");
                    event.consume();
                    return;
                }

                // Date and time conversion check
                if ("PM".equals(amPmDropdown.getValue()) && hour < 12) {
                    hour += 12;
                } else if ("AM".equals(amPmDropdown.getValue()) && hour == 12) {
                    hour = 0;
                }

                LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

                // If a new class was entered, save it
                if (!newClass.isEmpty()) {
                    PlannerService plannerService = new PlannerService();
                    plannerService.addNewClass(newClass);
                }

                // Everything is validated; no event.consume(), window will close normally here

            } catch (Exception e) {
                showAlert("Invalid Input", "Ensure all fields contain valid numbers and the date format is correct.");
                event.consume(); // Keep window open
            }
        });

// Keep this to create and return the event only if validation passed
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    String finalClass = newClassField.getText().isEmpty() ? classDropdown.getValue() : newClassField.getText().trim();
                    int month = Integer.parseInt(monthField.getText());
                    int day = Integer.parseInt(dayField.getText());
                    int year = Integer.parseInt(yearField.getText());
                    int hour = Integer.parseInt(hourField.getText());
                    int minute = Integer.parseInt(minuteField.getText());

                    if ("PM".equals(amPmDropdown.getValue()) && hour < 12) {
                        hour += 12;
                    } else if ("AM".equals(amPmDropdown.getValue()) && hour == 12) {
                        hour = 0;
                    }

                    LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

                    // Save new class if needed
                    if (!newClassField.getText().trim().isEmpty()) {
                        PlannerService plannerService = new PlannerService();
                        plannerService.addNewClass(finalClass);
                    }

                    return new TimeSlot(finalClass, eventNameField.getText().trim(), dateTime, descField.getText().trim());

                } catch (Exception ignored) {
                    // This shouldn't happen due to prior validation, but return null if it does
                    return null;
                }
            }
            return null; // Cancel button
        });



    }



    /**  Show Alert */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
