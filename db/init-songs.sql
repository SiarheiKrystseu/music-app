CREATE TABLE IF NOT EXISTS songs (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    artist VARCHAR(255),
    album VARCHAR(255),
    length VARCHAR(255),
    resource_id VARCHAR(255),
    release_year VARCHAR(255)
);

INSERT INTO songs (name, artist, album, length, resource_id, release_year) VALUES
('Song1', 'Artist1', 'Album1', '3:30', '1', '2001'),
('Song2', 'Artist2', 'Album2', '4:30', '2', '2002'),
('Song3', 'Artist3', 'Album3', '5:30', '3', '2003');