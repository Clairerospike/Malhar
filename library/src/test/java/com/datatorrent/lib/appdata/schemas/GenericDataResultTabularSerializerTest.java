/*
 *  Copyright (c) 2012-2015 Malhar, Inc.
 *  All Rights Reserved.
 */

package com.datatorrent.lib.appdata.schemas;

import com.datatorrent.lib.appdata.gpo.GPOMutable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Timothy Farkas: tim@datatorrent.com
 */
public class GenericDataResultTabularSerializerTest
{
  private static final Logger logger = LoggerFactory.getLogger(GenericDataResultTabularSerializerTest.class);

  @Test
  public void simpleResultSerializerTest()
  {
    Map<String, Type> fieldToTypeA = Maps.newHashMap();
    fieldToTypeA.put("a", Type.LONG);
    fieldToTypeA.put("b", Type.STRING);

    FieldsDescriptor fdA = new FieldsDescriptor(fieldToTypeA);

    GPOMutable gpoA = new GPOMutable(fdA);
    gpoA.setField("a", 1L);
    gpoA.setField("b", "hello");

    FieldsDescriptor fdB = new FieldsDescriptor(fieldToTypeA);

    GPOMutable gpoB = new GPOMutable(fdB);
    gpoB.setField("a", 2L);
    gpoB.setField("b", "world");

    List<GPOMutable> values = Lists.newArrayList();
    values.add(gpoA);
    values.add(gpoB);

    GenericDataQueryTabular gQuery = new GenericDataQueryTabular("1",
                                                                 GenericDataQueryTabular.TYPE,
                                                                 new Fields(Sets.newHashSet("a", "b")));

    GenericDataResultTabular result = new GenericDataResultTabular(gQuery,
                                                                   values);
    result.setType(GenericDataResultTabular.TYPE);

    GenericDataResultTabularSerializer serializer = new GenericDataResultTabularSerializer();

    final String expectedJSONResult =
    "{\"id\":\"1\",\"type\":\"dataResult\",\"data\":[{\"b\":\"hello\",\"a\":1},{\"b\":\"world\",\"a\":2}]}";

    Assert.assertEquals("The json doesn't match.", expectedJSONResult, serializer.serialize(result));
  }
}
