-- Local PostgreSQL bootstrap for the Temple Digital Services Platform.
-- Run as a superuser (for example postgres). This script only creates the database.
-- It does not create roles or set passwords.
--
-- Example (psql):
--   \i 01_create_database.sql

SELECT 'CREATE DATABASE temple_platform_dev'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'temple_platform_dev')\gexec
