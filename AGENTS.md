# AGENTS.md

## Scope

This file applies to the entire repository unless a more specific `AGENTS.md`
exists in a subdirectory.

## Project Inputs and Versioning

- Treat `中间件理解情况.txt` as the primary record of the user's current
  middleware understanding, misconceptions, open questions, and learning gaps.
  Read the relevant section before planning follow-up exercises, but do not
  rewrite the user's learning record unless explicitly asked.
- Treat files under `rfs/` as read-only resume references unless the user
  explicitly asks to edit a specific file.
- Preserve the completed learning Demo as a reproducible baseline. When adding
  post-learning product features or broader engineering improvements, create or
  use a clearly named second-development version instead of overwriting the
  baseline implementation.
- Keep the baseline and second-development version separate enough that their
  code, configuration, ports, Compose resources, README commands, tests, and
  evidence cannot be confused. Document the source baseline and the differences
  introduced by the second-development version.
- Do not duplicate local databases, Docker volumes, logs, build outputs, or
  secrets when creating a second-development version. Copy only source code,
  configuration templates, tests, and documentation needed to reproduce it.

## Working Guidelines

- Read the relevant files before making changes.
- Keep changes focused on the requested task.
- Follow the existing project structure, naming conventions, and coding style.
- Do not remove or overwrite unrelated user changes.
- Avoid introducing new dependencies unless they are necessary.
- Add brief comments only when the code is not self-explanatory.
- Use `docs/resume-driven-roadmap.md` when choosing the next middleware exercise.
- Reconcile the roadmap with the latest `中间件理解情况.txt` before selecting a
  stage. Learning gaps demonstrated by the user take priority over adding more
  middleware keywords.
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

## Follow-up Learning Priorities

Unless the user explicitly changes the scope, schedule the following as separate
stages and do not silently fold them into one large rewrite:

1. Redis pressure testing: compare representative cached and uncached paths with
   appropriate tools such as `wrk`, `hey`, Apache JMeter, or Gatling. Choose the
   smallest suitable tool set for the environment rather than requiring every
   tool. Record machine and container resources, dataset, concurrency, duration,
   warm-up, throughput, P50/P95/P99, error rate, Redis hit behavior, and database
   impact.
2. Elasticsearch database and index follow-up: use the existing project notes and
   implementation as the source of truth; do not repeat already understood basic
   material. Focus new work on demonstrated gaps, correctness, synchronization,
   recovery, and reproducible evidence.
3. XXL-JOB completion: go beyond the current compensation-only exercise by adding
   at least one scheduled or batch-processing scenario and integrating a real
   XXL-JOB Admin instance. Verify Admin configuration, executor registration,
   handler execution, retry/failure behavior, and, when relevant, batching,
   sharding, or idempotency.
4. Prometheus system visibility: extend business metrics with useful JVM, process,
   host, container, and dependency indicators where the local environment permits.
   Provide a reproducible Prometheus/Grafana view instead of treating an exposed
   metrics endpoint or a reachable Grafana login page as a completed dashboard.
5. Logging and business tracing: improve structured logs, correlation or trace IDs,
   cross-component context propagation, error classification, and searchable
   business events. Add distributed tracing only when the call chain justifies it,
   and verify a complete business path plus at least one failure path.

Redis pressure testing and the observability work above are required learning-plan
items. Their absence must remain documented as a limitation until reproducible
results exist.

## Validation

- Run the most relevant available checks after making changes.
- Prefer targeted tests for small changes and broader checks for shared behavior.
- For pressure tests, commit the test plan or script and a concise result summary;
  do not rely on screenshots alone. Separate warm-up from measured runs and repeat
  enough times to identify unstable results.
- For scheduled jobs, verify both automated scheduling through XXL-JOB Admin and a
  controlled failure or retry path; a manual HTTP trigger alone is insufficient.
- For metrics, logs, and tracing, verify that one business request can be followed
  from entry to outcome and that failures can be distinguished from successful
  or duplicate processing.
- Never claim a performance improvement from incomparable runs. Keep hardware,
  dataset, application configuration, dependency state, and load model consistent,
  and record any unavoidable differences.
- If validation cannot be run, state the reason clearly.

## Documentation

- Update documentation when behavior, configuration, or usage changes.
- Keep examples concise and consistent with the current implementation.
- Update `docs/resume-driven-roadmap.md` when stage order or acceptance criteria
  change, and append verified results to `docs/implementation-log.md`.
- At the end of a stage, update the relevant section of the learning plan with
  completed evidence, remaining knowledge gaps, and the next single-stage handoff.
- Write public-facing documentation in a natural developer voice. Avoid
  AI-flavored wording, assistant/tool process descriptions, exaggerated praise,
  and phrases that make the content look generated rather than maintained by the
  project owner.

## Document Output Rules

- Do not create, modify, convert, or export Word, PDF, spreadsheet, presentation,
  or other non-text document formats unless the user explicitly requests that
  format.
- For long learning reviews, prefer the existing relevant `.txt` or Markdown file
  instead of creating an unrelated document.
- Before editing an existing non-text document, explain the intended edits and
  obtain explicit authorization.

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
