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
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.storage.table.TableQuery.QueryComparisons;
import com.microsoft.azure.storage.table.TableRequestOptions.PropertyResolver;

/**
 * This sample illustrates basic usage of the various Table Primitives provided
 * in the Storage Client Library including TablePayloadFormat, PropertyResolver
 * and TableRequestOptions. These are used to specify the payload format for
 * table operations and to provide the property type information at runtime when
 * using JsonNoMetadata.
 */
public class TablePayloadFormatTask extends AsyncTask<String, Void, Void> {

    protected static CloudTableClient tableClient;
    protected static CloudTable table;
    protected final static String tableName = "tablebasics";

    private TextView view;
    private MainActivity act;

    public TablePayloadFormatTask(MainActivity act, TextView view) {
        this.view = view;
        this.act = act;
    }

    @Override
    protected Void doInBackground(String... arg0) {
        act.printSampleStartInfo("PayloadFormat");

        try {
            // Setup the cloud storage account.
            CloudStorageAccount account = CloudStorageAccount
                    .parse(MainActivity.storageConnectionString);

            // Create a table service client.
            CloudTableClient tableClient = account.createCloudTableClient();

            // Set the payload format on the client. For more information about
            // the support for JSON, check the following document:
            // http://blogs.msdn.com/b/windowsazurestorage/archive/2013/12/05/windows-azure-tables-introducing-json.aspx.
            tableClient.getDefaultRequestOptions().setTablePayloadFormat(
                    TablePayloadFormat.JsonNoMetadata);

            // Retrieve a reference to a table.
            // Append a random UUID to the end of the table name so that this
            // sample can be run more than once in quick succession.
            CloudTable table = tableClient.getTableReference("tablepayloadformat"
                            + UUID.randomUUID().toString().replace("-", ""));

            // Create the table if it doesn't already exist.
            table.createIfNotExists();

            // Create a new customer entity.
            CustomerEntity customer1 = new CustomerEntity("Harp", "Walter");
            customer1.setEmail("Walter@contoso.com");
            customer1.setPhoneNumber("425-555-0101");
            customer1.setId(UUID.randomUUID());

            // Create an operation to add the new customer to the
            // tablepayloadformat table.
            TableOperation insertCustomer1 = TableOperation.insert(customer1);

            // Submit the operation to the table service.
            table.execute(insertCustomer1);

            // When using JsonNoMetadata the client library will "infer" the property types(int, double,
            // String and some booleans) by inspecting the type information on the POJO entity type
            // provided by the client. Additionally, in some scenarios clients may wish to provide
            // the property type information at runtime such as when querying with the DynamicTableEntity
            // or doing complex queries that may return heterogeneous entities. To support this scenario
            // the user should implement PropertyResolver which allows users to return an EdmType
            // for each property based on the data received from the service.
            class PropertyResolverClass implements PropertyResolver {

                @Override
                public EdmType propertyResolver(String pk, String rk,
                        String key, String value) {
                    if (key.equals("Email")) {
                        return EdmType.STRING;
                    } else if (key.equals("PhoneNumber")) {
                        return EdmType.STRING;
                    } else if (key.equals("Id")) {
                        return EdmType.GUID;
                    }
                    return null;
                }
            }

            // Using the property resolver to infer property types.
            // Create a TableRequestOptions object.
            TableRequestOptions options = new TableRequestOptions();

            // Set the propertyResolver on TableRequestOptions.
            options.setPropertyResolver(new PropertyResolverClass());

            // Retrieve all entities in a partition.
            // Create a filter condition where the partition key is "Harp".
            String partitionFilter = TableQuery.generateFilterCondition(
                    "PartitionKey", QueryComparisons.EQUAL, "Harp");

            // Specify a partition query, using "Harp" as the partition key
            // filter.
            TableQuery<DynamicTableEntity> partitionQuery = TableQuery.from(
                    DynamicTableEntity.class).where(partitionFilter);

            // Loop through the results, displaying information about the entity. Note that the
            // TableRequestOptions is passed in to the execute method in order
            // to resolve the individual properties to their types since JsonNoMetadata is
            // being used.
            for (DynamicTableEntity entity : table.execute(partitionQuery,
                    options, null /* operationContext */)) {
                act.outputText(view,
                        entity.getPartitionKey()
                                + " "
                                + entity.getRowKey()
                                + "\t"
                                + entity.getProperties().get("Email")
                                        .getValueAsString()
                                + "\t"
                                + entity.getProperties().get("PhoneNumber")
                                        .getValueAsString()
                                + "\t"
                                + entity.getProperties().get("Id")
                                        .getValueAsUUID());
            }

            // When a POJO entity is used, it's fine to not specify a property resolver since
            // the client library uses the class type to infer the correct property types.
            // Specify a partition query, using "Harp" as the partition key filter.
            TableQuery<CustomerEntity> partitionQueryPOJO = TableQuery.from(
                    CustomerEntity.class).where(partitionFilter);

            // Loop through the results, displaying information about the entity. Note that the
            // TableRequestOptions is passed in to the execute method in order to resolve
            // the individual properties to their types since JsonNoMetadata is being used.
            for (CustomerEntity entity : table.execute(partitionQueryPOJO)) {
                act.outputText(
                        view,
                        entity.getPartitionKey() + " " + entity.getRowKey()
                                + "\t" + entity.getEmail() + "\t"
                                + entity.getPhoneNumber() + "\t"
                                + entity.getId());
            }

            // Delete the table.
            table.deleteIfExists();

        } catch (Throwable t) {
            act.printException(t);
        }

        act.printSampleCompleteInfo("PayloadFormat");

        return null;
    }
}