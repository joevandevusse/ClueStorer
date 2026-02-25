# Maven Commands Reference

## Day-to-Day Development

**`mvn test`** is the right command for normal development. Maven compiles incrementally (only recompiles changed files) and reruns tests. The `target/` directory is updated in place and does not accumulate bloat over time. It is also excluded from git via `.gitignore`.

## Command Reference

| Command | When to use |
|---|---|
| `mvn test` | Every day — fast, incremental, use this during development |
| `mvn clean test` | When something seems wrong and you suspect stale compiled classes |
| `mvn verify` | Runs through the full lifecycle including packaging — overkill for most local dev |
| `mvn clean verify` | What a CI/CD pipeline would run for a full, authoritative build |

## Notes

- `target/` is in `.gitignore` — it never touches the repo regardless of which command you run.
- The `clean` step deletes `target/` and forces a full recompile. Only needed when troubleshooting a strange build issue, not as a routine step.
- `verify` runs phases beyond `test` (package, integration-test, verify). There are no integration tests configured in this project, so `mvn test` covers everything relevant.
