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
package com.microsoft.azure.storage.samples.table;

import java.util.UUID;

import android.os.AsyncTask;
import android.widget.TextView;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.samples.MainActivity;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.storage.table.TableQuery.QueryComparisons;

/**
 * This sample illustrates basic usage of the various Table Primitives provided
 * in the Storage Client Library including TableOperation, TableBatchOperation,
 * and TableQuery.
 */
public class TableGettingStartedTask extends AsyncTask<String, Void, Void> {

    protected static CloudTableClient tableClient;
    protected static CloudTable table;
    protected final static String tableName = "tablebasics";

    private TextView view;
    private MainActivity act;

    public TableGettingStartedTask(MainActivity act, TextView view) {
        this.view = view;
        this.act = act;
    }

    @Override
    protected Void doInBackground(String... arg0) {

        act.printSampleStartInfo("TableBasics");

        try {
            // Setup the cloud storage account.
            CloudStorageAccount account = CloudStorageAccount
                    .parse(MainActivity.storageConnectionString);

            // Create a table service client.
            tableClient = account.createCloudTableClient();

            // Retrieve a reference to a table.
            // Append a random UUID to the end of the table name so that this
            // sample can be run more than once in quick succession.
            table = tableClient.getTableReference(tableName
                    + UUID.randomUUID().toString().replace("-", ""));

            // Create the table if it doesn't already exist.
            table.createIfNotExists();

            // Illustrates how to list the tables.
            this.BasicListing();

            // Illustrates how to form and execute a single insert operation.
            this.BasicInsertEntity();

            // Illustrates how to form and execute a batch operation.
            this.BasicBatch();

            // Illustrates how to form and execute a query operation.
            this.BasicQuery();

            // Illustrates how to form and execute an upsert operation.
            this.BasicUpsert();

            // Illustrates how to form and execute an entity delete operation.
            this.BasicDeleteEntity();

            // Delete the table.
            table.deleteIfExists();

        } catch (Throwable t) {
            act.printException(t);
        }

        act.printSampleCompleteInfo("TableBasics");

        return null;
    }

    /**
     * Illustrates how to form and execute a single insert operation.
     * 
     * @throws StorageException
     */
    public void BasicInsertEntity() throws StorageException {
        // Note: the limitations on an insert operation are
        // - the serialized payload must be 1 MB or less
        // - up to 252 properties in addition to the partition key, row key and
        // timestamp. 255 properties in total
        // - the serialized payload of each property must be 64 KB or less

        // Create a new customer entity.
        CustomerEntity customer1 = new CustomerEntity("Harp", "Walter");
        customer1.setEmail("Walter@contoso.com");
        customer1.setPhoneNumber("425-555-0101");

        // Create an operation to add the new customer to the tablebasics table.
        TableOperation insertCustomer1 = TableOperation.insert(customer1);

        // Submit the operation to the table service.
        table.execute(insertCustomer1);
    }

    /**
     * Illustrates how to form and execute a batch operation.
     * 
     * @throws StorageException
     */
    public void BasicBatch() throws StorageException {
        // Note: the limitations on a batch operation are
        // - up to 100 operations
        // - all operations must share the same PartitionKey
        // - if a retrieve is used it can be the only operation in the batch
        // - the serialized batch payload must be 4 MB or less

        // Define a batch operation.
        TableBatchOperation batchOperation = new TableBatchOperation();

        // Create a customer entity to add to the table.
        CustomerEntity customer = new CustomerEntity("Smith", "Jeff");
        customer.setEmail("Jeff@contoso.com");
        customer.setPhoneNumber("425-555-0104");
        batchOperation.insert(customer);

        // Create another customer entity to add to the table.
        CustomerEntity customer2 = new CustomerEntity("Smith", "Ben");
        customer2.setEmail("Ben@contoso.com");
        customer2.setPhoneNumber("425-555-0102");
        batchOperation.insert(customer2);

        // Create a third customer entity to add to the table.
        CustomerEntity customer3 = new CustomerEntity("Smith", "Denise");
        customer3.setEmail("Denise@contoso.com");
        customer3.setPhoneNumber("425-555-0103");
        batchOperation.insert(customer3);

        // Execute the batch of operations on the "tablebasics" table.
        table.execute(batchOperation);
    }

    /**
     * Illustrates how to form and execute a query operation.
     * 
     * @throws StorageException
     */
    public void BasicQuery() throws StorageException {
        // Retrieve a single entity.
        // Retrieve the entity with partition key of "Smith" and row key of
        // "Jeff".
        TableOperation retrieveSmithJeff = TableOperation.retrieve("Smith",
                "Jeff", CustomerEntity.class);

        // Submit the operation to the table service and get the specific
        // entity.
        @SuppressWarnings("unused")
        CustomerEntity specificEntity = table.execute(retrieveSmithJeff)
                .getResultAsType();

        // Retrieve all entities in a partition.
        // Create a filter condition where the partition key is "Smith".
        String partitionFilter = TableQuery.generateFilterCondition(
                "PartitionKey", QueryComparisons.EQUAL, "Smith");

        // Specify a partition query, using "Smith" as the partition key filter.
        TableQuery<CustomerEntity> partitionQuery = TableQuery.from(
                CustomerEntity.class).where(partitionFilter);

        // Loop through the results, displaying information about the entity.
        for (CustomerEntity entity : table.execute(partitionQuery)) {
            act.outputText(
                    view,
                    entity.getPartitionKey() + " " + entity.getRowKey() + "\t"
                            + entity.getEmail() + "\t"
                            + entity.getPhoneNumber());
        }
    }

    /**
     * Illustrates how to form and execute an upsert operation.
     * 
     * @throws StorageException
     */
    public void BasicUpsert() throws StorageException {
        // Retrieve the entity with partition key of "Smith" and row key of
        // "Jeff".
        TableOperation retrieveSmithJeff = TableOperation.retrieve("Smith",
                "Jeff", CustomerEntity.class);

        // Submit the operation to the table service and get the specific
        // entity.
        CustomerEntity specificEntity = table.execute(retrieveSmithJeff)
                .getResultAsType();

        // Specify a new phone number.
        specificEntity.setPhoneNumber("425-555-0105");

        // Create an operation to replace the entity.
        TableOperation replaceEntity = TableOperation.merge(specificEntity);

        // Submit the operation to the table service.
        table.execute(replaceEntity);
    }

    /**
     * Illustrates how to form and execute an entity delete operation.
     * 
     * @throws StorageException
     */
    public void BasicDeleteEntity() throws StorageException {
        // Create an operation to retrieve the entity with partition key of
        // "Smith" and row key of "Jeff".
        TableOperation retrieveSmithJeff = TableOperation.retrieve("Smith",
                "Jeff", CustomerEntity.class);

        // Retrieve the entity with partition key of "Smith" and row key of
        // "Jeff".
        CustomerEntity entitySmithJeff = table.execute(retrieveSmithJeff)
                .getResultAsType();

        // Create an operation to delete the entity.
        TableOperation deleteSmithJeff = TableOperation.delete(entitySmithJeff);

        // Submit the delete operation to the table service.
        table.execute(deleteSmithJeff);
    }

    /**
     * Illustrates how to list the tables.
     */
    public void BasicListing() {
        // List the tables with a given prefix.
        Iterable<String> listTables = tableClient.listTables(tableName, null,
                null);
        for (String s : listTables) {
            act.outputText(view, String.format("List of tables: %s", s));
        }
    }
}