package model;

public class RewardTier {
    public final String projectId;
    public final String tierName;
    public final long minAmount;
    public int quota;

    public RewardTier(String projectId, String tierName, long minAmount, int quota) {
        this.projectId = projectId;
        this.tierName = tierName;
        this.minAmount = minAmount;
        this.quota = quota;
    }
}
