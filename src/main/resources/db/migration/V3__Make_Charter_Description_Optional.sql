-- Make Charter description column optional
ALTER TABLE charters ALTER COLUMN description DROP NOT NULL;
