--liquibase formatted sql
--changeset tms:operations-0005-state-code-varchar
--comment: 0004 created state/country code columns as CHAR(2) (bpchar in
--         Postgres) but the entities use @Column(length=2) which Hibernate
--         expects as VARCHAR(2).  Schema-validate refused to start with a
--         type mismatch.  Convert to VARCHAR — data is compatible, and
--         Hibernate stops complaining.

ALTER TABLE operations.company  ALTER COLUMN country_code  TYPE VARCHAR(2);
ALTER TABLE operations.company  ALTER COLUMN state_code    TYPE VARCHAR(2);
ALTER TABLE operations.driver   ALTER COLUMN country_code  TYPE VARCHAR(2);
ALTER TABLE operations.driver   ALTER COLUMN state_code    TYPE VARCHAR(2);
ALTER TABLE operations.driver   ALTER COLUMN license_state TYPE VARCHAR(2);
ALTER TABLE operations.truck    ALTER COLUMN plate_state   TYPE VARCHAR(2);
ALTER TABLE operations.trailer  ALTER COLUMN plate_state   TYPE VARCHAR(2);
