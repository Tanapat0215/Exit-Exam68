package controller;

import model.*;
import repo.CsvDb;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class CrowdfundController {
    private final CsvDb db;
    private User currentUser = null;

    public CrowdfundController(Path dataDir) throws IOException {
        this.db = new CsvDb(dataDir);
    }

    // --- Authentication (ง่าย ๆ) ---
    public boolean login(String username) {
        for (User u : db.users.values()) {
            if (u.username.equalsIgnoreCase(username)) {
                currentUser = u;
                return true;
            }
        }
        return false;
    }
    public void logout() { currentUser = null; }
    public User getCurrentUser() { return currentUser; }

    // --- Accessors for GUI ---
    public Collection<Project> getAllProjects() { return db.projects.values(); }
    public List<RewardTier> getTiers(String projectId) { return db.tiersByProject.getOrDefault(projectId, new ArrayList<>()); }
    public long getRejectedCount() { return db.rejectedCount; }
    public long getSuccessCount() {
        return db.pledges.stream().filter(pl -> "SUCCESS".equals(pl.status)).count();
    }
    public Project getProject(String id){ return db.projects.get(id); }

    // --- Searching / Sorting / Filtering ---
    public List<Project> search(String keyword, String category, String sortKey){
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Project> list = new ArrayList<>(db.projects.values());
        if (!kw.isBlank()){
            list.removeIf(p -> !(p.name.toLowerCase().contains(kw) || p.projectId.contains(kw)));
        }
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)){
            list.removeIf(p -> !p.category.equalsIgnoreCase(category));
        }
        switch (sortKey){
            case "Ending Soon":
                list.sort(Comparator.comparing(p -> p.deadline));
                break;
            case "Raised (High→Low)":
                list.sort((a,b) -> Long.compare(b.raised, a.raised));
                break;
            case "Newest Id":
                list.sort((a,b) -> b.projectId.compareTo(a.projectId));
                break;
            default:
                list.sort(Comparator.comparing(p -> p.projectId));
        }
        return list;
    }

    // --- Business Rules & Pledge ---
    public String pledge(String projectId, long amount, String tierNameOrEmpty) throws IOException {
        if (currentUser == null) return "Please login first.";
        Project p = db.projects.get(projectId);
        if (p == null) return "Project not found.";

        if (!p.deadline.isAfter(LocalDate.now())) {
            db.rejectedCount++; db.saveStats();
            appendPledge("REJECTED", currentUser.userId, projectId, amount, tierNameOrEmpty);
            return "Rejected: Project deadline has passed.";
        }
        // Tier validations
        model.RewardTier chosen = null;
        if (tierNameOrEmpty != null && !tierNameOrEmpty.isBlank()) {
            for (model.RewardTier t : db.tiersByProject.getOrDefault(projectId, Collections.emptyList())) {
                if (t.tierName.equalsIgnoreCase(tierNameOrEmpty)) { chosen = t; break; }
            }
            if (chosen == null) {
                db.rejectedCount++; db.saveStats();
                appendPledge("REJECTED", currentUser.userId, projectId, amount, tierNameOrEmpty);
                return "Rejected: Reward tier not found.";
            }
            if (amount < chosen.minAmount) {
                db.rejectedCount++; db.saveStats();
                appendPledge("REJECTED", currentUser.userId, projectId, amount, tierNameOrEmpty);
                return "Rejected: Amount below tier minimum.";
            }
            if (chosen.quota <= 0) {
                db.rejectedCount++; db.saveStats();
                appendPledge("REJECTED", currentUser.userId, projectId, amount, tierNameOrEmpty);
                return "Rejected: Tier quota exhausted.";
            }
        }

        // Apply
        p.raised += amount;
        if (chosen != null) chosen.quota--;

        db.saveProjects();
        db.saveRewardTiers();
        appendPledge("SUCCESS", currentUser.userId, projectId, amount, tierNameOrEmpty);
        return "Success: Thank you for your support!";
    }

    private void appendPledge(String status, String userId, String projectId, long amount, String tier) throws IOException {
        String id = "pl" + (db.pledges.size() + 1);
        Pledge pl = new Pledge(id, userId, projectId, LocalDateTime.now(), amount, tier, status);
        db.pledges.add(pl);
        db.appendPledge(pl);
    }

    public Set<String> listCategories(){
        Set<String> cats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Project p : db.projects.values()) cats.add(p.category);
        return cats;
    }
}
