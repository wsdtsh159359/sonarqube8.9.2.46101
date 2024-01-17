CREATE TABLE "GROUPS_USERS"(
    "USER_ID" BIGINT,
    "GROUP_UUID" VARCHAR(40),
    "GROUP_ID" BIGINT
);
CREATE INDEX "INDEX_GROUPS_USERS_ON_USER_ID" ON "GROUPS_USERS"("USER_ID");
CREATE INDEX "INDEX_GROUPS_USERS_ON_GROUP_ID" ON "GROUPS_USERS"("GROUP_ID");
CREATE UNIQUE INDEX "GROUPS_USERS_UNIQUE" ON "GROUPS_USERS"("GROUP_ID", "USER_ID");
