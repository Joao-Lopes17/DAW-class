-- Create the schema dbo
CREATE SCHEMA IF NOT EXISTS dbo;

-- Create table for users in the dbo schema
CREATE TABLE dbo.users
(
    id                      SERIAL PRIMARY KEY,
    username                VARCHAR(255) UNIQUE NOT NULL,
    password_validation     VARCHAR(255)       NOT NULL
);

-- Create table for channels in the dbo schema
CREATE TABLE dbo.channels
(
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    owner_id        INT          NOT NULL,
    type            VARCHAR(10)  NOT NULL CHECK (type IN ('PUBLIC', 'PRIVATE')),
    FOREIGN KEY (owner_id) REFERENCES dbo.users (id)
);

-- Create table for invitations in the dbo schema
CREATE TABLE dbo.invitations
(
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(255) UNIQUE NOT NULL,
    inviter_id      INT NOT NULL,
    invitee_id      INT,
    channel_id      INT,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    permission      VARCHAR(15) NOT NULL CHECK (permission IN ('READ_ONLY', 'READ_WRITE')),
    FOREIGN KEY (inviter_id) REFERENCES dbo.users (id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES dbo.users (id) ON DELETE SET NULL,
    FOREIGN KEY (channel_id) REFERENCES dbo.channels (id) ON DELETE CASCADE
);

-- Create table for messages in the dbo schema
CREATE TABLE dbo.messages
(
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL,
    channel_id      INT NOT NULL,
    content         VARCHAR(255) NOT NULL,
    time_stamp      TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    FOREIGN KEY (channel_id) REFERENCES dbo.channels (id)
);

-- Create table for tokens in the dbo schema
CREATE TABLE dbo.tokens
(
    token_validation VARCHAR(256) PRIMARY KEY,
    user_id          INT REFERENCES dbo.users (id),
    created_at       BIGINT NOT NULL,
    last_used_at     BIGINT NOT NULL
);

-- Create table for participants in the dbo schema
CREATE TABLE dbo.participants
(
    id                    SERIAL PRIMARY KEY,
    channel_id            INT NOT NULL,
    user_id               INT NOT NULL,
    permission            VARCHAR(15) NOT NULL CHECK (permission IN ('READ_ONLY', 'READ_WRITE')),
    FOREIGN KEY (channel_id) REFERENCES dbo.channels (id),
    FOREIGN KEY (user_id) REFERENCES dbo.users (id)
);
