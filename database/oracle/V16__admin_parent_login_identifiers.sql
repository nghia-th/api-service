-- Adds optional login identifiers so Admin/Parent can log in with email, username, OR phone
-- (2026-09-05 request) - see AuthService#loginParent/#loginAdmin. Both new columns are nullable
-- with NO backfill for existing rows (an existing account simply has no username until it sets
-- one via the new self-service "set username" endpoint) - this was an explicit user decision.
ALTER TABLE admin
    ADD (username VARCHAR2(100));

ALTER TABLE admin
    ADD (phone VARCHAR2(20));

-- Oracle also treats NULL as distinct in a UNIQUE constraint, same reasoning as the other dialects.
ALTER TABLE admin
    ADD CONSTRAINT uq_admin_username UNIQUE (username);

ALTER TABLE parent
    ADD (username VARCHAR2(100));

ALTER TABLE parent
    ADD CONSTRAINT uq_parent_username UNIQUE (username);
