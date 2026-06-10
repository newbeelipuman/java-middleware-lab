# AGENTS.md

## Scope

This file applies to the entire repository unless a more specific `AGENTS.md`
exists in a subdirectory.

## Working Guidelines

- Read the relevant files before making changes.
- Keep changes focused on the requested task.
- Follow the existing project structure, naming conventions, and coding style.
- Do not remove or overwrite unrelated user changes.
- Avoid introducing new dependencies unless they are necessary.
- Add brief comments only when the code is not self-explanatory.
- Treat files under `rfs/` as read-only resume references unless the user explicitly
  asks to edit a specific file.
- Use `docs/resume-driven-roadmap.md` when choosing the next middleware exercise.
- Do not present an unverified resume claim as a fact. Add code, tests, metrics, and
  a reproducible verification command before promoting a claim to the resume.
- Use one conversation or background thread for one middleware stage. Do not
  implement later stages in the same thread unless the user explicitly changes the
  stage scope.
- Before starting a new middleware stage, read `docs/resume-driven-roadmap.md`,
  `docs/rocketmq-enterprise-development-flow.md`, and
  `docs/implementation-log.md`.
- At the end of each stage, record tests, Docker Compose integration checks,
  failure drills, known limitations, and the next stage handoff prompt.

## Validation

- Run the most relevant available checks after making changes.
- Prefer targeted tests for small changes and broader checks for shared behavior.
- If validation cannot be run, state the reason clearly.

## Documentation

- Update documentation when behavior, configuration, or usage changes.
- Keep examples concise and consistent with the current implementation.

## Public Repository Safety

Before preparing files for a public GitHub repository:

- Do not upload real secrets, API keys, database passwords, tokens, or local `.env`
  files.
- Do not upload local databases, Docker volumes, runtime logs, PID files, build
  outputs, or IDE metadata.
- Do not upload private resume references, interview notes, papers, editable
  diagrams, or personal materials unless explicitly approved.
- Prefer uploading schema SQL, sample data SQL, `.env.example`, screenshots, and
  reproducible run commands instead of local runtime state.
- Review `.gitignore` before the first commit.

## Delivery Notes

When reporting completed work, include:

- A short summary of the changes.
- The validation commands that were run.
- Any remaining limitations or follow-up items.
