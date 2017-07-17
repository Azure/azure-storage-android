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
package com.microsoft.azure.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Assert;

import com.microsoft.azure.storage.analytics.CloudAnalyticsClient;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTableClient;

public class TestHelper {
	private static CloudStorageAccount account;
    private static CloudStorageAccount premiumAccount;

	public static String connectionString;
    public static String premiumConnectionString;
	public static String accountName;
    public static String premiumAccountName;
	public static String accountKey;
    public static String premiumAccountKey;
	public static StorageUri blobEndpoint;
    public static StorageUri premiumBlobEndpoint;
	public static StorageUri queueEndpoint;
	public static StorageUri tableEndpoint;

	public static CloudBlobClient createCloudBlobClient() throws StorageException {
        return getAccount().createCloudBlobClient();
    }
	
	public static CloudFileClient createCloudFileClient() throws StorageException {
		return getAccount().createCloudFileClient();
    }

	public static CloudQueueClient createCloudQueueClient() throws StorageException {
		return getAccount().createCloudQueueClient();
	}

	public static CloudTableClient createCloudTableClient() throws StorageException {
		return getAccount().createCloudTableClient();
	}

    public static CloudBlobClient createPremiumCloudBlobClient() throws StorageException {
        CloudBlobClient client = getPremiumBlobAccount().createCloudBlobClient();
        return client;
    }

    public static CloudBlobClient createCloudBlobClient(SharedAccessAccountPolicy policy, boolean useHttps)
            throws StorageException, InvalidKeyException, URISyntaxException {
        
        CloudStorageAccount sasAccount = getAccount();
        final String token = sasAccount.generateSharedAccessSignature(policy);
        final StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(token);
        
        final URI blobUri = new URI(useHttps ? "https" : "http", sasAccount.getBlobEndpoint().getAuthority(), 
        		sasAccount.getBlobEndpoint().getPath(), sasAccount.getBlobEndpoint().getQuery(), null);

        sasAccount = new CloudStorageAccount(creds, blobUri, sasAccount.getQueueEndpoint(), sasAccount.getTableEndpoint(), 
        		sasAccount.getFileEndpoint());
        return sasAccount.createCloudBlobClient();
    }

    public static CloudFileClient createCloudFileClient(SharedAccessAccountPolicy policy, boolean useHttps)
            throws StorageException, InvalidKeyException, URISyntaxException {

        CloudStorageAccount sasAccount = getAccount();
        final String token = sasAccount.generateSharedAccessSignature(policy);
        final StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(token);
        
        final URI fileUri = new URI(useHttps ? "https" : "http", sasAccount.getFileEndpoint().getAuthority(), 
        		sasAccount.getFileEndpoint().getPath(), sasAccount.getFileEndpoint().getQuery(), null);
        
        sasAccount = new CloudStorageAccount(
                creds, sasAccount.getBlobEndpoint(), sasAccount.getQueueEndpoint(), sasAccount.getTableEndpoint(),
                fileUri);
        return sasAccount.createCloudFileClient();
    }

    public static CloudQueueClient createCloudQueueClient(SharedAccessAccountPolicy policy, boolean useHttps)
            throws StorageException, InvalidKeyException, URISyntaxException {

        CloudStorageAccount sasAccount = getAccount();
        final String token = sasAccount.generateSharedAccessSignature(policy);
        final StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(token);
        
        final URI queueUri = new URI(useHttps ? "https" : "http", sasAccount.getQueueEndpoint().getAuthority(), 
        		sasAccount.getQueueEndpoint().getPath(), sasAccount.getQueueEndpoint().getQuery(), null);
        
        sasAccount = new CloudStorageAccount(creds, sasAccount.getBlobEndpoint(), queueUri,
                sasAccount.getTableEndpoint(), sasAccount.getFileEndpoint());
        return sasAccount.createCloudQueueClient();
    }

    public static CloudTableClient createCloudTableClient(SharedAccessAccountPolicy policy, boolean useHttps)
            throws StorageException, InvalidKeyException, URISyntaxException {

        CloudStorageAccount sasAccount = getAccount();
        final String token = sasAccount.generateSharedAccessSignature(policy);
        final StorageCredentials creds =
                new StorageCredentialsSharedAccessSignature(token);

        final URI tableUri = new URI(useHttps ? "https" : "http", sasAccount.getTableEndpoint().getAuthority(), 
        		sasAccount.getTableEndpoint().getPath(), sasAccount.getTableEndpoint().getQuery(), null);
        
        sasAccount = new CloudStorageAccount(creds, sasAccount.getBlobEndpoint(), sasAccount.getQueueEndpoint(), 
        		tableUri, sasAccount.getFileEndpoint());
        return sasAccount.createCloudTableClient();
    }


    public static CloudAnalyticsClient createCloudAnalyticsClient() throws StorageException {
    	return getAccount().createCloudAnalyticsClient();
    }

	public static void verifyServiceStats(ServiceStats stats) {
		Assert.assertNotNull(stats);
		if (stats.getGeoReplication().getLastSyncTime() != null) {
			Assert.assertEquals(GeoReplicationStatus.LIVE, stats.getGeoReplication().getStatus());
		}
		else {
			Assert.assertTrue(stats.getGeoReplication().getStatus() == GeoReplicationStatus.BOOTSTRAP
					|| stats.getGeoReplication().getStatus() == GeoReplicationStatus.UNAVAILABLE);
		}
	}

	public static void assertByteArrayEquals(byte[] expected, byte[] actual) {
		if (expected == null || actual == null) {
			Assert.assertNull(expected);
			Assert.assertNull(actual);
		}
		else {
			Assert.assertEquals(expected.length, actual.length);
			for (int i = 0; i < expected.length; i++) {
				Assert.assertEquals(expected[i], actual[i]);
			}
		}
	}

	private static CloudStorageAccount getAccount() throws StorageException {
		// Only do this the first time TestBase is called as storage account is static
		if (account == null) {
			// if connectionString is set, use that as an account string
			// if accountName and accountKey are set, use those to setup the account with default endpoints
			// if all of the endpoints are set, use those to create custom endpoints
			try {
				if (connectionString != null) {
					account = CloudStorageAccount.parse(connectionString);
				}
				else if (accountName != null && accountKey != null) {
					StorageCredentialsAccountAndKey credentials = new StorageCredentialsAccountAndKey(accountName, accountKey);
					if(blobEndpoint == null || queueEndpoint == null || tableEndpoint == null){
						account = new CloudStorageAccount(credentials);
					} else {
						account = new CloudStorageAccount(credentials,blobEndpoint, queueEndpoint, tableEndpoint);
					}
				} else {
					throw new StorageException("CredentialsNotSpecified", 
							"Credentials must be specified in the TestHelper class in order to run tests.", 
							Constants.HeaderConstants.HTTP_UNUSED_306, 
							null,
							null);
				}
			}
			catch (Exception e) {
				throw StorageException.translateException(null, e, null);
			}
		}
		return account;
	}

    private static CloudStorageAccount getPremiumBlobAccount() throws StorageException {
        // Only do this the first time TestBase is called as storage account is static
        if (premiumAccount == null) {
            // if connectionString is set, use that as an account string
            // if accountName and accountKey are set, use those to setup the account with default endpoints
            // if all of the endpoints are set, use those to create custom endpoints
            try {
                if (premiumConnectionString != null) {
                    premiumAccount = CloudStorageAccount.parse(premiumConnectionString);
                }
                else if (premiumAccountName != null && premiumAccountKey != null) {
                    StorageCredentialsAccountAndKey credentials = new StorageCredentialsAccountAndKey(premiumAccountName, premiumAccountKey);
                    if(premiumBlobEndpoint == null){
                        premiumAccount = new CloudStorageAccount(credentials);
                    } else {
                        premiumAccount = new CloudStorageAccount(credentials, premiumBlobEndpoint, null , null);
                    }
                } else {
                    throw new StorageException("CredentialsNotSpecified",
                            "Credentials must be specified in the TestHelper class in order to run tests.",
                            Constants.HeaderConstants.HTTP_UNUSED_306,
                            null,
                            null);
                }
            }
            catch (Exception e) {
                throw StorageException.translateException(null, e, null);
            }
        }
        return premiumAccount;
    }

    protected static void enableFiddler() {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "8888");
    }

    public static byte[] getRandomBuffer(int length) {
        final Random randGenerator = new Random();
        final byte[] buff = new byte[length];
        randGenerator.nextBytes(buff);
        return buff;
    }

    public static ByteArrayInputStream getRandomDataStream(int length) {
        return new ByteArrayInputStream(getRandomBuffer(length));
    }

    public static void assertStreamsAreEqual(ByteArrayInputStream src, ByteArrayInputStream dst) {
        dst.reset();
        src.reset();
        Assert.assertEquals(src.available(), dst.available());

        while (src.available() > 0) {
            Assert.assertEquals(src.read(), dst.read());
        }

        Assert.assertFalse(dst.available() > 0);
    }

    public static void assertStreamsAreEqualAtIndex(ByteArrayInputStream src, ByteArrayInputStream dst, int srcIndex,
            int dstIndex, int length, int bufferSize) throws IOException {
        dst.reset();
        src.reset();

        dst.skip(dstIndex);
        src.skip(srcIndex);
        byte[] srcBuffer = new byte[bufferSize];
        byte[] destBuffer = new byte[bufferSize];
        src.read(srcBuffer);
        dst.read(destBuffer);

        for (int i = 0; i < length; i++) {
            Assert.assertEquals(src.read(), dst.read());
        }
    }

    public static URI defiddler(URI uri) throws URISyntaxException {
        String fiddlerString = "ipv4.fiddler";
        String replacementString = "127.0.0.1";

        String uriString = uri.toString();
        if (uriString.contains(fiddlerString)) {
            return new URI(uriString.replace(fiddlerString, replacementString));
        }
        else {
            return uri;
        }
    }

    public static void assertURIsEqual(URI expected, URI actual, boolean ignoreQueryOrder) {
        if (expected == null) {
        	Assert.assertEquals(null, actual);
        }

        Assert.assertEquals(expected.getScheme(), actual.getScheme());
        Assert.assertEquals(expected.getAuthority(), actual.getAuthority());
        Assert.assertEquals(expected.getPath(), actual.getPath());
        Assert.assertEquals(expected.getFragment(), actual.getFragment());

        ArrayList<String> expectedQueries = new ArrayList<String>(Arrays.asList(expected.getQuery().split("&")));
        ArrayList<String> actualQueries = new ArrayList<String>(Arrays.asList(actual.getQuery().split("&")));

        Assert.assertEquals(expectedQueries.size(), actualQueries.size());
        for (String expectedQuery : expectedQueries) {
        	Assert.assertTrue(expectedQuery, actualQueries.remove(expectedQuery));
        }

        Assert.assertTrue(actualQueries.isEmpty());
    }
}
