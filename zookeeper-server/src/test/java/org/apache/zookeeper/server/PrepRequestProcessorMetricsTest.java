/**
 * Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.zookeeper.server;

import org.apache.jute.BinaryOutputArchive;
import org.apache.jute.Record;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.StatPersisted;
import org.apache.zookeeper.proto.DeleteRequest;
import org.apache.zookeeper.proto.SetDataRequest;
import org.apache.zookeeper.server.ZooKeeperServer.ChangeRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepRequestProcessorMetricsTest {
    private static final Logger LOG = LoggerFactory.getLogger(PrepRequestProcessorMetricsTest.class);

    ZooKeeperServer zks;
    RequestProcessor nextProcessor;
    boolean oldSkipAcl;

    @Before
    public void setup() throws NoSuchFieldException {
        oldSkipAcl = PrepRequestProcessor.skipACL;

        zks = mock(ZooKeeperServer.class);
        zks.sessionTracker = mock(SessionTracker.class);

        Deque<ChangeRecord> outstandingChanges = new ArrayDeque<>();
        Map<String, ChangeRecord> outstandingChangesForPath = new HashMap<>();
        when(zks.getOutstandingChanges()).thenReturn(outstandingChanges);
        when(zks.getOutstandingChangesForPath()).thenReturn(outstandingChangesForPath);

        ZKDatabase db = mock(ZKDatabase.class);
        when(zks.getZKDatabase()).thenReturn(db);

        DataNode node = new DataNode(new byte[1], null, mock(StatPersisted.class));
        when(db.getNode(anyString())).thenReturn(node);

        Set<String> ephemerals = new HashSet<>();
        ephemerals.add("/crystalmountain");
        ephemerals.add("/stevenspass");
        when(db.getEphemerals(anyLong())).thenReturn(ephemerals);

        nextProcessor = mock(RequestProcessor.class);

        ServerMetrics.resetAll();
    }

    @After
    public void teardown() {
        PrepRequestProcessor.skipACL = oldSkipAcl;
    }

    private Request createRequest(Record record, int opCode) throws IOException {
        // encoding
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
        record.serialize(boa, "request");
        baos.close();
        return new Request(null, 1l, 0, opCode, ByteBuffer.wrap(baos.toByteArray()), null);
    }

    private Request createRequest(String path, int opCode) throws IOException {
        Record record;
        switch (opCode) {
            case ZooDefs.OpCode.setData:
                record = new SetDataRequest(path, new byte[0], -1);
                break;
            case ZooDefs.OpCode.delete:
                record = new DeleteRequest(path, -1);
                break;
            default:
                record = new DeleteRequest(path, -1);
                break;
        }

        return createRequest(record, opCode);
    }

    private Request createRequest(long sessionId, int opCode) {
        return new Request(null, sessionId, 0, opCode, null, null);
    }

    @Test
    public void testPrepRequestProcessorMetrics() throws Exception {
        PrepRequestProcessor prepRequestProcessor = new PrepRequestProcessor(zks, nextProcessor);
        PrepRequestProcessor.skipACL = true;

        //setData will generate one change
        prepRequestProcessor.processRequest(createRequest("/foo", ZooDefs.OpCode.setData));
        //delete will generate two changes, one for itself, one for its parent
        prepRequestProcessor.processRequest(createRequest("/foo/bar", ZooDefs.OpCode.delete));
        //mocking two ephemeral nodes exists for this session so two changes
        prepRequestProcessor.processRequest(createRequest(2, ZooDefs.OpCode.closeSession));

        Map<String, Object> values = ServerMetrics.getAllValues();
        Assert.assertEquals(3L, values.get("prep_processor_request_queued"));

        prepRequestProcessor.start();

        Thread.sleep(500);

        values = ServerMetrics.getAllValues();
        Assert.assertEquals(3L, values.get("max_prep_processor_queue_size"));

        Assert.assertThat((long)values.get("min_prep_processor_queue_time_ms"), greaterThan(0l));
        Assert.assertEquals(3L, values.get("cnt_prep_processor_queue_time_ms"));

        Assert.assertEquals(3L, values.get("cnt_prep_process_time"));
        Assert.assertThat((long)values.get("min_prep_process_time"), greaterThan(0l));

        Assert.assertEquals(1L, values.get("cnt_close_session_prep_time"));
        Assert.assertThat((long)values.get("min_close_session_prep_time"), greaterThan(0L));

        Assert.assertEquals(5L, values.get("outstanding_changes_queued"));
    }
}
