CREATE DATABASE IF NOT EXISTS chess CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use chess;
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    elo_rating INT DEFAULT 1200,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);
CREATE TABLE games (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NULL,  -- NULL nếu PVE (đấu với AI)
    is_pve BOOLEAN DEFAULT 0,
    winner_id INT NULL,   -- ID người thắng, hoặc NULL nếu hòa
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    moves TEXT NULL,      -- Lưu lịch sử nước đi (có thể dùng PGN hoặc JSON)
    elo_change_player1 INT DEFAULT 0,
    elo_change_player2 INT DEFAULT 0,
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE moves (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    move_number INT NOT NULL,
    player_id INT NOT NULL,
    from_position VARCHAR(2) NOT NULL,
    to_position VARCHAR(2) NOT NULL,
    piece_type VARCHAR(20) NOT NULL,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE
);
DELIMITER //
CREATE TRIGGER update_elo_after_game
AFTER INSERT ON games
FOR EACH ROW
BEGIN
    IF NEW.elo_change_player1 != 0 THEN
        UPDATE users SET elo_rating = elo_rating + NEW.elo_change_player1 WHERE id = NEW.player1_id;
    END IF;
    IF NEW.player2_id IS NOT NULL AND NEW.elo_change_player2 != 0 THEN
        UPDATE users SET elo_rating = elo_rating + NEW.elo_change_player2 WHERE id = NEW.player2_id;
    END IF;
END;
//
DELIMITER ;
select * from users