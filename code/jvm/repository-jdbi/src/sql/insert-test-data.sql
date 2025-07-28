-- Insert sample users
INSERT INTO dbo.users (username, password_validation) VALUES
('alice', 'hashed_password_123'),
('bob', 'hashed_password_456'),
('charlie', 'hashed_password_789');

-- Insert sample channels
INSERT INTO dbo.channels (name, owner_id, type) VALUES
('General', 1, 'PUBLIC'),
('Private Chat', 2, 'PRIVATE'),
('Team Alpha', 3, 'PUBLIC');

-- Insert sample invitations
INSERT INTO dbo.invitations (code, inviter_id, invitee_id, channel_id, created_at, expires_at, used, permission) VALUES
('INVITE123', 1, 2, 1, NOW(), NOW() + INTERVAL '7 days', FALSE, 'READ_WRITE'),
('INVITE456', 2, NULL, 2, NOW(), NOW() + INTERVAL '3 days', FALSE, 'READ_ONLY'),
('INVITE789', 3, 1, 3, NOW(), NOW() + INTERVAL '1 day', TRUE, 'READ_WRITE');

-- Insert sample invitations
INSERT INTO dbo.invitations (code, inviter_id, invitee_id, channel_id, used, permission) VALUES
('INVITE123', 1, 2, 1, FALSE, 'READ_WRITE'),
('INVITE456', 2, NULL, 2, FALSE, 'READ_ONLY'),
('INVITE789', 3, 1, 3, TRUE, 'READ_WRITE');

-- Insert sample messages
INSERT INTO dbo.messages (user_id, channel_id, content, time_stamp) VALUES
(1, 1, 'Hello, world!', NOW()),
(2, 1, 'Hi Alice!', NOW()),
(3, 3, 'Welcome to Team Alpha.', NOW());

-- Insert sample tokens
INSERT INTO dbo.tokens (token_validation, user_id, created_at, last_used_at) VALUES
('token_abc123', 1, EXTRACT(EPOCH FROM NOW()), EXTRACT(EPOCH FROM NOW())),
('token_def456', 2, EXTRACT(EPOCH FROM NOW()), EXTRACT(EPOCH FROM NOW())),
('token_ghi789', 3, EXTRACT(EPOCH FROM NOW()), EXTRACT(EPOCH FROM NOW()));

-- Insert sample participants
INSERT INTO dbo.participants (channel_id, user_id, permission) VALUES
(1, 1, 'READ_WRITE'),
(1, 2, 'READ_WRITE'),
(2, 2, 'READ_WRITE'),
(3, 3, 'READ_WRITE'),
(3, 1, 'READ_ONLY');
