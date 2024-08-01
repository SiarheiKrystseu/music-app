-- Create table
CREATE TABLE IF NOT EXISTS storage (
    id SERIAL PRIMARY KEY,
    storage_type VARCHAR(255) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL
);

-- Insert initial data
INSERT INTO storage (storage_type, bucket, path) VALUES
('STAGING', 'staging-bucket-name', '/files'),
('PERMANENT', 'permanent-bucket-name', '/files');