-- Least-privilege roles for local PostgreSQL.
-- Run as a superuser against temple_platform_dev AFTER the database exists.
-- Do not commit real passwords. Set them in the psql session first:
--
--   \c temple_platform_dev
--   \set owner_password 'replace-me'
--   \set app_password 'replace-me'
--   \i 02_roles_and_grants.sql
--
-- Dev default (Module 03): a single application user can run Flyway and DML.
-- Hardened layout (this module): temple_migrator owns DDL/Flyway; temple_app is DML-only.
--
-- WARNING — do NOT run this script against an existing Module 03/05 local database
-- and then use the app immediately. This is optional/reference bootstrap for a
-- properly planned hardened setup, not part of Module 05 local verification.
--
-- - Creating temple_migrator does not transfer ownership of existing objects.
-- - Existing tables, sequences, functions, and Flyway-managed objects may still
--   be owned by temple_app or another existing owner.
-- - REVOKE CREATE ON SCHEMA public FROM temple_app must happen only after the
--   migration-owner model is correctly established.
-- - Switching SPRING_FLYWAY_USERNAME to temple_migrator requires appropriate
--   ownership and privileges for existing and future Flyway migrations.
-- - This script does not change object ownership; plan and execute that separately.

SELECT format('CREATE ROLE temple_migrator LOGIN PASSWORD %L', :'owner_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'temple_migrator')\gexec

SELECT format('CREATE ROLE temple_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'temple_app')\gexec

GRANT CONNECT ON DATABASE temple_platform_dev TO temple_migrator;
GRANT CONNECT ON DATABASE temple_platform_dev TO temple_app;

GRANT CREATE, USAGE ON SCHEMA public TO temple_migrator;
GRANT USAGE ON SCHEMA public TO temple_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM temple_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO temple_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO temple_app;

ALTER DEFAULT PRIVILEGES FOR ROLE temple_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO temple_app;

ALTER DEFAULT PRIVILEGES FOR ROLE temple_migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO temple_app;
