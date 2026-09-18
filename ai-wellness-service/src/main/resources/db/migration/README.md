# Flyway database migration scripts live in this directory.
#
# Naming convention:  V{version}__{description}.sql
# Examples:
#   V1__create_users_table.sql
#   V2__create_teams_table.sql
#   V3__create_activities_table.sql
#
# IMPORTANT:
#   - Never INSERT production/business data in migration scripts.
#   - If dev seed data is needed, place it in db/dev/ and
#     reference it only from application-dev.yml.
#   - Production migrations must only define schema (DDL), not data (DML).
#
# Phase 2 will add the first migration: V1__initial_schema.sql
