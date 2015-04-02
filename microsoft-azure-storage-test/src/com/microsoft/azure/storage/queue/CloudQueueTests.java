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
package com.microsoft.azure.storage.queue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.AuthenticationScheme;
import com.microsoft.azure.storage.LocationMode;
import com.microsoft.azure.storage.NameValidator;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.RetryNoRetry;
import com.microsoft.azure.storage.SendingRequestEvent;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageErrorCodeStrings;
import com.microsoft.azure.storage.StorageEvent;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.TestHelper;
import com.microsoft.azure.storage.core.PathUtility;

/**
 * Queue Tests
 */
@SuppressWarnings("deprecation")
public class CloudQueueTests extends TestCase {

    private CloudQueue queue;

    @Override
    public void setUp() throws URISyntaxException, StorageException {
        this.queue = QueueTestHelper.getRandomQueueReference();
        this.queue.createIfNotExists();
    }

    @Override
    public void tearDown() throws StorageException {
        this.queue.deleteIfExists();
    }

    /**
     * Tests queue name validation.
     */
    public void testCloudQueueNameValidation()
    {
        NameValidator.validateQueueName("alpha");
        NameValidator.validateQueueName("4lphanum3r1c");
        NameValidator.validateQueueName("middle-dash");

        invalidQueueTestHelper(null, "Null not allowed.", "Invalid queue name. The name may not be null, empty, or whitespace only.");
        invalidQueueTestHelper("$root", "Alphanumeric or dashes only.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("double--dash", "No double dash.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("CapsLock", "Lowercase only.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("illegal$char", "Alphanumeric or dashes only.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("illegal!char", "Alphanumeric or dashes only.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("white space", "Alphanumeric or dashes only.", "Invalid queue name. Check MSDN for more information about valid naming.");
        invalidQueueTestHelper("2c", "Between 3 and 63 characters.", "Invalid queue name length. The name must be between 3 and 63 characters long.");
        invalidQueueTestHelper(new String(new char[64]).replace("\0", "n"), "Between 3 and 63 characters.", "Invalid queue name length. The name must be between 3 and 63 characters long.");
    }

    private void invalidQueueTestHelper(String queueName, String failMessage, String exceptionMessage)
    {
        try
        {
            NameValidator.validateQueueName(queueName);
            fail(failMessage);
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }
    
    /**
     * Get permissions from string
     */
    public void testQueuePermissionsFromString() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date start = cal.getTime();
        cal.add(Calendar.MINUTE, 30);
        Date expiry = cal.getTime();

        SharedAccessQueuePolicy policy = new SharedAccessQueuePolicy();
        policy.setSharedAccessStartTime(start);
        policy.setSharedAccessExpiryTime(expiry);

        policy.setPermissionsFromString("raup");
        assertEquals(EnumSet.of(SharedAccessQueuePermissions.READ, SharedAccessQueuePermissions.ADD,
                SharedAccessQueuePermissions.UPDATE, SharedAccessQueuePermissions.PROCESSMESSAGES),
                policy.getPermissions());

        policy.setPermissionsFromString("rap");
        assertEquals(EnumSet.of(SharedAccessQueuePermissions.READ, SharedAccessQueuePermissions.ADD,
                SharedAccessQueuePermissions.PROCESSMESSAGES), policy.getPermissions());

        policy.setPermissionsFromString("ar");
        assertEquals(EnumSet.of(SharedAccessQueuePermissions.READ, SharedAccessQueuePermissions.ADD),
                policy.getPermissions());

        policy.setPermissionsFromString("u");
        assertEquals(EnumSet.of(SharedAccessQueuePermissions.UPDATE), policy.getPermissions());
    }

    public void testQueueGetSetPermissionTest() throws StorageException, InterruptedException {
        QueuePermissions expectedPermissions;
        QueuePermissions testPermissions;

        // Test new permissions.
        expectedPermissions = new QueuePermissions();
        testPermissions = this.queue.downloadPermissions();
        assertQueuePermissionsEqual(expectedPermissions, testPermissions);

        // Test setting empty permissions.
        this.queue.uploadPermissions(expectedPermissions);
        Thread.sleep(30000);
        testPermissions = this.queue.downloadPermissions();
        assertQueuePermissionsEqual(expectedPermissions, testPermissions);

        // Add a policy, check setting and getting.
        SharedAccessQueuePolicy policy1 = new SharedAccessQueuePolicy();
        Calendar now = GregorianCalendar.getInstance();
        policy1.setSharedAccessStartTime(now.getTime());
        now.add(Calendar.MINUTE, 10);
        policy1.setSharedAccessExpiryTime(now.getTime());

        policy1.setPermissions(EnumSet.of(SharedAccessQueuePermissions.READ,
                SharedAccessQueuePermissions.PROCESSMESSAGES, SharedAccessQueuePermissions.ADD,
                SharedAccessQueuePermissions.UPDATE));
        expectedPermissions.getSharedAccessPolicies().put(UUID.randomUUID().toString(), policy1);

        this.queue.uploadPermissions(expectedPermissions);
        Thread.sleep(30000);
        testPermissions = this.queue.downloadPermissions();
        assertQueuePermissionsEqual(expectedPermissions, testPermissions);
    }

    public void testQueueSAS() throws StorageException, URISyntaxException, InvalidKeyException, InterruptedException {
        this.queue.addMessage(new CloudQueueMessage("sas queue test"));
        QueuePermissions expectedPermissions;

        expectedPermissions = new QueuePermissions();
        // Add a policy, check setting and getting.
        SharedAccessQueuePolicy policy1 = new SharedAccessQueuePolicy();
        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.MINUTE, -15);
        policy1.setSharedAccessStartTime(now.getTime());
        now.add(Calendar.MINUTE, 30);
        policy1.setSharedAccessExpiryTime(now.getTime());
        String identifier = UUID.randomUUID().toString();

        policy1.setPermissions(EnumSet.of(SharedAccessQueuePermissions.READ,
                SharedAccessQueuePermissions.PROCESSMESSAGES, SharedAccessQueuePermissions.ADD,
                SharedAccessQueuePermissions.UPDATE));
        expectedPermissions.getSharedAccessPolicies().put(identifier, policy1);

        this.queue.uploadPermissions(expectedPermissions);
        Thread.sleep(30000);

        CloudQueue identifierSasQueue = new CloudQueue(PathUtility.addToQuery(this.queue.getUri(),
                this.queue.generateSharedAccessSignature(null, identifier)));

        identifierSasQueue.downloadAttributes();
        identifierSasQueue.exists();

        identifierSasQueue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage message1 = identifierSasQueue.retrieveMessage();
        identifierSasQueue.deleteMessage(message1);

        CloudQueue policySasQueue = new CloudQueue(PathUtility.addToQuery(this.queue.getUri(),
                this.queue.generateSharedAccessSignature(policy1, null)));
        policySasQueue.exists();
        policySasQueue.downloadAttributes();

        policySasQueue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage message2 = policySasQueue.retrieveMessage();
        policySasQueue.deleteMessage(message2);

        // do not give the client and check that the new queue's client has the correct perms
        CloudQueue queueFromUri = new CloudQueue(PathUtility.addToQuery(this.queue.getStorageUri(),
                this.queue.generateSharedAccessSignature(null, "readperm")));
        assertEquals(StorageCredentialsSharedAccessSignature.class.toString(), queueFromUri.getServiceClient()
                .getCredentials().getClass().toString());

        // pass in a client which will have different permissions and check the sas permissions are used
        // and that the properties set in the old service client are passed to the new client
        CloudQueueClient queueClient = policySasQueue.getServiceClient();

        // set some arbitrary settings to make sure they are passed on
        queueClient.getDefaultRequestOptions().setLocationMode(LocationMode.PRIMARY_THEN_SECONDARY);
        queueClient.getDefaultRequestOptions().setTimeoutIntervalInMs(1000);
        queueClient.getDefaultRequestOptions().setRetryPolicyFactory(new RetryNoRetry());

        queueFromUri = new CloudQueue(PathUtility.addToQuery(this.queue.getStorageUri(),
                this.queue.generateSharedAccessSignature(null, "readperm")), queueClient);
        assertEquals(StorageCredentialsSharedAccessSignature.class.toString(), queueFromUri.getServiceClient()
                .getCredentials().getClass().toString());

        assertEquals(queueClient.getDefaultRequestOptions().getLocationMode(), queueFromUri.getServiceClient()
                .getDefaultRequestOptions().getLocationMode());
        assertEquals(queueClient.getDefaultRequestOptions().getTimeoutIntervalInMs(), queueFromUri.getServiceClient()
                .getDefaultRequestOptions().getTimeoutIntervalInMs());
        assertEquals(queueClient.getDefaultRequestOptions().getRetryPolicyFactory().getClass(), queueFromUri
                .getServiceClient().getDefaultRequestOptions().getRetryPolicyFactory().getClass());
    }

    static void assertQueuePermissionsEqual(QueuePermissions expected, QueuePermissions actual) {
        HashMap<String, SharedAccessQueuePolicy> expectedPolicies = expected.getSharedAccessPolicies();
        HashMap<String, SharedAccessQueuePolicy> actualPolicies = actual.getSharedAccessPolicies();
        assertEquals("SharedAccessPolicies.Count", expectedPolicies.size(), actualPolicies.size());
        for (String name : expectedPolicies.keySet()) {
            assertTrue("Key" + name + " doesn't exist", actualPolicies.containsKey(name));
            SharedAccessQueuePolicy expectedPolicy = expectedPolicies.get(name);
            SharedAccessQueuePolicy actualPolicy = actualPolicies.get(name);
            assertEquals("Policy: " + name + "\tPermissions\n", expectedPolicy.getPermissions().toString(),
                    actualPolicy.getPermissions().toString());
            assertEquals("Policy: " + name + "\tStartDate\n", expectedPolicy.getSharedAccessStartTime().toString(),
                    actualPolicy.getSharedAccessStartTime().toString());
            assertEquals("Policy: " + name + "\tExpireDate\n", expectedPolicy.getSharedAccessExpiryTime().toString(),
                    actualPolicy.getSharedAccessExpiryTime().toString());

        }

    }

    public void testQueueClientConstructor() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();

        CloudQueue queue1 = new CloudQueue(queueName, qClient);
        assertEquals(queueName, queue1.getName());
        assertTrue(queue1.getUri().toString().endsWith(queueName));
        assertEquals(qClient, queue1.getServiceClient());

        CloudQueue queue2 = new CloudQueue(new URI(QueueTestHelper.appendQueueName(qClient.getEndpoint(), queueName)),
                qClient);

        assertEquals(queueName, queue2.getName());
        assertEquals(qClient, queue2.getServiceClient());

        CloudQueue queue3 = new CloudQueue(queueName, qClient);
        assertEquals(queueName, queue3.getName());
        assertEquals(qClient, queue3.getServiceClient());
    }

    public void testGetMetadata() throws StorageException {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("ExistingMetadata", "ExistingMetadataValue");
        this.queue.setMetadata(metadata);
        this.queue.uploadMetadata();
        this.queue.downloadAttributes();
        assertEquals(this.queue.getMetadata().get("ExistingMetadata"), "ExistingMetadataValue");
        assertTrue(this.queue.getMetadata().containsKey("ExistingMetadata"));

        HashMap<String, String> empytMetadata = null;
        this.queue.setMetadata(empytMetadata);
        this.queue.uploadMetadata();
        this.queue.downloadAttributes();
        assertTrue(this.queue.getMetadata().size() == 0);
    }

    public void testUploadMetadata() throws URISyntaxException, StorageException {
        CloudQueue queueForGet = new CloudQueue(this.queue.getUri(), this.queue.getServiceClient());

        HashMap<String, String> metadata1 = new HashMap<String, String>();
        metadata1.put("ExistingMetadata1", "ExistingMetadataValue1");
        this.queue.setMetadata(metadata1);

        queueForGet.downloadAttributes();
        assertFalse(queueForGet.getMetadata().containsKey("ExistingMetadata1"));

        this.queue.uploadMetadata();
        queueForGet.downloadAttributes();
        assertTrue(queueForGet.getMetadata().containsKey("ExistingMetadata1"));
    }

    public void testUploadMetadataNullInput() throws URISyntaxException, StorageException {
        CloudQueue queueForGet = new CloudQueue(this.queue.getUri(), this.queue.getServiceClient());

        HashMap<String, String> metadata1 = new HashMap<String, String>();
        String key = "ExistingMetadata1" + UUID.randomUUID().toString().replace("-", "");
        metadata1.put(key, "ExistingMetadataValue1");
        this.queue.setMetadata(metadata1);

        queueForGet.downloadAttributes();
        assertFalse(queueForGet.getMetadata().containsKey(key));

        this.queue.uploadMetadata();
        queueForGet.downloadAttributes();
        assertTrue(queueForGet.getMetadata().containsKey(key));

        this.queue.setMetadata(null);
        this.queue.uploadMetadata();
        queueForGet.downloadAttributes();
        assertTrue(queueForGet.getMetadata().size() == 0);
    }

    public void testUploadMetadataClearExisting() throws URISyntaxException, StorageException {
        CloudQueue queueForGet = new CloudQueue(this.queue.getUri(), this.queue.getServiceClient());

        HashMap<String, String> metadata1 = new HashMap<String, String>();
        String key = "ExistingMetadata1" + UUID.randomUUID().toString().replace("-", "");
        metadata1.put(key, "ExistingMetadataValue1");
        this.queue.setMetadata(metadata1);

        queueForGet.downloadAttributes();
        assertFalse(queueForGet.getMetadata().containsKey(key));

        HashMap<String, String> metadata2 = new HashMap<String, String>();
        this.queue.setMetadata(metadata2);
        this.queue.uploadMetadata();
        queueForGet.downloadAttributes();
        assertTrue(queueForGet.getMetadata().size() == 0);
    }

    public void testUploadMetadataNotFound() throws URISyntaxException, StorageException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.uploadMetadata();
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testQueueCreate() throws URISyntaxException, StorageException {
        CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        OperationContext createQueueContext = new OperationContext();
        try {
            queue.create(null, createQueueContext);
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            try {
                HashMap<String, String> metadata1 = new HashMap<String, String>();
                metadata1.put("ExistingMetadata1", "ExistingMetadataValue1");
                queue.setMetadata(metadata1);
                queue.create();
                fail();
            }
            catch (StorageException e) {
                assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_CONFLICT);

            }

            queue.downloadAttributes();
            OperationContext createQueueContext2 = new OperationContext();
            queue.create(null, createQueueContext2);
            assertEquals(createQueueContext2.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
        }
        finally {
            queue.delete();
        }
    }

    public void testQueueCreateAlreadyExists() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {
            OperationContext createQueueContext1 = new OperationContext();
            queue.create(null, createQueueContext1);
            assertEquals(createQueueContext1.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            OperationContext createQueueContext2 = new OperationContext();
            queue.create(null, createQueueContext2);
            assertEquals(createQueueContext2.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testQueueCreateAfterDelete() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {
            OperationContext createQueueContext1 = new OperationContext();
            assertTrue(queue.createIfNotExists(null, createQueueContext1));
            assertEquals(createQueueContext1.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            assertTrue(queue.deleteIfExists());
            try {
                queue.create();
                fail("Queue CreateIfNotExists did not throw exception while trying to create a queue in BeingDeleted State");
            }
            catch (StorageException ex) {
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getHttpStatusCode(),
                        HttpURLConnection.HTTP_CONFLICT);
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getExtendedErrorInformation()
                        .getErrorCode(), StorageErrorCodeStrings.QUEUE_BEING_DELETED);
            }
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testQueueCreateIfNotExists() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {
            OperationContext createQueueContext = new OperationContext();
            assertTrue(queue.createIfNotExists(null, createQueueContext));
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            assertFalse(queue.createIfNotExists());
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testQueueCreateIfNotExistsAfterCreate() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {
            OperationContext createQueueContext1 = new OperationContext();
            assertTrue(queue.createIfNotExists(null, createQueueContext1));

            OperationContext createQueueContext2 = new OperationContext();
            assertFalse(queue.createIfNotExists(null, createQueueContext2));
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testQueueCreateIfNotExistsAfterDelete() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {

            OperationContext createQueueContext1 = new OperationContext();
            assertTrue(queue.createIfNotExists(null, createQueueContext1));
            assertEquals(createQueueContext1.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            assertTrue(queue.deleteIfExists());
            try {
                queue.createIfNotExists();
                fail("Queue CreateIfNotExists did not throw exception while trying to create a queue in BeingDeleted State");
            }
            catch (StorageException ex) {
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getHttpStatusCode(),
                        HttpURLConnection.HTTP_CONFLICT);
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getExtendedErrorInformation()
                        .getErrorCode(), StorageErrorCodeStrings.QUEUE_BEING_DELETED);
            }
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testQueueDelete() throws URISyntaxException, StorageException {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        CloudQueue queue = qClient.getQueueReference(queueName);
        assertEquals(queueName, queue.getName());

        try {
            OperationContext createQueueContext = new OperationContext();
            queue.create(null, createQueueContext);
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            OperationContext deleteQueueContext = new OperationContext();
            queue.delete(null, deleteQueueContext);
            assertEquals(deleteQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);

            try {
                queue.downloadAttributes();
                fail();
            }
            catch (StorageException ex) {
                assertEquals("Expected 404 Exception", ex.getHttpStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);
            }
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testDeleteQueueIfExists() throws URISyntaxException, StorageException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        assertFalse(queue.deleteIfExists());

        try {
            final OperationContext createQueueContext = new OperationContext();
            queue.create(null, createQueueContext);
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            assertTrue(queue.deleteIfExists());
            assertFalse(queue.deleteIfExists());

            try {
                queue.create();
                fail("Queue CreateIfNotExists did not throw exception while trying to create a queue in BeingDeleted State");
            }
            catch (StorageException ex) {
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getHttpStatusCode(),
                        HttpURLConnection.HTTP_CONFLICT);
                assertEquals("Expected 409 Exception, QueueBeingDeleted not thrown", ex.getExtendedErrorInformation()
                        .getErrorCode(), StorageErrorCodeStrings.QUEUE_BEING_DELETED);
            }
        }
        finally {
            queue.delete();
        }
    }

    public void testCloudQueueDeleteIfExistsErrorCode() throws StorageException, URISyntaxException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.delete();
            fail("Queue should not already exist.");
        }
        catch (StorageException e) {
            assertEquals(StorageErrorCodeStrings.QUEUE_NOT_FOUND, e.getErrorCode());
        }

        OperationContext ctx = new OperationContext();
        ctx.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                if (((HttpURLConnection) eventArg.getConnectionObject()).getRequestMethod().equals("DELETE")) {
                    try {
                        queue.delete();
                        assertFalse(queue.exists());
                    }
                    catch (StorageException e) {
                        fail("Delete should succeed.");
                    }
                }
            }
        });

        try {
            queue.create();

            // Queue deletes succeed before garbage collection occurs.
            assertTrue(queue.deleteIfExists(null, ctx));
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testDeleteNonExistingQueue() throws URISyntaxException, StorageException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        final OperationContext existQueueContext1 = new OperationContext();
        assertTrue(!queue.exists(null, existQueueContext1));
        assertEquals(existQueueContext1.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);

        try {
            queue.delete();
            fail("Queue delete no exsiting queue. ");
        }
        catch (StorageException ex) {
            assertEquals("Expected 404 Exception", ex.getHttpStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    public void testQueueExist() throws URISyntaxException, StorageException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        final OperationContext existQueueContext1 = new OperationContext();
        assertTrue(!queue.exists(null, existQueueContext1));
        assertEquals(existQueueContext1.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);

        try {
            final OperationContext createQueueContext = new OperationContext();
            queue.create(null, createQueueContext);
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

            final OperationContext existQueueContext2 = new OperationContext();
            assertTrue(queue.exists(null, existQueueContext2));
            assertEquals(existQueueContext2.getLastResult().getStatusCode(), HttpURLConnection.HTTP_OK);
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testClearMessages() throws  StorageException {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        int count = 0;
        for (CloudQueueMessage m : this.queue.peekMessages(32)) {
            assertNotNull(m);
            count++;
        }

        assertTrue(count == 2);

        OperationContext oc = new OperationContext();
        this.queue.clear(null, oc);
        assertEquals(oc.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);

        count = 0;
        for (CloudQueueMessage m : this.queue.peekMessages(32)) {
            assertNotNull(m);
            count++;
        }

        assertTrue(count == 0);
    }

    public void testClearMessagesEmptyQueue() throws StorageException {
        this.queue.clear();
        this.queue.delete();
    }

    public void testClearMessagesNotFound() throws  StorageException, URISyntaxException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.clear();
            fail();
        }
        catch (StorageException ex) {
            assertEquals("Expected 404 Exception", ex.getHttpStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    public void testAddMessage() throws  StorageException {
        String msgContent = UUID.randomUUID().toString();
        final CloudQueueMessage message = new CloudQueueMessage(msgContent);
        this.queue.addMessage(message);
        CloudQueueMessage msgFromRetrieve1 = this.queue.retrieveMessage();
        assertEquals(message.getMessageContentAsString(), msgContent);
        assertEquals(msgFromRetrieve1.getMessageContentAsString(), msgContent);
    }
    
    public void testAddMessageUnicode() throws StorageException {
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("Le dÃ©bat sur l'identitÃ© nationale, l'idÃ©e du prÃ©sident Nicolas Sarkozy de dÃ©choir des personnes d'origine Ã©trangÃ¨re de la nationalitÃ© franÃ§aise ... certains cas et les rÃ©centes mesures prises contre les Roms ont choquÃ© les experts, qui rendront leurs conclusions le 27 aoÃ»t.");
        messages.add("Ð’Ð°Ñˆ Ð»Ð¾Ð³Ð¸Ð½ Yahoo! Ð´Ð°ÐµÑ‚ Ð´Ð¾Ñ�Ñ‚ÑƒÐ¿ Ðº Ñ‚Ð°ÐºÐ¸Ð¼ Ð¼Ð¾Ñ‰Ð½Ñ‹Ð¼ Ð¸Ð½Ñ�Ñ‚Ñ€ÑƒÐ¼ÐµÐ½Ñ‚Ð°Ð¼ Ñ�Ð²Ñ�Ð·Ð¸, ÐºÐ°Ðº Ñ�Ð»ÐµÐºÑ‚Ñ€Ð¾Ð½Ð½Ð°Ñ� Ð¿Ð¾Ñ‡Ñ‚Ð°, Ð¾Ñ‚Ð¿Ñ€Ð°Ð²ÐºÐ° Ð¼Ð³Ð½Ð¾Ð²ÐµÐ½Ð½Ñ‹Ñ… Ñ�Ð¾Ð¾Ð±Ñ‰ÐµÐ½Ð¸Ð¹, Ñ„ÑƒÐ½ÐºÑ†Ð¸Ð¸ Ð±ÐµÐ·Ð¾Ð¿Ð°Ñ�Ð½Ð¾Ñ�Ñ‚Ð¸, Ð² Ñ‡Ð°Ñ�Ñ‚Ð½Ð¾Ñ�Ñ‚Ð¸, Ð°Ð½Ñ‚Ð¸Ð²Ð¸Ñ€ÑƒÑ�Ð½Ñ‹Ðµ Ñ�Ñ€ÐµÐ´Ñ�Ñ‚Ð²Ð° Ð¸ Ð±Ð»Ð¾ÐºÐ¸Ñ€Ð¾Ð²Ñ‰Ð¸Ðº Ð²Ñ�Ð¿Ð»Ñ‹Ð²Ð°ÑŽÑ‰ÐµÐ¹ Ñ€ÐµÐºÐ»Ð°Ð¼Ñ‹, Ð¸ Ð¸Ð·Ð±Ñ€Ð°Ð½Ð½Ð¾Ðµ, Ð½Ð°Ð¿Ñ€Ð¸Ð¼ÐµÑ€, Ñ„Ð¾Ñ‚Ð¾ Ð¸ Ð¼ÑƒÐ·Ñ‹ÐºÐ° Ð² Ñ�ÐµÑ‚Ð¸ â€” Ð²Ñ�Ðµ Ð±ÐµÑ�Ð¿Ð»Ð°Ñ‚");
        messages.add("æ�®æ–°å�Žç¤¾8æœˆ12æ—¥ç”µ 8æœˆ11æ—¥æ™šï¼ŒèˆŸæ›²å¢ƒå†…å†�æ¬¡å‡ºçŽ°å¼ºé™�é›¨å¤©æ°”ï¼Œä½¿ç‰¹å¤§å±±æ´ªæ³¥çŸ³æµ�ç�¾æƒ…é›ªä¸ŠåŠ éœœã€‚ç™½é¾™æ±Ÿæ°´åœ¨æ¢¨å��å­�æ�‘çš„äº¤æ±‡åœ°å¸¦å½¢æˆ�ä¸€ä¸ªæ–°çš„å °å¡žæ¹–ï¼Œæ°´ä½�æ¯”å¹³æ—¶é«˜å‡º3ç±³ã€‚ç”˜è‚ƒçœ�å›½åœŸèµ„æº�åŽ…å‰¯åŽ…é•¿å¼ å›½å�Žå½“æ—¥22æ—¶è®¸åœ¨æ–°é—»å�‘å¸ƒä¼šä¸Šä»‹ç»�ï¼Œæˆªè‡³12æ—¥21æ—¶50åˆ†ï¼ŒèˆŸæ›²å °å¡žæ¹–å °å¡žä½“å·²æ¶ˆé™¤ï¼Œæºƒå��é™©æƒ…å·²æ¶ˆé™¤ï¼Œç›®å‰�é’ˆå¯¹å °å¡žæ¹–çš„ä¸»è¦�å·¥ä½œæ˜¯ç–�é€šæ²³é�“ã€‚");
        messages.add("×œ ×›×•×œ×�\", ×”×“×”×™×� ×™×¢×œ×•×Ÿ, ×•×™×™×©×¨ ×§×• ×¢×� ×”×¢×“×•×ª ×©×ž×¡×¨ ×¨×�×© ×”×ž×ž×©×œ×”, ×‘× ×™×ž×™×Ÿ × ×ª× ×™×”×•, ×œ×•×•×¢×“×ª ×˜×™×¨×§×œ. ×œ×“×‘×¨×™×•, ×�×›×Ÿ ×”×©×¨×™×� ×“× ×• ×¨×§ ×‘×”×™×‘×˜×™×� ×”×ª×§×©×•×¨×ª×™×™×� ×©×œ ×¢×¦×™×¨×ª ×”×ž×©×˜: \"×‘×©×‘×™×¢×™×™×” ×œ×� ×”×ª×§×™×™×� ×“×™×•×Ÿ ×¢×œ ×”×�×œ×˜×¨× ×˜×™×‘×•×ª. ×¢×¡×§× ×• ×‘×”×™×‘×˜×™×� ");
        messages.add("Prozent auf 0,5 Prozent. Im Vergleich zum Vorjahresquartal wuchs die deutsche Wirtschaft von Januar bis MÃ¤rz um 2,1 Prozent. Auch das ist eine Korrektur nach oben, ursprÃ¼nglich waren es hier 1,7 Prozent");
        messages.add("<?xml version=\"1.0\"?>\n<!DOCTYPE PARTS SYSTEM \"parts.dtd\">\n<?xml-stylesheet type=\"text/css\" href=\"xmlpartsstyle.css\"?>\n<PARTS>\n   <TITLE>Computer Parts</TITLE>\n   <PART>\n      <ITEM>Motherboard</ITEM>\n      <MANUFACTURER>ASUS</MANUFACTURER>\n      <MODEL>"
                + "P3B-F</MODEL>\n      <COST> 123.00</COST>\n   </PART>\n   <PART>\n      <ITEM>Video Card</ITEM>\n      <MANUFACTURER>ATI</MANUFACTURER>\n      <MODEL>All-in-Wonder Pro</MODEL>\n      <COST> 160.00</COST>\n   </PART>\n   <PART>\n      <ITEM>Sound Card</ITEM>\n      <MANUFACTURER>"
                + "Creative Labs</MANUFACTURER>\n      <MODEL>Sound Blaster Live</MODEL>\n      <COST> 80.00</COST>\n   </PART>\n   <PART>\n      <ITEM> inch Monitor</ITEM>\n      <MANUFACTURER>LG Electronics</MANUFACTURER>\n      <MODEL> 995E</MODEL>\n      <COST> 290.00</COST>\n   </PART>\n</PARTS>");

        for (int i = 0; i < messages.size(); i++) {
            String msg = messages.get(i);
            this.queue.addMessage(new CloudQueueMessage(msg));
            CloudQueueMessage readBack = this.queue.retrieveMessage();
            assertEquals(msg, readBack.getMessageContentAsString());
            this.queue.deleteMessage(readBack);
        }

        this.queue.setShouldEncodeMessage(false);
        for (int i = 0; i < messages.size(); i++) {
            String msg = messages.get(i);
            this.queue.addMessage(new CloudQueueMessage(msg));
            CloudQueueMessage readBack = this.queue.retrieveMessage();
            assertEquals(msg, readBack.getMessageContentAsString());
            this.queue.deleteMessage(readBack);
        }
    }

    public void testAddMessageLargeVisibilityDelay() throws  StorageException
            {
        String msgContent = UUID.randomUUID().toString();
        final CloudQueueMessage message = new CloudQueueMessage(msgContent);
        this.queue.addMessage(message, 100, 50, null, null);
        CloudQueueMessage msgFromRetrieve1 = this.queue.retrieveMessage();
        assertNull(msgFromRetrieve1);
    }

    public void testDeleteMessageWithDifferentQueueInstance() throws  StorageException, URISyntaxException
            {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final String queueName = QueueTestHelper.generateRandomQueueName();
        final CloudQueue queue1 = qClient.getQueueReference(queueName);
        try {
            queue1.create();

            String msgContent = UUID.randomUUID().toString();
            final CloudQueueMessage message = new CloudQueueMessage(msgContent);
            queue1.addMessage(message);
            CloudQueueMessage msgFromRetrieved = queue1.retrieveMessage();

            final CloudQueue queue2 = qClient.getQueueReference(queueName);
            queue2.deleteMessage(msgFromRetrieved);
        }
        finally {
            queue1.deleteIfExists();
        }
    }

    public void testAddMessageToNonExistingQueue() throws  StorageException, URISyntaxException
            {
        final CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final CloudQueue queue = qClient.getQueueReference(QueueTestHelper.generateRandomQueueName());

        String messageContent = "messagetest";
        CloudQueueMessage message1 = new CloudQueueMessage(messageContent);

        try {
            queue.addMessage(message1);
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    public void testQueueUnicodeAndXmlMessageTest() throws  StorageException
            {
        String msgContent = "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¥ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â½<?xml version= 1.0  encoding= utf-8  ?>";
        final CloudQueueMessage message = new CloudQueueMessage(msgContent);
        this.queue.addMessage(message);
        CloudQueueMessage msgFromRetrieve1 = this.queue.retrieveMessage();
        assertEquals(message.getMessageContentAsString(), msgContent);
        assertEquals(msgFromRetrieve1.getMessageContentAsString(), msgContent);
    }

    public void testAddMessageLargeMessageInput() throws  StorageException
            {
        final Random rand = new Random();

        byte[] content = new byte[64 * 1024];
        rand.nextBytes(content);
        CloudQueueMessage message1 = new CloudQueueMessage(new String(content));

        try {
            this.queue.addMessage(message1);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        this.queue.delete();
    }

    public void testAddMessageWithVisibilityTimeout() throws StorageException, InterruptedException {
        this.queue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage m1 = this.queue.retrieveMessage();
        Date d1 = m1.getExpirationTime();
        this.queue.deleteMessage(m1);

        Thread.sleep(2000);

        this.queue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage m2 = this.queue.retrieveMessage();
        Date d2 = m2.getExpirationTime();
        this.queue.deleteMessage(m2);
        assertTrue(d1.before(d2));
    }

    public void testAddMessageNullMessage() throws  StorageException {
        try {
            this.queue.addMessage(null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testAddMessageSpecialVisibilityTimeout() throws  StorageException
            {
        CloudQueueMessage message = new CloudQueueMessage("test");
        this.queue.addMessage(message, 1, 0, null, null);
        this.queue.addMessage(message, 7 * 24 * 60 * 60, 0, null, null);
        this.queue.addMessage(message, 7 * 24 * 60 * 60, 7 * 24 * 60 * 60 - 1, null, null);

        try {
            this.queue.addMessage(message, -1, 0, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.addMessage(message, 0, -1, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.addMessage(message, 7 * 24 * 60 * 60, 7 * 24 * 60 * 60, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.addMessage(message, 7 * 24 * 60 * 60 + 1, 0, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.addMessage(message, 0, 7 * 24 * 60 * 60 + 1, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.updateMessage(message, 0, EnumSet.of(MessageUpdateFields.CONTENT), null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testDeleteMessage() throws  StorageException {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        for (CloudQueueMessage message : this.queue.retrieveMessages(32)) {
            OperationContext deleteQueueContext = new OperationContext();
            this.queue.deleteMessage(message, null, deleteQueueContext);
            assertEquals(deleteQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
        }

        assertTrue(this.queue.retrieveMessage() == null);
    }

    public void testQueueCreateAddingMetadata() throws  StorageException, URISyntaxException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        final HashMap<String, String> metadata = new HashMap<String, String>(5);
        for (int i = 0; i < 5; i++) {
            metadata.put("key" + i, "value" + i);
        }

        queue.setMetadata(metadata);

        final OperationContext createQueueContext = new OperationContext();

        try {
            queue.create(null, createQueueContext);
            assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);
        }
        finally {
            queue.deleteIfExists();
        }
    }

    public void testDeleteMessageNullMessage() throws  StorageException
            {
        try {
            this.queue.deleteMessage(null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testRetrieveMessage() throws StorageException, InterruptedException {
        this.queue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        OperationContext opContext = new OperationContext();
        CloudQueueMessage message1 = this.queue.retrieveMessage(10, null /*QueueRequestOptions*/, opContext);
        Date expirationTime1 = message1.getExpirationTime();
        Date insertionTime1 = message1.getInsertionTime();
        Date nextVisibleTime1 = message1.getNextVisibleTime();

        assertEquals(HttpURLConnection.HTTP_OK, opContext.getLastResult().getStatusCode());

        this.queue.deleteMessage(message1);

        Thread.sleep(2000);

        this.queue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage message2 = this.queue.retrieveMessage();
        Date expirationTime2 = message2.getExpirationTime();
        Date insertionTime2 = message2.getInsertionTime();
        Date nextVisibleTime2 = message2.getNextVisibleTime();
        this.queue.deleteMessage(message2);
        assertTrue(expirationTime1.before(expirationTime2));
        assertTrue(insertionTime1.before(insertionTime2));
        assertTrue(nextVisibleTime1.before(nextVisibleTime2));
    }

    public void testRetrieveMessageNonExistingQueue() throws  StorageException, URISyntaxException
            {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.retrieveMessage();
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testRetrieveMessageInvalidInput() throws  StorageException, URISyntaxException
            {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();

        try {
            queue.retrieveMessage(-1, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            queue.retrieveMessage(7 * 24 * 3600 + 1, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testRetrieveMessagesFromEmptyQueue() throws StorageException {
        for (CloudQueueMessage m : this.queue.retrieveMessages(32)) {
            assertTrue(m.getId() != null);
            assertTrue(m.getPopReceipt() == null);
        }
    }

    public void testRetrieveMessagesNonFound() throws  StorageException, URISyntaxException
            {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.retrieveMessages(1);
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testDequeueCountIncreases() throws StorageException, InterruptedException {
        this.queue.addMessage(new CloudQueueMessage("message"), 20, 0, null, null);
        CloudQueueMessage message1 = this.queue.retrieveMessage(1, null, null);
        assertTrue(message1.getDequeueCount() == 1);

        for (int i = 2; i < 5; i++) {
            Thread.sleep(2000);
            CloudQueueMessage message2 = this.queue.retrieveMessage(1, null, null);
            assertTrue(message2.getDequeueCount() == i);
        }

    }

    public void testRetrieveMessageSpecialVisibilityTimeout() throws  StorageException
            {
        try {
            this.queue.retrieveMessage(-1, null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testRetrieveMessages() throws  StorageException {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        for (CloudQueueMessage m : this.queue.retrieveMessages(32)) {
            assertTrue(m.getId() != null);
            assertTrue(m.getPopReceipt() != null);
        }
    }

    public void testRetrieveMessagesInvalidInput() throws  StorageException
            {
        for (int i = 0; i < 33; i++) {
            this.queue.addMessage(new CloudQueueMessage("test" + i));
        }

        this.queue.retrieveMessages(1, 1, null, null);
        this.queue.retrieveMessages(32, 1, null, null);

        try {
            this.queue.retrieveMessages(-1);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.retrieveMessages(0);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.retrieveMessages(33);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testPeekMessage() throws  StorageException {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage msg = this.queue.peekMessage();
        assertTrue(msg.getId() != null);
        assertTrue(msg.getPopReceipt() == null);

        this.queue.delete();
    }

    public void testPeekMessages() throws  StorageException {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        for (CloudQueueMessage m : this.queue.peekMessages(32)) {
            assertTrue(m.getId() != null);
            assertTrue(m.getPopReceipt() == null);
        }
    }

    public void testPeekMessagesInvalidInput() throws  StorageException
            {
        for (int i = 0; i < 33; i++) {
            this.queue.addMessage(new CloudQueueMessage("test" + i));
        }

        this.queue.peekMessages(1);
        this.queue.peekMessages(32);

        try {
            this.queue.peekMessages(-1);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.peekMessages(0);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        try {
            this.queue.peekMessages(33);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testPeekMessageNonExistingQueue() throws  StorageException, URISyntaxException
            {
        CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.peekMessage();
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testPeekMessagesNonFound() throws  StorageException, URISyntaxException {
        CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.peekMessages(1);
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testPeekMessagesFromEmptyQueue() throws StorageException {
        for (CloudQueueMessage m : this.queue.peekMessages(32)) {
            assertTrue(m.getId() != null);
            assertTrue(m.getPopReceipt() == null);
        }
    }


    public void testUpdateMessage() throws StorageException {
        this.queue.clear();

        String messageContent = "messagetest";
        CloudQueueMessage message1 = new CloudQueueMessage(messageContent);
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage(messageContent);
        this.queue.addMessage(message2);

        String newMesage = message1.getMessageContentAsString() + "updated";

        for (CloudQueueMessage message : this.queue.retrieveMessages(32)) {
            OperationContext oc = new OperationContext();
            message.setMessageContent(newMesage);
            this.queue.updateMessage(message, 0, EnumSet.of(MessageUpdateFields.VISIBILITY), null, oc);
            assertEquals(oc.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
            CloudQueueMessage messageFromGet = this.queue.retrieveMessage();
            assertEquals(messageFromGet.getMessageContentAsString(), messageContent);
        }
    }

    public void testUpdateMessageFullPass() throws  StorageException, InterruptedException
             {
        CloudQueueMessage message = new CloudQueueMessage("message");
        this.queue.addMessage(message, 20, 0, null, null);
        CloudQueueMessage message1 = this.queue.retrieveMessage();
        String popreceipt1 = message1.getPopReceipt();
        Date NextVisibleTim1 = message1.getNextVisibleTime();
        this.queue.updateMessage(message1, 100, EnumSet.of(MessageUpdateFields.VISIBILITY), null, null);
        String popreceipt2 = message1.getPopReceipt();
        Date NextVisibleTim2 = message1.getNextVisibleTime();
        assertTrue(popreceipt2 != popreceipt1);
        assertTrue(NextVisibleTim1.before(NextVisibleTim2));

        Thread.sleep(2000);

        String newMesage = message.getMessageContentAsString() + "updated";
        message.setMessageContent(newMesage);
        OperationContext oc = new OperationContext();
        this.queue.updateMessage(message1, 100, EnumSet.of(MessageUpdateFields.CONTENT), null, oc);
        assertEquals(oc.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
        String popreceipt3 = message1.getPopReceipt();
        Date NextVisibleTim3 = message1.getNextVisibleTime();
        assertTrue(popreceipt3 != popreceipt2);
        assertTrue(NextVisibleTim2.before(NextVisibleTim3));

        assertTrue(this.queue.retrieveMessage() == null);

        this.queue.updateMessage(message1, 0, EnumSet.of(MessageUpdateFields.VISIBILITY), null, null);

        CloudQueueMessage messageFromGet = this.queue.retrieveMessage();
        assertEquals(messageFromGet.getMessageContentAsString(), message1.getMessageContentAsString());
    }

    public void testUpdateMessageWithContentChange() throws  StorageException
            {
        CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        for (CloudQueueMessage message : this.queue.retrieveMessages(32)) {
            OperationContext oc = new OperationContext();
            message.setMessageContent(message.getMessageContentAsString() + "updated");
            this.queue.updateMessage(message, 100, EnumSet.of(MessageUpdateFields.CONTENT), null, oc);
            assertEquals(oc.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);
        }
    }

    public void testUpdateMessageNullMessage() throws  StorageException
            {
        try {
            this.queue.updateMessage(null, 0);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }
    }

    public void testUpdateMessageInvalidMessage() throws  StorageException
            {
        CloudQueueMessage message = new CloudQueueMessage("test");
        this.queue.addMessage(message, 1, 0, null, null);

        try {
            this.queue.updateMessage(message, 0, EnumSet.of(MessageUpdateFields.CONTENT), null, null);
            fail();
        }
        catch (final IllegalArgumentException e) {
        }

        this.queue.delete();
    }

    public void testGetApproximateMessageCount() throws StorageException {
        assertTrue(this.queue.getApproximateMessageCount() == 0);
        this.queue.addMessage(new CloudQueueMessage("message1"));
        this.queue.addMessage(new CloudQueueMessage("message2"));
        assertTrue(this.queue.getApproximateMessageCount() == 0);
        this.queue.downloadAttributes();
        assertTrue(this.queue.getApproximateMessageCount() == 2);
        this.queue.delete();
    }

    public void testShouldEncodeMessage() throws  StorageException {
        String msgContent = UUID.randomUUID().toString();
        final CloudQueueMessage message = new CloudQueueMessage(msgContent);
        this.queue.setShouldEncodeMessage(true);
        this.queue.addMessage(message);
        CloudQueueMessage msgFromRetrieve1 = this.queue.retrieveMessage();
        assertEquals(msgFromRetrieve1.getMessageContentAsString(), msgContent);
        this.queue.deleteMessage(msgFromRetrieve1);

        this.queue.setShouldEncodeMessage(false);
        this.queue.addMessage(message);
        CloudQueueMessage msgFromRetrieve2 = this.queue.retrieveMessage();
        assertEquals(msgFromRetrieve2.getMessageContentAsString(), msgContent);
        this.queue.deleteMessage(msgFromRetrieve2);

        this.queue.setShouldEncodeMessage(true);
    }

    public void testQueueDownloadAttributes() throws  StorageException, URISyntaxException {
        final CloudQueueMessage message1 = new CloudQueueMessage("messagetest1");
        this.queue.addMessage(message1);

        final CloudQueueMessage message2 = new CloudQueueMessage("messagetest2");
        this.queue.addMessage(message2);

        final HashMap<String, String> metadata = new HashMap<String, String>(5);
        int sum = 5;
        for (int i = 0; i < sum; i++) {
            metadata.put("key" + i, "value" + i);
        }

        this.queue.setMetadata(metadata);
        this.queue.uploadMetadata();

        CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        final CloudQueue queue2 = qClient.getQueueReference(this.queue.getName());
        queue2.downloadAttributes();

        assertEquals(sum, queue2.getMetadata().size());
    }

    public void testQueueDownloadAttributesNotFound() throws  StorageException, URISyntaxException {
        final CloudQueue queue = QueueTestHelper.getRandomQueueReference();
        try {
            queue.downloadAttributes();
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND);

        }
    }

    public void testQueueUpdateMetaData() throws  StorageException {
        final HashMap<String, String> metadata = new HashMap<String, String>(5);
        for (int i = 0; i < 5; i++) {
            metadata.put("key" + i, "value" + i);
        }

        this.queue.setMetadata(metadata);
        this.queue.uploadMetadata();
    }

    public void testSASClientParse() throws StorageException,  InvalidKeyException, URISyntaxException {
        // Add a policy, check setting and getting.
        SharedAccessQueuePolicy policy1 = new SharedAccessQueuePolicy();
        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.MINUTE, -15);
        policy1.setSharedAccessStartTime(now.getTime());
        now.add(Calendar.MINUTE, 30);
        policy1.setSharedAccessExpiryTime(now.getTime());

        policy1.setPermissions(EnumSet.of(SharedAccessQueuePermissions.READ,
                SharedAccessQueuePermissions.PROCESSMESSAGES, SharedAccessQueuePermissions.ADD,
                SharedAccessQueuePermissions.UPDATE));

        String sasString = this.queue.generateSharedAccessSignature(policy1, null);

        URI queueUri = new URI("http://myaccount.queue.core.windows.net/myqueue");

        CloudQueueClient queueClient1 = new CloudQueueClient(new URI("http://myaccount.queue.core.windows.net/"),
                new StorageCredentialsSharedAccessSignature(sasString));

        CloudQueue queue1 = new CloudQueue(queueUri, queueClient1);
        queue1.getName();

        CloudQueueClient queueClient2 = new CloudQueueClient(new URI("http://myaccount.queue.core.windows.net/"),
                new StorageCredentialsSharedAccessSignature(sasString));
        CloudQueue queue2 = new CloudQueue(queueUri, queueClient2);
        queue2.getName();
    }

    public void testQueueSharedKeyLite() throws  StorageException, URISyntaxException {
        CloudQueueClient qClient = TestHelper.createCloudQueueClient();
        qClient.setAuthenticationScheme(AuthenticationScheme.SHAREDKEYLITE);
        CloudQueue queue = qClient.getQueueReference(QueueTestHelper.generateRandomQueueName());

        OperationContext createQueueContext = new OperationContext();
        queue.create(null, createQueueContext);
        assertEquals(createQueueContext.getLastResult().getStatusCode(), HttpURLConnection.HTTP_CREATED);

        try {
            HashMap<String, String> metadata1 = new HashMap<String, String>();
            metadata1.put("ExistingMetadata1", "ExistingMetadataValue1");
            queue.setMetadata(metadata1);
            queue.create();
            fail();
        }
        catch (StorageException e) {
            assertTrue(e.getHttpStatusCode() == HttpURLConnection.HTTP_CONFLICT);

        }

        queue.downloadAttributes();
        OperationContext createQueueContext2 = new OperationContext();
        queue.create(null, createQueueContext2);
        assertEquals(createQueueContext2.getLastResult().getStatusCode(), HttpURLConnection.HTTP_NO_CONTENT);

        queue.delete();
    }
}
