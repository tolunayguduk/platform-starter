-- Human-readable explanation of what a function does, shown next to its key in the admin panel's
-- function catalog. Purely descriptive - never read by any authorization or UI-policy logic.
ALTER TABLE permission ADD COLUMN description VARCHAR(255) NULL;
