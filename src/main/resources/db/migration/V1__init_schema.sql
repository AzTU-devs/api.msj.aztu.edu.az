-- =====================================================================
--  Machine Science Journal (AzTU) — initial schema
--  PostgreSQL 16+ (developed on Neon PG18)
--
--  Redesign notes vs. the legacy MySQL/MyISAM database:
--    * Real referential integrity (FKs) everywhere — MyISAM had none.
--    * Passwords are bcrypt hashes, never plaintext.
--    * Unified accounts: one `users` table + role join, not users vs. admin.
--    * Articles support multiple ordered authors (not just a submitter id).
--    * comments.a_id (string) -> proper bigint FK.
--    * First-class metrics: raw event log + fast counters + daily rollups
--      + external citations, so views/downloads/citations are all tracked.
--    * Multilingual CMS via JSONB {"az","en","ru"} instead of _en/_ru columns.
--    * Enum-like fields are varchar + CHECK (JPA maps to Java enums as STRING).
-- =====================================================================

-- updated_at auto-touch --------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
--  IDENTITY & ACCESS
-- =====================================================================

CREATE TABLE users (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email            varchar(255) NOT NULL,
    password_hash    varchar(255),                       -- null => external/invited, must set password
    first_name       varchar(120) NOT NULL,
    last_name        varchar(120) NOT NULL,
    middle_name      varchar(120),                        -- legacy father_name
    -- academic / contact profile
    title            varchar(120),                        -- Prof., Dr., ...
    degree           varchar(150),                        -- academic degree
    position         varchar(200),
    affiliation      text,
    country          varchar(120),
    city             varchar(120),
    postal_code      varchar(30),
    phone            varchar(60),
    orcid            varchar(30),                          -- 0000-0000-0000-0000
    scopus_id        varchar(40),
    website_url      varchar(300),
    avatar_url       varchar(500),
    bio              text,
    -- account state
    status           varchar(20)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','DEACTIVATED')),
    email_verified_at timestamptz,
    is_available_reviewer boolean NOT NULL DEFAULT true,   -- reviewer availability toggle
    preferred_locale varchar(5)  NOT NULL DEFAULT 'en'
                     CHECK (preferred_locale IN ('az','en','ru')),
    last_login_at    timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
CREATE UNIQUE INDEX ux_users_orcid ON users (orcid) WHERE orcid IS NOT NULL;
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE roles (
    code        varchar(30) PRIMARY KEY
                CHECK (code IN ('ADMIN','EDITOR_IN_CHIEF','EDITOR','REVIEWER','AUTHOR')),
    name        varchar(100) NOT NULL,
    description text
);

CREATE TABLE user_roles (
    user_id   bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_code varchar(30) NOT NULL REFERENCES roles(code) ON DELETE CASCADE,
    granted_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_code)
);
CREATE INDEX ix_user_roles_role ON user_roles(role_code);

-- opaque refresh tokens (rotation + revocation), access tokens stay stateless JWTs
CREATE TABLE refresh_tokens (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  varchar(255) NOT NULL,                    -- sha-256 of the raw token
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    replaced_by bigint REFERENCES refresh_tokens(id),
    user_agent  varchar(400),
    ip_address  varchar(64),
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens(user_id);

-- single-use tokens for email verification & password reset
CREATE TABLE auth_tokens (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose    varchar(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFY','PASSWORD_RESET','INVITE')),
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_auth_token_hash ON auth_tokens(token_hash);
CREATE INDEX ix_auth_tokens_user ON auth_tokens(user_id, purpose);

-- =====================================================================
--  PUBLIC EDITORIAL BOARD (curated display list — distinct from platform users)
-- =====================================================================

CREATE TABLE board_members (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name     varchar(200) NOT NULL,
    title         varchar(400),                            -- affiliation / role line
    section       varchar(30)  NOT NULL DEFAULT 'BOARD'
                  CHECK (section IN ('EDITOR_IN_CHIEF','HONORARY','BOARD')),
    photo_url     varchar(500),
    orcid_url     varchar(300),
    scopus_url    varchar(300),
    email         varchar(255),
    country       varchar(120),
    user_id       bigint REFERENCES users(id) ON DELETE SET NULL,  -- optional link to an account
    sort_order    int NOT NULL DEFAULT 0,
    is_active     boolean NOT NULL DEFAULT true,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_board_members_section ON board_members(section, sort_order);
CREATE TRIGGER trg_board_members_updated BEFORE UPDATE ON board_members
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
--  JOURNAL STRUCTURE: ISSUES
-- =====================================================================

CREATE TABLE issues (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    volume       int,
    number       int,
    year         int NOT NULL,
    title        varchar(300) NOT NULL,                   -- "Machine Science 2025 - Number I"
    description  text,
    cover_url    varchar(500),
    full_pdf_url varchar(500),                            -- whole-issue PDF
    doi          varchar(120),
    slug         varchar(160) NOT NULL,
    status       varchar(20) NOT NULL DEFAULT 'DRAFT'
                 CHECK (status IN ('DRAFT','PUBLISHED')),
    published_at date,
    sort_order   int NOT NULL DEFAULT 0,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_issues_slug ON issues(slug);
CREATE UNIQUE INDEX ux_issues_vol_num ON issues(year, volume, number)
    WHERE volume IS NOT NULL AND number IS NOT NULL;
CREATE INDEX ix_issues_status_year ON issues(status, year DESC);
CREATE TRIGGER trg_issues_updated BEFORE UPDATE ON issues
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
--  ARTICLES / SUBMISSIONS
-- =====================================================================

CREATE TABLE articles (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          varchar(500) NOT NULL,
    abstract       text,
    keywords       text,                                  -- semicolon/comma separated
    subject_area   varchar(120),                          -- Machine design / Automation & ICT / ...
    language       varchar(5) NOT NULL DEFAULT 'en',
    -- workflow
    status         varchar(30) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT','SUBMITTED','WITH_EDITOR','UNDER_REVIEW',
                                     'REVISION_REQUESTED','RESUBMITTED','ACCEPTED','REJECTED',
                                     'COPYEDITING','IN_PRODUCTION','PUBLISHED','WITHDRAWN')),
    submitter_id   bigint NOT NULL REFERENCES users(id),           -- corresponding/submitting author account
    handling_editor_id bigint REFERENCES users(id) ON DELETE SET NULL,
    -- publication
    issue_id       bigint REFERENCES issues(id) ON DELETE SET NULL,
    doi            varchar(120),
    page_start     int,
    page_end       int,
    article_order  int,                                   -- order within an issue
    -- lifecycle timestamps
    submitted_at   timestamptz,
    decided_at     timestamptz,
    published_at   date,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_articles_doi ON articles(doi) WHERE doi IS NOT NULL;
CREATE INDEX ix_articles_status ON articles(status);
CREATE INDEX ix_articles_submitter ON articles(submitter_id);
CREATE INDEX ix_articles_issue ON articles(issue_id);
CREATE INDEX ix_articles_handling_editor ON articles(handling_editor_id);
CREATE INDEX ix_articles_published ON articles(published_at DESC) WHERE status = 'PUBLISHED';
-- full-text search over title + abstract + keywords
CREATE INDEX ix_articles_fts ON articles USING gin (
    to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(abstract,'') || ' ' || coalesce(keywords,''))
);
CREATE TRIGGER trg_articles_updated BEFORE UPDATE ON articles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ordered authors (may or may not be platform users)
CREATE TABLE article_authors (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id     bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id        bigint REFERENCES users(id) ON DELETE SET NULL,
    first_name     varchar(120) NOT NULL,
    last_name      varchar(120) NOT NULL,
    email          varchar(255),
    affiliation    text,
    country        varchar(120),
    orcid          varchar(30),
    author_order   int NOT NULL DEFAULT 0,
    is_corresponding boolean NOT NULL DEFAULT false
);
CREATE INDEX ix_article_authors_article ON article_authors(article_id, author_order);

-- files: manuscript, supplementary, revision, camera-ready PDF, cover letter
CREATE TABLE article_files (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id    bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    kind          varchar(20) NOT NULL
                  CHECK (kind IN ('MANUSCRIPT','SUPPLEMENTARY','REVISION','CAMERA_READY','COVER_LETTER','PUBLISHED_PDF')),
    original_name varchar(300) NOT NULL,
    storage_key   varchar(500) NOT NULL,                  -- path/key in object storage
    content_type  varchar(120),
    size_bytes    bigint,
    version       int NOT NULL DEFAULT 1,
    uploaded_by   bigint REFERENCES users(id) ON DELETE SET NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_article_files_article ON article_files(article_id, kind);

-- immutable status history (legacy articles_status)
CREATE TABLE article_status_history (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id  bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    from_status varchar(30),
    to_status   varchar(30) NOT NULL,
    changed_by  bigint REFERENCES users(id) ON DELETE SET NULL,
    comment     text,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_article_status_hist_article ON article_status_history(article_id, created_at);

-- =====================================================================
--  PEER REVIEW
-- =====================================================================

CREATE TABLE review_assignments (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id   bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    reviewer_id  bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by  bigint REFERENCES users(id) ON DELETE SET NULL,
    round        int NOT NULL DEFAULT 1,
    status       varchar(20) NOT NULL DEFAULT 'INVITED'
                 CHECK (status IN ('INVITED','ACCEPTED','DECLINED','IN_PROGRESS','SUBMITTED','CANCELLED','OVERDUE')),
    invited_at   timestamptz NOT NULL DEFAULT now(),
    responded_at timestamptz,
    due_date     date,
    completed_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_review_assign_unique ON review_assignments(article_id, reviewer_id, round);
CREATE INDEX ix_review_assign_reviewer ON review_assignments(reviewer_id, status);
CREATE TRIGGER trg_review_assign_updated BEFORE UPDATE ON review_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE reviews (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assignment_id      bigint NOT NULL REFERENCES review_assignments(id) ON DELETE CASCADE,
    article_id         bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    reviewer_id        bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommendation     varchar(20) NOT NULL
                       CHECK (recommendation IN ('ACCEPT','MINOR_REVISION','MAJOR_REVISION','REJECT')),
    score              int CHECK (score BETWEEN 1 AND 10),
    comments_to_author text,
    comments_to_editor text,
    attachment_key     varchar(500),
    submitted_at       timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_reviews_assignment ON reviews(assignment_id);
CREATE INDEX ix_reviews_article ON reviews(article_id);

-- editorial discussion / comments (legacy comments; a_id string -> bigint FK)
CREATE TABLE article_discussions (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id  bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    author_id   bigint REFERENCES users(id) ON DELETE SET NULL,
    body        text NOT NULL,
    visibility  varchar(20) NOT NULL DEFAULT 'EDITORIAL'
                CHECK (visibility IN ('EDITORIAL','AUTHOR','REVIEWER','PUBLIC')),
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_article_discussions_article ON article_discussions(article_id, created_at);

-- =====================================================================
--  METRICS: raw events, fast counters, daily rollups, citations
-- =====================================================================

-- per-article denormalized counters for fast reads
CREATE TABLE article_metrics (
    article_id       bigint PRIMARY KEY REFERENCES articles(id) ON DELETE CASCADE,
    view_count       bigint NOT NULL DEFAULT 0,
    abstract_view_count bigint NOT NULL DEFAULT 0,
    download_count   bigint NOT NULL DEFAULT 0,
    citation_count   bigint NOT NULL DEFAULT 0,
    updated_at       timestamptz NOT NULL DEFAULT now()
);

-- raw event log (append-only) — supports dedup and time-series analytics
CREATE TABLE article_events (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id   bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    event_type   varchar(20) NOT NULL
                 CHECK (event_type IN ('ABSTRACT_VIEW','FULLTEXT_VIEW','PDF_DOWNLOAD')),
    occurred_at  timestamptz NOT NULL DEFAULT now(),
    ip_hash      varchar(64),                             -- hashed, never store raw IP
    session_hash varchar(64),
    country      varchar(2),
    referrer     varchar(500),
    user_agent   varchar(400)
);
CREATE INDEX ix_article_events_article_time ON article_events(article_id, occurred_at);
CREATE INDEX ix_article_events_type_time ON article_events(event_type, occurred_at);
-- dedup guard: at most one counted event per article/type/session/day.
-- Cast is anchored to UTC so the expression is IMMUTABLE and indexable.
CREATE UNIQUE INDEX ux_article_events_dedup
    ON article_events(article_id, event_type, session_hash, ((occurred_at AT TIME ZONE 'UTC')::date))
    WHERE session_hash IS NOT NULL;

-- daily rollup for charts
CREATE TABLE article_metric_daily (
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    day        date   NOT NULL,
    views      bigint NOT NULL DEFAULT 0,
    downloads  bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (article_id, day)
);
CREATE INDEX ix_metric_daily_day ON article_metric_daily(day);

-- external citations discovered via Crossref/Scopus/Scholar or entered manually
CREATE TABLE citations (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id    bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    source        varchar(20) NOT NULL
                  CHECK (source IN ('CROSSREF','SCOPUS','GOOGLE_SCHOLAR','WOS','MANUAL')),
    citing_doi    varchar(150),
    citing_title  text,
    citing_authors text,
    citing_venue  varchar(400),
    citing_year   int,
    url           varchar(600),
    discovered_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_citations_unique ON citations(article_id, source, citing_doi)
    WHERE citing_doi IS NOT NULL;
CREATE INDEX ix_citations_article ON citations(article_id);

-- =====================================================================
--  CMS / SITE CONTENT (multilingual via JSONB {"az","en","ru"})
-- =====================================================================

CREATE TABLE content_pages (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug       varchar(160) NOT NULL,                     -- about, scope, author-guidelines, ethics, ...
    title      jsonb NOT NULL DEFAULT '{}'::jsonb,
    body       jsonb NOT NULL DEFAULT '{}'::jsonb,
    status     varchar(20) NOT NULL DEFAULT 'PUBLISHED'
               CHECK (status IN ('DRAFT','PUBLISHED')),
    sort_order int NOT NULL DEFAULT 0,
    updated_by bigint REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_content_pages_slug ON content_pages(slug);
CREATE TRIGGER trg_content_pages_updated BEFORE UPDATE ON content_pages
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE announcements (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      jsonb NOT NULL DEFAULT '{}'::jsonb,
    body       jsonb NOT NULL DEFAULT '{}'::jsonb,
    image_url  varchar(500),
    link_url   varchar(500),
    is_pinned  boolean NOT NULL DEFAULT false,
    status     varchar(20) NOT NULL DEFAULT 'PUBLISHED'
               CHECK (status IN ('DRAFT','PUBLISHED')),
    published_at date NOT NULL DEFAULT current_date,
    sort_order int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_announcements_status ON announcements(status, published_at DESC);
CREATE TRIGGER trg_announcements_updated BEFORE UPDATE ON announcements
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- singleton journal configuration (legacy site_set)
CREATE TABLE journal_settings (
    id                smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    journal_title     jsonb NOT NULL DEFAULT '{}'::jsonb,
    tagline           jsonb NOT NULL DEFAULT '{}'::jsonb,
    about             jsonb NOT NULL DEFAULT '{}'::jsonb,
    issn_print        varchar(20),
    issn_online       varchar(20),
    doi_prefix        varchar(30),
    publisher         varchar(200),
    email             varchar(120),
    phone             varchar(60),
    address           jsonb NOT NULL DEFAULT '{}'::jsonb,
    indexed_in        jsonb NOT NULL DEFAULT '[]'::jsonb,  -- ["INSPEC", ...]
    social            jsonb NOT NULL DEFAULT '{}'::jsonb,  -- {"facebook":..,"instagram":..}
    publication_fee   varchar(60),
    logo_url          varchar(500),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_journal_settings_updated BEFORE UPDATE ON journal_settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
--  INBOUND FORMS & NOTIFICATIONS
-- =====================================================================

CREATE TABLE contact_messages (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name  varchar(200) NOT NULL,
    email      varchar(255),
    phone      varchar(60),
    subject    varchar(200),
    message    text NOT NULL,
    status     varchar(20) NOT NULL DEFAULT 'NEW'
               CHECK (status IN ('NEW','READ','REPLIED','ARCHIVED','SPAM')),
    ip_address varchar(64),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_contact_messages_status ON contact_messages(status, created_at DESC);

-- in-app notifications (legacy nav activity log)
CREATE TABLE notifications (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       varchar(40) NOT NULL,                      -- SUBMISSION_RECEIVED, REVIEW_ASSIGNED, DECISION, ...
    title      varchar(255) NOT NULL,
    body       text,
    link_url   varchar(400),
    is_read    boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user ON notifications(user_id, is_read, created_at DESC);

-- outbound email audit
CREATE TABLE email_log (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    to_address   varchar(255) NOT NULL,
    subject      varchar(300),
    template     varchar(80),
    status       varchar(20) NOT NULL DEFAULT 'SENT'
                 CHECK (status IN ('SENT','FAILED','QUEUED')),
    error        text,
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_email_log_to ON email_log(to_address, created_at DESC);

-- lightweight admin audit trail
CREATE TABLE audit_log (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id   bigint REFERENCES users(id) ON DELETE SET NULL,
    action     varchar(80) NOT NULL,
    entity     varchar(80),
    entity_id  varchar(80),
    metadata   jsonb,
    ip_address varchar(64),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_log_entity ON audit_log(entity, entity_id);
CREATE INDEX ix_audit_log_actor ON audit_log(actor_id, created_at DESC);
