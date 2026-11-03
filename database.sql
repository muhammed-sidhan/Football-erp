-- Football Tournament Management System
-- MySQL 8+

CREATE DATABASE IF NOT EXISTS football_tournament_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE football_tournament_db;

CREATE TABLE IF NOT EXISTS tournaments (
    tournament_id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_name VARCHAR(120) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location VARCHAR(150) NOT NULL,
    status ENUM('UPCOMING', 'ONGOING', 'COMPLETED') NOT NULL DEFAULT 'UPCOMING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tournament_dates CHECK (end_date >= start_date),
    INDEX idx_tournaments_status (status),
    INDEX idx_tournaments_dates (start_date, end_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS teams (
    team_id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_id INT NOT NULL,
    team_name VARCHAR(120) NOT NULL,
    logo VARCHAR(255),
    description TEXT,
    coach_name VARCHAR(120),
    registration_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_teams_tournament FOREIGN KEY (tournament_id)
        REFERENCES tournaments (tournament_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_team_name_per_tournament UNIQUE (tournament_id, team_name),
    INDEX idx_teams_tournament_status (tournament_id, registration_status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'CO_ADMIN') NOT NULL,
    team_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT fk_users_team FOREIGN KEY (team_id)
        REFERENCES teams (team_id) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_users_role (role),
    INDEX idx_users_team (team_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS players (
    player_id INT AUTO_INCREMENT PRIMARY KEY,
    team_id INT NOT NULL,
    player_name VARCHAR(120) NOT NULL,
    age TINYINT UNSIGNED NOT NULL,
    jersey_number TINYINT UNSIGNED NOT NULL,
    position ENUM('Goalkeeper', 'Defender', 'Midfielder', 'Forward') NOT NULL,
    registration_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_team FOREIGN KEY (team_id)
        REFERENCES teams (team_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_jersey_per_team UNIQUE (team_id, jersey_number),
    CONSTRAINT chk_player_age CHECK (age BETWEEN 5 AND 70),
    INDEX idx_players_team_status (team_id, registration_status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_id INT NOT NULL,
    home_team_id INT NOT NULL,
    away_team_id INT NOT NULL,
    match_date DATE NOT NULL,
    match_time TIME NOT NULL,
    venue VARCHAR(150) NOT NULL,
    home_score TINYINT UNSIGNED NOT NULL DEFAULT 0,
    away_score TINYINT UNSIGNED NOT NULL DEFAULT 0,
    status ENUM('UPCOMING', 'LIVE', 'COMPLETED') NOT NULL DEFAULT 'UPCOMING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_tournament FOREIGN KEY (tournament_id)
        REFERENCES tournaments (tournament_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_matches_home_team FOREIGN KEY (home_team_id)
        REFERENCES teams (team_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_matches_away_team FOREIGN KEY (away_team_id)
        REFERENCES teams (team_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_matches_tournament_date (tournament_id, match_date, match_time),
    INDEX idx_matches_status (status)
) ENGINE=InnoDB;

-- MySQL does not allow this CHECK constraint on columns that participate in
-- foreign keys with referential actions. Enforce the same rule with triggers.
DROP TRIGGER IF EXISTS chk_matches_different_teams_insert;
DROP TRIGGER IF EXISTS chk_matches_different_teams_update;

DELIMITER $$

CREATE TRIGGER chk_matches_different_teams_insert
BEFORE INSERT ON matches
FOR EACH ROW
BEGIN
    IF NEW.home_team_id = NEW.away_team_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A match must have different home and away teams';
    END IF;
END$$

CREATE TRIGGER chk_matches_different_teams_update
BEFORE UPDATE ON matches
FOR EACH ROW
BEGIN
    IF NEW.home_team_id = NEW.away_team_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A match must have different home and away teams';
    END IF;
END$$

DELIMITER ;

-- Sample records. INSERT IGNORE keeps this script safe to run more than once.
INSERT IGNORE INTO tournaments
    (tournament_id, tournament_name, description, start_date, end_date, location, status)
VALUES
    (1, 'College Football League 2026', 'A friendly inter-college football league.',
     '2026-12-01', '2026-12-15', 'University Sports Ground', 'UPCOMING');

INSERT IGNORE INTO teams
    (team_id, tournament_id, team_name, logo, description, coach_name, registration_status)
VALUES
    (1, 1, 'Campus United', NULL, 'The home college football team.', 'Rahul Sharma', 'APPROVED'),
    (2, 1, 'Tech Strikers', NULL, 'A student football team from the technology department.', 'Anita Verma', 'APPROVED');

-- BCrypt hash for the sample password: password
INSERT INTO users (user_id, username, password, role, team_id)
VALUES
    (1, 'admin', '$2a$10$L1R6CwvwdTLqxwjHRfGod.M5dxuEI11wFXVM7GfNedUaEqdASdP2y', 'ADMIN', NULL),
    (2, 'coadmin', '$2a$10$L1R6CwvwdTLqxwjHRfGod.M5dxuEI11wFXVM7GfNedUaEqdASdP2y', 'CO_ADMIN', 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role),
    team_id = VALUES(team_id);

INSERT IGNORE INTO players
    (player_id, team_id, player_name, age, jersey_number, position, registration_status)
VALUES
    (1, 1, 'Arjun Mehta', 20, 10, 'Forward', 'APPROVED'),
    (2, 2, 'Karan Singh', 21, 1, 'Goalkeeper', 'APPROVED');

INSERT IGNORE INTO matches
    (match_id, tournament_id, home_team_id, away_team_id, match_date, match_time, venue, home_score, away_score, status)
VALUES
    (1, 1, 1, 2, '2026-12-03', '16:00:00', 'University Sports Ground', 0, 0, 'UPCOMING');
