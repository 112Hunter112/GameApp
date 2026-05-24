-- Runs once when Testcontainers boots the postgis image, against the test DB
-- created by Testcontainers (the postgis Docker image's own init only enables
-- the extension on the default `postgres` database, not on the test DB).
CREATE EXTENSION IF NOT EXISTS postgis;
