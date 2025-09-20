# Crowdfund MVC — Swing + CSV (Search/Filter/Sort)

- GUI: Java Swing with search by keyword / filter by category / sort (Ending Soon, Raised Desc, Newest Id).
- Backend: CSV files (`data/`), simple controller enforcing business rules (deadline future, tier min, quota, rejected counter).
- Auth: type username from `users.csv` then click *Login* (session simulation).

## Build & Run
Compile all sources under `src/` (tested on Java 11+). Example commands:

### Windows (PowerShell)
```
$files = Get-ChildItem -Recurse -Filter *.java | %% { $_.FullName }
javac -d out $files
java -cp out app.MainSwing
```

### macOS/Linux
```
javac -d out $(find src -name "*.java")
java -cp out app.MainSwing
```

## Notes mapping to exam requirements
- 3 views (in GUI form): Project list with search/filter/sort, Project detail with progress bar, Stats dialog.
- Business rules: deadline future, amount >= tier min (if chosen), decrement quota, count rejected.
- Sample data: ≥8 projects across ≥3 categories, each with ≥2 tiers, ≥10 users; mix of accepted/rejected after you test.
