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

import java.net.URISyntaxException;
import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.TableRequestOptions.PropertyResolver;
import com.microsoft.azure.storage.table.TableTestHelper.Class1;

public class TableODataTests extends TestCase {

    TableRequestOptions options;
    DynamicTableEntity ent;

    private CloudTable table;

    @Override
    public void setUp() throws StorageException, URISyntaxException {
        this.table = TableTestHelper.getRandomTableReference();
        this.table.createIfNotExists();

        final CloudTableClient tClient = TableTestHelper.createCloudTableClient();
        this.options = TableRequestOptions.populateAndApplyDefaults(this.options, tClient);
        this.options.setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);

        // Insert Entity
        this.ent = new DynamicTableEntity();
        this.ent.setPartitionKey("jxscl_odata");
        this.ent.setRowKey(UUID.randomUUID().toString());

        this.ent.getProperties().put("foo2", new EntityProperty("bar2"));
        this.ent.getProperties().put("foo", new EntityProperty("bar"));
        this.ent.getProperties().put("fooint", new EntityProperty(1234));

        this.table.execute(TableOperation.insert(this.ent), this.options, null);
    }

    @Override
    public void tearDown() throws StorageException {
        this.table.execute(TableOperation.delete(this.ent), this.options, null);
        this.table.deleteIfExists();
    }

    public void testTableOperationRetrieveJsonNoMetadataFail() {

        // set custom property resolver
        this.options.setPropertyResolver(new CustomPropertyResolver());

        try {
            this.table.execute(TableOperation.retrieve(this.ent.getPartitionKey(), this.ent.getRowKey(), Class1.class),
                    this.options, null);
            fail("Invalid property resolver should throw");
        }
        catch (StorageException e) {
            assertEquals("Failed to parse property 'fooint' with value '1234' as type 'Edm.Guid'", e.getMessage());
        }
    }

    public void testTableOperationRetrieveJsonNoMetadataResolverFail() {

        // set custom property resolver which throws
        this.options.setPropertyResolver(new ThrowingPropertyResolver());

        try {
            this.table.execute(TableOperation.retrieve(this.ent.getPartitionKey(), this.ent.getRowKey(), Class1.class),
                    this.options, null);
            fail("Invalid property resolver should throw");
        }
        catch (StorageException e) {
            assertEquals(
                    "The custom property resolver delegate threw an exception. Check the inner exception for more details.",
                    e.getMessage());
            assertTrue(e.getCause().getClass() == IllegalArgumentException.class);
        }
    }

    class CustomPropertyResolver implements PropertyResolver {
        @Override
        public EdmType propertyResolver(String pk, String rk, String key, String value) {
            if (key.equals("fooint")) {
                return EdmType.GUID;
            }

            return EdmType.STRING;
        }
    }

    class ThrowingPropertyResolver implements PropertyResolver {
        @Override
        public EdmType propertyResolver(String pk, String rk, String key, String value) {
            if (key.equals("fooint")) {
                throw new IllegalArgumentException();
            }

            return EdmType.STRING;
        }
    }
}
