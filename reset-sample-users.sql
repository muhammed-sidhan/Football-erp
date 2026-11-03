USE football_tournament_db;

-- Resets the documented classroom accounts to password: password.
UPDATE users
SET password = '$2a$10$L1R6CwvwdTLqxwjHRfGod.M5dxuEI11wFXVM7GfNedUaEqdASdP2y'
WHERE username IN ('admin', 'coadmin');

SELECT username, role FROM users WHERE username IN ('admin', 'coadmin');
