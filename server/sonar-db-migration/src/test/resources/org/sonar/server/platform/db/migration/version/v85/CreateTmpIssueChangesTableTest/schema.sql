CREATE TABLE "ISSUE_CHANGES"(
    "UUID" VARCHAR(40) NOT NULL,
    "KEE" VARCHAR(50),
    "ISSUE_KEY" VARCHAR(50) NOT NULL,
    "USER_LOGIN" VARCHAR(255),
    "CHANGE_TYPE" VARCHAR(20),
    "CHANGE_DATA" CLOB,
    "CREATED_AT" BIGINT,
    "UPDATED_AT" BIGINT,
    "ISSUE_CHANGE_CREATION_DATE" BIGINT
);
ALTER TABLE "ISSUE_CHANGES" ADD CONSTRAINT "PK_ISSUE_CHANGES" PRIMARY KEY("UUID");
CREATE INDEX "ISSUE_CHANGES_ISSUE_KEY" ON "ISSUE_CHANGES"("ISSUE_KEY");
CREATE INDEX "ISSUE_CHANGES_KEE" ON "ISSUE_CHANGES"("KEE");

CREATE TABLE "ISSUES"(
    "KEE" VARCHAR(50) NOT NULL,
    "RULE_UUID" VARCHAR(40),
    "SEVERITY" VARCHAR(10),
    "MANUAL_SEVERITY" BOOLEAN NOT NULL,
    "MESSAGE" VARCHAR(4000),
    "LINE" INTEGER,
    "GAP" DOUBLE,
    "STATUS" VARCHAR(20),
    "RESOLUTION" VARCHAR(20),
    "CHECKSUM" VARCHAR(1000),
    "REPORTER" VARCHAR(255),
    "ASSIGNEE" VARCHAR(255),
    "AUTHOR_LOGIN" VARCHAR(255),
    "ACTION_PLAN_KEY" VARCHAR(50),
    "ISSUE_ATTRIBUTES" VARCHAR(4000),
    "EFFORT" INTEGER,
    "CREATED_AT" BIGINT,
    "UPDATED_AT" BIGINT,
    "ISSUE_CREATION_DATE" BIGINT,
    "ISSUE_UPDATE_DATE" BIGINT,
    "ISSUE_CLOSE_DATE" BIGINT,
    "TAGS" VARCHAR(4000),
    "COMPONENT_UUID" VARCHAR(50),
    "PROJECT_UUID" VARCHAR(50),
    "LOCATIONS" BLOB,
    "ISSUE_TYPE" TINYINT,
    "FROM_HOTSPOT" BOOLEAN
);
ALTER TABLE "ISSUES" ADD CONSTRAINT "PK_ISSUES" PRIMARY KEY("KEE");
CREATE INDEX "ISSUES_ASSIGNEE" ON "ISSUES"("ASSIGNEE");
CREATE INDEX "ISSUES_COMPONENT_UUID" ON "ISSUES"("COMPONENT_UUID");
CREATE INDEX "ISSUES_CREATION_DATE" ON "ISSUES"("ISSUE_CREATION_DATE");
CREATE UNIQUE INDEX "ISSUES_KEE" ON "ISSUES"("KEE");
CREATE INDEX "ISSUES_PROJECT_UUID" ON "ISSUES"("PROJECT_UUID");
CREATE INDEX "ISSUES_RESOLUTION" ON "ISSUES"("RESOLUTION");
CREATE INDEX "ISSUES_UPDATED_AT" ON "ISSUES"("UPDATED_AT");
CREATE INDEX "ISSUES_RULE_UUID" ON "ISSUES"("RULE_UUID");
