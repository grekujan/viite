CREATE UNIQUE INDEX ROAD_ADDRESS_DUPLICATE_CHECK ON ROAD_ADDRESS (road_number, road_part_number, track_code, start_date, end_date, start_addr_m, end_addr_m, valid_from, valid_to, terminated, discontinuity, road_type, ely);