
ALTER TABLE operators DROP COLUMN IF EXISTS operator_type;
ALTER TABLE operators DROP COLUMN IF EXISTS company_id;

ALTER TABLE operators
    ADD COLUMN company_id BIGINT,
    ADD COLUMN operator_type VARCHAR(20);

UPDATE operators SET operator_type = 'SYSTEM' WHERE operator_type IS NULL;

ALTER TABLE operators
    ALTER COLUMN operator_type SET NOT NULL;

ALTER TABLE operators
    ADD CONSTRAINT fk_operator_company
        FOREIGN KEY (company_id) REFERENCES companies(id);
