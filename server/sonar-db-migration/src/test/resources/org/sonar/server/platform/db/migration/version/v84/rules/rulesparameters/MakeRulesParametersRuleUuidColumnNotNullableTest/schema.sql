CREATE TABLE "RULES"(
    "ID" INTEGER NOT NULL AUTO_INCREMENT (1,1),
    "NAME" VARCHAR(200),
    "PLUGIN_RULE_KEY" VARCHAR(200) NOT NULL,
    "PLUGIN_KEY" VARCHAR(200),
    "PLUGIN_CONFIG_KEY" VARCHAR(200),
    "PLUGIN_NAME" VARCHAR(255) NOT NULL,
    "SCOPE" VARCHAR(20) NOT NULL,
    "DESCRIPTION" CLOB(2147483647),
    "PRIORITY" INTEGER,
    "STATUS" VARCHAR(40),
    "LANGUAGE" VARCHAR(20),
    "DEF_REMEDIATION_FUNCTION" VARCHAR(20),
    "DEF_REMEDIATION_GAP_MULT" VARCHAR(20),
    "DEF_REMEDIATION_BASE_EFFORT" VARCHAR(20),
    "GAP_DESCRIPTION" VARCHAR(4000),
    "SYSTEM_TAGS" VARCHAR(4000),
    "IS_TEMPLATE" BOOLEAN DEFAULT FALSE NOT NULL,
    "DESCRIPTION_FORMAT" VARCHAR(20),
    "RULE_TYPE" TINYINT,
    "SECURITY_STANDARDS" VARCHAR(4000),
    "IS_AD_HOC" BOOLEAN NOT NULL,
    "IS_EXTERNAL" BOOLEAN NOT NULL,
    "CREATED_AT" BIGINT,
    "UPDATED_AT" BIGINT,
    "UUID" VARCHAR(40) NOT NULL,
    "TEMPLATE_UUID" VARCHAR(40)
);
ALTER TABLE "RULES" ADD CONSTRAINT "PK_RULES" PRIMARY KEY("ID");
CREATE UNIQUE INDEX "RULES_REPO_KEY" ON "RULES"("PLUGIN_RULE_KEY", "PLUGIN_NAME");

CREATE TABLE "RULES_PARAMETERS"(
    "UUID" VARCHAR(40) NOT NULL,
    "RULE_ID" INTEGER NOT NULL,
    "NAME" VARCHAR(128) NOT NULL,
    "DESCRIPTION" VARCHAR(4000),
    "PARAM_TYPE" VARCHAR(512) NOT NULL,
    "DEFAULT_VALUE" VARCHAR(4000),
    "RULE_UUID" VARCHAR(40)
);
ALTER TABLE "RULES_PARAMETERS" ADD CONSTRAINT "PK_RULES_PARAMETERS" PRIMARY KEY("UUID");
CREATE INDEX "RULES_PARAMETERS_RULE_ID" ON "RULES_PARAMETERS"("RULE_ID");
CREATE UNIQUE INDEX "RULES_PARAMETERS_UNIQUE" ON "RULES_PARAMETERS"("RULE_ID", "NAME");
