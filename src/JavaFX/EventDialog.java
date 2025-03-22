package JavaFX;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.List;

public class EventDialog extends Dialog<TimeSlot> {
    private ComboBox<String> classDropdown;
    private TextField newClassField, eventNameField, monthField, dayField, yearField, hourField, minuteField;
    private ComboBox<String> amPmDropdown;
    private TextArea descField;

    public EventDialog(List<String> classList) {
        this(classList, null);
    }

    public EventDialog(List<String> classList, String preselectedClass) {
        setTitle("Add Event");

        classDropdown = new ComboBox<>();
        classDropdown.getItems().add("Add New Class...");
        classDropdown.getItems().addAll(classList);
        classDropdown.setPromptText("Select Existing Class or Add New");

        newClassField = new TextField();
        newClassField.setPromptText("Enter New Class Name");

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

        monthField = new TextField(); monthField.setPromptText("MM");
        dayField = new TextField(); dayField.setPromptText("DD");
        yearField = new TextField(); yearField.setPromptText("YYYY");

        hourField = new TextField(); hourField.setPromptText("HH");
        minuteField = new TextField(); minuteField.setPromptText("MM");

        amPmDropdown = new ComboBox<>();
        amPmDropdown.getItems().addAll("AM", "PM");
        amPmDropdown.setPromptText("AM/PM");

        descField = new TextArea();
        descField.setPromptText("Event Description (Optional)");
        descField.setWrapText(true);
        descField.setPrefRowCount(3);

        VBox formLayout = new VBox(10);
        formLayout.getChildren().addAll(
                new Label("Select Class or Add New:"), classDropdown, newClassField,
                new Label("Event Name:"), eventNameField,
                new Label("Date:"), monthField, dayField, yearField,
                new Label("Time:"), hourField, minuteField, amPmDropdown,
                new Label("Description:"), descField
        );
        double fieldWidth = 300;
        classDropdown.setPrefWidth(fieldWidth);
        eventNameField.setMaxWidth(fieldWidth);
        monthField.setMaxWidth(fieldWidth);
        dayField.setMaxWidth(fieldWidth);
        yearField.setMaxWidth(fieldWidth);
        hourField.setMaxWidth(fieldWidth);
        minuteField.setMaxWidth(fieldWidth);
        amPmDropdown.setMaxWidth(fieldWidth);
        descField.setMaxWidth(fieldWidth);
        formLayout.setPadding(new Insets(10));

        Region translucentBox = new Region();
        translucentBox.setId("backgroundBox");
        translucentBox.setMouseTransparent(true);
        translucentBox.prefWidthProperty().bind(formLayout.layoutBoundsProperty().map(b -> b.getWidth()));
        translucentBox.prefHeightProperty().bind(formLayout.layoutBoundsProperty().map(b -> b.getHeight()));

        ScrollPane scrollPane = new ScrollPane(formLayout);
        scrollPane.setPrefSize(400, 500);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane stackPane = new StackPane(translucentBox, scrollPane);
        stackPane.setPadding(new Insets(20));
        getDialogPane().setContent(stackPane);
        getDialogPane().setPrefSize(400, 500);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateFields()) {
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return createTimeSlot();
            }
            return null;
        });

        applyStyles();
    }

    private void applyStyles() {
        Scene scene = getDialogPane().getScene();
        if (scene != null) {
            String cssFile = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(cssFile);
            PlannerApp.getInstance().setBackground((Region) scene.getRoot());
        }
    }

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
            if ("PM".equals(amPm) && hour < 12) hour += 12;
            if ("AM".equals(amPm) && hour == 12) hour = 0;

            LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);

            if (!newClassField.getText().trim().isEmpty()) {
                PlannerService plannerService = new PlannerService();
                if (!plannerService.classExists(finalClass)) {
                    plannerService.addNewClass(finalClass);
                }
            }

            return new TimeSlot(finalClass, eventName, dateTime, descField.getText().trim());

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Ensure numeric fields contain valid numbers.");
            return null;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        PlannerApp.getInstance().setBackground(dialogPane);
        dialogPane.getStyleClass().add("custom-alert");
        String cssFile = getClass().getResource("styles.css").toExternalForm();
        dialogPane.getStylesheets().add(cssFile);

        alert.showAndWait();
    }
}
