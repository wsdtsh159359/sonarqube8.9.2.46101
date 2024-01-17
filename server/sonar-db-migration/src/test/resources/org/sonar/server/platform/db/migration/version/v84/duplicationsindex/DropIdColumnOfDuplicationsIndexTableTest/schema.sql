CREATE TABLE "DUPLICATIONS_INDEX"(
    "ID" BIGINT NOT NULL,
    "ANALYSIS_UUID" VARCHAR(50) NOT NULL,
    "COMPONENT_UUID" VARCHAR(50) NOT NULL,
    "HASH" VARCHAR(50) NOT NULL,
    "INDEX_IN_FILE" INTEGER NOT NULL,
    "START_LINE" INTEGER NOT NULL,
    "END_LINE" INTEGER NOT NULL,
    "UUID" VARCHAR(40) NOT NULL,
);
ALTER TABLE "DUPLICATIONS_INDEX" ADD CONSTRAINT "PK_DUPLICATIONS_INDEX" PRIMARY KEY("UUID");
CREATE INDEX "DUPLICATIONS_INDEX_HASH" ON "DUPLICATIONS_INDEX"("HASH");
CREATE INDEX "DUPLICATION_ANALYSIS_COMPONENT" ON "DUPLICATIONS_INDEX"("ANALYSIS_UUID", "COMPONENT_UUID");
