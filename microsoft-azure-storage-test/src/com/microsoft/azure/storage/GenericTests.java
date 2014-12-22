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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import junit.framework.TestCase;

import com.microsoft.azure.storage.blob.BlobOutputStream;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.BlobTestHelper;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.SR;
import com.microsoft.azure.storage.core.Utility;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;

public class GenericTests extends TestCase {

    public void testReadTimeoutIssue() throws URISyntaxException, StorageException, IOException {
        // part 1
        byte[] buffer = BlobTestHelper.getRandomBuffer(1 * 1024 * 1024);

        // set the maximum execution time
        BlobRequestOptions options = new BlobRequestOptions();
        options.setMaximumExecutionTimeInMs(5000);

        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(generateRandomContainerName());

        String blobName = "testBlob";
        final CloudBlockBlob blockBlobRef = container.getBlockBlobReference(blobName);
        blockBlobRef.setStreamWriteSizeInBytes(1 * 1024 * 1024);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
        BlobOutputStream blobOutputStream = null;

        try {
            container.createIfNotExists();
            blobOutputStream = blockBlobRef.openOutputStream(null, options, null);
            try {
                blobOutputStream.write(inputStream, buffer.length);
            }
            finally {
                blobOutputStream.close();
            }
            assertTrue(blockBlobRef.exists());
        }
        finally {
            inputStream.close();
            container.deleteIfExists();
        }

        // part 2
        int length2 = 10 * 1024 * 1024;
        byte[] uploadBuffer2 = BlobTestHelper.getRandomBuffer(length2);

        CloudBlobClient blobClient2 = TestHelper.createCloudBlobClient();
        CloudBlobContainer container2 = blobClient2.getContainerReference(generateRandomContainerName());

        String blobName2 = "testBlob";
        final CloudBlockBlob blockBlobRef2 = container2.getBlockBlobReference(blobName2);

        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(uploadBuffer2);

        try {
            container2.createIfNotExists();

            blockBlobRef2.upload(inputStream2, length2);
        }
        finally {
            inputStream2.close();
            container2.deleteIfExists();
        }
    }

    /**
     * Make sure that if a request throws an error when it is being built that the request is not sent.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testExecutionEngineErrorHandling() throws URISyntaxException, StorageException {
        CloudBlobContainer container = BlobTestHelper.getRandomContainerReference();
        try {
            final ArrayList<Boolean> callList = new ArrayList<Boolean>();

            OperationContext opContext = new OperationContext();
            opContext.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {
                // insert a metadata element with an empty value
                @Override
                public void eventOccurred(SendingRequestEvent eventArg) {
                    callList.add(true);
                }
            });

            container.getMetadata().put("key", " "); // invalid value
            try {
                container.uploadMetadata(null, null, opContext);
                fail(SR.METADATA_KEY_INVALID);
            }
            catch (StorageException e) {
                // make sure a request was not sent
                assertEquals(0, callList.size());

                assertEquals(SR.METADATA_VALUE_INVALID, e.getMessage());
            }
        }
        finally {
            container.deleteIfExists();
        }
    }

    public void testUserAgentString() throws URISyntaxException, StorageException {
        // Test with a blob request
        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("container1");
        OperationContext sendingRequestEventContext = new OperationContext();
        sendingRequestEventContext.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                assertEquals(
                        Constants.HeaderConstants.USER_AGENT_PREFIX
                                + "/"
                                + Constants.HeaderConstants.USER_AGENT_VERSION
                                + " "
                                + String.format(Utility.LOCALE_US, "(Android %s; %s; %s)",
                                        android.os.Build.VERSION.RELEASE, android.os.Build.BRAND,
                                        android.os.Build.MODEL), ((HttpURLConnection) eventArg.getConnectionObject())
                                .getRequestProperty(Constants.HeaderConstants.USER_AGENT));
            }
        });
        container.exists(null, null, sendingRequestEventContext);

        // Test with a queue request
        CloudQueueClient queueClient = TestHelper.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference("queue1");
        queue.exists(null, sendingRequestEventContext);

        // Test with a table request
        CloudTableClient tableClient = TestHelper.createCloudTableClient();
        CloudTable table = tableClient.getTableReference("table1");
        table.exists(null, sendingRequestEventContext);
    }

    public void testUserHeaders() throws URISyntaxException, StorageException {
        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("container1");
        OperationContext context = new OperationContext();

        // no user headers
        container.exists(null, null, context);

        // add user headers
        HashMap<String, String> userHeaders = new HashMap<String, String>();
        userHeaders.put("x-ms-foo", "bar");
        userHeaders.put("x-ms-hello", "value");
        context.setUserHeaders(userHeaders);
        StorageEvent<SendingRequestEvent> event = new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                HttpURLConnection connection = (HttpURLConnection) eventArg.getConnectionObject();
                assertNotNull(connection.getRequestProperty("x-ms-foo"));
                assertNotNull(connection.getRequestProperty("x-ms-hello"));
            }
        };

        context.getSendingRequestEventHandler().addListener(event);
        container.exists(null, null, context);

        // clear user headers
        userHeaders.clear();
        context.getSendingRequestEventHandler().removeListener(event);
        context.setUserHeaders(userHeaders);
        context.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                HttpURLConnection connection = (HttpURLConnection) eventArg.getConnectionObject();
                assertNull(connection.getRequestProperty("x-ms-foo"));
                assertNull(connection.getRequestProperty("x-ms-hello"));
            }
        });

        container.exists(null, null, context);
    }

    public void testNullRetryPolicy() throws URISyntaxException, StorageException {
        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("container1");

        blobClient.getDefaultRequestOptions().setRetryPolicyFactory(null);
        container.exists();
    }

    public void testMaximumExecutionTime() throws URISyntaxException, StorageException {
        OperationContext opContext = new OperationContext();
        setDelay(opContext, 2500);

        // set the maximum execution time
        BlobRequestOptions options = new BlobRequestOptions();
        options.setMaximumExecutionTimeInMs(2000);

        // set the location mode to secondary, secondary request should fail
        // so set the timeout low to save time failing (or fail with a timeout)
        options.setLocationMode(LocationMode.SECONDARY_THEN_PRIMARY);
        options.setTimeoutIntervalInMs(1000);

        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(generateRandomContainerName());

        try {
            // 1. download attributes will fail as the container does not exist
            // 2. the executor will attempt to retry as it is accessing secondary
            // 3. maximum execution time should prevent the retry from being made
            container.downloadAttributes(null, options, opContext);
            fail("Maximum execution time was reached but request did not fail.");
        }
        catch (StorageException e) {
            assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getMessage());
        }
    }

    public void testMaximumExecutionTimeBlobWrites() throws URISyntaxException, StorageException, IOException {
        byte[] buffer = BlobTestHelper.getRandomBuffer(80 * 1024 * 1024);

        // set the maximum execution time
        BlobRequestOptions options = new BlobRequestOptions();
        options.setMaximumExecutionTimeInMs(5000);

        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(generateRandomContainerName());

        String blobName = "testBlob";
        final CloudBlockBlob blockBlobRef = container.getBlockBlobReference(blobName);
        blockBlobRef.setStreamWriteSizeInBytes(1 * 1024 * 1024);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
        BlobOutputStream blobOutputStream = null;

        try {
            container.createIfNotExists();

            // make sure max timeout is thrown by Utility.writeToOutputStream() on upload
            try {
                blockBlobRef.upload(inputStream, buffer.length, null, options, null);
                fail("Maximum execution time was reached but request did not fail.");
            }
            catch (StorageException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getMessage());
            }
            catch (IOException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
            }
            assertFalse(blockBlobRef.exists());

            // make sure max timeout applies on a per service request basis if the user creates the stream
            // adds a delay so the first service request should fail
            OperationContext opContext = new OperationContext();
            setDelay(opContext, 6000);
            blobOutputStream = blockBlobRef.openOutputStream(null, options, opContext);
            try {
                blobOutputStream.write(inputStream, buffer.length);
                fail("Maximum execution time was reached but request did not fail.");
            }
            catch (StorageException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
            }
            catch (IOException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
            }
            finally {
                try {
                    blobOutputStream.close();
                }
                catch (IOException e) {
                    assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
                }
            }
            assertFalse(blockBlobRef.exists());

            // make sure max timeout applies on a per service request basis if the user creates the stream
            // adds a delay so the first service request should fail
            blobOutputStream = blockBlobRef.openOutputStream(null, options, opContext);
            try {
                blobOutputStream.write(buffer);
                fail("Maximum execution time was reached but request did not fail.");
            }
            catch (IOException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
            }
            finally {
                try {
                    blobOutputStream.close();
                }
                catch (IOException e) {
                    assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
                }
            }
            assertFalse(blockBlobRef.exists());

            // make sure max timeout applies on a per service request basis only if the user creates the stream
            // should succeed as even if all requests would exceed the timeout, each one won't
            blobOutputStream = blockBlobRef.openOutputStream(null, options, null);
            try {
                blobOutputStream.write(inputStream, buffer.length);
            }
            finally {
                blobOutputStream.close();
            }
            assertTrue(blockBlobRef.exists());
        }
        finally {
            inputStream.close();
            container.deleteIfExists();
        }
    }

    public void testMaximumExecutionTimeBlobByteArray() throws URISyntaxException, StorageException, IOException {
        int length = 10 * 1024 * 1024;
        byte[] uploadBuffer = BlobTestHelper.getRandomBuffer(length);
        byte[] downloadBuffer = new byte[length];

        // set a delay in sending request
        OperationContext opContext = new OperationContext();
        setDelay(opContext, 2500);

        // set the maximum execution time
        BlobRequestOptions options = new BlobRequestOptions();
        options.setMaximumExecutionTimeInMs(2000);

        CloudBlobClient blobClient = TestHelper.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(generateRandomContainerName());

        String blobName = "testBlob";
        final CloudBlockBlob blockBlobRef = container.getBlockBlobReference(blobName);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(uploadBuffer);

        try {
            container.createIfNotExists();

            blockBlobRef.upload(inputStream, length);
            assertTrue(blockBlobRef.exists());

            try {
                blockBlobRef.downloadToByteArray(downloadBuffer, 0, null, options, opContext);
                fail("Maximum execution time was reached but request did not fail.");
            }
            catch (StorageException e) {
                assertEquals(SR.MAXIMUM_EXECUTION_TIMEOUT_EXCEPTION, e.getCause().getMessage());
            }
        }
        finally {
            inputStream.close();
            container.deleteIfExists();
        }
    }

    public void testDateStringParsingWithRounding() throws ParseException {
        String fullDateString = "1999-12-31T23:59:45.1234567Z";
        SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
        Date milliDate = testFormat.parse("1999-12-31T23:59:45.123 -0000");
        assertEquals(milliDate, Utility.parseDate(fullDateString));

        fullDateString = "1999-04-30T23:59:55.9876Z";
        long millisSinceEpoch = 925516795987L;

        Date deciDate = Utility.parseDate(fullDateString.replace("876Z", "Z"));
        assertEquals(deciDate.getTime(), (millisSinceEpoch / 100) * 100);

        Date centiDate = Utility.parseDate(fullDateString.replace("76Z", "Z"));
        assertEquals(centiDate.getTime(), (millisSinceEpoch / 10) * 10);

        milliDate = Utility.parseDate(fullDateString);
        assertEquals(milliDate.getTime(), millisSinceEpoch);
    }
    
    public void testDateStringParsing() throws ParseException {
        // 2014-12-07T09:15:12.123Z  from Java
        testDate("2014-12-07T09:15:12.123Z", 1417943712123L, 0, false, false);

        // 2015-01-14T14:53:32.800Z  from Java
        testDate("2015-01-14T14:53:32.800Z", 1421247212800L, 0, false, false);

        // 2014-11-29T22:55:21.9876543Z  from .Net
        testDate("2014-11-29T22:55:21.9876543Z", 1417301721987L, 6543, false, false);

        // 2015-02-14T03:11:13.0000229Z  from .Net
        testDate("2015-02-14T03:11:13.0000229Z", 1423883473000L, 229, false, false);
    }

    public void testDateStringParsingCrossVersion() throws ParseException {
        // 2014-12-07T09:15:12.123Z  from Java, milliseconds are incorrectly left-padded
        testDate("2014-12-07T09:15:12.0000123Z", 1417943712123L, 0, true, false);

        // 2015-01-14T14:53:32.800Z  from Java, milliseconds are incorrectly left-padded
        testDate("2015-01-14T14:53:32.0000800Z", 1421247212800L, 0, true, false);

        // 2014-11-29T22:55:21.9876543Z  from .Net
        testDate("2014-11-29T22:55:21.9876543Z", 1417301721987L, 6543, true, false);

        // 2015-02-14T03:11:13.0000229Z  from .Net
        testDate("2015-02-14T03:11:13.0000229Z", 1423883473000L, 229, true, false);
    }
    
    public void testDateStringParsingWithBackwardCompatibility() throws ParseException {
        // 2014-12-07T09:15:12.123Z  from Java
        testDate("2014-12-07T09:15:12.123Z", 1417943712123L, 0, false, true);

        // 2015-01-14T14:53:32.800Z  from Java
        testDate("2015-01-14T14:53:32.800Z", 1421247212800L, 0, false, true);

        // 2014-11-29T22:55:21.9876543Z  from .Net
        testDate("2014-11-29T22:55:21.9876543Z", 1417301721987L, 6543, false, true);

        // 2015-02-14T03:11:13.0000229Z  from .Net
        testDate("2015-02-14T03:11:13.0000229Z", 1423883473000L, 229, false, true);
    }

    public void testDateStringParsingCrossVersionWithBackwardCompatibility() throws ParseException {
        // 2014-12-07T09:15:12.123Z  from Java, milliseconds are incorrectly left-padded
        testDate("2014-12-07T09:15:12.0000123Z", 1417943712123L, 0, true, true);

        // 2015-01-14T14:53:32.800Z  from Java, milliseconds are incorrectly left-padded
        testDate("2015-01-14T14:53:32.0000800Z", 1421247212800L, 0, true, true);

        // 2014-11-29T22:55:21.9876543Z  from .Net
        testDate("2014-11-29T22:55:21.9876543Z", 1417301721987L, 6543, true, true);

        // 2015-02-14T03:11:13.0000229Z  from .Net
        testDate("2015-02-14T03:11:13.0000229Z", 1423883473000L, 229, true, true);
    }

    private static void testDate(final String dateString, final long intendedMilliseconds, final int ticks,
            final boolean writtenPre2, final boolean dateBackwardCompatibility) {
        assertTrue(ticks >= 0);     // ticks is non-negative
        assertTrue(ticks <= 9999);  // ticks do not overflow into milliseconds
        long expectedMilliseconds = intendedMilliseconds;
        
        if (dateBackwardCompatibility && (intendedMilliseconds % 1000 == 0) && (ticks < 1000)) {
            // when no milliseconds are present dateBackwardCompatibility causes up to 3 digits of ticks
            // to be read as milliseconds
            expectedMilliseconds += ticks;
        } else if (writtenPre2 && !dateBackwardCompatibility && (ticks == 0)) {
            // without DateBackwardCompatibility, milliseconds stored by Java prior to 0.4.0 are lost
            expectedMilliseconds -= expectedMilliseconds % 1000;
        }
        
        assertEquals(expectedMilliseconds, Utility.parseDate(dateString, dateBackwardCompatibility).getTime());
    }

    public void testDateStringFormatting() {
        String fullDateString = "2014-12-07T09:15:12.123Z";
        String outDateString = Utility.getJavaISO8601Time(Utility.parseDate(fullDateString));
        assertEquals(fullDateString, outDateString);
        
        fullDateString = "2015-01-14T14:53:32.800Z";
        outDateString = Utility.getJavaISO8601Time(Utility.parseDate(fullDateString));
        assertEquals(fullDateString, outDateString);
        
        // Ensure that trimming of trailing zeroes by the service does not affect this
        fullDateString = "2015-01-14T14:53:32.8Z";
        outDateString = Utility.getJavaISO8601Time(Utility.parseDate(fullDateString));
        fullDateString = fullDateString.replace("Z", "00Z");
        assertEquals(fullDateString, outDateString);
        
        // Ensure that trimming of trailing zeroes by the service does not affect this
        // even with dateBackwardCompatibility
        fullDateString = "2015-01-14T14:53:32.0000800Z";
        outDateString = Utility.getJavaISO8601Time(Utility.parseDate(fullDateString, true));
        fullDateString = "2015-01-14T14:53:32.800Z";
        assertEquals(fullDateString, outDateString);
    }

    private static String generateRandomContainerName() {
        String containerName = "container" + UUID.randomUUID().toString();
        return containerName.replace("-", "");
    }

    private void setDelay(final OperationContext ctx, final int timeInMs) {

        ctx.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                try {
                    Thread.sleep(timeInMs);
                }
                catch (InterruptedException e) {
                    // do nothing
                }
            }
        });
    }
}
