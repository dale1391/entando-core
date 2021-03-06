ALTER TABLE pages_metadata_draft ADD COLUMN groupcode character varying(30);
ALTER TABLE pages_metadata_online ADD COLUMN groupcode character varying(30);
UPDATE pages_metadata_online SET groupcode = (SELECT pages.groupcode FROM pages WHERE pages_metadata_online.code = pages.code);
UPDATE pages_metadata_draft SET groupcode = (SELECT pages.groupcode FROM pages WHERE pages_metadata_draft.code = pages.code);
ALTER TABLE pages DROP groupcode;
ALTER TABLE resources ADD COLUMN owner character varying(128);
ALTER TABLE contents ADD COLUMN restriction character varying(40);
ALTER TABLE widgetcatalog ADD COLUMN bundleid character varying(150);
ALTER TABLE widgetcatalog ADD COLUMN configui character varying;
ALTER TABLE authusers ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authusergrouprole ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authuserprofileattrroles ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authusershortcuts ALTER COLUMN username TYPE character varying(80);