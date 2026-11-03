USE football_tournament_db;

-- Resets the documented classroom accounts to password: password.
UPDATE users
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE username IN ('admin', 'coadmin');

SELECT username, role FROM users WHERE username IN ('admin', 'coadmin');
