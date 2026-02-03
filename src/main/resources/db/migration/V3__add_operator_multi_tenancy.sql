ALTER TABLE operators
    ADD COLUMN company_id    BIGINT,
    ADD COLUMN operator_type VARCHAR(20) NOT NULL;

UPDATE operators SET operator_type = 'SYSTEM' WHERE operator_type IS NULL;

ALTER TABLE operators
ADD CONSTRAINT fk_operator_company
FOREIGN KEY (company_id) REFERENCES companies(id);


