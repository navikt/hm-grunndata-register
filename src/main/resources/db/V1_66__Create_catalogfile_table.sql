CREATE table catalog_file_v1
(
    id UUID PRIMARY KEY,
    file_name VARCHAR (255) NOT NULL,
    file_size BIGINT NOT NULL,
    catalog_list JSONB NOT NULL,
    supplier_id UUID NOT NULL,
    created_by VARCHAR (255) NOT NULL,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    status VARCHAR (32) NOT NULL
);