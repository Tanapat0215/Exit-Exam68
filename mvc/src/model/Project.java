package model;

import java.time.LocalDate;

public class Project {
    public final String projectId;     // 8 digits, first not 0
    public final String name;
    public final String category;
    public final long target;          // > 0
    public final LocalDate deadline;   // must be future
    public long raised;                // start at 0

    public Project(String projectId, String name, String category, long target, LocalDate deadline, long raised) {
        this.projectId = projectId;
        this.name = name;
        this.category = category;
        this.target = target;
        this.deadline = deadline;
        this.raised = raised;
    }

    public long remaining() {
        return Math.max(0, target - raised);
    }

    public double progress() {
        if (target <= 0) return 0.0;
        return Math.min(1.0, (double)raised / (double)target);
    }
}
