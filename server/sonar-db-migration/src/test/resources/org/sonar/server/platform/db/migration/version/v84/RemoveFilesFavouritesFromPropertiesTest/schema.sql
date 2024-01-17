CREATE TABLE "COMPONENTS"(
    "UUID" VARCHAR(50) NOT NULL,
    "ORGANIZATION_UUID" VARCHAR(40) NOT NULL,
    "KEE" VARCHAR(400),
    "DEPRECATED_KEE" VARCHAR(400),
    "NAME" VARCHAR(2000),
    "LONG_NAME" VARCHAR(2000),
    "DESCRIPTION" VARCHAR(2000),
    "ENABLED" BOOLEAN DEFAULT TRUE NOT NULL,
    "SCOPE" VARCHAR(3),
    "QUALIFIER" VARCHAR(10),
    "PRIVATE" BOOLEAN NOT NULL,
    "ROOT_UUID" VARCHAR(50) NOT NULL,
    "LANGUAGE" VARCHAR(20),
    "COPY_COMPONENT_UUID" VARCHAR(50),
    "PATH" VARCHAR(2000),
    "UUID_PATH" VARCHAR(1500) NOT NULL,
    "PROJECT_UUID" VARCHAR(50) NOT NULL,
    "MODULE_UUID" VARCHAR(50),
    "MODULE_UUID_PATH" VARCHAR(1500),
    "MAIN_BRANCH_PROJECT_UUID" VARCHAR(50),
    "B_CHANGED" BOOLEAN,
    "B_NAME" VARCHAR(500),
    "B_LONG_NAME" VARCHAR(500),
    "B_DESCRIPTION" VARCHAR(2000),
    "B_ENABLED" BOOLEAN,
    "B_QUALIFIER" VARCHAR(10),
    "B_LANGUAGE" VARCHAR(20),
    "B_COPY_COMPONENT_UUID" VARCHAR(50),
    "B_PATH" VARCHAR(2000),
    "B_UUID_PATH" VARCHAR(1500),
    "B_MODULE_UUID" VARCHAR(50),
    "B_MODULE_UUID_PATH" VARCHAR(1500),
    "CREATED_AT" TIMESTAMP
);
CREATE INDEX "PROJECTS_ORGANIZATION" ON "COMPONENTS"("ORGANIZATION_UUID");
CREATE UNIQUE INDEX "PROJECTS_KEE" ON "COMPONENTS"("KEE");
CREATE INDEX "PROJECTS_MODULE_UUID" ON "COMPONENTS"("MODULE_UUID");
CREATE INDEX "PROJECTS_PROJECT_UUID" ON "COMPONENTS"("PROJECT_UUID");
CREATE INDEX "PROJECTS_QUALIFIER" ON "COMPONENTS"("QUALIFIER");
CREATE INDEX "PROJECTS_ROOT_UUID" ON "COMPONENTS"("ROOT_UUID");
CREATE INDEX "PROJECTS_UUID" ON "COMPONENTS"("UUID");

CREATE TABLE "PROPERTIES"(
    "UUID" VARCHAR(40) NOT NULL,
    "PROP_KEY" VARCHAR(512) NOT NULL,
    "USER_UUID" VARCHAR(40),
    "IS_EMPTY" BOOLEAN NOT NULL,
    "TEXT_VALUE" VARCHAR(4000),
    "CLOB_VALUE" CLOB(2147483647),
    "CREATED_AT" BIGINT NOT NULL,
    "COMPONENT_UUID" VARCHAR(40)
);
ALTER TABLE "PROPERTIES" ADD CONSTRAINT "PK_PROPERTIES" PRIMARY KEY("UUID");
CREATE INDEX "PROPERTIES_KEY" ON "PROPERTIES"("PROP_KEY");
