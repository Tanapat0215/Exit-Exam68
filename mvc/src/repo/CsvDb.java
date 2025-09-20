package repo;

import model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CsvDb {
    private final Path dataDir;
    public Map<String, Project> projects = new HashMap<>();
    public Map<String, List<RewardTier>> tiersByProject = new HashMap<>();
    public Map<String, User> users = new HashMap<>();
    public List<Pledge> pledges = new ArrayList<>();
    public long rejectedCount = 0;

    public CsvDb(Path dataDir) throws IOException {
        this.dataDir = dataDir;
        loadAll();
    }

    private static List<String[]> readCsv(Path p) throws IOException {
        if (!Files.exists(p)) return new ArrayList<>();
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (int i = 1 /* skip header */; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            rows.add(parseCsvLine(lines.get(i)));
        }
        return rows;
    }

    private static String[] parseCsvLine(String line) {
        // Simple CSV parser handling quotes
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else { inQuotes = false; }
                } else { cur.append(c); }
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else { cur.append(c); }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static void writeCsv(Path p, List<String> lines) throws IOException {
        Files.createDirectories(p.getParent());
        Files.write(p, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void loadAll() throws IOException {
        loadProjects();
        loadRewardTiers();
        loadUsers();
        loadPledges();
        loadStats();
    }

    private void loadProjects() throws IOException {
        Path p = dataDir.resolve("projects.csv");
        projects.clear();
        List<String[]> rows = readCsv(p);
        for (String[] r : rows) {
            // projectId,name,category,target,deadline,raised
            String id = r[0];
            Project prj = new Project(
                id, r[1], r[2],
                Long.parseLong(r[3]),
                LocalDate.parse(r[4]),
                Long.parseLong(r[5])
            );
            projects.put(id, prj);
        }
    }

    private void loadRewardTiers() throws IOException {
        Path p = dataDir.resolve("reward_tiers.csv");
        tiersByProject.clear();
        for (String[] r : readCsv(p)) {
            RewardTier t = new RewardTier(
                r[0], r[1], Long.parseLong(r[2]), Integer.parseInt(r[3])
            );
            tiersByProject.computeIfAbsent(t.projectId, k -> new ArrayList<>()).add(t);
        }
    }

    private void loadUsers() throws IOException {
        Path p = dataDir.resolve("users.csv");
        users.clear();
        for (String[] r : readCsv(p)) {
            users.put(r[0], new User(r[0], r[1]));
        }
    }

    private void loadPledges() throws IOException {
        Path p = dataDir.resolve("pledges.csv");
        pledges.clear();
        if (!Files.exists(p)) return;
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] r = parseCsvLine(lines.get(i));
            if (r.length < 7) continue;
            pledges.add(new Pledge(
                r[0], r[1], r[2], LocalDateTime.parse(r[3]), Long.parseLong(r[4]), r[5], r[6]
            ));
        }
    }

    private void loadStats() throws IOException {
        Path p = dataDir.resolve("stats.csv");
        if (!Files.exists(p)) { rejectedCount = 0; return; }
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] r = parseCsvLine(lines.get(i));
            if ("rejectedCount".equals(r[0])) {
                rejectedCount = Long.parseLong(r[1]);
            }
        }
    }

    public void saveProjects() throws IOException {
        Path p = dataDir.resolve("projects.csv");
        List<String> out = new ArrayList<>();
        out.add("projectId,name,category,target,deadline,raised");
        List<Project> sorted = projects.values().stream()
                .sorted(Comparator.comparing(a -> a.projectId))
                .collect(Collectors.toList());
        for (Project prj : sorted) {
            out.add(String.join(",",
                prj.projectId, esc(prj.name), esc(prj.category),
                String.valueOf(prj.target),
                prj.deadline.toString(),
                String.valueOf(prj.raised)
            ));
        }
        writeCsv(p, out);
    }

    public void saveRewardTiers() throws IOException {
        Path p = dataDir.resolve("reward_tiers.csv");
        List<String> out = new ArrayList<>();
        out.add("projectId,tierName,minAmount,quota");
        for (String pid : tiersByProject.keySet()) {
            for (RewardTier t : tiersByProject.get(pid)) {
                out.add(String.join(",",
                    pid, esc(t.tierName), String.valueOf(t.minAmount), String.valueOf(t.quota)
                ));
            }
        }
        writeCsv(p, out);
    }

    public void appendPledge(Pledge pl) throws IOException {
        Path p = dataDir.resolve("pledges.csv");
        boolean exists = Files.exists(p);
        List<String> out = new ArrayList<>();
        if (exists) out = Files.readAllLines(p, StandardCharsets.UTF_8);
        else out.add("pledgeId,userId,projectId,datetime,amount,tierName,status");
        out.add(String.join(",",
            pl.pledgeId, pl.userId, pl.projectId, pl.datetime.toString(),
            String.valueOf(pl.amount), esc(pl.tierName), pl.status
        ));
        writeCsv(p, out);
    }

    public void saveStats() throws IOException {
        Path p = dataDir.resolve("stats.csv");
        List<String> out = new ArrayList<>();
        out.add("key,value");
        out.add("rejectedCount," + rejectedCount);
        writeCsv(p, out);
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains(""")) {
            return """ + s.replace(""","""") + """;
        }
        return s;
    }
}
