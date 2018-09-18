--------------------------------------------------------
--  Constraints for Table PROJECT
--------------------------------------------------------

ALTER TABLE "PROJECT" ADD CONSTRAINT "PROJECT_PK" PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PROJECT" MODIFY ("CHECK_COUNTER" NOT NULL ENABLE);
ALTER TABLE "PROJECT" MODIFY ("CREATED_DATE" NOT NULL ENABLE);
ALTER TABLE "PROJECT" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "PROJECT" MODIFY ("NAME" NOT NULL ENABLE);
ALTER TABLE "PROJECT" MODIFY ("STATE" NOT NULL ENABLE);
ALTER TABLE "PROJECT" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table LINK_TYPE
--------------------------------------------------------

ALTER TABLE "LINK_TYPE" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "LINK_TYPE" MODIFY ("MODIFIED_DATE" NOT NULL ENABLE);
ALTER TABLE "LINK_TYPE" MODIFY ("LINK_TYPE" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table CALIBRATION_POINT
--------------------------------------------------------

ALTER TABLE "CALIBRATION_POINT" ADD CONSTRAINT "CALIBRATION_POINT_PK" PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "CALIBRATION_POINT" MODIFY ("ADDRESS_M" NOT NULL ENABLE);
ALTER TABLE "CALIBRATION_POINT" MODIFY ("LINK_M" NOT NULL ENABLE);
ALTER TABLE "CALIBRATION_POINT" MODIFY ("PROJECT_ID" NOT NULL ENABLE);
ALTER TABLE "CALIBRATION_POINT" MODIFY ("PROJECT_LINK_ID" NOT NULL ENABLE);
ALTER TABLE "CALIBRATION_POINT" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table MISSING_ROAD_ADDRESS
--------------------------------------------------------

ALTER TABLE "MISSING_ROAD_ADDRESS" MODIFY ("ANOMALY_CODE" NOT NULL ENABLE);
ALTER TABLE "MISSING_ROAD_ADDRESS" MODIFY ("LINK_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PUBLISHED_ROAD_NETWORK
--------------------------------------------------------

ALTER TABLE "PUBLISHED_ROAD_NETWORK" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PUBLISHED_ROAD_NETWORK" MODIFY ("CREATED" NOT NULL ENABLE);
ALTER TABLE "PUBLISHED_ROAD_NETWORK" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table EXPORT_LOCK
--------------------------------------------------------

ALTER TABLE "EXPORT_LOCK" ADD CONSTRAINT "CK_EXPORT_ID" CHECK (id = 1) ENABLE;
ALTER TABLE "EXPORT_LOCK" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "EXPORT_LOCK" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table INCOMPLETE_LINK
--------------------------------------------------------

ALTER TABLE "INCOMPLETE_LINK" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Constraints for Table ROAD_ADDRESS_CHANGES
--------------------------------------------------------

ALTER TABLE "ROAD_ADDRESS_CHANGES" MODIFY ("NEW_ELY" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS_CHANGES" MODIFY ("NEW_ROAD_TYPE" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS_CHANGES" MODIFY ("NEW_DISCONTINUITY" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS_CHANGES" MODIFY ("CHANGE_TYPE" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS_CHANGES" MODIFY ("PROJECT_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table MUNICIPALITY
--------------------------------------------------------

ALTER TABLE "MUNICIPALITY" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "MUNICIPALITY" ADD CONSTRAINT "MUNI_ELY_MANTAINER_UNIQUE" UNIQUE ("ID", "ELY_NRO", "ROAD_MAINTAINER_ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;

--------------------------------------------------------
--  Constraints for Table PROJECT_RESERVED_ROAD_PART
--------------------------------------------------------

ALTER TABLE "PROJECT_RESERVED_ROAD_PART" ADD CONSTRAINT "RESERVED_ROAD_CHECK" UNIQUE ("ROAD_NUMBER", "ROAD_PART_NUMBER")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PROJECT_RESERVED_ROAD_PART" ADD CONSTRAINT "COMBINED" UNIQUE ("PROJECT_ID", "ROAD_NUMBER", "ROAD_PART_NUMBER")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PROJECT_RESERVED_ROAD_PART" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PROJECT_RESERVED_ROAD_PART" MODIFY ("PROJECT_ID" NOT NULL ENABLE);
ALTER TABLE "PROJECT_RESERVED_ROAD_PART" MODIFY ("ROAD_PART_NUMBER" NOT NULL ENABLE);
ALTER TABLE "PROJECT_RESERVED_ROAD_PART" MODIFY ("ROAD_NUMBER" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table ROAD_ADDRESS
--------------------------------------------------------

ALTER TABLE "ROAD_ADDRESS" ADD CONSTRAINT "ROAD_ADDRESS_HISTORY_CHECK" UNIQUE ("ROAD_NUMBER", "ROAD_PART_NUMBER", "START_ADDR_M", "END_ADDR_M", "TRACK_CODE", "DISCONTINUITY", "START_DATE", "END_DATE", "VALID_FROM", "VALID_TO", "ELY", "ROAD_TYPE", "TERMINATED")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "ROAD_ADDRESS" ADD CONSTRAINT "CK_TERMINATION_END_DATE" CHECK (TERMINATED = 0 OR (TERMINATED in (1,2) AND END_DATE IS NOT NULL)) ENABLE;
ALTER TABLE "ROAD_ADDRESS" MODIFY ("TERMINATED" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" ADD CONSTRAINT "RA_FLOATING_IS_BOOLEAN" CHECK (floating in ('1','0')) ENABLE;
ALTER TABLE "ROAD_ADDRESS" ADD CONSTRAINT "ROAD_ADDRESS_PK" PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "ROAD_ADDRESS" MODIFY ("CALIBRATION_POINTS" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("VALID_FROM" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("START_DATE" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("LRM_POSITION_ID" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("END_ADDR_M" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("START_ADDR_M" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("DISCONTINUITY" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("TRACK_CODE" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("ROAD_PART_NUMBER" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("ROAD_NUMBER" NOT NULL ENABLE);
ALTER TABLE "ROAD_ADDRESS" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TRAFFIC_DIRECTION
--------------------------------------------------------

ALTER TABLE "TRAFFIC_DIRECTION" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "TRAFFIC_DIRECTION" MODIFY ("MODIFIED_DATE" NOT NULL ENABLE);
ALTER TABLE "TRAFFIC_DIRECTION" MODIFY ("TRAFFIC_DIRECTION" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PROJECT_LINK
--------------------------------------------------------

ALTER TABLE "PROJECT_LINK" ADD CONSTRAINT "PROJECT_LINK_ADDRESS_CHK1" CHECK ((road_number is null and road_part_number is null) or
(road_number is not null and road_part_number is not null and start_addr_m is not null and end_addr_m is not null)) ENABLE;
ALTER TABLE "PROJECT_LINK" ADD CONSTRAINT "PROJECT_LINK_PK" PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PROJECT_LINK" MODIFY ("ROAD_TYPE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("CALIBRATION_POINTS" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("CREATED_DATE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("LRM_POSITION_ID" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("ROAD_PART_NUMBER" CONSTRAINT "PART_NOT_NULL" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("ROAD_NUMBER" CONSTRAINT "ROAD_NOT_NULL" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("DISCONTINUITY_TYPE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("TRACK_CODE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("PROJECT_ID" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table ROAD_NAMES
--------------------------------------------------------

ALTER TABLE "ROAD_NAMES" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "ROAD_NAMES" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "ROAD_NAMES" MODIFY ("VALID_FROM" NOT NULL ENABLE);
ALTER TABLE "ROAD_NAMES" MODIFY ("START_DATE" NOT NULL ENABLE);
ALTER TABLE "ROAD_NAMES" MODIFY ("ROAD_NAME" NOT NULL ENABLE);
ALTER TABLE "ROAD_NAMES" MODIFY ("ROAD_NUMBER" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TEMP_ID
--------------------------------------------------------

ALTER TABLE "TEMP_ID" ADD PRIMARY KEY ("ID") ENABLE;
--------------------------------------------------------
--  Constraints for Table FUNCTIONAL_CLASS
--------------------------------------------------------

ALTER TABLE "FUNCTIONAL_CLASS" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "FUNCTIONAL_CLASS" MODIFY ("MODIFIED_DATE" NOT NULL ENABLE);
ALTER TABLE "FUNCTIONAL_CLASS" MODIFY ("FUNCTIONAL_CLASS" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table ADMINISTRATIVE_CLASS
--------------------------------------------------------

ALTER TABLE "ADMINISTRATIVE_CLASS" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "ADMINISTRATIVE_CLASS" MODIFY ("CREATED_DATE" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PROJECT_LINK_HISTORY
--------------------------------------------------------

ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("ROAD_TYPE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("CALIBRATION_POINTS" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("CREATED_DATE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("LRM_POSITION_ID" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("DISCONTINUITY_TYPE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("TRACK_CODE" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("PROJECT_ID" NOT NULL ENABLE);
ALTER TABLE "PROJECT_LINK_HISTORY" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table SERVICE_USER
--------------------------------------------------------

ALTER TABLE "SERVICE_USER" ADD UNIQUE ("USERNAME")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "SERVICE_USER" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "SERVICE_USER" MODIFY ("USERNAME" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table ROAD_NETWORK_ERRORS
--------------------------------------------------------

ALTER TABLE "ROAD_NETWORK_ERRORS" ADD PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "ROAD_NETWORK_ERRORS" MODIFY ("ERROR_CODE" NOT NULL ENABLE);
ALTER TABLE "ROAD_NETWORK_ERRORS" MODIFY ("ROAD_ADDRESS_ID" NOT NULL ENABLE);
ALTER TABLE "ROAD_NETWORK_ERRORS" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PUBLISHED_ROAD_ADDRESS
--------------------------------------------------------

ALTER TABLE "PUBLISHED_ROAD_ADDRESS" ADD CONSTRAINT "PK_PUBLISHED_ROAD_ADDRESS" PRIMARY KEY ("NETWORK_ID", "ROAD_ADDRESS_ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "PUBLISHED_ROAD_ADDRESS" MODIFY ("ROAD_ADDRESS_ID" NOT NULL ENABLE);
ALTER TABLE "PUBLISHED_ROAD_ADDRESS" MODIFY ("NETWORK_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table LRM_POSITION
--------------------------------------------------------

ALTER TABLE "LRM_POSITION" ADD CONSTRAINT "LRM_POSITION_PK" PRIMARY KEY ("ID")
USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS"  ENABLE;
ALTER TABLE "LRM_POSITION" ADD CONSTRAINT "START_MEASURE_POSITIVE" CHECK (start_measure >= 0) ENABLE;
ALTER TABLE "LRM_POSITION" MODIFY ("MODIFIED_DATE" NOT NULL ENABLE);
ALTER TABLE "LRM_POSITION" MODIFY ("ADJUSTED_TIMESTAMP" NOT NULL ENABLE);
--------------------------------------------------------
--  Ref Constraints for Table CALIBRATION_POINT
--------------------------------------------------------

ALTER TABLE "CALIBRATION_POINT" ADD CONSTRAINT "CALIBRATION_POINT_FK1" FOREIGN KEY ("PROJECT_LINK_ID")
  REFERENCES "PROJECT_LINK" ("ID") ON DELETE CASCADE ENABLE;
ALTER TABLE "CALIBRATION_POINT" ADD CONSTRAINT "CALIBRATION_POINT_FK3" FOREIGN KEY ("PROJECT_ID")
  REFERENCES "PROJECT" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PROJECT_LINK
--------------------------------------------------------

ALTER TABLE "PROJECT_LINK" ADD CONSTRAINT "FK_LINK_RESERVED" FOREIGN KEY ("PROJECT_ID", "ROAD_NUMBER", "ROAD_PART_NUMBER")
  REFERENCES "PROJECT_RESERVED_ROAD_PART" ("PROJECT_ID", "ROAD_NUMBER", "ROAD_PART_NUMBER") ENABLE;
ALTER TABLE "PROJECT_LINK" ADD CONSTRAINT "PROJECT_LINK_POSITION_FK2" FOREIGN KEY ("LRM_POSITION_ID")
  REFERENCES "LRM_POSITION" ("ID") ENABLE;
ALTER TABLE "PROJECT_LINK" ADD CONSTRAINT "PROJECT_LINK_PROJECT_ID_FK1" FOREIGN KEY ("PROJECT_ID")
  REFERENCES "PROJECT" ("ID") ON DELETE CASCADE ENABLE;
ALTER TABLE "PROJECT_LINK" ADD FOREIGN KEY ("ROAD_ADDRESS_ID")
  REFERENCES "ROAD_ADDRESS" ("ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PROJECT_LINK_HISTORY
--------------------------------------------------------

ALTER TABLE "PROJECT_LINK_HISTORY" ADD CONSTRAINT "PROJECT_LINK_HIST_POSITION_FK2" FOREIGN KEY ("LRM_POSITION_ID")
  REFERENCES "LRM_POSITION" ("ID") ENABLE;
ALTER TABLE "PROJECT_LINK_HISTORY" ADD CONSTRAINT "PROJECT_LINK_HIST_PROJECT_FK1" FOREIGN KEY ("PROJECT_ID")
  REFERENCES "PROJECT" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PROJECT_RESERVED_ROAD_PART
--------------------------------------------------------

ALTER TABLE "PROJECT_RESERVED_ROAD_PART" ADD FOREIGN KEY ("PROJECT_ID")
  REFERENCES "PROJECT" ("ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PUBLISHED_ROAD_ADDRESS
--------------------------------------------------------

ALTER TABLE "PUBLISHED_ROAD_ADDRESS" ADD CONSTRAINT "FK_NETWORK_ID" FOREIGN KEY ("NETWORK_ID")
  REFERENCES "PUBLISHED_ROAD_NETWORK" ("ID") ENABLE;
ALTER TABLE "PUBLISHED_ROAD_ADDRESS" ADD CONSTRAINT "FK_PUBLISHED_ROAD_ADDRESS_ID" FOREIGN KEY ("ROAD_ADDRESS_ID")
  REFERENCES "ROAD_ADDRESS" ("ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table ROAD_ADDRESS
--------------------------------------------------------

ALTER TABLE "ROAD_ADDRESS" ADD CONSTRAINT "ROAD_ADDRESS_FK1" FOREIGN KEY ("LRM_POSITION_ID")
  REFERENCES "LRM_POSITION" ("ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table ROAD_NETWORK_ERRORS
--------------------------------------------------------

ALTER TABLE "ROAD_NETWORK_ERRORS" ADD CONSTRAINT "FK_ROAD_ADDRESS_ERROR" FOREIGN KEY ("ROAD_ADDRESS_ID")
  REFERENCES "ROAD_ADDRESS" ("ID") ENABLE;