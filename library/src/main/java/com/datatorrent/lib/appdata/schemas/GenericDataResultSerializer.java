/*
 *  Copyright (c) 2012-2015 Malhar, Inc.
 *  All Rights Reserved.
 */

package com.datatorrent.lib.appdata.schemas;

import com.datatorrent.lib.appdata.dimensions.DimensionsDescriptor;
import com.datatorrent.lib.appdata.gpo.GPOMutable;
import com.datatorrent.lib.appdata.gpo.GPOUtils;
import com.datatorrent.lib.appdata.qr.CustomDataSerializer;
import com.datatorrent.lib.appdata.qr.Result;
import com.google.common.collect.Sets;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 *
 * @author Timothy Farkas: tim@datatorrent.com
 */
public class GenericDataResultSerializer implements CustomDataSerializer
{
  private static final Logger logger = LoggerFactory.getLogger(GenericDataResultSerializer.class);

  public GenericDataResultSerializer()
  {
  }

  @Override
  public String serialize(Result result)
  {
    try {
      return serializeHelper(result);
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String serializeHelper(Result result) throws Exception
  {
    GenericDataResult dataResult = (GenericDataResult) result;

    JSONObject jo = new JSONObject();

    jo.put(Result.FIELD_ID, dataResult.getId());
    jo.put(Result.FIELD_TYPE, dataResult.getType());

    JSONArray data = new JSONArray();
    jo.put(Result.FIELD_DATA, data);

    Fields fields = dataResult.getDataQuery().getFields();
    logger.info("fields: {}", fields);

    List<GPOMutable> keys = dataResult.getKeys();
    List<GPOMutable> values = dataResult.getValues();

    boolean hasTime = fields.getFields().contains(DimensionsDescriptor.DIMENSION_TIME);

    if(hasTime) {
      Set<String> fieldsSet = Sets.newHashSet();
      fieldsSet.addAll(fields.getFields());
      fieldsSet.remove(DimensionsDescriptor.DIMENSION_TIME);
      fields = new Fields(fieldsSet);
    }

    for(int index = 0;
        index < keys.size();
        index++) {
      GPOMutable value = values.get(index);
      JSONObject valueJO = GPOUtils.serializeJSONObject(value, fields);

      GPOMutable key = keys.get(index);

      if(hasTime) {
        long time = key.getFieldLong(DimensionsDescriptor.DIMENSION_TIME);
        valueJO.put(DimensionsDescriptor.DIMENSION_TIME, SchemaUtils.getDateString(time));
      }

      data.put(valueJO);
    }

    if(!dataResult.getDataQuery().getOneTime()) {
      jo.put(GenericDataResult.FIELD_COUNTDOWN,
             dataResult.getCountdown());
    }

    return jo.toString();
  }
}
