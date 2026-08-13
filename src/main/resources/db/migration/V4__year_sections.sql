-- =====================================================================
--  V4 — year sections: per-issue submission deadline + a fuller lifecycle
--
--  Each year has two sections (Number I / II). An issue now moves through
--  DRAFT -> OPEN (accepting submissions, until the deadline) -> PUBLISHED
--  (live/current) -> ARCHIVED (still public in the archive, no longer current).
-- =====================================================================

ALTER TABLE issues ADD COLUMN submission_deadline date;

ALTER TABLE issues DROP CONSTRAINT issues_status_check;
ALTER TABLE issues ADD CONSTRAINT issues_status_check
    CHECK (status IN ('DRAFT', 'OPEN', 'PUBLISHED', 'ARCHIVED'));
