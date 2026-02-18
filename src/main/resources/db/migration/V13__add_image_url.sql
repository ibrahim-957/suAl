ALTER TABLE affordable_packages
    ADD COLUMN image_url VARCHAR(255);

UPDATE affordable_packages
SET image_url = 'https://res.cloudinary.com/dmmzwnbuh/image/upload/v1770147600/product/ubcg0pwkvcizfvgqllth.jpg';

ALTER TABLE affordable_packages
    ALTER COLUMN image_url SET NOT NULL;
