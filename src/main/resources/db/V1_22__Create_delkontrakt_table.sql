CREATE TABLE delkontrakt_reg_v1(
    id UUID PRIMARY KEY,
    agreement_id UUID,
    identifier VARCHAR(255),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    sort_nr INTEGER NOT NULL
);
