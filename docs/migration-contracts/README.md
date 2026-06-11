# Migration Contracts

Store interfaces, schemas, configuration templates, and runbooks that are mature
enough to migrate into the future hospital operations project.

Each contract should include:

- Source specialization project and commit.
- Public interface, table schema, event format, or configuration shape.
- Compatibility and versioning rules.
- Operational checks and failure handling.
- Evidence links proving the contract was tested.

Do not migrate teaching domain models such as products, appointments, or doctors
into the larger project.
