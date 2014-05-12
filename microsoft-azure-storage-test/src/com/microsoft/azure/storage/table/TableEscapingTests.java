/**
 * Copyright Microsoft Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.azure.storage.table;

import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.TableTestHelper.Class1;

/**
 * Table Escaping Tests
 */
public class TableEscapingTests extends TestCase {

    private CloudTable table;

    @Override
    public void setUp() throws Exception {
        table = TableTestHelper.getRandomTableReference();
        table.createIfNotExists();
    }

    @Override
    public void tearDown() throws Exception {
        table.deleteIfExists();
    }

    public void testEmptyString() throws StorageException {
        doEscapeTest("", false, true);
    }

    public void testEmptyStringBatch() throws StorageException {
        doEscapeTest("", true, true);
    }

    public void testRandomChars() throws StorageException {
        doEscapeTest("!$'\"()*+,;=", false);
    }

    public void testRandomCharsBatch() throws StorageException {
        doEscapeTest("!$'\"()*+,;=", true);
    }

    public void testPercent25() throws StorageException {
        doEscapeTest("foo%25", false, true);
    }

    public void testPercent25Batch() throws StorageException {
        doEscapeTest("foo%25", true, true);
    }

    public void testRegularPKInQuery() throws StorageException {
        doQueryEscapeTest("data");
    }

    public void testSpecialChars() throws StorageException {
        doEscapeTest("\\ // @ ? <?", true);
    }

    public void testSpecialCharsBatch() throws StorageException {
        doEscapeTest("\\ // @ ? <?", true);
    }

    public void testUnicode() throws StorageException {
        doEscapeTest("\u00A9\u770b\u5168\u90e8", false, true);
        doEscapeTest("char中文test", false, true);
        doEscapeTest("charä¸­æ–‡test", false, true);
        doEscapeTest("世界你好", false, true);
    }

    public void testUnicodeBatch() throws StorageException {
        doEscapeTest("\u00A9\u770b\u5168\u90e8", true, true);
        doEscapeTest("char中文test", true, true);
        doEscapeTest("charä¸­æ–‡test", true, true);
        doEscapeTest("世界你好", true, true);
    }

    public void testUnicodeInQuery() throws StorageException {
        doQueryEscapeTest("char中文test");
        doQueryEscapeTest("charä¸­æ–‡test");
        doQueryEscapeTest("世界你好");
        doQueryEscapeTest("\u00A9\u770b\u5168\u90e8");
    }

    public void testWhiteSpaceOnly() throws StorageException {
        doEscapeTest("     ", false, true);
    }

    public void testWhiteSpaceOnlyBatch() throws StorageException {
        doEscapeTest("     ", true, true);
    }

    public void testWhiteSpaceOnlyInQuery() throws StorageException {
        doQueryEscapeTest("     ");
    }

    public void testXmlTest() throws StorageException {
        doEscapeTest("</>", false);
        doEscapeTest("<tag>", false);
        doEscapeTest("</entry>", false);
        doEscapeTest("!<", false);
        doEscapeTest("<!%^&j", false);
    }

    public void testXmlTestBatch() throws StorageException {
        doEscapeTest("</>", false);
        doEscapeTest("<tag>", false);
        doEscapeTest("</entry>", false);
        doEscapeTest("!<", false);
        doEscapeTest("<!%^&j", false);
    }

    private void doEscapeTest(String data, boolean useBatch) throws StorageException {
        doEscapeTest(data, useBatch, false);
    }

    private void doEscapeTest(String data, boolean useBatch, boolean includeInKey) throws StorageException {
        TableRequestOptions options = new TableRequestOptions();

        options.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);
        doEscapeTestHelper(data, useBatch, includeInKey, options);

        options.setTablePayloadFormat(TablePayloadFormat.Json);
        doEscapeTestHelper(data, useBatch, includeInKey, options);

        options.setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);
        doEscapeTestHelper(data, useBatch, includeInKey, options);

        options.setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);
        options.setPropertyResolver(new Class1());
        doEscapeTestHelper(data, useBatch, includeInKey, options);
    }

    private void doEscapeTestHelper(String data, boolean useBatch, boolean includeInKey, TableRequestOptions options)
            throws StorageException {
        Class1 ref = new Class1();
        ref.setA(data);
        ref.setPartitionKey(includeInKey ? "temp" + data : "temp");
        ref.setRowKey(UUID.randomUUID().toString());

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.insert(ref);
            table.execute(batch, options, null);
        }
        else {
            table.execute(TableOperation.insert(ref), options, null);
        }

        TableResult res = null;

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class);
            res = table.execute(batch, options, null).get(0);
        }
        else {
            res = table.execute(TableOperation.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class), options,
                    null);
        }

        Class1 retObj = res.getResultAsType();
        assertEquals(ref.getA(), retObj.getA());
        assertEquals(ref.getPartitionKey(), retObj.getPartitionKey());

        ref.setEtag(retObj.getEtag());
        ref.setB(data);

        // Merge
        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.merge(ref);
            table.execute(batch, options, null);
        }
        else {
            table.execute(TableOperation.merge(ref), options, null);
        }

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class);
            res = table.execute(batch, options, null).get(0);
        }
        else {
            res = table.execute(TableOperation.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class), options,
                    null);
        }

        retObj = res.getResultAsType();
        assertEquals(ref.getA(), retObj.getA());
        assertEquals(ref.getB(), retObj.getB());

        // Replace
        ref.setEtag(retObj.getEtag());
        ref.setC(data);

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.replace(ref);
            table.execute(batch, options, null);
        }
        else {
            table.execute(TableOperation.replace(ref), options, null);
        }

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class);
            res = table.execute(batch, options, null).get(0);
        }
        else {
            res = table.execute(TableOperation.retrieve(ref.getPartitionKey(), ref.getRowKey(), Class1.class), options,
                    null);
        }

        retObj = res.getResultAsType();
        assertEquals(ref.getA(), retObj.getA());
        assertEquals(ref.getB(), retObj.getB());
        assertEquals(ref.getC(), retObj.getC());

        if (useBatch) {
            TableBatchOperation batch = new TableBatchOperation();
            batch.delete(retObj);
            res = table.execute(batch, options, null).get(0);
        }
        else {
            res = table.execute(TableOperation.delete(retObj), options, null);
        }
    }

    private void doQueryEscapeTest(String data) throws StorageException {
        TableRequestOptions options = new TableRequestOptions();

        options.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);
        doQueryEscapeTestHelper(data, options);

        options.setTablePayloadFormat(TablePayloadFormat.Json);
        doQueryEscapeTestHelper(data, options);

        options.setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);
        doQueryEscapeTestHelper(data, options);

        options.setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);
        options.setPropertyResolver(new Class1());
        doQueryEscapeTestHelper(data, options);
    }

    private void doQueryEscapeTestHelper(String data, TableRequestOptions options) throws StorageException {
        Class1 ref = new Class1();
        ref.setA(data);
        ref.setPartitionKey(UUID.randomUUID().toString());
        ref.setRowKey("foo");

        table.execute(TableOperation.insert(ref), options, null);
        TableQuery<Class1> query = TableQuery.from(Class1.class).where(
                String.format("(PartitionKey eq '%s') and (A eq '%s')", ref.getPartitionKey(), data));

        int count = 0;

        for (Class1 ent : table.execute(query, options, null)) {
            count++;
            assertEquals(ent.getA(), ref.getA());
            assertEquals(ent.getB(), ref.getB());
            assertEquals(ent.getC(), ref.getC());
            assertEquals(ent.getPartitionKey(), ref.getPartitionKey());
            assertEquals(ent.getRowKey(), ref.getRowKey());
        }

        assertEquals(count, 1);
    }
}
