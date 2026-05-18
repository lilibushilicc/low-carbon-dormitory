CREATE TABLE IF NOT EXISTS system_storage_r2_config (
    config_key varchar(64) PRIMARY KEY,
    endpoint varchar(255) NOT NULL,
    access_key_id varchar(255) NOT NULL,
    secret_access_key varchar(255) NOT NULL,
    bucket varchar(255) NOT NULL,
    public_base_url varchar(255) NOT NULL,
    region varchar(50) NOT NULL,
    update_time timestamp without time zone NOT NULL DEFAULT now()
);
