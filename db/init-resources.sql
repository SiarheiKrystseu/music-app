BEGIN;
CREATE SEQUENCE resource_sequence START WITH 1;
COMMIT;

BEGIN;
CREATE TABLE IF NOT EXISTS resources (
    id BIGINT PRIMARY KEY DEFAULT nextval('resource_sequence'),
    location VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
COMMIT;
INSERT INTO resources (location) VALUES
('http://localhost:4510/bucket-name/file1.mp3'),
('http://localhost:4510/bucket-name/file2.mp3'),
('http://localhost:4510/bucket-name/file3.mp3');