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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Locale;
import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.core.Base64;
import com.microsoft.azure.storage.core.SR;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;

public class StorageAccountTests extends TestCase {

    public static final String ACCOUNT_NAME = UUID.randomUUID().toString();
    public static final String ACCOUNT_KEY = Base64.encode(UUID.randomUUID().toString().getBytes());

    public void testStorageCredentialsAnonymous() throws URISyntaxException, StorageException {
        StorageCredentialsAnonymous cred = new StorageCredentialsAnonymous();

        assertNull(cred.getAccountName());

        URI testUri = new URI("http://test/abc?querya=1");
        assertEquals(testUri, cred.transformUri(testUri));
    }

    public void testStorageCredentialsSharedKey() throws URISyntaxException, StorageException {
        StorageCredentialsAccountAndKey cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);

        assertEquals(ACCOUNT_NAME, cred.getAccountName());

        URI testUri = new URI("http://test/abc?querya=1");
        assertEquals(testUri, cred.transformUri(testUri));

        assertEquals(ACCOUNT_KEY, cred.getCredentials().exportBase64EncodedKey());
        byte[] dummyKey = { 0, 1, 2 };
        String base64EncodedDummyKey = Base64.encode(dummyKey);
        cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, base64EncodedDummyKey);
        assertEquals(base64EncodedDummyKey, cred.getCredentials().exportBase64EncodedKey());

        dummyKey[0] = 3;
        base64EncodedDummyKey = Base64.encode(dummyKey);
        cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, base64EncodedDummyKey);
        assertEquals(base64EncodedDummyKey, cred.getCredentials().exportBase64EncodedKey());
    }

    public void testStorageCredentialsSAS() throws URISyntaxException, StorageException {
        String token = "?sp=abcde&api-version=2014-02-14&sig=1";

        StorageCredentialsSharedAccessSignature cred = new StorageCredentialsSharedAccessSignature(token);

        assertNull(cred.getAccountName());

        URI testUri = new URI("http://test/abc");
        assertEquals(testUri + token, cred.transformUri(testUri).toString());

        testUri = new URI("http://test/abc?query=a&query2=b");
        String expectedUri = "http://test/abc?api-version=2014-02-14&sp=abcde&query=a&query2=b&sig=1";
        assertEquals(expectedUri, cred.transformUri(testUri).toString());
    }

    public void testStorageCredentialsEmptyKeyValue() throws URISyntaxException, InvalidKeyException {
        String emptyKeyValueAsString = "";
        String emptyKeyConnectionString = String.format(Locale.US,
                "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=", ACCOUNT_NAME);

        StorageCredentialsAccountAndKey credentials1 = new StorageCredentialsAccountAndKey(ACCOUNT_NAME,
                emptyKeyValueAsString);
        assertEquals(ACCOUNT_NAME, credentials1.getAccountName());
        assertEquals(emptyKeyValueAsString, Base64.encode(credentials1.getCredentials().exportKey()));

        CloudStorageAccount account1 = new CloudStorageAccount(credentials1, true);
        assertEquals(emptyKeyConnectionString, account1.toString(true));
        assertNotNull(account1.getCredentials());
        assertEquals(ACCOUNT_NAME, account1.getCredentials().getAccountName());
        assertEquals(emptyKeyValueAsString,
                Base64.encode(((StorageCredentialsAccountAndKey) (account1.getCredentials())).getCredentials()
                        .exportKey()));

        CloudStorageAccount account2 = CloudStorageAccount.parse(emptyKeyConnectionString);
        assertEquals(emptyKeyConnectionString, account2.toString(true));
        assertNotNull(account2.getCredentials());
        assertEquals(ACCOUNT_NAME, account2.getCredentials().getAccountName());
        assertEquals(emptyKeyValueAsString,
                Base64.encode(((StorageCredentialsAccountAndKey) (account2.getCredentials())).getCredentials()
                        .exportKey()));

        StorageCredentialsAccountAndKey credentials2 = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);
        assertEquals(ACCOUNT_NAME, credentials2.getAccountName());
        assertEquals(ACCOUNT_KEY, Base64.encode(credentials2.getCredentials().exportKey()));

        byte[] emptyKeyValueAsByteArray = new byte[0];
        StorageCredentialsAccountAndKey credentials3 = new StorageCredentialsAccountAndKey(ACCOUNT_NAME,
                emptyKeyValueAsByteArray);
        assertEquals(ACCOUNT_NAME, credentials3.getAccountName());
        assertEquals(Base64.encode(emptyKeyValueAsByteArray), Base64.encode(credentials3.getCredentials().exportKey()));
    }

    public void testStorageCredentialsNullKeyValue() {
        String nullKeyValueAsString = null;

        try {
            new StorageCredentialsAccountAndKey(ACCOUNT_NAME, nullKeyValueAsString);
            fail("Did not hit expected exception");
        }
        catch (NullPointerException ex) {
            //            assertEquals(SR.KEY_NULL, ex.getMessage());
        }

        StorageCredentialsAccountAndKey credentials2 = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);
        assertEquals(ACCOUNT_NAME, credentials2.getAccountName());
        assertEquals(ACCOUNT_KEY, Base64.encode(credentials2.getCredentials().exportKey()));

        byte[] nullKeyValueAsByteArray = null;
        try {
            new StorageCredentialsAccountAndKey(ACCOUNT_NAME, nullKeyValueAsByteArray);
            fail("Did not hit expected exception");
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.KEY_NULL, ex.getMessage());
        }
    }

    private void AccountsAreEqual(CloudStorageAccount a, CloudStorageAccount b) {
        // endpoints are the same
        assertEquals(a.getBlobEndpoint(), b.getBlobEndpoint());
        assertEquals(a.getQueueEndpoint(), b.getQueueEndpoint());
        assertEquals(a.getTableEndpoint(), b.getTableEndpoint());
        assertEquals(a.getFileEndpoint(), b.getFileEndpoint());

        // storage uris are the same
        assertEquals(a.getBlobStorageUri(), b.getBlobStorageUri());
        assertEquals(a.getQueueStorageUri(), b.getQueueStorageUri());
        assertEquals(a.getTableStorageUri(), b.getTableStorageUri());
        assertEquals(a.getFileStorageUri(), b.getFileStorageUri());

        // seralized representatons are the same.
        String aToStringNoSecrets = a.toString();
        String aToStringWithSecrets = a.toString(true);
        String bToStringNoSecrets = b.toString(false);
        String bToStringWithSecrets = b.toString(true);
        assertEquals(aToStringNoSecrets, bToStringNoSecrets);
        assertEquals(aToStringWithSecrets, bToStringWithSecrets);

        // credentials are the same
        if (a.getCredentials() != null && b.getCredentials() != null) {
            assertEquals(a.getCredentials().getClass(), b.getCredentials().getClass());
        }
        else if (a.getCredentials() == null && b.getCredentials() == null) {
            return;
        }
        else {
            fail("credentials mismatch");
        }
    }

    public void testCloudStorageAccountDevelopmentStorageAccount() throws InvalidKeyException, URISyntaxException {
        CloudStorageAccount devstoreAccount = CloudStorageAccount.getDevelopmentStorageAccount();
        assertEquals(devstoreAccount.getBlobStorageUri().getPrimaryUri(), new URI(
                "http://127.0.0.1:10000/devstoreaccount1"));
        assertEquals(devstoreAccount.getQueueStorageUri().getPrimaryUri(), new URI(
                "http://127.0.0.1:10001/devstoreaccount1"));
        assertEquals(devstoreAccount.getTableStorageUri().getPrimaryUri(), new URI(
                "http://127.0.0.1:10002/devstoreaccount1"));

        assertEquals(devstoreAccount.getBlobStorageUri().getSecondaryUri(), new URI(
                "http://127.0.0.1:10000/devstoreaccount1-secondary"));
        assertEquals(devstoreAccount.getQueueStorageUri().getSecondaryUri(), new URI(
                "http://127.0.0.1:10001/devstoreaccount1-secondary"));
        assertEquals(devstoreAccount.getTableStorageUri().getSecondaryUri(), new URI(
                "http://127.0.0.1:10002/devstoreaccount1-secondary"));

        String devstoreAccountToStringWithSecrets = devstoreAccount.toString(true);
        CloudStorageAccount testAccount = CloudStorageAccount.parse(devstoreAccountToStringWithSecrets);

        AccountsAreEqual(testAccount, devstoreAccount);
        // Following should not throw exception:
        CloudStorageAccount.parse(devstoreAccountToStringWithSecrets);
    }

    public void testCloudStorageAccountDefaultStorageAccountWithHttp() throws URISyntaxException, InvalidKeyException {
        StorageCredentialsAccountAndKey cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);
        CloudStorageAccount cloudStorageAccount = new CloudStorageAccount(cred, false);
        assertEquals(cloudStorageAccount.getBlobEndpoint(),
                new URI(String.format("http://%s.blob.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getQueueEndpoint(),
                new URI(String.format("http://%s.queue.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getTableEndpoint(),
                new URI(String.format("http://%s.table.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getFileEndpoint(),
                new URI(String.format("http://%s.file.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getBlobStorageUri().getSecondaryUri(),
                new URI(String.format("http://%s-secondary.blob.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getQueueStorageUri().getSecondaryUri(),
                new URI(String.format("http://%s-secondary.queue.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getTableStorageUri().getSecondaryUri(),
                new URI(String.format("http://%s-secondary.table.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getFileStorageUri().getSecondaryUri(),
                new URI(String.format("http://%s-secondary.file.core.windows.net", ACCOUNT_NAME)));

        String cloudStorageAccountToStringWithSecrets = cloudStorageAccount.toString(true);
        CloudStorageAccount testAccount = CloudStorageAccount.parse(cloudStorageAccountToStringWithSecrets);

        AccountsAreEqual(testAccount, cloudStorageAccount);
    }

    public void testCloudStorageAccountDefaultStorageAccountWithHttps() throws URISyntaxException, InvalidKeyException {
        StorageCredentialsAccountAndKey cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);
        CloudStorageAccount cloudStorageAccount = new CloudStorageAccount(cred, true);
        assertEquals(cloudStorageAccount.getBlobEndpoint(),
                new URI(String.format("https://%s.blob.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getQueueEndpoint(),
                new URI(String.format("https://%s.queue.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getTableEndpoint(),
                new URI(String.format("https://%s.table.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getFileEndpoint(),
                new URI(String.format("https://%s.file.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getBlobStorageUri().getSecondaryUri(),
                new URI(String.format("https://%s-secondary.blob.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getQueueStorageUri().getSecondaryUri(),
                new URI(String.format("https://%s-secondary.queue.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getTableStorageUri().getSecondaryUri(),
                new URI(String.format("https://%s-secondary.table.core.windows.net", ACCOUNT_NAME)));
        assertEquals(cloudStorageAccount.getFileStorageUri().getSecondaryUri(),
                new URI(String.format("https://%s-secondary.file.core.windows.net", ACCOUNT_NAME)));

        String cloudStorageAccountToStringWithSecrets = cloudStorageAccount.toString(true);
        CloudStorageAccount testAccount = CloudStorageAccount.parse(cloudStorageAccountToStringWithSecrets);

        AccountsAreEqual(testAccount, cloudStorageAccount);
    }

    public void testCloudStorageAccountConnectionStringRoundtrip() throws InvalidKeyException, URISyntaxException {
        String accountString1 = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s",
                ACCOUNT_NAME, ACCOUNT_KEY);

        String accountString2 = String.format(
                "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;QueueEndpoint=%s", ACCOUNT_NAME,
                ACCOUNT_KEY, "https://alternate.queue.endpoint/");

        connectionStringRoundtripHelper(accountString1);
        connectionStringRoundtripHelper(accountString2);
    }

    private void connectionStringRoundtripHelper(String accountString) throws InvalidKeyException, URISyntaxException {
        CloudStorageAccount originalAccount = CloudStorageAccount.parse(accountString);
        String copiedAccountString = originalAccount.toString(true);
        //        assertEquals(accountString, copiedAccountString);
        CloudStorageAccount copiedAccount = CloudStorageAccount.parse(copiedAccountString);

        // make sure it round trips
        this.AccountsAreEqual(originalAccount, copiedAccount);
    }

    public void testCloudStorageAccountClientMethods() throws URISyntaxException {
        StorageCredentialsAccountAndKey cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);

        CloudStorageAccount account = new CloudStorageAccount(cred, false);
        CloudBlobClient blob = account.createCloudBlobClient();
        CloudQueueClient queue = account.createCloudQueueClient();
        CloudTableClient table = account.createCloudTableClient();
        CloudFileClient file = account.createCloudFileClient();

        // check endpoints  
        assertEquals("Blob endpoint doesn't match account", account.getBlobEndpoint(), blob.getEndpoint());
        assertEquals("Queue endpoint doesn't match account", account.getQueueEndpoint(), queue.getEndpoint());
        assertEquals("Table endpoint doesn't match account", account.getTableEndpoint(), table.getEndpoint());
        assertEquals("File endpoint doesn't match account", account.getFileEndpoint(), file.getEndpoint());

        // check storage uris
        assertEquals("Blob endpoint doesn't match account", account.getBlobStorageUri(), blob.getStorageUri());
        assertEquals("Queue endpoint doesn't match account", account.getQueueStorageUri(), queue.getStorageUri());
        assertEquals("Table endpoint doesn't match account", account.getTableStorageUri(), table.getStorageUri());
        assertEquals("File endpoint doesn't match account", account.getFileStorageUri(), file.getStorageUri());

        // check creds
        assertEquals("Blob creds don't match account", account.getCredentials(), blob.getCredentials());
        assertEquals("Queue creds don't match account", account.getCredentials(), queue.getCredentials());
        assertEquals("Table creds don't match account", account.getCredentials(), table.getCredentials());
        assertEquals("File creds don't match account", account.getCredentials(), file.getCredentials());
    }

    public void testCloudStorageAccountClientUriVerify() throws URISyntaxException, StorageException {
        StorageCredentialsAccountAndKey cred = new StorageCredentialsAccountAndKey(ACCOUNT_NAME, ACCOUNT_KEY);
        CloudStorageAccount cloudStorageAccount = new CloudStorageAccount(cred, true);

        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("container1");
        assertEquals(cloudStorageAccount.getBlobEndpoint().toString() + "/container1", container.getUri().toString());

        CloudQueueClient queueClient = cloudStorageAccount.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference("queue1");
        assertEquals(cloudStorageAccount.getQueueEndpoint().toString() + "/queue1", queue.getUri().toString());

        CloudTableClient tableClient = cloudStorageAccount.createCloudTableClient();
        CloudTable table = tableClient.getTableReference("table1");
        assertEquals(cloudStorageAccount.getTableEndpoint().toString() + "/table1", table.getUri().toString());

        CloudFileClient fileClient = cloudStorageAccount.createCloudFileClient();
        CloudFileShare share = fileClient.getShareReference("share1");
        assertEquals(cloudStorageAccount.getFileEndpoint().toString() + "/share1", share.getUri().toString());
    }

    public void testCloudStorageAccountParseNullEmpty() throws InvalidKeyException, URISyntaxException {
        // parse() should throw exception when passing in null or empty string
        try {
            CloudStorageAccount.parse(null);
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING, ex.getMessage());
        }

        try {
            CloudStorageAccount.parse("");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING, ex.getMessage());
        }
    }

    public void testCloudStorageAccountDevStoreFalseFails()
            throws InvalidKeyException, URISyntaxException {
        try {
            CloudStorageAccount.parse("UseDevelopmentStorage=false");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING_DEV_STORE_NOT_TRUE, ex.getMessage());
        }
    }

    public void testCloudStorageAccountDevStoreFalsePlusAccountFails()
            throws InvalidKeyException, URISyntaxException {
        try {
            CloudStorageAccount.parse("UseDevelopmentStorage=false;AccountName=devstoreaccount1");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING_DEV_STORE_NOT_TRUE, ex.getMessage());
        }
    }

    public void testCloudStorageAccountDevStoreFalsePlusEndpointFails()
            throws InvalidKeyException, URISyntaxException {
        try {
            CloudStorageAccount.parse("UseDevelopmentStorage=false;"
                    + "BlobEndpoint=http://127.0.0.1:1000/devstoreaccount1");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING_DEV_STORE_NOT_TRUE, ex.getMessage());
        }
    }
    
    public void testCloudStorageAccountDevStoreFalsePlusEndpointSuffixFails()
            throws InvalidKeyException, URISyntaxException {
        try {
            CloudStorageAccount
                    .parse("UseDevelopmentStorage=false;EndpointSuffix=core.chinacloudapi.cn");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING_DEV_STORE_NOT_TRUE, ex.getMessage());
        }
    }

    public void testCloudStorageAccountDefaultEndpointOverride() throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount
                .parse("DefaultEndpointsProtocol=http;BlobEndpoint=http://customdomain.com/;AccountName=asdf;AccountKey=123=");

        assertEquals(new URI("http://customdomain.com/"), account.getBlobEndpoint());
        assertNull(account.getBlobStorageUri().getSecondaryUri());
    }

    public void testCloudStorageAccountDevStore() throws URISyntaxException {
        // default
        CloudStorageAccount account = CloudStorageAccount.getDevelopmentStorageAccount();
        assertEquals(new URI("http://127.0.0.1:10000/devstoreaccount1"), account.getBlobEndpoint());
        assertEquals(new URI("http://127.0.0.1:10001/devstoreaccount1"), account.getQueueEndpoint());
        assertEquals(new URI("http://127.0.0.1:10002/devstoreaccount1"), account.getTableEndpoint());
        assertEquals(new URI("http://127.0.0.1:10000/devstoreaccount1-secondary"), account.getBlobStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://127.0.0.1:10001/devstoreaccount1-secondary"), account.getQueueStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://127.0.0.1:10002/devstoreaccount1-secondary"), account.getTableStorageUri()
                .getSecondaryUri());

        // proxy
        account = CloudStorageAccount.getDevelopmentStorageAccount(new URI("http://ipv4.fiddler"));
        assertEquals(new URI("http://ipv4.fiddler:10000/devstoreaccount1"), account.getBlobEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10001/devstoreaccount1"), account.getQueueEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10002/devstoreaccount1"), account.getTableEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10000/devstoreaccount1-secondary"), account.getBlobStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://ipv4.fiddler:10001/devstoreaccount1-secondary"), account.getQueueStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://ipv4.fiddler:10002/devstoreaccount1-secondary"), account.getTableStorageUri()
                .getSecondaryUri());
    }

    public void testCloudStorageAccountDevStoreProxyUri() throws InvalidKeyException, URISyntaxException {
        CloudStorageAccount account = CloudStorageAccount
                .parse("UseDevelopmentStorage=true;DevelopmentStorageProxyUri=http://ipv4.fiddler");

        assertEquals(new URI("http://ipv4.fiddler:10000/devstoreaccount1"), account.getBlobEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10001/devstoreaccount1"), account.getQueueEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10002/devstoreaccount1"), account.getTableEndpoint());
        assertEquals(new URI("http://ipv4.fiddler:10000/devstoreaccount1-secondary"), account.getBlobStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://ipv4.fiddler:10001/devstoreaccount1-secondary"), account.getQueueStorageUri()
                .getSecondaryUri());
        assertEquals(new URI("http://ipv4.fiddler:10002/devstoreaccount1-secondary"), account.getTableStorageUri()
                .getSecondaryUri());
    }

    public void testCloudStorageAccountDevStoreRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "UseDevelopmentStorage=true";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountDevStoreProxyRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "UseDevelopmentStorage=true;DevelopmentStorageProxyUri=http://ipv4.fiddler/";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountDefaultCloudRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "EndpointSuffix=a.b.c;DefaultEndpointsProtocol=http;AccountName=test;"
                + "AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountExplicitCloudRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "EndpointSuffix=a.b.c;BlobEndpoint=https://blobs/;AccountName=test;"
                + "AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountAnonymousRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "BlobEndpoint=http://blobs/";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));

        CloudStorageAccount account = new CloudStorageAccount(
                null, new StorageUri(new URI("http://blobs/")), null, null, null);

        AccountsAreEqual(account, CloudStorageAccount.parse(account.toString(true)));
    }
    
    public void testCloudStorageAccountInvalidAnonymousRoundtrip()
            throws InvalidKeyException, URISyntaxException {
        String accountString = "AccountKey=abc=";
        try {
            assertNull(CloudStorageAccount.parse(accountString));
            fail();
        }
        catch (Exception ex) {
            assertEquals(SR.INVALID_CONNECTION_STRING, ex.getMessage());
        }
    }

    public void testCloudStorageAccountEmptyValues() throws InvalidKeyException, URISyntaxException {
        String accountString = ";EndpointSuffix=a.b.c;;BlobEndpoint=http://blobs/;;"
                + "AccountName=test;;AccountKey=abc=;;;";
        String validAccountString = "EndpointSuffix=a.b.c;BlobEndpoint=http://blobs/;"
                + "AccountName=test;AccountKey=abc=";

        assertEquals(validAccountString, CloudStorageAccount.parse(accountString).toString(true));
    }
    
    public void testCloudStorageAccountEndpointSuffix()
            throws InvalidKeyException, URISyntaxException, StorageException {
        final String mooncake = "core.chinacloudapi.cn";
        final String fairfax = "core.usgovcloudapi.net";
        
        // Endpoint suffix for mooncake
        CloudStorageAccount accountParse = CloudStorageAccount.parse(
                "DefaultEndpointsProtocol=http;AccountName=test;"
                + "AccountKey=abc=;EndpointSuffix=" + mooncake);
        CloudStorageAccount accountConstruct = new CloudStorageAccount(accountParse.getCredentials(),
                false, accountParse.getEndpointSuffix());
        assertNotNull(accountParse);
        assertNotNull(accountConstruct);
        assertNotNull(accountParse.getBlobEndpoint());
        assertEquals(accountParse.getBlobEndpoint(), accountConstruct.getBlobEndpoint());
        assertTrue(accountParse.getBlobEndpoint().toString().endsWith(mooncake));
        
        // Endpoint suffix for fairfax
        accountParse = CloudStorageAccount.parse(
                "TableEndpoint=http://tables/;DefaultEndpointsProtocol=http;"
                + "AccountName=test;AccountKey=abc=;EndpointSuffix=" + fairfax);
        accountConstruct = new CloudStorageAccount(accountParse.getCredentials(),
                false, accountParse.getEndpointSuffix());
        assertNotNull(accountParse);
        assertNotNull(accountConstruct);
        assertNotNull(accountParse.getBlobEndpoint());
        assertEquals(accountParse.getBlobEndpoint(), accountConstruct.getBlobEndpoint());
        assertTrue(accountParse.getBlobEndpoint().toString().endsWith(fairfax));
        
        // Explicit table endpoint should override endpoint suffix for fairfax
        CloudTableClient tableClientParse = accountParse.createCloudTableClient();
        assertNotNull(tableClientParse);
        assertEquals(accountParse.getTableEndpoint(), tableClientParse.getEndpoint());
        assertTrue(tableClientParse.getEndpoint().toString().endsWith("tables/"));
    }

    public void testCloudStorageAccountJustBlobToString() throws InvalidKeyException, URISyntaxException {
        String accountString = "BlobEndpoint=http://blobs/;AccountName=test;AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountJustQueueToString() throws InvalidKeyException, URISyntaxException {
        String accountString = "QueueEndpoint=http://queue/;AccountName=test;AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountJustTableToString() throws InvalidKeyException, URISyntaxException {
        String accountString = "TableEndpoint=http://table/;AccountName=test;AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountJustFileToString() throws InvalidKeyException, URISyntaxException {
        String accountString = "FileEndpoint=http://file/;AccountName=test;AccountKey=abc=";

        assertEquals(accountString, CloudStorageAccount.parse(accountString).toString(true));
    }

    public void testCloudStorageAccountExportKey() throws InvalidKeyException, URISyntaxException {
        String accountKeyString = "abc2564=";
        String accountString = "BlobEndpoint=http://blobs/;AccountName=test;AccountKey=" + accountKeyString;
        CloudStorageAccount account = CloudStorageAccount.parse(accountString);
        StorageCredentialsAccountAndKey accountAndKey = (StorageCredentialsAccountAndKey) account.getCredentials();
        String key = accountAndKey.getCredentials().getKey().getBase64EncodedKey();
        assertEquals(accountKeyString, key);

        byte[] keyBytes = accountAndKey.getCredentials().exportKey();
        byte[] expectedKeyBytes = Base64.decode(accountKeyString);
        TestHelper.assertByteArrayEquals(expectedKeyBytes, keyBytes);
    }
}
