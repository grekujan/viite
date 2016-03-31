INSERT ALL
-- Cases 5&6 (divided): OLD_ID: 5169516 --> NEW_ID: 6565223, NEW_ID: 6565226
  INTO ASSET (ID,ASSET_TYPE_ID,FLOATING,CREATED_BY) values (700001,20,0,'testfixture')
  INTO LRM_POSITION (ID, LINK_ID, MML_ID, START_MEASURE, END_MEASURE, SIDE_CODE) VALUES (50000020, 5169516, null, 0.000, 10.551, 1)
  INTO ASSET_LINK (ASSET_ID,POSITION_ID) values (700001,50000020)
  INTO SINGLE_CHOICE_VALUE (ASSET_ID,ENUMERATED_VALUE_ID,PROPERTY_ID) values (700001,(select id from enumerated_value where value = 40),(select id from property where public_id = 'rajoitus'))

--Cases 5&6 (divided into three): OLD_ID: 5169764 --> NEW_IDS: 6565284,  6565286, 6565287
  INTO ASSET (ID,ASSET_TYPE_ID,FLOATING,CREATED_BY) values (700002,20,0,'testfixture')
  INTO LRM_POSITION (ID, LINK_ID, MML_ID, START_MEASURE, END_MEASURE, SIDE_CODE) VALUES (50000022, 5169764, null, 0.000, 380.551, 1)
  INTO ASSET_LINK (ASSET_ID,POSITION_ID) values (700002,50000022)
  INTO SINGLE_CHOICE_VALUE (ASSET_ID,ENUMERATED_VALUE_ID,PROPERTY_ID) values (700002,(select id from enumerated_value where value = 40),(select id from property where public_id = 'rajoitus'))

-- Cases 1&2 (3 old links combined):  OLD_ID: 2225999, OLD_ID: 2226035, OLD_ID: 2226036  --> NEW_ID: 6564314

  INTO ASSET (ID,ASSET_TYPE_ID,FLOATING,CREATED_BY) values (700003,20,0,'testfixture')
  INTO LRM_POSITION (ID, LINK_ID, MML_ID, START_MEASURE, END_MEASURE, SIDE_CODE) VALUES (50000024, 2225999, null, 0.000, 20.551, 1)
  INTO ASSET_LINK (ASSET_ID,POSITION_ID) values (700003,50000024)
  INTO SINGLE_CHOICE_VALUE (ASSET_ID,ENUMERATED_VALUE_ID,PROPERTY_ID) values (700003,(select id from enumerated_value where value = 40),(select id from property where public_id = 'rajoitus'))

  INTO ASSET (ID,ASSET_TYPE_ID,FLOATING,CREATED_BY) values (700004,20,0,'testfixture')
  INTO LRM_POSITION (ID, LINK_ID, MML_ID, START_MEASURE, END_MEASURE, SIDE_CODE) VALUES (50000026, 2226035, null, 0.000, 20.551, 1)
  INTO ASSET_LINK (ASSET_ID,POSITION_ID) values (700004,50000026)
  INTO SINGLE_CHOICE_VALUE (ASSET_ID,ENUMERATED_VALUE_ID,PROPERTY_ID) values (700004,(select id from enumerated_value where value = 40),(select id from property where public_id = 'rajoitus'))

  INTO ASSET (ID,ASSET_TYPE_ID,FLOATING,CREATED_BY) values (700005,20,0,'testfixture')
  INTO LRM_POSITION (ID, LINK_ID, MML_ID, START_MEASURE, END_MEASURE, SIDE_CODE) VALUES (50000028, 2226036, null, 0.000, 20.551, 1)
  INTO ASSET_LINK (ASSET_ID,POSITION_ID) values (700005,50000028)
  INTO SINGLE_CHOICE_VALUE (ASSET_ID,ENUMERATED_VALUE_ID,PROPERTY_ID) values (700005,(select id from enumerated_value where value = 40),(select id from property where public_id = 'rajoitus'))

SELECT * from dual;