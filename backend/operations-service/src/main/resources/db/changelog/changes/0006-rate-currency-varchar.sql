--liquibase formatted sql
--changeset tms:operations-0006-rate-currency-varchar
--comment: Same story as 0005: 0002 declared load.rate_currency as CHAR(3)
--         (bpchar), but the entity's @Column(length=3) on a String maps to
--         VARCHAR(3).  Convert; the 'USD' default is unaffected.

ALTER TABLE operations.load ALTER COLUMN rate_currency TYPE VARCHAR(3);
