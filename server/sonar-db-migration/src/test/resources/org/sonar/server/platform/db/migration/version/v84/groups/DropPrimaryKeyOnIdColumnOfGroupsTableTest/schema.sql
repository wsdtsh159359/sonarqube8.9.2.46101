CREATE TABLE "GROUPS"(
    "ID" INTEGER NOT NULL AUTO_INCREMENT (1,1),
    "UUID" VARCHAR(40) NOT NULL,
    "ORGANIZATION_UUID" VARCHAR(40) NOT NULL,
    "NAME" VARCHAR(500),
    "DESCRIPTION" VARCHAR(200),
    "CREATED_AT" TIMESTAMP,
    "UPDATED_AT" TIMESTAMP
);
ALTER TABLE "GROUPS" ADD CONSTRAINT "PK_GROUPS" PRIMARY KEY("ID");
