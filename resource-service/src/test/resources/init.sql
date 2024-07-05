CREATE TABLE resources (
    id BIGINT NOT NULL DEFAULT nextval('resource_sequence'),
    location VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO resources (location) VALUES
('http://localhost:4510/bucket-name/file1.mp3'),
('http://localhost:4510/bucket-name/file2.mp3'),
('http://localhost:4510/bucket-name/file3.mp3');