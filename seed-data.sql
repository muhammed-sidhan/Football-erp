-- Football Tournament demo data
-- Run database.sql first. This script is safe to run repeatedly.

USE football_tournament_db;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO tournaments
    (tournament_id, tournament_name, description, start_date, end_date, location, status)
VALUES
    (1, 'College Football League 2026', 'A friendly inter-college league featuring local student teams.', '2026-12-01', '2026-12-15', 'University Sports Ground', 'UPCOMING'),
    (2, 'Winter Champions Cup', 'A short knockout competition for the strongest community teams.', '2026-11-01', '2026-11-30', 'City Stadium', 'ONGOING'),
    (3, 'Spring Youth Challenge 2027', 'A youth development tournament focused on fair play and teamwork.', '2027-03-10', '2027-03-20', 'Northside Athletic Park', 'UPCOMING')
ON DUPLICATE KEY UPDATE
    tournament_name = VALUES(tournament_name), description = VALUES(description),
    start_date = VALUES(start_date), end_date = VALUES(end_date),
    location = VALUES(location), status = VALUES(status);

INSERT INTO teams
    (team_id, tournament_id, team_name, logo, description, coach_name, registration_status)
VALUES
    (1, 1, 'Campus United', 'https://placehold.co/96x96/0d2a4d/b7f36b?text=CU', 'The home college football team with a fast passing style.', 'Rahul Sharma', 'APPROVED'),
    (2, 1, 'Tech Strikers', 'https://placehold.co/96x96/16a34a/ffffff?text=TS', 'A student football team from the technology department.', 'Anita Verma', 'APPROVED'),
    (3, 1, 'Arts Athletic', 'https://placehold.co/96x96/7c3aed/ffffff?text=AA', 'Creative players who bring energy and flair to every match.', 'Vikram Joshi', 'APPROVED'),
    (4, 2, 'Metro Rangers', 'https://placehold.co/96x96/ea580c/ffffff?text=MR', 'A disciplined community side built around a strong defence.', 'Sanjay Rao', 'APPROVED'),
    (5, 2, 'Harbour FC', 'https://placehold.co/96x96/0284c7/ffffff?text=HF', 'A coastal club known for quick counter-attacks.', 'Meera Nair', 'APPROVED'),
    (6, 3, 'Young Lions', 'https://placehold.co/96x96/f59e0b/071a33?text=YL', 'An emerging youth team gaining tournament experience.', 'Priya Kapoor', 'PENDING')
ON DUPLICATE KEY UPDATE
    tournament_id = VALUES(tournament_id), team_name = VALUES(team_name), logo = VALUES(logo),
    description = VALUES(description), coach_name = VALUES(coach_name),
    registration_status = VALUES(registration_status);

-- BCrypt hash for the sample password: password
INSERT INTO users (user_id, username, password, role, team_id)
VALUES
    (1, 'admin', '$2a$10$L1R6CwvwdTLqxwjHRfGod.M5dxuEI11wFXVM7GfNedUaEqdASdP2y', 'ADMIN', NULL),
    (2, 'coadmin', '$2a$10$L1R6CwvwdTLqxwjHRfGod.M5dxuEI11wFXVM7GfNedUaEqdASdP2y', 'CO_ADMIN', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username), password = VALUES(password), role = VALUES(role), team_id = VALUES(team_id);

INSERT INTO players
    (player_id, team_id, player_name, age, jersey_number, position, registration_status)
VALUES
    (1, 1, 'Arjun Mehta', 20, 10, 'Forward', 'APPROVED'),
    (2, 1, 'Dev Malhotra', 21, 4, 'Defender', 'APPROVED'),
    (3, 1, 'Ishaan Roy', 19, 8, 'Midfielder', 'APPROVED'),
    (4, 2, 'Karan Singh', 21, 1, 'Goalkeeper', 'APPROVED'),
    (5, 2, 'Aarav Shah', 20, 9, 'Forward', 'APPROVED'),
    (6, 2, 'Rohan Das', 22, 5, 'Defender', 'PENDING'),
    (7, 3, 'Kabir Khan', 19, 7, 'Forward', 'APPROVED'),
    (8, 3, 'Nikhil Bose', 20, 6, 'Midfielder', 'APPROVED'),
    (9, 3, 'Aditya Sen', 21, 2, 'Defender', 'APPROVED'),
    (10, 4, 'Vivek Patil', 26, 1, 'Goalkeeper', 'APPROVED'),
    (11, 4, 'Manav Kulkarni', 27, 11, 'Forward', 'APPROVED'),
    (12, 4, 'Soham Iyer', 24, 3, 'Defender', 'APPROVED'),
    (13, 5, 'Ayaan Fernandes', 25, 10, 'Forward', 'APPROVED'),
    (14, 5, 'Neil Thomas', 24, 8, 'Midfielder', 'APPROVED'),
    (15, 5, 'Joel Dsouza', 26, 4, 'Defender', 'APPROVED'),
    (16, 6, 'Yash Gupta', 16, 9, 'Forward', 'PENDING'),
    (17, 6, 'Ritvik Jain', 15, 1, 'Goalkeeper', 'PENDING'),
    (18, 6, 'Aman Bhat', 16, 6, 'Midfielder', 'PENDING')
ON DUPLICATE KEY UPDATE
    team_id = VALUES(team_id), player_name = VALUES(player_name), age = VALUES(age),
    jersey_number = VALUES(jersey_number), position = VALUES(position),
    registration_status = VALUES(registration_status);

INSERT INTO matches
    (match_id, tournament_id, home_team_id, away_team_id, match_date, match_time, venue, home_score, away_score, status)
VALUES
    (1, 1, 1, 2, '2026-12-03', '16:00:00', 'University Sports Ground', 0, 0, 'UPCOMING'),
    (2, 1, 3, 1, '2026-12-06', '15:30:00', 'University Sports Ground', 0, 0, 'UPCOMING'),
    (3, 1, 2, 3, '2026-12-10', '16:30:00', 'University Sports Ground', 0, 0, 'UPCOMING'),
    (4, 2, 4, 5, '2026-11-05', '18:00:00', 'City Stadium', 2, 1, 'COMPLETED'),
    (5, 2, 5, 4, '2026-11-12', '18:00:00', 'City Stadium', 0, 0, 'LIVE'),
    (6, 3, 6, 1, '2027-03-12', '10:00:00', 'Northside Athletic Park', 0, 0, 'UPCOMING'),
    (7, 3, 2, 6, '2027-03-14', '10:00:00', 'Northside Athletic Park', 0, 0, 'UPCOMING'),
    (8, 2, 4, 5, '2026-11-20', '18:30:00', 'City Stadium', 3, 3, 'COMPLETED'),
    (9, 2, 5, 4, '2026-11-27', '18:30:00', 'City Stadium', 1, 0, 'UPCOMING')
ON DUPLICATE KEY UPDATE
    tournament_id = VALUES(tournament_id), home_team_id = VALUES(home_team_id),
    away_team_id = VALUES(away_team_id), match_date = VALUES(match_date),
    match_time = VALUES(match_time), venue = VALUES(venue), home_score = VALUES(home_score),
    away_score = VALUES(away_score), status = VALUES(status);

SELECT 'Seed data loaded' AS result;
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'football_tournament_db'
  AND table_name IN ('tournaments', 'teams', 'users', 'players', 'matches')
ORDER BY table_name;
