-- ============================================================
-- V1: Core authentication tables for Vetalya
-- ============================================================

-- user_auth: central user identity table
CREATE TABLE IF NOT EXISTS user_auth (
    user_id         VARCHAR(26)     PRIMARY KEY,
    role            VARCHAR(20)     NOT NULL CHECK (role IN ('ROLE_VET', 'ROLE_PET_OWNER')),
    email           VARCHAR(255),
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    phone           VARCHAR(20),
    phone_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(255),
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
    token_version   INTEGER         NOT NULL DEFAULT 0,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_auth_email ON user_auth(email);

-- auth_provider: links external OAuth providers to user_auth
CREATE TABLE IF NOT EXISTS auth_provider (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id) ON DELETE CASCADE,
    provider            VARCHAR(10)     NOT NULL CHECK (provider IN ('GOOGLE', 'APPLE', 'PHONE')),
    provider_user_id    VARCHAR(255)    NOT NULL,
    provider_email      VARCHAR(255),
    -- For Apple token revocation (App Store requirement): store Apple's refresh token encrypted
    provider_refresh_token TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(provider, provider_user_id)
);

CREATE INDEX IF NOT EXISTS idx_auth_provider_user_id ON auth_provider(user_id);

-- refresh_token: opaque refresh tokens with rotation and family-based reuse detection
CREATE TABLE IF NOT EXISTS refresh_token (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id) ON DELETE CASCADE,
    token_hash      VARCHAR(64)     NOT NULL UNIQUE,
    family_id       UUID            NOT NULL,
    client_type     VARCHAR(10)     NOT NULL CHECK (client_type IN ('web', 'android', 'ios')),
    device_id       VARCHAR(255),
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMPTZ     NOT NULL,
    used_at         TIMESTAMPTZ,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_family_id ON refresh_token(family_id);

-- one_time_code: short-lived authorization codes for mobile OAuth callback
CREATE TABLE IF NOT EXISTS one_time_code (
    id              BIGSERIAL       PRIMARY KEY,
    code_hash       VARCHAR(64)     NOT NULL UNIQUE,
    user_id         VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id) ON DELETE CASCADE,
    client_type     VARCHAR(10)     NOT NULL,
    device_id       VARCHAR(255),
    new_user        BOOLEAN         NOT NULL DEFAULT FALSE,
    profile_complete BOOLEAN        NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- vet: profile for veterinarians
CREATE TABLE IF NOT EXISTS vet (
    user_id             VARCHAR(26)     PRIMARY KEY REFERENCES user_auth(user_id) ON DELETE CASCADE,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    registration_no     VARCHAR(50),
    dob                 DATE,
    photo_url           VARCHAR(500),
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- pet_owner: profile for pet owners
CREATE TABLE IF NOT EXISTS pet_owner (
    user_id         VARCHAR(26)     PRIMARY KEY REFERENCES user_auth(user_id) ON DELETE CASCADE,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    address1        VARCHAR(255),
    address2        VARCHAR(255),
    pincode         VARCHAR(10),
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(100),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
