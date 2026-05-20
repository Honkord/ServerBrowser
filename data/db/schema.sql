-- Server Browser database schema (SQLite)
-- See docs/architecture/arch.txt

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    user_id         TEXT PRIMARY KEY,
    username        TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    salt            BLOB NOT NULL,
    email           TEXT,
    display_name    TEXT,
    created_at      TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS oauth_identities (
    provider            TEXT NOT NULL,
    provider_subject    TEXT NOT NULL,
    user_id             TEXT NOT NULL,
    email               TEXT,
    created_at          TEXT NOT NULL,
    PRIMARY KEY (provider, provider_subject),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_states (
    state           TEXT PRIMARY KEY,
    provider        TEXT NOT NULL,
    code_verifier   TEXT NOT NULL,
    expires_at      TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS api_keys (
    key_id          TEXT PRIMARY KEY,
    user_id         TEXT NOT NULL,
    api_key_hash    TEXT NOT NULL,
    expires_at      TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sessions (
    session_id      TEXT PRIMARY KEY,
    user_id         TEXT NOT NULL,
    access_token    TEXT NOT NULL UNIQUE,
    refresh_token   TEXT NOT NULL,
    expires_at      TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    log_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         TEXT,
    event_type      TEXT NOT NULL,
    message         TEXT NOT NULL,
    created_at      TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS connection_history (
    history_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         TEXT,
    remote_host     TEXT NOT NULL,
    remote_port     INTEGER NOT NULL,
    protocol        TEXT NOT NULL,
    bytes_streamed  INTEGER NOT NULL DEFAULT 0,
    started_at      TEXT NOT NULL,
    ended_at        TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS cache_indexes (
    cache_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    cache_key       TEXT NOT NULL UNIQUE,
    cache_type      TEXT NOT NULL,
    payload_ref     TEXT,
    expires_at      TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
-- idx_users_email and OAuth indexes are created in db/migrations/001_oauth.sql after column upgrades
CREATE INDEX IF NOT EXISTS idx_sessions_access_token ON sessions(access_token);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_connection_history_started_at ON connection_history(started_at);
CREATE INDEX IF NOT EXISTS idx_cache_indexes_expires_at ON cache_indexes(expires_at);

CREATE TABLE IF NOT EXISTS dns_phonebook (
    record_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    zone            TEXT NOT NULL DEFAULT 'server_browser.org',
    record_name     TEXT NOT NULL,
    record_type     TEXT NOT NULL,
    target          TEXT NOT NULL,
    port            INTEGER,
    ttl_seconds     INTEGER NOT NULL DEFAULT 300,
    UNIQUE (zone, record_name)
);

CREATE INDEX IF NOT EXISTS idx_dns_phonebook_zone_name ON dns_phonebook(zone, record_name);
