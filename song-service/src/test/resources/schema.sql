CREATE TABLE IF NOT EXISTS songs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    artist VARCHAR(255),
    album VARCHAR(255),
    length VARCHAR(255),
    resource_id INT,
    release_year INT
);