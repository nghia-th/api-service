-- Adds optional login identifiers so Admin/Parent can log in with email, username, OR phone
-- (2026-09-05 request) - see AuthService#loginParent/#loginAdmin. Both new columns are nullable
-- with NO backfill for existing rows (an existing account simply has no username until it sets
-- one via the new self-service "set username" endpoint) - this was an explicit user decision.
ALTER TABLE admin
    ADD username NVARCHAR(100);

ALTER TABLE admin
    ADD phone NVARCHAR(20);

-- Unlike the other 4 dialects, SQL Server's UNIQUE constraint treats NULL as a normal value (only
-- ONE NULL allowed), which would break as soon as a 2nd account had no username yet. A filtered
-- unique index (WHERE username IS NOT NULL) is the standard SQL Server workaround - it only
-- enforces uniqueness among rows that actually have a username set.
CREATE UNIQUE INDEX uq_admin_username ON admin (username) WHERE username IS NOT NULL;

ALTER TABLE parent
    ADD username NVARCHAR(100);

CREATE UNIQUE INDEX uq_parent_username ON parent (username) WHERE username IS NOT NULL;
