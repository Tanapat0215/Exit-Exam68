package model;

import java.time.LocalDateTime;

public class Pledge {
    public final String pledgeId;
    public final String userId;
    public final String projectId;
    public final LocalDateTime datetime;
    public final long amount;
    public final String tierName; // nullable: empty string means no tier
    public final String status;   // "SUCCESS" / "REJECTED"

    public Pledge(String pledgeId, String userId, String projectId,
                  LocalDateTime datetime, long amount, String tierName, String status) {
        this.pledgeId = pledgeId;
        this.userId = userId;
        this.projectId = projectId;
        this.datetime = datetime;
        this.amount = amount;
        this.tierName = tierName == null ? "" : tierName;
        this.status = status;
    }
}
