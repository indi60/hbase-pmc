/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hadoop.hbase.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.LargeTests;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.io.hfile.Compression;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A write/read/verify load test on a mini HBase cluster. Tests reading
 * and then writing.
 */
@Category(LargeTests.class)
@RunWith(Parameterized.class)
public class TestMiniClusterLoadSequential {

  private static final Log LOG = LogFactory.getLog(
      TestMiniClusterLoadSequential.class);

  protected static final byte[] TABLE = Bytes.toBytes("load_test_tbl");
  protected static final byte[] CF = Bytes.toBytes("load_test_cf");
  protected static final int NUM_THREADS = 8;
  protected static final int NUM_RS = 2;
  protected static final int TIMEOUT_MS = 120000;
  protected static final HBaseTestingUtility TEST_UTIL =
      new HBaseTestingUtility();

  protected final Configuration conf = TEST_UTIL.getConfiguration();
  protected final boolean isMultiPut;
  protected final DataBlockEncoding dataBlockEncoding;

  protected MultiThreadedWriter writerThreads;
  protected MultiThreadedReader readerThreads;
  protected int numKeys;

  protected Compression.Algorithm compression = Compression.Algorithm.NONE;

  public TestMiniClusterLoadSequential(boolean isMultiPut,
      DataBlockEncoding dataBlockEncoding) {
    this.isMultiPut = isMultiPut;
    this.dataBlockEncoding = dataBlockEncoding;
    conf.setInt(HConstants.HREGION_MEMSTORE_FLUSH_SIZE, 1024 * 1024);
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    List<Object[]> parameters = new ArrayList<Object[]>();
    for (boolean multiPut : new boolean[]{false, true}) {
      for (DataBlockEncoding dataBlockEncoding : new DataBlockEncoding[] {
          DataBlockEncoding.NONE, DataBlockEncoding.PREFIX }) {
        parameters.add(new Object[]{multiPut, dataBlockEncoding});
      }
    }
    return parameters;
  }

  @Before
  public void setUp() throws Exception {
    LOG.debug("Test setup: isMultiPut=" + isMultiPut);
    TEST_UTIL.startMiniCluster(1, NUM_RS);
  }

  @After
  public void tearDown() throws Exception {
    LOG.debug("Test teardown: isMultiPut=" + isMultiPut);
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test(timeout=TIMEOUT_MS)
  public void loadTest() throws Exception {
    prepareForLoadTest();
    runLoadTestOnExistingTable();
  }

  protected void runLoadTestOnExistingTable() throws IOException {
    writerThreads.start(0, numKeys, NUM_THREADS);
    writerThreads.waitForFinish();
    assertEquals(0, writerThreads.getNumWriteFailures());

    readerThreads.start(0, numKeys, NUM_THREADS);
    readerThreads.waitForFinish();
    assertEquals(0, readerThreads.getNumReadFailures());
    assertEquals(0, readerThreads.getNumReadErrors());
    assertEquals(numKeys, readerThreads.getNumKeysVerified());
  }

  protected void prepareForLoadTest() throws IOException {
    LOG.info("Starting load test: dataBlockEncoding=" + dataBlockEncoding +
        ", isMultiPut=" + isMultiPut);
    numKeys = numKeys();
    HBaseAdmin admin = new HBaseAdmin(conf);
    while (admin.getClusterStatus().getServers().size() < NUM_RS) {
      LOG.info("Sleeping until " + NUM_RS + " RSs are online");
      Threads.sleepWithoutInterrupt(1000);
    }
    admin.close();

    int numRegions = HBaseTestingUtility.createPreSplitLoadTestTable(conf,
        TABLE, CF, compression, dataBlockEncoding);

    TEST_UTIL.waitUntilAllRegionsAssigned(numRegions);

    writerThreads = new MultiThreadedWriter(conf, TABLE, CF);
    writerThreads.setMultiPut(isMultiPut);
    readerThreads = new MultiThreadedReader(conf, TABLE, CF, 100);
  }

  protected int numKeys() {
    return 10000;
  }

  protected HColumnDescriptor getColumnDesc(HBaseAdmin admin)
      throws TableNotFoundException, IOException {
    return admin.getTableDescriptor(TABLE).getFamily(CF);
  }

}
