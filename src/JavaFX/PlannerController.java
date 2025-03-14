package JavaFX;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PlannerController {
    private TableView<TimeSlot> eventTable;
    private ComboBox<String> classDropdown;
    private Button addEventBtn, addClassBtn;
    private PlannerService plannerService;

    public PlannerController() {
        System.out.println("[DEBUGGER] Initializing PlannerController"); // Debugger

        plannerService = new PlannerService();

        eventTable = new TableView<>();
        eventTable.setPlaceholder(new Label("No upcoming events"));

        classDropdown = new ComboBox<>();
        classDropdown.getItems().addAll(plannerService.loadClasses());
        System.out.println("[DEBUGGER] Loaded classes into dropdown: " + classDropdown.getItems()); // Debugger

        addEventBtn = new Button("Add Event");
        addClassBtn = new Button("Add Class");

        addEventBtn.setOnAction(e -> {
            System.out.println("[DEBUGGER] Add Event button clicked"); // Debugger
            plannerService.addEvent();
        });

        addClassBtn.setOnAction(e -> {
            System.out.println("[DEBUGGER] Add Class button clicked"); // Debugger
            plannerService.addClass();
            updateClassDropdown();
        });
    }

    public VBox getMainLayout() {
        System.out.println("[DEBUGGER] Creating main layout"); // Debugger

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(new Label("Upcoming Events:"), eventTable,
                classDropdown, addEventBtn, addClassBtn);

        return layout;
    }

    private void updateClassDropdown() {
        classDropdown.getItems().clear();
        classDropdown.getItems().addAll(plannerService.loadClasses());
        System.out.println("[DEBUGGER] Updated class dropdown: " + classDropdown.getItems()); // Debugger
    }
}
