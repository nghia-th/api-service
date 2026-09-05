-- Adds optional login identifiers so Admin/Parent can log in with email, username, OR phone
-- (2026-09-05 request) - see AuthService#loginParent/#loginAdmin. Both new columns are nullable
-- with NO backfill for existing rows (an existing account simply has no username until it sets
-- one via the new self-service "set username" endpoint) - this was an explicit user decision.
ALTER TABLE admin
    ADD COLUMN username TEXT;

ALTER TABLE admin
    ADD COLUMN phone TEXT;

-- SQLite has no ALTER TABLE ADD CONSTRAINT - a UNIQUE INDEX is the equivalent, and SQLite also
-- treats NULL as distinct so rows with no username yet coexist freely.
CREATE UNIQUE INDEX uq_admin_username ON admin (username);

ALTER TABLE parent
    ADD COLUMN username TEXT;

CREATE UNIQUE INDEX uq_parent_username ON parent (username);
