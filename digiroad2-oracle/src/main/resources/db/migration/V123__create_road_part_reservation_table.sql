CREATE TABLE PROJECT_RESERVED_ROAD_PART
(	"ID" NUMBER,
	"ROAD_NUMBER" NUMBER NOT NULL ENABLE,
	"ROAD_PART_NUMBER" NUMBER NOT NULL ENABLE,
	"PROJECT_ID" NUMBER NOT NULL ENABLE,
	"CREATED_BY" VARCHAR2(128 BYTE),
	"FIRST_LINK_ID" NUMBER,
	"ROAD_LENGTH" NUMBER,
	"ADDRESS_LENGTH" NUMBER,
  "DISCONTINUITY" NUMBER,
  "ELY" NUMBER,
	 PRIMARY KEY ("ID"),
	 CONSTRAINT "COMBINED" UNIQUE ("PROJECT_ID", "ROAD_NUMBER", "ROAD_PART_NUMBER"),
	 FOREIGN KEY ("PROJECT_ID")
	  REFERENCES "PROJECT" ("ID") ENABLE
   );

INSERT INTO PROJECT_RESERVED_ROAD_PART(id, road_number, road_part_number, project_id, created_by)
  (SELECT viite_general_seq.nextval, road_number, road_part_number, project_id, created_by
  from (SELECT pl.road_number, pl.road_part_number, project_id, MAX(p.CREATED_BY) as created_by FROM PROJECT p JOIN
PROJECT_LINK pl ON (p.id = pl.project_id) WHERE p.STATE IN (1,2,4,99) group by road_number, road_part_number, project_id) foo);

ALTER TABLE PROJECT_LINK
MODIFY (road_number NUMBER CONSTRAINT road_not_null NOT NULL);

ALTER TABLE PROJECT_LINK
MODIFY (road_part_number NUMBER CONSTRAINT part_not_null NOT NULL);

ALTER TABLE PROJECT_LINK
ADD CONSTRAINT fk_link_reserved
   FOREIGN KEY (project_id, road_number, road_part_number)
   REFERENCES PROJECT_RESERVED_ROAD_PART(project_id, road_number, road_part_number);

CREATE TABLE PROJECT_LINK_HISTORY 
   (	"ID" NUMBER NOT NULL ENABLE, 
	"PROJECT_ID" NUMBER NOT NULL ENABLE, 
	"TRACK_CODE" NUMBER NOT NULL ENABLE, 
	"DISCONTINUITY_TYPE" NUMBER DEFAULT 5 NOT NULL ENABLE, 
	"ROAD_NUMBER" NUMBER, 
	"ROAD_PART_NUMBER" NUMBER, 
	"START_ADDR_M" NUMBER, 
	"END_ADDR_M" NUMBER, 
	"LRM_POSITION_ID" NUMBER NOT NULL ENABLE, 
	"CREATED_BY" VARCHAR2(32 BYTE) NOT NULL ENABLE, 
	"MODIFIED_BY" VARCHAR2(32 BYTE), 
	"CREATED_DATE" DATE DEFAULT SYSDATE NOT NULL ENABLE, 
	"MODIFIED_DATE" DATE, 
	"STATUS" NUMBER, 
	"CALIBRATION_POINTS" NUMBER DEFAULT 0 NOT NULL ENABLE, 
	"ROAD_TYPE" NUMBER DEFAULT 99 NOT NULL ENABLE, 
	 CONSTRAINT "PROJECT_LINK_HIST_PROJECT_FK1" FOREIGN KEY ("PROJECT_ID")
	  REFERENCES "PROJECT" ("ID") ON DELETE CASCADE ENABLE, 
	 CONSTRAINT "PROJECT_LINK_HIST_POSITION_FK2" FOREIGN KEY ("LRM_POSITION_ID")
	  REFERENCES "LRM_POSITION" ("ID") ENABLE);

  CREATE INDEX "PROJECT_LINK_HIST_PRJ_IDX" ON "PROJECT_LINK_HISTORY" ("PROJECT_ID");

  CREATE INDEX "PROJECT_LINK_HIST_ROAD_IDX" ON "PROJECT_LINK_HISTORY" ("ROAD_NUMBER", "ROAD_PART_NUMBER");