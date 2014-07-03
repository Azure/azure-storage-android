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
import java.util.Random;

import junit.framework.Assert;

import com.microsoft.azure.storage.analytics.CloudAnalyticsClient;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTableClient;

public class TestHelper {
	private static CloudStorageAccount account;

	private final static AuthenticationScheme defaultAuthenticationScheme = AuthenticationScheme.SHAREDKEYFULL;

	public static String connectionString;
	public static String accountName;
	public static String accountKey;
	public static StorageUri blobEndpoint;
	public static StorageUri queueEndpoint;
	public static StorageUri tableEndpoint;

	public static CloudBlobClient createCloudBlobClient() throws StorageException {
		CloudBlobClient client = getAccount().createCloudBlobClient();
		client.setAuthenticationScheme(defaultAuthenticationScheme);
		return client;
	}
	
    public static CloudFileClient createCloudFileClient() throws StorageException {
        CloudFileClient client = getAccount().createCloudFileClient();
        client.setAuthenticationScheme(defaultAuthenticationScheme);
        return client;
    }

	public static CloudQueueClient createCloudQueueClient() throws StorageException {
		CloudQueueClient client = getAccount().createCloudQueueClient();
		client.setAuthenticationScheme(defaultAuthenticationScheme);
		return client;
	}

	public static CloudTableClient createCloudTableClient() throws StorageException {
		CloudTableClient client = getAccount().createCloudTableClient();
		client.setAuthenticationScheme(defaultAuthenticationScheme);
		return client;
	}

    public static CloudAnalyticsClient createCloudAnalyticsClient() throws StorageException {
        CloudAnalyticsClient client = getAccount().createCloudAnalyticsClient();
        return client;
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
}
