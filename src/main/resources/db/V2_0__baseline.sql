CREATE
EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;



CREATE TABLE agreement_reg_v1
(
    id                 uuid                   NOT NULL,
    draft_status       character varying(32)  NOT NULL,
    agreement_status   character varying(32)  NOT NULL,
    title              text,
    reference          character varying(255) NOT NULL,
    created            timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated            timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published          timestamp without time zone NOT NULL,
    expired            timestamp without time zone NOT NULL,
    created_by_user    character varying(255) NOT NULL,
    updated_by_user    character varying(255) NOT NULL,
    created_by         character varying(255) NOT NULL,
    updated_by         character varying(255) NOT NULL,
    agreement_data     jsonb                  NOT NULL,
    version            bigint                 NOT NULL,
    previous_agreement uuid
);



CREATE TABLE archive_v1
(
    id               uuid                   NOT NULL,
    oid              uuid                   NOT NULL,
    keywords         text DEFAULT ''::text NOT NULL,
    payload          text DEFAULT '{}'::text NOT NULL,
    type             character varying(255) NOT NULL,
    status           character varying(32)  NOT NULL,
    created          timestamp without time zone NOT NULL,
    dispose_time     timestamp without time zone NOT NULL,
    archived_by_user character varying(255) NOT NULL
);



CREATE TABLE bestillingsordning_reg_v1
(
    id              uuid                   NOT NULL,
    hms_art_nr      character varying(255) NOT NULL,
    navn            character varying(512) NOT NULL,
    status          character varying(32)  NOT NULL,
    updated_by_user character varying(255) NOT NULL,
    created_by_user character varying(255) NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deactivated     timestamp without time zone
);



CREATE TABLE catalog_file_v1
(
    id              uuid                                 NOT NULL,
    file_name       character varying(255)               NOT NULL,
    file_size       bigint                               NOT NULL,
    catalog_list    jsonb                                NOT NULL,
    supplier_id     uuid                                 NOT NULL,
    updated_by_user character varying(255)               NOT NULL,
    created         timestamp without time zone NOT NULL,
    updated         timestamp without time zone NOT NULL,
    status          character varying(32)                NOT NULL,
    order_ref       character varying(255) DEFAULT '0'::character varying NOT NULL,
    connected       boolean                DEFAULT false NOT NULL
);



CREATE TABLE catalog_import_v1
(
    id                uuid                   NOT NULL,
    agreement_action  character varying(32)  NOT NULL,
    order_ref         character varying(255) NOT NULL,
    hms_art_nr        character varying(255) NOT NULL,
    iso               character varying(32)  NOT NULL,
    title             text                   NOT NULL,
    supplier_ref      character varying(255) NOT NULL,
    reference         character varying(255) NOT NULL,
    post_nr           character varying(255) NOT NULL,
    date_from         timestamp without time zone NOT NULL,
    date_to           timestamp without time zone NOT NULL,
    article_action    character varying(32)  NOT NULL,
    article_type      character varying(32)  NOT NULL,
    functional_change character varying(32)  NOT NULL,
    for_children      character varying(32)  NOT NULL,
    supplier_name     character varying(255) NOT NULL,
    supplier_city     character varying(255) NOT NULL,
    main_product      boolean                NOT NULL,
    spare_part        boolean                NOT NULL,
    accessory         boolean                NOT NULL,
    created           timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated           timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    agreement_id      uuid DEFAULT '00000000-0000-0000-0000-000000000000'::uuid NOT NULL,
    supplier_id       uuid                   NOT NULL
);



CREATE TABLE delkontrakt_reg_v1
(
    id               uuid                   NOT NULL,
    agreement_id     uuid                   NOT NULL,
    identifier       character varying(255) NOT NULL,
    delkontrakt_data jsonb                  NOT NULL,
    created_by       character varying(32),
    updated_by       character varying(32),
    updated          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type             character varying(32) DEFAULT 'WITH_DELKONTRAKT'::character varying NOT NULL
);



CREATE TABLE digitalsoknadsortiment_reg_v1
(
    id                 uuid                   NOT NULL,
    sortiment_kategori character varying(255) NOT NULL,
    post_id            uuid                   NOT NULL,
    status             character varying(32)  NOT NULL,
    updated_by_user    character varying(255) NOT NULL,
    created_by_user    character varying(255) NOT NULL,
    created            timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated            timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deactivated        timestamp without time zone
);



CREATE TABLE event_item_v1
(
    event_id         uuid                   NOT NULL,
    oid              uuid                   NOT NULL,
    type             character varying(32)  NOT NULL,
    status           character varying(32)  NOT NULL,
    event_name       character varying(255) NOT NULL,
    by_user          character varying(255) NOT NULL,
    extra_key_values jsonb                  NOT NULL,
    payload          text                   NOT NULL,
    created          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE TABLE hm_dead_letter_v1
(
    event_id   character varying(255) NOT NULL,
    event_name character varying(255) NOT NULL,
    "json"     text                   NOT NULL,
    error      text                   NOT NULL,
    created    timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    topic      character varying(255) NOT NULL,
    river_name character varying(255) NOT NULL
);



CREATE TABLE isocategory_reg_v1
(
    iso_code         character varying(32)   NOT NULL,
    iso_title        character varying(1024) NOT NULL,
    iso_text         text                    NOT NULL,
    iso_text_short   text                    NOT NULL,
    iso_translations jsonb                   NOT NULL,
    iso_level        integer                 NOT NULL,
    is_active        boolean                 NOT NULL,
    show_tech        boolean                 NOT NULL,
    allow_multi      boolean                 NOT NULL,
    created_by_user  character varying(255)  NOT NULL,
    updated_by_user  character varying(255)  NOT NULL,
    created_by       character varying(32)   NOT NULL,
    updated_by       character varying(32)   NOT NULL,
    created          timestamp without time zone NOT NULL,
    updated          timestamp without time zone NOT NULL,
    search_words     jsonb DEFAULT '[]'::jsonb NOT NULL,
    iso_title_short  text
);



CREATE TABLE news_reg_v1
(
    id              uuid                   NOT NULL,
    title           character varying(512) NOT NULL,
    text            text                   NOT NULL,
    status          character varying(32)  NOT NULL,
    draft_status    character varying(32)  NOT NULL,
    published       timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expired         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    author          character varying(255) NOT NULL,
    created_by      character varying(255) NOT NULL,
    updated_by      character varying(255) NOT NULL,
    created_by_user character varying(255) NOT NULL,
    updated_by_user character varying(255) NOT NULL
);



CREATE TABLE otp_v1
(
    id      uuid                   NOT NULL,
    otp     character varying(255) NOT NULL,
    email   character varying(255) NOT NULL,
    used    boolean                NOT NULL,
    created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE TABLE paakrevdgodkjenningskurs_reg_v1
(
    id              uuid                   NOT NULL,
    isokode         character varying(255) NOT NULL,
    tittel          character varying(255) NOT NULL,
    kurs_id         numeric                NOT NULL,
    status          character varying(32)  NOT NULL,
    updated_by_user character varying(255) NOT NULL,
    created_by_user character varying(255) NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deactivated     timestamp without time zone
);



CREATE TABLE product_agreement_reg_v1
(
    id           uuid                                NOT NULL,
    product_id   uuid,
    title        character varying(1024)             NOT NULL,
    supplier_id  uuid                                NOT NULL,
    supplier_ref character varying(255)              NOT NULL,
    hms_artnr    character varying(255)              NOT NULL,
    agreement_id uuid                                NOT NULL,
    reference    character varying(255)              NOT NULL,
    post         integer                             NOT NULL,
    rank         integer                             NOT NULL,
    status       character varying(32)               NOT NULL,
    created_by   character varying(255)              NOT NULL,
    created      timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated      timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published    timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expired      timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    article_name character varying(1024),
    series_uuid  uuid,
    post_id      uuid                                NOT NULL,
    updated_by   character varying(64) DEFAULT 'HMDB'::character varying NOT NULL,
    spare_part   boolean               DEFAULT false NOT NULL,
    accessory    boolean               DEFAULT false NOT NULL,
    main_product boolean               DEFAULT true  NOT NULL
);



CREATE TABLE product_reg_v1
(
    id                  uuid                    NOT NULL,
    supplier_id         uuid                    NOT NULL,
    supplier_ref        character varying(255)  NOT NULL,
    series_id           character varying(255)  NOT NULL,
    iso_category        character varying(255)  NOT NULL,
    hms_artnr           character varying(255),
    title               character varying(1024) NOT NULL,
    article_name        character varying(1024) NOT NULL,
    draft_status        character varying(32)   NOT NULL,
    admin_status        character varying(32)   NOT NULL,
    registration_status character varying(32)   NOT NULL,
    message             text,
    admin_info          jsonb,
    created             timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated             timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published           timestamp without time zone,
    expired             timestamp without time zone NOT NULL,
    created_by_user     character varying(255)  NOT NULL,
    updated_by_user     character varying(255)  NOT NULL,
    created_by          character varying(64)   NOT NULL,
    updated_by          character varying(64)   NOT NULL,
    created_by_admin    boolean                 NOT NULL,
    product_data        jsonb                   NOT NULL,
    version             bigint                  NOT NULL,
    series_uuid         uuid                    NOT NULL,
    spare_part          boolean DEFAULT false   NOT NULL,
    accessory           boolean DEFAULT false   NOT NULL,
    main_product        boolean DEFAULT true    NOT NULL
);



CREATE TABLE product_reg_version_v1
(
    version_id           uuid                  NOT NULL,
    product_id           uuid                  NOT NULL,
    status               character varying(32) NOT NULL,
    admin_status         character varying(32) NOT NULL,
    draft_status         character varying(32) NOT NULL,
    updated              timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    product_registration jsonb                 NOT NULL,
    version              bigint                NOT NULL,
    updated_by           character varying(64) DEFAULT 'system'::character varying NOT NULL
);



CREATE TABLE produkttype_reg_v1
(
    id              uuid                   NOT NULL,
    isokode         character varying(255) NOT NULL,
    produkttype     character varying(255) NOT NULL,
    status          character varying(32)  NOT NULL,
    updated_by_user character varying(255) NOT NULL,
    created_by_user character varying(255) NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deactivated     timestamp without time zone
);



CREATE TABLE series_reg_v1
(
    id               uuid                                 NOT NULL,
    supplier_id      uuid                                 NOT NULL,
    identifier       character varying(255)               NOT NULL,
    title            character varying(1024)              NOT NULL,
    draft_status     character varying(32)                NOT NULL,
    status           character varying(32)                NOT NULL,
    created          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by_user  character varying(255)               NOT NULL,
    updated_by_user  character varying(255)               NOT NULL,
    created_by       character varying(64)                NOT NULL,
    updated_by       character varying(64)                NOT NULL,
    created_by_admin boolean                              NOT NULL,
    version          bigint                               NOT NULL,
    expired          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    text             text                    DEFAULT ''::text NOT NULL,
    iso_category     character varying(255)  DEFAULT ''::character varying NOT NULL,
    admin_status     character varying(255)  DEFAULT 'APPROVED'::character varying NOT NULL,
    series_data      jsonb                   DEFAULT '{}'::jsonb NOT NULL,
    count            integer                 DEFAULT 0,
    count_drafts     integer                 DEFAULT 0,
    count_published  integer                 DEFAULT 0,
    count_pending    integer                 DEFAULT 0,
    count_declined   integer                 DEFAULT 0,
    title_lowercase  character varying(1024) DEFAULT ''::character varying NOT NULL,
    formatted_text   text,
    published        timestamp without time zone,
    message          text,
    main_product     boolean                 DEFAULT true NOT NULL
);



CREATE TABLE series_reg_version_v1
(
    version_id          uuid                  NOT NULL,
    series_id           uuid                  NOT NULL,
    status              character varying(32) NOT NULL,
    admin_status        character varying(32) NOT NULL,
    draft_status        character varying(32) NOT NULL,
    updated             timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    series_registration jsonb                 NOT NULL,
    version             bigint                NOT NULL,
    updated_by          character varying(64) DEFAULT 'system'::character varying NOT NULL
);



CREATE TABLE supplier_reg_v1
(
    id              uuid                   NOT NULL,
    name            character varying(255) NOT NULL,
    status          character varying(32)  NOT NULL,
    supplier_data   jsonb                  NOT NULL,
    identifier      character varying(128) NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by_user character varying(255) NOT NULL,
    created_by_user character varying(255) NOT NULL,
    created_by      character varying(64)  NOT NULL,
    updated_by      character varying(64)  NOT NULL
);



CREATE TABLE techlabel_reg_v1
(
    id              uuid                    NOT NULL,
    identifier      character varying(255)  NOT NULL,
    label           character varying(255)  NOT NULL,
    guide           character varying(1024) NOT NULL,
    iso_code        character varying(255)  NOT NULL,
    type            character varying(32)   NOT NULL,
    unit            character varying(64),
    is_active       boolean                 NOT NULL,
    created_by_user character varying(255)  NOT NULL,
    updated_by_user character varying(255)  NOT NULL,
    created_by      character varying(32)   NOT NULL,
    updated_by      character varying(32)   NOT NULL,
    updated         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created         timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sort            integer DEFAULT 0       NOT NULL,
    definition      character varying(512),
    options         jsonb   DEFAULT '[]'::jsonb,
    is_key_label    boolean DEFAULT false   NOT NULL
);



CREATE TABLE user_v1
(
    id         uuid                   NOT NULL,
    name       character varying(255) NOT NULL,
    email      character varying(255) NOT NULL,
    roles      jsonb                  NOT NULL,
    attributes jsonb                  NOT NULL,
    token      text                   NOT NULL,
    created    timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated    timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



ALTER TABLE ONLY agreement_reg_v1
    ADD CONSTRAINT agreement_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY agreement_reg_v1
    ADD CONSTRAINT agreement_reg_v1_reference_key UNIQUE (reference);



ALTER TABLE ONLY archive_v1
    ADD CONSTRAINT archive_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY bestillingsordning_reg_v1
    ADD CONSTRAINT bestillingsordning_reg_v1_hms_art_nr_key UNIQUE (hms_art_nr);



ALTER TABLE ONLY bestillingsordning_reg_v1
    ADD CONSTRAINT bestillingsordning_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY catalog_file_v1
    ADD CONSTRAINT catalog_file_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY catalog_import_v1
    ADD CONSTRAINT catalog_import_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY delkontrakt_reg_v1
    ADD CONSTRAINT delkontrakt_reg_v1_identifier_key UNIQUE (identifier);



ALTER TABLE ONLY delkontrakt_reg_v1
    ADD CONSTRAINT delkontrakt_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY digitalsoknadsortiment_reg_v1
    ADD CONSTRAINT digitalsoknadsortiment_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY digitalsoknadsortiment_reg_v1
    ADD CONSTRAINT digitalsoknadsortiment_reg_v1_sortiment_kategori_post_id_key UNIQUE (sortiment_kategori, post_id);



ALTER TABLE ONLY event_item_v1
    ADD CONSTRAINT event_item_v1_pkey PRIMARY KEY (event_id);



ALTER TABLE ONLY hm_dead_letter_v1
    ADD CONSTRAINT hm_dead_letter_v1_pkey PRIMARY KEY (event_id);



ALTER TABLE ONLY isocategory_reg_v1
    ADD CONSTRAINT isocategory_reg_v1_pkey PRIMARY KEY (iso_code);



ALTER TABLE ONLY news_reg_v1
    ADD CONSTRAINT news_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY otp_v1
    ADD CONSTRAINT otp_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY paakrevdgodkjenningskurs_reg_v1
    ADD CONSTRAINT paakrevdgodkjenningskurs_reg_v1_isokode_key UNIQUE (isokode);



ALTER TABLE ONLY paakrevdgodkjenningskurs_reg_v1
    ADD CONSTRAINT paakrevdgodkjenningskurs_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY product_agreement_reg_v1
    ADD CONSTRAINT product_agreement_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY product_agreement_reg_v1
    ADD CONSTRAINT product_agreement_reg_v1_supplierid_supplierref_agreementid_pos UNIQUE (supplier_id, supplier_ref, agreement_id, post_id);



ALTER TABLE ONLY product_reg_v1
    ADD CONSTRAINT product_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY product_reg_v1
    ADD CONSTRAINT product_reg_v1_supplier_id_supplier_ref_key UNIQUE (supplier_id, supplier_ref);



ALTER TABLE ONLY product_reg_version_v1
    ADD CONSTRAINT product_reg_version_v1_pkey PRIMARY KEY (version_id);



ALTER TABLE ONLY produkttype_reg_v1
    ADD CONSTRAINT produkttype_reg_v1_isokode_key UNIQUE (isokode);



ALTER TABLE ONLY produkttype_reg_v1
    ADD CONSTRAINT produkttype_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY series_reg_v1
    ADD CONSTRAINT series_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY series_reg_version_v1
    ADD CONSTRAINT series_reg_version_v1_pkey PRIMARY KEY (version_id);



ALTER TABLE ONLY supplier_reg_v1
    ADD CONSTRAINT supplier_reg_v1_identifier_key UNIQUE (identifier);



ALTER TABLE ONLY supplier_reg_v1
    ADD CONSTRAINT supplier_reg_v1_name_key UNIQUE (name);



ALTER TABLE ONLY supplier_reg_v1
    ADD CONSTRAINT supplier_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY techlabel_reg_v1
    ADD CONSTRAINT techlabel_reg_v1_pkey PRIMARY KEY (id);



ALTER TABLE ONLY user_v1
    ADD CONSTRAINT user_v1_email_key UNIQUE (email);



ALTER TABLE ONLY user_v1
    ADD CONSTRAINT user_v1_pkey PRIMARY KEY (id);



CREATE INDEX agreement_reg_v1_draft_status_idx ON agreement_reg_v1 USING btree (draft_status);



CREATE INDEX agreement_reg_v1_title_idx ON agreement_reg_v1 USING btree (title);



CREATE INDEX agreement_reg_v1_updated_idx ON agreement_reg_v1 USING btree (updated);



CREATE INDEX bestillingsordning_hms_art_nr_index ON bestillingsordning_reg_v1 USING btree (hms_art_nr);



CREATE INDEX catalog_import_v1_hms_art_nr_idx ON catalog_import_v1 USING btree (hms_art_nr);



CREATE INDEX catalog_import_v1_order_ref_idx ON catalog_import_v1 USING btree (order_ref);



CREATE UNIQUE INDEX catalog_import_v1_order_ref_supplier_ref_idx ON catalog_import_v1 USING btree (order_ref, supplier_ref);



CREATE INDEX delkontrakt_reg_v1_agreement_id_idx ON delkontrakt_reg_v1 USING btree (agreement_id);



CREATE INDEX event_item_v1_oid_idx ON event_item_v1 USING btree (oid);



CREATE INDEX event_item_v1_status_idx ON event_item_v1 USING btree (status);



CREATE INDEX idx_archive_v1_keywords ON archive_v1 USING btree (keywords);



CREATE INDEX idx_archive_v1_oid ON archive_v1 USING btree (oid);



CREATE INDEX idx_archive_v1_status ON archive_v1 USING btree (status);



CREATE INDEX news_reg_v1_draft_status_idx ON news_reg_v1 USING btree (draft_status);



CREATE INDEX news_reg_v1_status_idx ON news_reg_v1 USING btree (status);



CREATE INDEX news_reg_v1_updated_idx ON news_reg_v1 USING btree (updated);



CREATE INDEX otp_v1_email_ref_idx ON otp_v1 USING btree (email);



CREATE INDEX product_agreement_hms_artnr_index ON product_agreement_reg_v1 USING btree (hms_artnr);



CREATE INDEX product_agreement_product_id_index ON product_agreement_reg_v1 USING btree (product_id);



CREATE INDEX product_agreement_reg_v1_agreement_id_idx ON product_agreement_reg_v1 USING btree (agreement_id);



CREATE INDEX product_agreement_reg_v1_series_uuid_idx ON product_agreement_reg_v1 USING btree (series_uuid);



CREATE INDEX product_agreement_reg_v1_supplier_id_supplier_ref_idx ON product_agreement_reg_v1 USING btree (supplier_id, supplier_ref);



CREATE INDEX product_agreement_status_index ON product_agreement_reg_v1 USING btree (status);



CREATE INDEX product_reg_v1_admin_status_idx ON product_reg_v1 USING btree (admin_status);



CREATE INDEX product_reg_v1_created_by_user_idx ON product_reg_v1 USING btree (created_by_user);



CREATE INDEX product_reg_v1_draft_status_idx ON product_reg_v1 USING btree (draft_status);



CREATE INDEX product_reg_v1_hms_artnr_idx ON product_reg_v1 USING btree (hms_artnr);



CREATE INDEX product_reg_v1_series_id_idx ON product_reg_v1 USING btree (series_id);



CREATE INDEX product_reg_v1_series_uuid_idx ON product_reg_v1 USING btree (series_uuid);



CREATE INDEX product_reg_v1_supplier_id_idx ON product_reg_v1 USING btree (supplier_id);



CREATE INDEX product_reg_v1_updated_by_user_idx ON product_reg_v1 USING btree (updated_by_user);



CREATE INDEX product_reg_v1_updated_idx ON product_reg_v1 USING btree (updated);



CREATE INDEX product_reg_version_v1_product_admin_status_draft_status_id_idx ON product_reg_version_v1 USING btree (product_id, admin_status, draft_status);



CREATE INDEX product_reg_version_v1_product_id_idx ON product_reg_version_v1 USING btree (product_id);



CREATE INDEX product_reg_version_v1_updated_idx ON product_reg_version_v1 USING btree (updated);



CREATE INDEX series_reg_version_v1_series_admin_status_draft_status_id_idx ON series_reg_version_v1 USING btree (series_id, admin_status, draft_status);



CREATE INDEX series_reg_version_v1_series_id_idx ON series_reg_version_v1 USING btree (series_id);



CREATE INDEX series_reg_version_v1_updated_idx ON series_reg_version_v1 USING btree (updated);



ALTER TABLE ONLY product_agreement_reg_v1
    ADD CONSTRAINT fk_post_id FOREIGN KEY (post_id) REFERENCES delkontrakt_reg_v1(id) ON
DELETE
CASCADE;



ALTER TABLE ONLY product_agreement_reg_v1
    ADD CONSTRAINT fk_product_reg_v1_product_agreement_reg_v1 FOREIGN KEY (product_id) REFERENCES product_reg_v1(id) ON
DELETE
RESTRICT;






