CREATE TABLE "MISSING_ROAD_ADDRESS"
   (	"LINK_ID" NUMBER(38,0) 			NOT NULL,
    "START_ADDR_M" NUMBER,
		"END_ADDR_M" NUMBER,
		"ROAD_NUMBER" NUMBER,
    "ROAD_PART_NUMBER" NUMBER,
		"ANOMALY_CODE" NUMBER(2,0)		NOT NULL
   );
CREATE INDEX GAP_ROAD_ADDRESS_ID ON MISSING_ROAD_ADDRESS ("ROAD_NUMBER", "ROAD_PART_NUMBER", "START_ADDR_M");
CREATE INDEX GAP_ROAD_ADDRESS_LINKID_ID ON MISSING_ROAD_ADDRESS ("LINK_ID");