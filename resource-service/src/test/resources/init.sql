CREATE SEQUENCE resource_sequence START 1;

CREATE TABLE resources (
    id BIGINT NOT NULL DEFAULT nextval('resource_sequence'),
    data oid,
    PRIMARY KEY (id)
);

INSERT INTO resources (data) VALUES
(1::oid),
(2::oid),
(3::oid);