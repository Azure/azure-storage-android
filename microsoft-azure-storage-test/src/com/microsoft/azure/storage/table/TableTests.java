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

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.LocationMode;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.RetryNoRetry;
import com.microsoft.azure.storage.SendingRequestEvent;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageEvent;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.core.PathUtility;
import com.microsoft.azure.storage.table.TableTestHelper.Class1;

public class TableTests extends TestCase {

	public void testIsUsePathStyleUri() throws InvalidKeyException, URISyntaxException, StorageException {
		// normal account
		StorageCredentials creds = new StorageCredentialsAccountAndKey("testAccountName",
				"/g6UPBuy0ypCpAbYTL6/KA+dI/7gyoWvLFYmah3IviUP1jykOHHOlA==");
		CloudStorageAccount account = new CloudStorageAccount(creds);
		testIsUsePathStyleUri(creds, account.getTableEndpoint().toString(), false);

		// normal account with path
		creds = new StorageCredentialsAccountAndKey("testAccountName",
				"/g6UPBuy0ypCpAbYTL6/KA+dI/7gyoWvLFYmah3IviUP1jykOHHOlA==");
		account = new CloudStorageAccount(creds);
		testIsUsePathStyleUri(creds, account.getTableEndpoint().toString() + "/mytable", false);

		// custom endpoint
		creds = new StorageCredentialsAccountAndKey("testAccountName",
				"/g6UPBuy0ypCpAbYTL6/KA+dI/7gyoWvLFYmah3IviUP1jykOHHOlA==");
		testIsUsePathStyleUri(creds, "http://www.contoso.com", false);

		// dev store
		account = CloudStorageAccount.getDevelopmentStorageAccount();
		testIsUsePathStyleUri(account.getCredentials(), account.getTableEndpoint().toString(), true);

		// dev store with proxy
		account = CloudStorageAccount.getDevelopmentStorageAccount(new URI("http://ipv4.fiddler"));
		testIsUsePathStyleUri(account.getCredentials(), account.getTableEndpoint().toString(), true);

		// custom endpoints ipv4 with path-style (internal test)
		creds = new StorageCredentialsAccountAndKey("testAccountName",
				"/g6UPBuy0ypCpAbYTL6/KA+dI/7gyoWvLFYmah3IviUP1jykOHHOlA==");
		testIsUsePathStyleUri(creds, "http://93.184.216.119/testAccountName", true);
	}

	private static void testIsUsePathStyleUri(StorageCredentials creds, String tableEndpoint, boolean usePathStyleUris)
			throws URISyntaxException, InvalidKeyException, StorageException {
		CloudTableClient tableClient = new CloudTableClient(new URI(tableEndpoint), creds);
		assertEquals(usePathStyleUris, tableClient.isUsePathStyleUris());

		CloudTable table = tableClient.getTableReference("mytable");
		assertEquals(tableEndpoint + "/mytable", table.getUri().toString());

		String sasToken = table.generateSharedAccessSignature(null, "fakeIdentifier", null, null, null, null);
		tableClient = new CloudTableClient(new URI(tableEndpoint),
				new StorageCredentialsSharedAccessSignature(sasToken));
		assertEquals(usePathStyleUris, tableClient.isUsePathStyleUris());

		table = new CloudTable(table.getUri(), tableClient);
		assertEquals(tableEndpoint + "/mytable", table.getUri().toString());
	}

	/**
	 * Get permissions from string
	 */

	public void testTablePermissionsFromString() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		Date start = cal.getTime();
		cal.add(Calendar.MINUTE, 30);
		Date expiry = cal.getTime();

		SharedAccessTablePolicy policy = new SharedAccessTablePolicy();
		policy.setSharedAccessStartTime(start);
		policy.setSharedAccessExpiryTime(expiry);

		policy.setPermissionsFromString("raud");
		assertEquals(EnumSet.of(SharedAccessTablePermissions.QUERY, SharedAccessTablePermissions.ADD,
				SharedAccessTablePermissions.UPDATE, SharedAccessTablePermissions.DELETE), policy.getPermissions());

		policy.setPermissionsFromString("rad");
		assertEquals(EnumSet.of(SharedAccessTablePermissions.QUERY, SharedAccessTablePermissions.ADD,
				SharedAccessTablePermissions.DELETE), policy.getPermissions());

		policy.setPermissionsFromString("ar");
		assertEquals(EnumSet.of(SharedAccessTablePermissions.QUERY, SharedAccessTablePermissions.ADD),
				policy.getPermissions());

		policy.setPermissionsFromString("u");
		assertEquals(EnumSet.of(SharedAccessTablePermissions.UPDATE), policy.getPermissions());
	}

	public void testTableCreateAndAttemptCreateOnceExists() throws StorageException,  URISyntaxException {

		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);
		tClient.getDefaultRequestOptions().setTablePayloadFormat(TablePayloadFormat.Json);
		try {
			table.create();
			assertTrue(table.exists());

			// Should fail as it already exists
			try {
				table.create();
				fail();
			}
			catch (StorageException ex) {
				assertEquals(ex.getErrorCode(), "TableAlreadyExists");
			}
		}
		finally {
			// cleanup
			table.deleteIfExists();
		}
	}

	public void testTableCreateExistsAndDelete() throws StorageException,  URISyntaxException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);
		try {
			assertTrue(table.createIfNotExists());
			assertTrue(table.exists());
			assertTrue(table.deleteIfExists());
		}
		finally {
			// cleanup
			table.deleteIfExists();
		}
	}

	public void testTableCreateIfNotExists() throws StorageException,  URISyntaxException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);
		try {
			assertTrue(table.createIfNotExists());
			assertTrue(table.exists());
			assertFalse(table.createIfNotExists());
		}
		finally {
			// cleanup
			table.deleteIfExists();
		}
	}

	public void testTableDeleteIfExists() throws StorageException,  URISyntaxException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);
		assertFalse(table.deleteIfExists());

		table.create();
		assertTrue(table.exists());
		assertTrue(table.deleteIfExists());

		assertFalse(table.deleteIfExists());
	}
	
    public void testCloudTableDeleteIfExistsErrorCode() throws StorageException, URISyntaxException {
        final CloudTable table = TableTestHelper.getRandomTableReference();

        try {
            assertFalse(table.deleteIfExists());
            OperationContext ctx = new OperationContext();
            ctx.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

                @Override
                public void eventOccurred(SendingRequestEvent eventArg) {
                    if (((HttpURLConnection) eventArg.getConnectionObject()).getRequestMethod().equals("DELETE")) {
                        try {
                            table.delete();
                            assertFalse(table.exists());
                        }
                        catch (StorageException e) {
                            fail("Delete should succeed.");
                        }
                    }
                }
            });

            table.create();

            // The second delete of a table will return a 404
            assertFalse(table.deleteIfExists(null, ctx));
        }
        finally {
            table.deleteIfExists();
        }
    }

	public void testTableDeleteWhenExistAndNotExists() throws StorageException,  URISyntaxException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);

		try {
			// Should fail as it doesnt already exists
			try {
				table.delete();
				fail();
			}
			catch (StorageException ex) {
				assertEquals(ex.getMessage(), "Not Found");
			}

			table.create();
			assertTrue(table.exists());
			table.delete();
			assertFalse(table.exists());
		}
		finally {
			table.deleteIfExists();
		}
	}

	public void testTableDoesTableExist() throws StorageException,  URISyntaxException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);

		try {
			assertFalse(table.exists());
			assertTrue(table.createIfNotExists());
			assertTrue(table.exists());
		}
		finally {
			// cleanup
			table.deleteIfExists();
		}
	}

	public void testTableGetSetPermissionTest() throws StorageException,  URISyntaxException,
	InterruptedException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();
		String tableName = TableTestHelper.generateRandomTableName();

		CloudTable table = tClient.getTableReference(tableName);
		table.create();

		TablePermissions expectedPermissions;
		TablePermissions testPermissions;

		try {
			// Test new permissions.
			expectedPermissions = new TablePermissions();
			testPermissions = table.downloadPermissions();
			assertTablePermissionsEqual(expectedPermissions, testPermissions);

			// Test setting empty permissions.
			table.uploadPermissions(expectedPermissions);
			Thread.sleep(30000);

			testPermissions = table.downloadPermissions();
			assertTablePermissionsEqual(expectedPermissions, testPermissions);

			// Add a policy, check setting and getting.
			SharedAccessTablePolicy policy1 = new SharedAccessTablePolicy();
			Calendar now = GregorianCalendar.getInstance();
			policy1.setSharedAccessStartTime(now.getTime());
			now.add(Calendar.MINUTE, 10);
			policy1.setSharedAccessExpiryTime(now.getTime());

			policy1.setPermissions(EnumSet.of(SharedAccessTablePermissions.ADD, SharedAccessTablePermissions.QUERY,
					SharedAccessTablePermissions.UPDATE, SharedAccessTablePermissions.DELETE));
			expectedPermissions.getSharedAccessPolicies().put(UUID.randomUUID().toString(), policy1);

			table.uploadPermissions(expectedPermissions);
			Thread.sleep(30000);
			testPermissions = table.downloadPermissions();
			assertTablePermissionsEqual(expectedPermissions, testPermissions);
		}
		finally {
			// cleanup
			table.deleteIfExists();
		}
	}

	public void testTableSas() throws StorageException,  URISyntaxException, InvalidKeyException,
	InterruptedException {
		CloudTableClient tClient = TableTestHelper.createCloudTableClient();

		// use capital letters to make sure SAS signature converts name to lower case correctly
		String name = "CAPS" + TableTestHelper.generateRandomTableName();
		CloudTable table = tClient.getTableReference(name);
		table.create();

		TablePermissions expectedPermissions = new TablePermissions();
		String identifier = UUID.randomUUID().toString();
		// Add a policy, check setting and getting.
		SharedAccessTablePolicy policy1 = new SharedAccessTablePolicy();
		Calendar now = GregorianCalendar.getInstance();
		now.add(Calendar.MINUTE, -10);
		policy1.setSharedAccessStartTime(now.getTime());
		now.add(Calendar.MINUTE, 30);
		policy1.setSharedAccessExpiryTime(now.getTime());

		policy1.setPermissions(EnumSet.of(SharedAccessTablePermissions.ADD, SharedAccessTablePermissions.QUERY,
				SharedAccessTablePermissions.UPDATE, SharedAccessTablePermissions.DELETE));
		expectedPermissions.getSharedAccessPolicies().put(identifier, policy1);

		table.uploadPermissions(expectedPermissions);
		Thread.sleep(30000);

		// Insert 500 entities in Batches to query
		for (int i = 0; i < 5; i++) {
			TableBatchOperation batch = new TableBatchOperation();

			for (int j = 0; j < 100; j++) {
				Class1 ent = TableTestHelper.generateRandomEntity("javatables_batch_" + Integer.toString(i));
				ent.setRowKey(String.format("%06d", j));
				batch.insert(ent);
			}

			table.execute(batch);
		}

		String sasString = table.generateSharedAccessSignature(policy1, null, "javatables_batch_0", "0",
				"javatables_batch_9", "9");
		CloudTableClient tableClientFromPermission = new CloudTableClient(tClient.getEndpoint(),
				new StorageCredentialsSharedAccessSignature(sasString));

		CloudTable policySasTable = tableClientFromPermission.getTableReference(name);
		policySasTable.exists();

		// do not give the client and check that the new table's client has the correct perms
		CloudTable tableFromUri = new CloudTable(PathUtility.addToQuery(table.getStorageUri(), table
				.generateSharedAccessSignature((SharedAccessTablePolicy) null, identifier, "javatables_batch_0", "0",
						"javatables_batch_9", "9")));
		assertEquals(StorageCredentialsSharedAccessSignature.class.toString(), tableFromUri.getServiceClient()
				.getCredentials().getClass().toString());

		// pass in a client which will have different permissions and check the sas permissions are used
		// and that the properties set in the old service client are passed to the new client
		CloudTableClient tableClient = policySasTable.getServiceClient();

		// set some arbitrary settings to make sure they are passed on
		tableClient.getDefaultRequestOptions().setLocationMode(LocationMode.PRIMARY_THEN_SECONDARY);
		tableClient.getDefaultRequestOptions().setTimeoutIntervalInMs(1000);
		tableClient.getDefaultRequestOptions().setTablePayloadFormat(TablePayloadFormat.JsonNoMetadata);
		tableClient.getDefaultRequestOptions().setRetryPolicyFactory(new RetryNoRetry());

		tableFromUri = new CloudTable(PathUtility.addToQuery(table.getStorageUri(), table
				.generateSharedAccessSignature((SharedAccessTablePolicy) null, identifier, "javatables_batch_0", "0",
						"javatables_batch_9", "9")), tableClient);
		assertEquals(StorageCredentialsSharedAccessSignature.class.toString(), tableFromUri.getServiceClient()
				.getCredentials().getClass().toString());

		assertEquals(tableClient.getDefaultRequestOptions().getLocationMode(), tableFromUri.getServiceClient()
				.getDefaultRequestOptions().getLocationMode());
		assertEquals(tableClient.getDefaultRequestOptions().getTimeoutIntervalInMs(), tableFromUri.getServiceClient()
				.getDefaultRequestOptions().getTimeoutIntervalInMs());
		assertEquals(tableClient.getDefaultRequestOptions().getTablePayloadFormat(), tableFromUri.getServiceClient()
				.getDefaultRequestOptions().getTablePayloadFormat());
		assertEquals(tableClient.getDefaultRequestOptions().getRetryPolicyFactory().getClass(), tableFromUri
				.getServiceClient().getDefaultRequestOptions().getRetryPolicyFactory().getClass());
	}

	private static void assertTablePermissionsEqual(TablePermissions expected, TablePermissions actual) {
		HashMap<String, SharedAccessTablePolicy> expectedPolicies = expected.getSharedAccessPolicies();
		HashMap<String, SharedAccessTablePolicy> actualPolicies = actual.getSharedAccessPolicies();
		assertEquals("SharedAccessPolicies.Count", expectedPolicies.size(), actualPolicies.size());
		for (String name : expectedPolicies.keySet()) {
			assertTrue("Key" + name + " doesn't exist", actualPolicies.containsKey(name));
			SharedAccessTablePolicy expectedPolicy = expectedPolicies.get(name);
			SharedAccessTablePolicy actualPolicy = actualPolicies.get(name);
			assertEquals("Policy: " + name + "\tPermissions\n", expectedPolicy.getPermissions().toString(),
					actualPolicy.getPermissions().toString());
			assertEquals("Policy: " + name + "\tStartDate\n", expectedPolicy.getSharedAccessStartTime().toString(),
					actualPolicy.getSharedAccessStartTime().toString());
			assertEquals("Policy: " + name + "\tExpireDate\n", expectedPolicy.getSharedAccessExpiryTime().toString(),
					actualPolicy.getSharedAccessExpiryTime().toString());

		}

	}
}
