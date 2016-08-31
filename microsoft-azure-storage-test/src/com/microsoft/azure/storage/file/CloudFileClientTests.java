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
package com.microsoft.azure.storage.file;

import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.TestRunners.CloudTests;
import com.microsoft.azure.storage.TestRunners.DevFabricTests;
import com.microsoft.azure.storage.TestRunners.DevStoreTests;
import com.microsoft.azure.storage.core.SR;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * File Client Tests
 */
public class CloudFileClientTests {
    /**
     * Tests doing a listShares.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    @Test
    @Category({ DevFabricTests.class, DevStoreTests.class, CloudTests.class })
    public void testListSharesTest() throws StorageException, URISyntaxException {
        CloudFileClient fileClient = FileTestHelper.createCloudFileClient();
        ArrayList<String> shareList = new ArrayList<String>();
        String prefix = UUID.randomUUID().toString();
        try {
            for (int i = 0; i < 30; i++) {
                shareList.add(prefix + i);
                fileClient.getShareReference(prefix + i).create();
            }

            int count = 0;
            for (final CloudFileShare share : fileClient.listShares(prefix)) {
                assertEquals(CloudFileShare.class, share.getClass());
                count++;
            }
            assertEquals(30, count);

            ResultContinuation token = null;
            do {

                ResultSegment<CloudFileShare> segment = fileClient.listSharesSegmented(prefix, ShareListingDetails.ALL,
                        15, token, null, null);

                for (final CloudFileShare share : segment.getResults()) {
                    share.downloadAttributes();
                    assertEquals(CloudFileShare.class, share.getClass());
                    shareList.remove(share.getName());
                    share.delete();
                }

                token = segment.getContinuationToken();
            } while (token != null);

            assertEquals(0, shareList.size());
        }
        finally {
            for (final String shareName : shareList) {
                fileClient.getShareReference(shareName).deleteIfExists();
            }
        }
    }
    
    /**
     * Tests doing a listShares to ensure maxResults validation is working.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    @Test
    @Category({ DevFabricTests.class, DevStoreTests.class, CloudTests.class })
    public void testListSharesMaxResultsValidationTest() throws StorageException, URISyntaxException {
        CloudFileClient fileClient = FileTestHelper.createCloudFileClient();
        String prefix = UUID.randomUUID().toString();
            
        // Validation should cause each of these to fail
        for (int i = 0; i >= -2; i--) {
            try{ 
                fileClient.listSharesSegmented(
                        prefix, ShareListingDetails.ALL, i, null, null, null);
                fail();
            }
            catch (IllegalArgumentException e) {
                assertTrue(String.format(SR.PARAMETER_SHOULD_BE_GREATER_OR_EQUAL, "maxResults", 1)
                        .equals(e.getMessage()));
            }
        }
        assertNotNull(fileClient.listSharesSegmented("thereshouldntbeanyshareswiththisprefix"));
    }
}