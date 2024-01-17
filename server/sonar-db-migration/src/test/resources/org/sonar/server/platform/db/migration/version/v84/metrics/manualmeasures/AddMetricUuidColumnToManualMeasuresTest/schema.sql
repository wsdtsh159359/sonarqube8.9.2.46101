CREATE TABLE "MANUAL_MEASURES"(
    "UUID" VARCHAR(40) NOT NULL,
    "METRIC_ID" INTEGER NOT NULL,
    "VALUE" DOUBLE,
    "TEXT_VALUE" VARCHAR(4000),
    "USER_UUID" VARCHAR(255),
    "DESCRIPTION" VARCHAR(4000),
    "CREATED_AT" BIGINT,
    "UPDATED_AT" BIGINT,
    "COMPONENT_UUID" VARCHAR(50) NOT NULL
);
ALTER TABLE "MANUAL_MEASURES" ADD CONSTRAINT "PK_MANUAL_MEASURES" PRIMARY KEY("UUID");
CREATE INDEX "MANUAL_MEASURES_COMPONENT_UUID" ON "MANUAL_MEASURES"("COMPONENT_UUID");

CREATE TABLE "METRICS"(
    "ID" INTEGER NOT NULL AUTO_INCREMENT (1,1),
    "NAME" VARCHAR(64) NOT NULL,
    "DESCRIPTION" VARCHAR(255),
    "DIRECTION" INTEGER DEFAULT 0 NOT NULL,
    "DOMAIN" VARCHAR(64),
    "SHORT_NAME" VARCHAR(64),
    "QUALITATIVE" BOOLEAN DEFAULT FALSE NOT NULL,
    "VAL_TYPE" VARCHAR(8),
    "USER_MANAGED" BOOLEAN DEFAULT FALSE,
    "ENABLED" BOOLEAN DEFAULT TRUE,
    "WORST_VALUE" DOUBLE,
    "BEST_VALUE" DOUBLE,
    "OPTIMIZED_BEST_VALUE" BOOLEAN,
    "HIDDEN" BOOLEAN,
    "DELETE_HISTORICAL_DATA" BOOLEAN,
    "DECIMAL_SCALE" INTEGER,
    "UUID" VARCHAR(40) NOT NULL
);
ALTER TABLE "METRICS" ADD CONSTRAINT "PK_METRICS" PRIMARY KEY("ID");
CREATE UNIQUE INDEX "METRICS_UNIQUE_NAME" ON "METRICS"("NAME");
