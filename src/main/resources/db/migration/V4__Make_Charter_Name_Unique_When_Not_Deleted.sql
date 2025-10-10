-- Make unique indexes conditional on deleted_at IS NULL
-- This allows the same values to be reused after soft deletion

-- charters: vessel_id + name must be unique when not deleted
DROP INDEX IF EXISTS uq_charters_vessel_name;
CREATE UNIQUE INDEX uq_charters_vessel_name ON charters(vessel_id, name) WHERE deleted_at IS NULL;

-- charter_projects: charter_id + name + environment must be unique when not deleted
DROP INDEX IF EXISTS uq_projects_name_env;
CREATE UNIQUE INDEX uq_projects_name_env ON charter_projects(charter_id, name, environment) WHERE deleted_at IS NULL;

-- crew: user_id + vessel_id must be unique when not deleted (table constraint needs migration)
ALTER TABLE crew DROP CONSTRAINT IF EXISTS uq_crew_user_vessel;
CREATE UNIQUE INDEX uq_crew_user_vessel ON crew(user_id, vessel_id) WHERE deleted_at IS NULL;

-- crew_charter_member: crew_id + charter_id must be unique when not deleted (table constraint needs migration)
ALTER TABLE crew_charter_member DROP CONSTRAINT IF EXISTS uq_crew_charter_member;
CREATE UNIQUE INDEX uq_crew_charter_member ON crew_charter_member(crew_id, charter_id) WHERE deleted_at IS NULL;

-- project_applications: project_id + slug must be unique when not deleted (table constraint needs migration)
ALTER TABLE project_applications DROP CONSTRAINT IF EXISTS uq_project_slug;
CREATE UNIQUE INDEX uq_project_slug ON project_applications(project_id, slug) WHERE deleted_at IS NULL;

-- webhook_subscriptions: vessel_id + charter_id + project_id + url must be unique when not deleted
DROP INDEX IF EXISTS uq_webhook_scope_url;
CREATE UNIQUE INDEX uq_webhook_scope_url ON webhook_subscriptions(vessel_id, charter_id, project_id, url) WHERE deleted_at IS NULL;
