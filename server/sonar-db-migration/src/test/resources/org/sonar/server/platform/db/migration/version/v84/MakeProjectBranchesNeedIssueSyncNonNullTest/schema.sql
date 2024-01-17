CREATE TABLE "PROJECT_BRANCHES"(
    "UUID" VARCHAR(50) NOT NULL,
    "PROJECT_UUID" VARCHAR(50) NOT NULL,
    "KEE" VARCHAR(255) NOT NULL,
    "BRANCH_TYPE" VARCHAR(12),
    "MERGE_BRANCH_UUID" VARCHAR(50),
    "KEY_TYPE" VARCHAR(12) NOT NULL,
    "PULL_REQUEST_BINARY" BLOB,
    "MANUAL_BASELINE_ANALYSIS_UUID" VARCHAR(40),
    "CREATED_AT" BIGINT NOT NULL,
    "UPDATED_AT" BIGINT NOT NULL,
    "EXCLUDE_FROM_PURGE" BOOLEAN DEFAULT FALSE NOT NULL,
    "NEED_ISSUE_SYNC" BOOLEAN
);
ALTER TABLE "PROJECT_BRANCHES" ADD CONSTRAINT "PK_PROJECT_BRANCHES" PRIMARY KEY("UUID");
CREATE UNIQUE INDEX "PROJECT_BRANCHES_KEE_KEY_TYPE" ON "PROJECT_BRANCHES"("PROJECT_UUID", "KEE", "KEY_TYPE");
