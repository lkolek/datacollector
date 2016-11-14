/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.processor.zip;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.config.OnStagePreConditionFailure;
import com.streamsets.pipeline.sdk.ProcessorRunner;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.sdk.StageRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestFieldZipProcessor {

  @Test
  public void testFieldZip() throws StageException {
    FieldZipConfig zipConfig = new FieldZipConfig();
    zipConfig.firstField = "/list1";
    zipConfig.secondField = "/list2";
    zipConfig.zippedFieldPath = "/zipped";
    FieldZipConfigBean configBean = new FieldZipConfigBean();
    configBean.fieldZipConfigs = Lists.newArrayList(zipConfig);
    configBean.valuesOnly = false;
    configBean.onStagePreConditionFailure = OnStagePreConditionFailure.TO_ERROR;
    FieldZipProcessor processor = new FieldZipProcessor(configBean);

    ProcessorRunner runner = new ProcessorRunner.Builder(FieldZipDProcessor.class, processor)
        .addOutputLane("a").build();
    runner.runInit();

    try {
      Map<String, Field> map = new LinkedHashMap<>();
      List<Field> listField1 = ImmutableList.of(
          Field.create(1),
          Field.create(2),
          Field.create(3));
      List<Field> listField2 = ImmutableList.of(
          Field.create(11),
          Field.create(12),
          Field.create(13));
      map.put("list1", Field.create(listField1));
      map.put("list2", Field.create(listField2));
      Record record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      StageRunner.Output output = runner.runProcess(ImmutableList.of(record));
      Assert.assertEquals(1, output.getRecords().get("a").size());
      Field field = output.getRecords().get("a").get(0).get();
      Assert.assertTrue(field.getValueAsMap().containsKey("list1"));
      Assert.assertTrue(field.getValueAsMap().containsKey("list2"));
      Assert.assertTrue(field.getValueAsMap().containsKey("zipped"));
      List<Field> zipped = field.getValueAsMap().get("zipped").getValueAsList();
      Assert.assertEquals(3, zipped.size());

      for (int i = 0; i < zipped.size(); i++) {
        Field zippedElem = zipped.get(i);
        Assert.assertEquals(i+1, zippedElem.getValueAsMap().get("list1").getValueAsInteger());
        Assert.assertEquals(i+11, zippedElem.getValueAsMap().get("list2").getValueAsInteger());
      }

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testOneLongList() throws StageException {
    FieldZipConfig zipConfig = new FieldZipConfig();
    zipConfig.firstField = "/list1";
    zipConfig.secondField = "/list2";
    zipConfig.zippedFieldPath = "/zipped";
    FieldZipConfigBean configBean = new FieldZipConfigBean();
    configBean.fieldZipConfigs = Lists.newArrayList(zipConfig);
    configBean.valuesOnly = false;
    configBean.onStagePreConditionFailure = OnStagePreConditionFailure.TO_ERROR;
    FieldZipProcessor processor = new FieldZipProcessor(configBean);

    ProcessorRunner runner = new ProcessorRunner.Builder(FieldZipDProcessor.class, processor)
        .addOutputLane("a").build();
    runner.runInit();

    try {
      Map<String, Field> map = new LinkedHashMap<>();
      List<Field> listField1 = ImmutableList.of(
          Field.create(1),
          Field.create(2));
      List<Field> listField2 = ImmutableList.of(
          Field.create(11),
          Field.create(12),
          Field.create(13),
          Field.create(14));
      map.put("list1", Field.create(listField1));
      map.put("list2", Field.create(listField2));
      Record record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      StageRunner.Output output = runner.runProcess(ImmutableList.of(record));
      Assert.assertEquals(1, output.getRecords().get("a").size());
      Field field = output.getRecords().get("a").get(0).get();
      Assert.assertTrue(field.getValueAsMap().containsKey("list1"));
      Assert.assertTrue(field.getValueAsMap().containsKey("list2"));
      Assert.assertTrue(field.getValueAsMap().containsKey("zipped"));
      List<Field> zipped = field.getValueAsMap().get("zipped").getValueAsList();
      Assert.assertEquals(2, zipped.size());
      List<Field> originalLong = field.getValueAsMap().get("list2").getValueAsList();
      Assert.assertEquals(4, originalLong.size());

      for (int i = 0; i < zipped.size(); i++) {
        Field zippedElem = zipped.get(i);
        Assert.assertEquals(i+1, zippedElem.getValueAsMap().get("list1").getValueAsInteger());
        Assert.assertEquals(i+11, zippedElem.getValueAsMap().get("list2").getValueAsInteger());
      }

      zipConfig.firstField = "/list2";
      zipConfig.secondField = "/list1";
      zipConfig.zippedFieldPath = "/zipped";
      configBean = new FieldZipConfigBean();
      configBean.fieldZipConfigs = Lists.newArrayList(zipConfig);
      configBean.valuesOnly = false;
      configBean.onStagePreConditionFailure = OnStagePreConditionFailure.TO_ERROR;
      processor = new FieldZipProcessor(configBean);

      runner = new ProcessorRunner.Builder(FieldZipDProcessor.class, processor)
          .addOutputLane("a").build();
      runner.runInit();

      map = new LinkedHashMap<>();
      listField1 = ImmutableList.of(
          Field.create(1),
          Field.create(2));
      listField2 = ImmutableList.of(
          Field.create(11),
          Field.create(12),
          Field.create(13),
          Field.create(14));
      map.put("list1", Field.create(listField1));
      map.put("list2", Field.create(listField2));
      record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      output = runner.runProcess(ImmutableList.of(record));
      Assert.assertEquals(1, output.getRecords().get("a").size());
      field = output.getRecords().get("a").get(0).get();
      Assert.assertTrue(field.getValueAsMap().containsKey("list1"));
      Assert.assertTrue(field.getValueAsMap().containsKey("list2"));
      Assert.assertTrue(field.getValueAsMap().containsKey("zipped"));
      zipped = field.getValueAsMap().get("zipped").getValueAsList();
      Assert.assertEquals(2, zipped.size());
      originalLong = field.getValueAsMap().get("list2").getValueAsList();
      Assert.assertEquals(4, originalLong.size());

      for (int i = 0; i < zipped.size(); i++) {
        Field zippedElem = zipped.get(i);
        Assert.assertEquals(i+1, zippedElem.getValueAsMap().get("list1").getValueAsInteger());
        Assert.assertEquals(i+11, zippedElem.getValueAsMap().get("list2").getValueAsInteger());
      }

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testMapListZip() throws StageException {
    FieldZipConfig zipConfig = new FieldZipConfig();
    zipConfig.firstField = "/list";
    zipConfig.secondField = "/map";
    zipConfig.zippedFieldPath = "/zipped";
    FieldZipConfigBean configBean = new FieldZipConfigBean();
    configBean.fieldZipConfigs = Lists.newArrayList(zipConfig);
    configBean.valuesOnly = false;
    configBean.onStagePreConditionFailure = OnStagePreConditionFailure.TO_ERROR;
    FieldZipProcessor processor = new FieldZipProcessor(configBean);

    ProcessorRunner runner = new ProcessorRunner.Builder(FieldZipDProcessor.class, processor)
        .addOutputLane("a").build();
    runner.runInit();

    try {
      Map<String, Field> map = new LinkedHashMap<>();
      List<Field> listField = ImmutableList.of(
          Field.create(1),
          Field.create(2),
          Field.create(3));
      LinkedHashMap<String, Field> mapField = new LinkedHashMap<>();
      mapField.put("mapField1", Field.create(11));
      mapField.put("mapField2", Field.create(12));
      mapField.put("mapField3", Field.create(13));
      map.put("list", Field.create(listField));
      map.put("map", Field.createListMap(mapField));
      Record record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      StageRunner.Output output = runner.runProcess(ImmutableList.of(record));
      Assert.assertEquals(1, output.getRecords().get("a").size());
      Field field = output.getRecords().get("a").get(0).get();
      Assert.assertTrue(field.getValueAsMap().containsKey("list"));
      Assert.assertTrue(field.getValueAsMap().containsKey("map"));
      Assert.assertTrue(field.getValueAsMap().containsKey("zipped"));
      List<Field> zipped = field.getValueAsMap().get("zipped").getValueAsList();
      Assert.assertEquals(3, zipped.size());

      for (int i = 0; i < zipped.size(); i++) {
        Field zippedElem = zipped.get(i);
        Assert.assertEquals(i+1, zippedElem.getValueAsMap().get("list").getValueAsInteger());
        Assert.assertEquals(i+11, zippedElem.getValueAsMap().get("mapField" + Integer.toString(i+1)).getValueAsInteger());
      }

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testMapZip() throws StageException {
    FieldZipConfig zipConfig = new FieldZipConfig();
    zipConfig.firstField = "/map1";
    zipConfig.secondField = "/map2";
    zipConfig.zippedFieldPath = "/zipped";
    FieldZipConfigBean configBean = new FieldZipConfigBean();
    configBean.fieldZipConfigs = Lists.newArrayList(zipConfig);
    configBean.valuesOnly = false;
    configBean.onStagePreConditionFailure = OnStagePreConditionFailure.TO_ERROR;
    FieldZipProcessor processor = new FieldZipProcessor(configBean);

    ProcessorRunner runner = new ProcessorRunner.Builder(FieldZipDProcessor.class, processor)
        .addOutputLane("a").build();
    runner.runInit();

    try {
      Map<String, Field> map = new LinkedHashMap<>();
      LinkedHashMap<String, Field> mapField1 = new LinkedHashMap<>();
      mapField1.put("mapField1", Field.create(1));
      mapField1.put("mapField2", Field.create(2));
      mapField1.put("mapField3", Field.create(3));
      LinkedHashMap<String, Field> mapField2 = new LinkedHashMap<>();
      mapField2.put("mapField11", Field.create(11));
      mapField2.put("mapField12", Field.create(12));
      mapField2.put("mapField13", Field.create(13));
      map.put("map1", Field.createListMap(mapField1));
      map.put("map2", Field.createListMap(mapField2));
      Record record = RecordCreator.create("s", "s:1");
      record.set(Field.create(map));

      StageRunner.Output output = runner.runProcess(ImmutableList.of(record));
      Assert.assertEquals(1, output.getRecords().get("a").size());
      Field field = output.getRecords().get("a").get(0).get();
      Assert.assertTrue(field.getValueAsMap().containsKey("map1"));
      Assert.assertTrue(field.getValueAsMap().containsKey("map2"));
      Assert.assertTrue(field.getValueAsMap().containsKey("zipped"));
      List<Field> zipped = field.getValueAsMap().get("zipped").getValueAsList();
      Assert.assertEquals(3, zipped.size());

      for (int i = 0; i < zipped.size(); i++) {
        Field zippedElem = zipped.get(i);
        Assert.assertEquals(i+1, zippedElem.getValueAsMap().get("mapField" + Integer.toString(i+1)).getValueAsInteger());
        Assert.assertEquals(i+11, zippedElem.getValueAsMap().get("mapField" + Integer.toString(i+11)).getValueAsInteger());
      }

    } finally {
      runner.runDestroy();
    }
  }
}
