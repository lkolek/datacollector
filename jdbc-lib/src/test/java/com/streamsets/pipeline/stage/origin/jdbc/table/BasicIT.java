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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.jdbc.table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.api.impl.ErrorMessage;
import com.streamsets.pipeline.lib.jdbc.JdbcErrors;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.sdk.SourceRunner;
import com.streamsets.pipeline.sdk.StageRunner;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BasicIT extends BaseTableJdbcSourceIT {
  private static final String STARS_INSERT_TEMPLATE = "INSERT into TEST.%s values (%s, '%s', '%s')";
  private static final String TRANSACTION_INSERT_TEMPLATE = "INSERT into TEST.%s values (%s, %s, '%s')";
  private static final String STREAMING_TABLE_INSERT_TEMPLATE = "INSERT into TEST.%s values (%s, '%s')";


  private static List<Record> EXPECTED_CRICKET_STARS_RECORDS;
  private static List<Record> EXPECTED_TENNIS_STARS_RECORDS;
  private static List<Record> EXPECTED_TRANSACTION_RECORDS;

  private static Record createSportsStarsRecords(int pid, String first_name, String last_name) {
    Record record = RecordCreator.create();
    LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
    fields.put("p_id", Field.create(pid));
    fields.put("first_name", Field.create(first_name));
    fields.put("last_name", Field.create(last_name));
    record.set(Field.createListMap(fields));
    return record;
  }

  private static List<Record> createTransactionRecords(int noOfRecords) {
    List<Record> records = new ArrayList<>();
    long currentTime = (System.currentTimeMillis() / 1000) * 1000;
    for (int i = 0; i < noOfRecords; i++) {
      Record record = RecordCreator.create();
      LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
      fields.put("unique_int", Field.create(i + 1));
      fields.put("t_date", Field.create(Field.Type.LONG, currentTime));
      fields.put("random_string", Field.create(UUID.randomUUID().toString()));
      record.set(Field.createListMap(fields));
      records.add(record);
      //making sure time is at least off by a second.
      currentTime = currentTime + 1000;
    }
    return records;
  }

  private static Record createStreamingTableRecord(int index) {
    Record record = RecordCreator.create();
    LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
    fields.put("unique_int", Field.create(Field.Type.INTEGER, index + 1));
    fields.put("random_string", Field.create(UUID.randomUUID().toString()));
    record.set(Field.createListMap(fields));
    return record;
  }

  @BeforeClass
  public static void setupTables() throws SQLException {
    EXPECTED_CRICKET_STARS_RECORDS = ImmutableList.of(
        createSportsStarsRecords(1, "Sachin", "Tendulkar"),
        createSportsStarsRecords(2, "Mahendra Singh", "Dhoni"),
        createSportsStarsRecords(3, "Don", "Bradman"),
        createSportsStarsRecords(4, "Brian", "Lara"),
        createSportsStarsRecords(5, "Allan", "Border"),
        createSportsStarsRecords(6, "Clive", "Lloyd"),
        createSportsStarsRecords(7, "Richard", "Hadlee"),
        createSportsStarsRecords(8, "Richie", "Benaud"),
        createSportsStarsRecords(9, "Sunil", "Gavaskar"),
        createSportsStarsRecords(10, "Shane", "Warne")
    );

    EXPECTED_TENNIS_STARS_RECORDS = ImmutableList.of(
        createSportsStarsRecords(1, "Novak", "Djokovic"),
        createSportsStarsRecords(2, "Andy", "Murray"),
        createSportsStarsRecords(3, "Stan", "Wawrinka"),
        createSportsStarsRecords(4, "Milos", "Raonic"),
        createSportsStarsRecords(5, "Kei", "Nishikori"),
        createSportsStarsRecords(6, "Rafael", "Nadal"),
        createSportsStarsRecords(7, "Roger", "Federer"),
        createSportsStarsRecords(8, "Dominic", "Thiem"),
        createSportsStarsRecords(9, "Tomas", "Berdych"),
        createSportsStarsRecords(10, "David", "Goffin"),
        createSportsStarsRecords(11, "Marin", "Cilic"),
        createSportsStarsRecords(12, "Gael", "Monfils"),
        createSportsStarsRecords(13, "Nick", "Kyrgios"),
        createSportsStarsRecords(14, "Roberto Bautista", "Agut"),
        createSportsStarsRecords(15, "Jo-Wilfried", "Tsonga")
    );

    EXPECTED_TRANSACTION_RECORDS = createTransactionRecords(20);

    try (Statement statement = connection.createStatement()) {
      //CRICKET_STARS
      statement.addBatch(
          "CREATE TABLE TEST.CRICKET_STARS " +
              "(p_id INT NOT NULL PRIMARY KEY, first_name VARCHAR(255), last_name VARCHAR(255))"
      );

      for (Record record : EXPECTED_CRICKET_STARS_RECORDS) {
        statement.addBatch(
            String.format(
                STARS_INSERT_TEMPLATE,
                "CRICKET_STARS",
                record.get("/p_id").getValueAsInteger(),
                record.get("/first_name").getValueAsString(),
                record.get("/last_name").getValueAsString()
            )
        );
      }

      //TENNIS STARS
      statement.addBatch(
          "CREATE TABLE TEST.TENNIS_STARS " +
              "(p_id INT NOT NULL PRIMARY KEY, first_name VARCHAR(255), last_name VARCHAR(255))"
      );

      for (Record record : EXPECTED_TENNIS_STARS_RECORDS) {
        statement.addBatch(
            String.format(
                STARS_INSERT_TEMPLATE,
                "TENNIS_STARS",
                record.get("/p_id").getValueAsInteger(),
                record.get("/first_name").getValueAsString(),
                record.get("/last_name").getValueAsString()
            )
        );
      }

      //TRANSACTION
      statement.addBatch(
          "CREATE TABLE TEST.TRANSACTION_TABLE " +
              "(unique_int INT NOT NULL , t_date BIGINT, random_string VARCHAR(255))"
      );

      for (Record record : EXPECTED_TRANSACTION_RECORDS) {
        statement.addBatch(
            String.format(
                TRANSACTION_INSERT_TEMPLATE,
                "TRANSACTION_TABLE",
                record.get("/unique_int").getValueAsInteger(),
                record.get("/t_date").getValueAsLong(),
                record.get("/random_string").getValueAsString()
            )
        );
      }

      statement.addBatch(
          "CREATE TABLE TEST.STREAMING_TABLE " +
              "(unique_int INT NOT NULL PRIMARY KEY, random_string VARCHAR(255))"
      );

      statement.executeBatch();
    }
  }

  @AfterClass
  public static void tearDown() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      for (String table : ImmutableList.of("CRICKET_STARS", "TENNIS_STARS", "TRANSACTION_TABLE", "STREAMING_TABLE")) {
        statement.addBatch(String.format(DROP_STATEMENT_TEMPLATE, database, table));
      }
      statement.executeBatch();
    }
  }

  @Test
  public void testNoTableMatchesTablePatternValidationError() throws Exception {
    TableConfigBean tableConfigBean =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("NO_TABLE%")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    List<Stage.ConfigIssue> issues = runner.runValidateConfigs();
    Assert.assertEquals(1, issues.size());
  }

  @Test
  public void testSingleTableSingleBatch() throws Exception {
    TableConfigBean tableConfigBean =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("CRICKET_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 1000);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(10, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS, records);
      Assert.assertEquals(0, runner.runProduce(output.getNewOffset(), 1000).getRecords().get("a").size());
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testSingleTableMultipleBatches() throws Exception {
    TableConfigBean tableConfigBean =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("CRICKET_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 5);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(5, 10), records);

      Assert.assertEquals(0, runner.runProduce(output.getNewOffset(), 1000).getRecords().get("a").size());
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testMultipleTablesSingleBatch() throws Exception {
    TableConfigBean tableConfigBean1 =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("CRICKET_STARS")
        .schema(database)
        .build();
    TableConfigBean tableConfigBean2 =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("TENNIS_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean1, tableConfigBean2))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 1000);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(10, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS, records);

      output = runner.runProduce(output.getNewOffset(), 1000);
      records = output.getRecords().get("a");
      Assert.assertEquals(15, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS, records);

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testBatchStrategySwitchTables() throws Exception {
    //With a '%_STARS' regex which has to select both tables.
    TableConfigBean tableConfigBean =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("%_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 5);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(5, 10), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(5, 10), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(10, 15), records);

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testBatchStrategyProcessAllRows() throws Exception {
    TableConfigBean tableConfigBean1 =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("TENNIS_STARS")
        .schema(database)
        .build();

    TableConfigBean tableConfigBean2 =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("CRICKET_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean1, tableConfigBean2))
        .batchTableStrategy(BatchTableStrategy.PROCESS_ALL_AVAILABLE_ROWS_FROM_TABLE)
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 5);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(5, 10), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TENNIS_STARS_RECORDS.subList(10, 15), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_CRICKET_STARS_RECORDS.subList(5, 10), records);
    } finally {
      runner.runDestroy();
    }
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testMetrics() throws Exception {
    //With a '%' regex which has to select both tables.
    TableConfigBean tableConfigBean =  new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
        .tablePattern("%_STARS")
        .schema(database)
        .build();

    TableJdbcSource tableJdbcSource = new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
        .tableConfigBeans(ImmutableList.of(tableConfigBean))
        .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    Stage.Context context = runner.getContext();
    try {
      Map<String, Object> gaugeMap = (Map<String, Object>)context.getGauge(TableJdbcSource.TABLE_METRICS).getValue();
      Integer numberOfTables = (int)gaugeMap.get(TableJdbcSource.TABLE_COUNT);
      Assert.assertEquals(2, numberOfTables.intValue());

      StageRunner.Output output = runner.runProduce("", 1000);
      Assert.assertEquals("TEST.CRICKET_STARS", gaugeMap.get(TableJdbcSource.CURRENT_TABLE));

      runner.runProduce(output.getNewOffset(), 1000);
      Assert.assertEquals("TEST.TENNIS_STARS", gaugeMap.get(TableJdbcSource.CURRENT_TABLE));
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testOverridePartitionColumn() throws Exception {
    TableConfigBean tableConfigBean =
        new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
            .tablePattern("TRANSACTION_TABLE")
            .schema(database)
            .overrideDefaultOffsetColumns(true)
            .offsetColumns(ImmutableList.of("T_DATE"))
            .build();

    TableJdbcSource tableJdbcSource =
        new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
            .tableConfigBeans(ImmutableList.of(tableConfigBean))
            .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce("", 5);
      List<Record> records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TRANSACTION_RECORDS.subList(0, 5), records);

      output = runner.runProduce(output.getNewOffset(), 5);
      records = output.getRecords().get("a");
      Assert.assertEquals(5, records.size());
      checkRecords(EXPECTED_TRANSACTION_RECORDS.subList(5, 10), records);

      output = runner.runProduce(output.getNewOffset(), 10);
      records = output.getRecords().get("a");
      Assert.assertEquals(10, records.size());
      checkRecords(EXPECTED_TRANSACTION_RECORDS.subList(10, 20), records);

    } finally {
      runner.runDestroy();
    }
  }

  private String testChangeInOffsetColumns(
      String table,
      String offset,
      List<String> offsetColumnsForThisRun,
      boolean shouldFail
  ) throws Exception {
    TableConfigBean tableConfigBean =
        new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
            .tablePattern(table)
            .schema(database)
            .overrideDefaultOffsetColumns(true)
            .offsetColumns(offsetColumnsForThisRun)
            .build();

    TableJdbcSource tableJdbcSource =
        new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
            .tableConfigBeans(ImmutableList.of(tableConfigBean))
            .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").setOnRecordError(OnRecordError.TO_ERROR)
        .build();
    runner.runInit();
    try {
      StageRunner.Output output = runner.runProduce(offset, 5);
      List<Record>  outputRecords = output.getRecords().get("a");
      if (shouldFail) {
        Assert.assertEquals(0, outputRecords.size());
        Assert.assertFalse(runner.getErrors().isEmpty());
        Assert.assertTrue(runner.getErrors().get(0).contains(JdbcErrors.JDBC_71.name()));
      } else {
        Assert.assertEquals(5, outputRecords.size());
        offset = output.getNewOffset();
      }
    } finally {
      runner.runDestroy();
    }
    return offset;
  }

  @Test
  public void testIncreaseNumberOfOffsetColumnsInConfig() throws Exception {
    String offset = "";
    String tableName = "TRANSACTION_TABLE";
    offset = testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("T_DATE"),
        false
    );

    //Now we added one more column to offset configuration, should fail
    testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("T_DATE", "UNIQUE_INT"),
        true
    );
  }

  @Test
  public void testDecreaseNumberOfOffsetColumnsInConfig() throws Exception {
    String offset = "";
    String tableName = "TRANSACTION_TABLE";
    offset = testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("T_DATE", "UNIQUE_INT"),
        false
    );

    //Now we removed one column from offset configuration, should fail
    testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("T_DATE"),
        true
    );
  }

  @Test
  public void testChangeInOffsetColumnNameInConfig() throws Exception {
    String offset = "";
    String tableName = "TRANSACTION_TABLE";
    offset = testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("UNIQUE_INT"),
        false
    );

    //Now we changed one column from offset configuration, should fail
    testChangeInOffsetColumns(
        tableName,
        offset,
        ImmutableList.of("T_DATE"),
        true
    );
  }

  @Test
  public void testStreamingInsertDuringSourceRun() throws Exception {
    TableConfigBean tableConfigBean =
        new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
            .tablePattern("STREAMING_TABLE")
            .schema(database)
            .build();
    TableJdbcSource tableJdbcSource =
        new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
            .tableConfigBeans(ImmutableList.of(tableConfigBean))
            .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a").build();
    runner.runInit();
    String offset = "";
    try {
      for (int i = 0 ; i < 10; i++) {
        Record record = createStreamingTableRecord(i);
        try (Statement statement = connection.createStatement()) {
          statement.execute(
              String.format(
                  STREAMING_TABLE_INSERT_TEMPLATE,
                  "STREAMING_TABLE",
                  record.get("/unique_int").getValueAsInteger(),
                  record.get("/random_string").getValueAsString()
              )
          );
        }
        StageRunner.Output output = runner.runProduce(offset, 1);
        List<Record> records = output.getRecords().get("a");
        Assert.assertEquals(1, records.size());
        checkRecords(ImmutableList.of(record), records);

        //Will close the old result set and generate an empty batch
        output = runner.runProduce(output.getNewOffset(), 1);
        Assert.assertTrue(output.getRecords().get("a").isEmpty());

        offset = output.getNewOffset();
      }
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testWrongInitialOffsetError() throws Exception {
    TableConfigBean tableConfigBean =
        new TableJdbcSourceTestBuilder.TableConfigBeanTestBuilder()
            .tablePattern("TRANSACTION_TABLE")
            .schema(database)
            .overrideDefaultOffsetColumns(true)
            .offsetColumns(ImmutableList.of("T_DATE"))
            .offsetColumnToInitialOffsetValue(
                ImmutableMap.of(
                    "T_DATE",
                    "${time:dateTimeToMilliseconds(time:extractDateFromString('abc', 'yyyy-mm-dd'))}"
                )
            )
            .build();

    TableJdbcSource tableJdbcSource =
        new TableJdbcSourceTestBuilder(JDBC_URL, true, USER_NAME, PASSWORD)
            .tableConfigBeans(ImmutableList.of(tableConfigBean))
            .build();

    SourceRunner runner = new SourceRunner.Builder(TableJdbcDSource.class, tableJdbcSource)
        .addOutputLane("a")
        .setOnRecordError(OnRecordError.TO_ERROR)
        .build();
    List<Stage.ConfigIssue> configIssues = runner.runValidateConfigs();
    Assert.assertEquals(1, configIssues.size());
    Assert.assertEquals(
        JdbcErrors.JDBC_73.getCode(),
        ((ErrorMessage)Whitebox.getInternalState(configIssues.get(0), "message")).getErrorCode()
    );
  }
}