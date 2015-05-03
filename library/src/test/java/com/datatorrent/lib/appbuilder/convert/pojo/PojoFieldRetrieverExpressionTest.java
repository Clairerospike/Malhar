/*
 * Copyright (c) 2015 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.lib.appbuilder.convert.pojo;

import com.datatorrent.lib.appdata.schemas.Type;
import com.datatorrent.lib.util.TestUtils;
import com.esotericsoftware.kryo.Kryo;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class PojoFieldRetrieverExpressionTest
{
  @Test
  public void simpleTest() throws Exception
  {
    Map<String, String> fieldToType = Maps.newHashMap();

    fieldToType.put("boolVal", Type.BOOLEAN.name());
    fieldToType.put("byteVal", Type.BYTE.name());
    fieldToType.put("charVal", Type.CHAR.name());
    fieldToType.put("stringVal", Type.STRING.name());
    fieldToType.put("shortVal", Type.SHORT.name());
    fieldToType.put("intVal", Type.INTEGER.name());
    fieldToType.put("longVal", Type.LONG.name());
    fieldToType.put("floatVal", Type.FLOAT.name());
    fieldToType.put("doubleVal", Type.DOUBLE.name());
    fieldToType.put("objVal", Type.OBJECT.name());

    Map<String, String> fieldToExpression = Maps.newHashMap();

    fieldToExpression.put("boolVal", "innerObj.boolVal");
    fieldToExpression.put("byteVal", "innerObj.byteVal");
    fieldToExpression.put("charVal", "innerObj.charVal");
    fieldToExpression.put("stringVal", "innerObj.stringVal");
    fieldToExpression.put("shortVal", "innerObj.shortVal");
    fieldToExpression.put("intVal", "innerObj.intVal");
    fieldToExpression.put("longVal", "innerObj.longVal");
    fieldToExpression.put("floatVal", "innerObj.floatVal");
    fieldToExpression.put("doubleVal", "innerObj.doubleVal");
    fieldToExpression.put("objVal", "innerObj.objVal");

    PojoFieldRetrieverExpression pfre = new PojoFieldRetrieverExpression();
    pfre.setFieldToType(fieldToType);
    pfre.setFieldToExpression(fieldToExpression);
    pfre.setFqClassName(TestObjAllTypes.class.getName());

    pfre.setup();

    TestObjAllTypes testObj = new TestObjAllTypes();

    Assert.assertEquals(testObj.innerObj.isBoolVal(), pfre.getBoolean("boolVal", testObj));
    Assert.assertEquals(testObj.innerObj.getByteVal(), pfre.getByte("byteVal", testObj));
    Assert.assertEquals(testObj.innerObj.getCharVal(), pfre.getChar("charVal", testObj));
    Assert.assertEquals(testObj.innerObj.getStringVal(), pfre.getString("stringVal", testObj));
    Assert.assertEquals(testObj.innerObj.getShortVal(), pfre.getShort("shortVal", testObj));
    Assert.assertEquals(testObj.innerObj.getIntVal(), pfre.getInt("intVal", testObj));
    Assert.assertEquals(testObj.innerObj.getLongVal(), pfre.getLong("longVal", testObj));
    Assert.assertEquals(testObj.innerObj.getFloatVal(), pfre.getFloat("floatVal", testObj), 0.0f);
    Assert.assertEquals(testObj.innerObj.getDoubleVal(), pfre.getDouble("doubleVal", testObj), 0.0f);
    Assert.assertEquals(testObj.innerObj.getObjVal(), pfre.getObject("objVal", testObj));

    Assert.assertEquals((Boolean) testObj.innerObj.isBoolVal(), pfre.get("boolVal", testObj));
    Assert.assertEquals((Byte) testObj.innerObj.getByteVal(), pfre.get("byteVal", testObj));
    Assert.assertEquals((Character) testObj.innerObj.getCharVal(), pfre.get("charVal", testObj));
    Assert.assertEquals(testObj.innerObj.getStringVal(), pfre.get("stringVal", testObj));
    Assert.assertEquals((Short) testObj.innerObj.getShortVal(), pfre.get("shortVal", testObj));
    Assert.assertEquals((Integer) testObj.innerObj.getIntVal(), pfre.get("intVal", testObj));
    Assert.assertEquals((Long) testObj.innerObj.getLongVal(), pfre.get("longVal", testObj));
    Assert.assertEquals((Float) testObj.innerObj.getFloatVal(), pfre.get("floatVal", testObj));
    Assert.assertEquals((Double) testObj.innerObj.getDoubleVal(), pfre.get("doubleVal", testObj));
    Assert.assertEquals(testObj.innerObj.getObjVal(), pfre.get("objVal", testObj));

    //Check serialization
    TestUtils.clone(new Kryo(), pfre);
  }
}
