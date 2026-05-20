ALTER TABLE users ADD COLUMN email TEXT;
ALTER TABLE users ADD COLUMN display_name TEXT;

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

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_oauth_identities_user_id ON oauth_identities(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_states_expires_at ON oauth_states(expires_at);
