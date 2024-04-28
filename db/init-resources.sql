CREATE SEQUENCE resource_sequence START 1;

CREATE TABLE resources (
    id BIGINT NOT NULL DEFAULT nextval('resource_sequence'),
    data oid,
    PRIMARY KEY (id)
);

INSERT INTO resources(data) VALUES (
    lo_import('/resources/Free_Test_Data_500KB_MP3.mp3')
);

INSERT INTO resources(data) VALUES (
    lo_import('/resources/Free_Test_Data_500KB_MP3.mp3')
);

INSERT INTO resources(data) VALUES (
    lo_import('/resources/Free_Test_Data_500KB_MP3.mp3')
);
