ALTER TABLE affordable_packages
    ADD COLUMN max_frequency INTEGER NOT NULL DEFAULT 4;

ALTER TABLE affordable_packages
    ADD CONSTRAINT chk_max_frequency_positive
        CHECK (max_frequency > 0 AND max_frequency <= 12);

CREATE INDEX idx_affordable_packages_max_frequency
    ON affordable_packages(max_frequency);

UPDATE affordable_packages
SET max_frequency = 4
WHERE max_frequency IS NULL;