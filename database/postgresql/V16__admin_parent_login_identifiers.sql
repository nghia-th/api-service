-- Adds optional login identifiers so Admin/Parent can log in with email, username, OR phone
-- (2026-09-05 request) - see AuthService#loginParent/#loginAdmin. Both new columns are nullable
-- with NO backfill for existing rows (an existing account simply has no username until it sets
-- one via the new self-service "set username" endpoint) - this was an explicit user decision.
ALTER TABLE admin
    ADD COLUMN username VARCHAR(100);

ALTER TABLE admin
    ADD COLUMN phone VARCHAR(20);

-- Standard UNIQUE constraint: Postgres treats NULL as distinct from NULL, so any number of admin
-- rows with no username yet can coexist - only a non-null username collision is rejected.
ALTER TABLE admin
    ADD CONSTRAINT uq_admin_username UNIQUE (username);

ALTER TABLE parent
    ADD COLUMN username VARCHAR(100);

ALTER TABLE parent
    ADD CONSTRAINT uq_parent_username UNIQUE (username);
