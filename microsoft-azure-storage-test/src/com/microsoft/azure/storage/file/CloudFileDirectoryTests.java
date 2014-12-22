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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import junit.framework.TestCase;

import com.microsoft.azure.storage.NameValidator;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.SendingRequestEvent;
import com.microsoft.azure.storage.StorageErrorCodeStrings;
import com.microsoft.azure.storage.StorageEvent;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.core.PathUtility;
import com.microsoft.azure.storage.core.SR;

/**
 * File Directory Tests
 */
public class CloudFileDirectoryTests extends TestCase {

    private CloudFileShare share;

    @Override
    public void setUp() throws URISyntaxException, StorageException {
        this.share = FileTestHelper.getRandomShareReference();
        this.share.create();
    }

    @Override
    public void tearDown() throws StorageException {
        this.share.deleteIfExists();
    }
    
    /**
     * Test directory name validation.
     */
    public void testCloudFileDirectoryNameValidation()
    {
        NameValidator.validateDirectoryName("alpha");
        NameValidator.validateDirectoryName("4lphanum3r1c");
        NameValidator.validateDirectoryName("middle-dash");
        NameValidator.validateDirectoryName("CAPS");
        NameValidator.validateDirectoryName("$root");
        NameValidator.validateDirectoryName("..");
        NameValidator.validateDirectoryName("CLOCK$");
        NameValidator.validateDirectoryName("endslash/");

        invalidDirectoryTestHelper(null, "No null.", "Invalid directory name. The name may not be null, empty, or whitespace only.");
        invalidDirectoryTestHelper("middle/slash", "Slashes only at the end.", "Invalid directory name. Check MSDN for more information about valid naming.");
        invalidDirectoryTestHelper("illegal\"char", "Illegal character.", "Invalid directory name. Check MSDN for more information about valid naming.");
        invalidDirectoryTestHelper("illegal:char?", "Illegal character.", "Invalid directory name. Check MSDN for more information about valid naming.");
        invalidDirectoryTestHelper("", "Between 1 and 255 characters.", "Invalid directory name. The name may not be null, empty, or whitespace only.");
        invalidDirectoryTestHelper(new String(new char[256]).replace("\0", "n"), "Between 1 and 255 characters.", "Invalid directory name length. The name must be between 1 and 255 characters long.");
    }

    private void invalidDirectoryTestHelper(String directoryName, String failMessage, String exceptionMessage)
    {
        try
        {
            NameValidator.validateDirectoryName(directoryName);
            fail(failMessage);
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }

    private boolean CloudFileDirectorySetup(CloudFileShare share) throws URISyntaxException, StorageException {
        try {
            CloudFileDirectory rootDirectory = share.getRootDirectoryReference();
            for (int i = 1; i < 3; i++) {
                CloudFileDirectory topDirectory = rootDirectory.getDirectoryReference("TopDir" + i);
                topDirectory.create();

                for (int j = 1; j < 3; j++) {
                    CloudFileDirectory midDirectory = topDirectory.getDirectoryReference("MidDir" + j);
                    midDirectory.create();

                    for (int k = 1; k < 3; k++) {
                        CloudFileDirectory endDirectory = midDirectory.getDirectoryReference("EndDir" + k);
                        endDirectory.create();

                        CloudFile file1 = endDirectory.getFileReference("EndFile" + k);
                        file1.create(0);
                    }
                }

                CloudFile file2 = topDirectory.getFileReference("File" + i);
                file2.create(0);
            }

            return true;
        }
        catch (StorageException e) {
            throw e;
        }
    }

    /**
     * Tests the CloudFileDirectory constructor.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryConstructor() throws URISyntaxException, StorageException {
        CloudFileShare share = FileTestHelper.getRandomShareReference();
        CloudFileDirectory directory = share.getRootDirectoryReference().getDirectoryReference("directory1");
        CloudFileDirectory directory2 = new CloudFileDirectory(directory.getStorageUri(), null);
        assertEquals(directory.getName(), directory2.getName());
        assertEquals(directory.getStorageUri(), directory2.getStorageUri());
        assertEquals(directory.getShare().getStorageUri(), directory2.getShare().getStorageUri());
        assertEquals(directory.getServiceClient().getStorageUri(), directory2.getServiceClient().getStorageUri());
    }

    /**
     * Test creation and deletion of a directory.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryCreateAndDelete() throws URISyntaxException, StorageException {
        CloudFileDirectory directory = this.share.getRootDirectoryReference().getDirectoryReference("directory1");
        directory.create();
        assertTrue(directory.exists());
        directory.delete();
        assertFalse(directory.exists());
    }

    /**
     * Tests createIfNotExists for a directory.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    public void testCloudFileDirectoryCreateIfNotExists() throws StorageException, URISyntaxException {
        CloudFileDirectory directory = this.share.getRootDirectoryReference().getDirectoryReference("directory1");
        assertTrue(directory.createIfNotExists());
        assertFalse(directory.createIfNotExists());
        directory.delete();
        assertTrue(directory.createIfNotExists());
    }

    /**
     * Tests deleteIfExists for a directory.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryDeleteIfExists() throws URISyntaxException, StorageException {
        CloudFileDirectory directory = this.share.getRootDirectoryReference().getDirectoryReference("directory1");
        assertFalse(directory.deleteIfExists());
        directory.create();
        assertTrue(directory.deleteIfExists());
        assertFalse(directory.deleteIfExists());
    }

    /**
     * Test listFilesAndDirectories for a directory.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    public void testCloudFileDirectoryListFilesAndDirectories() throws StorageException, URISyntaxException {
        if (CloudFileDirectorySetup(this.share)) {
            CloudFileDirectory topDir1 = this.share.getRootDirectoryReference().getDirectoryReference("TopDir1");
            Iterable<ListFileItem> list1 = topDir1.listFilesAndDirectories();
            assertTrue(list1.iterator().hasNext());
            ArrayList<ListFileItem> simpleList1 = new ArrayList<ListFileItem>();
            for (ListFileItem i : list1) {
                simpleList1.add(i);
            }
            ////Check if for 3 because if there were more than 3, the previous assert would have failed.
            ////So the only thing we need to make sure is that it is not less than 3. 
            assertTrue(simpleList1.size() == 3);

            ListFileItem item11 = simpleList1.get(0);
            assertEquals(item11.getUri().toString(), this.share.getUri() + "/TopDir1/File1");
            assertEquals("File1", ((CloudFile) item11).getName());

            ListFileItem item12 = simpleList1.get(1);
            assertEquals(item12.getUri().toString(), this.share.getUri() + "/TopDir1/MidDir1");
            assertEquals("MidDir1", ((CloudFileDirectory) item12).getName());

            ListFileItem item13 = simpleList1.get(2);
            assertEquals(item13.getUri().toString(), this.share.getUri() + "/TopDir1/MidDir2");
            assertEquals("MidDir2", ((CloudFileDirectory) item13).getName());
            CloudFileDirectory midDir2 = (CloudFileDirectory) item13;

            Iterable<ListFileItem> list2 = midDir2.listFilesAndDirectories();

            ArrayList<ListFileItem> simpleList2 = new ArrayList<ListFileItem>();
            for (ListFileItem i : list2) {
                simpleList2.add(i);
            }
            assertTrue(simpleList2.size() == 2);

            ListFileItem item21 = simpleList2.get(0);
            assertEquals(item21.getUri().toString(), this.share.getUri() + "/TopDir1/MidDir2/EndDir1");
            assertEquals("EndDir1", ((CloudFileDirectory) item21).getName());

            ListFileItem item22 = simpleList2.get(1);
            assertEquals(item22.getUri().toString(), this.share.getUri() + "/TopDir1/MidDir2/EndDir2");
            assertEquals("EndDir2", ((CloudFileDirectory) item22).getName());

            topDir1.downloadAttributes();
            assertNotNull(topDir1.getProperties().getEtag());
            assertNotNull(topDir1.getProperties().getLastModified());

            ResultSegment<ListFileItem> segment1 = topDir1.listFilesAndDirectoriesSegmented();
            assertFalse(segment1.getHasMoreResults());
        }
    }

    /**
     * Test listFilesAndDirectories for maxResults validation.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    public void CloudFileDirectoryListFilesAndDirectoriesMaxResultsValidation()
            throws StorageException, URISyntaxException {
        if (CloudFileDirectorySetup(this.share)) {
            CloudFileDirectory topDir =
                    this.share.getRootDirectoryReference().getDirectoryReference("TopDir1");
            
            // Validation should cause each of these to fail
            for (int i = 0; i >= -2; i--) {
                try {
                    topDir.listFilesAndDirectoriesSegmented(i, null, null, null);
                    fail();
                }
                catch (IllegalArgumentException e) {
                    assertTrue(String.format(SR.PARAMETER_SHOULD_BE_GREATER_OR_EQUAL, "maxResults", 1)
                            .equals(e.getMessage()));
                }
            }
            assertNotNull(topDir.listFilesAndDirectoriesSegmented());
        } else {
            fail();
        }
    }
    
    /**
     * Tests to make sure you can't delete a directory if it is nonempty.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryWithFilesDelete() throws URISyntaxException, StorageException {
        if (CloudFileDirectorySetup(this.share)) {
            CloudFileDirectory dir1 = this.share.getRootDirectoryReference().getDirectoryReference(
                    "TopDir1/MidDir1/EndDir1");
            CloudFile file1 = dir1.getFileReference("EndFile1");

            try {
                dir1.delete();
                fail("Shouldn't be able to delete directory when files still exist in it.");
            }
            catch (StorageException e) {
                assertEquals(e.getHttpStatusCode(), 409);
                assertEquals(e.getMessage(), "The specified directory is not empty.");
            }

            file1.delete();
            dir1.delete();
            assertFalse(file1.exists());
            assertFalse(dir1.exists());
        }
    }

    /*
    [TestMethod]
    [Description("CloudFileDirectory deleting a directory using conditional access")]
    [TestCategory(ComponentCategory.File)]
    [TestCategory(TestTypeCategory.UnitTest)]
    [TestCategory(SmokeTestCategory.NonSmoke)]
    [TestCategory(TenantTypeCategory.DevFabric), TestCategory(TenantTypeCategory.Cloud)]
    public void CloudFileDirectoryConditionalAccess()
    {
        CloudFileClient client = FileTestHelper.createCloudFileClient();
        String name = FileTestHelper.generateRandomShareName();
        CloudFileShare share = client.getShareReference(name);

        try
        {
            share.create();
            if (CloudFileDirectorySetup(share))
            {
                CloudFileDirectory dir1 = share.getRootDirectoryReference().getDirectoryReference("TopDir1/MidDir1/EndDir1");
                CloudFile file1 = dir1.getFileReference("EndFile1");
                file1.delete();
                dir1.FetchAttributes();
                String etag = dir1.Properties.ETag;

                TestHelper.ExpectedException(
                    () => dir1.delete(AccessCondition.GenerateIfNoneMatchCondition(etag), null),
                    "If none match on conditional test should throw",
                    HttpStatusCode.PreconditionFailed,
                    "ConditionNotMet");

                String invalidETag = "\"0x10101010\"";

                TestHelper.ExpectedException(
                    () => dir1.delete(AccessCondition.GenerateIfMatchCondition(invalidETag), null),
                    "If none match on conditional test should throw",
                    HttpStatusCode.PreconditionFailed,
                    "ConditionNotMet");

                dir1.delete(AccessCondition.GenerateIfMatchCondition(etag), null);

                // LastModifiedTime tests
                CloudFileDirectory dir2 = share.getRootDirectoryReference().getDirectoryReference("TopDir1/MidDir1/EndDir2");
                CloudFile file2 = dir2.getFileReference("EndFile2");
                file2.delete();
                dir2.FetchAttributes();
                DateTimeOffset currentModifiedTime = dir2.Properties.LastModified.Value;

                TestHelper.ExpectedException(
                    () => dir2.delete(AccessCondition.GenerateIfModifiedSinceCondition(currentModifiedTime), null),
                    "IfModifiedSince conditional on current modified time should throw",
                    HttpStatusCode.PreconditionFailed,
                    "ConditionNotMet");

                DateTimeOffset pastTime = currentModifiedTime.Subtract(TimeSpan.FromMinutes(5));
                TestHelper.ExpectedException(
                    () => dir2.delete(AccessCondition.GenerateIfNotModifiedSinceCondition(pastTime), null),
                    "IfNotModifiedSince conditional on past time should throw",
                    HttpStatusCode.PreconditionFailed,
                    "ConditionNotMet");

                DateTimeOffset ancientTime = currentModifiedTime.Subtract(TimeSpan.FromDays(5));
                TestHelper.ExpectedException(
                    () => dir2.delete(AccessCondition.GenerateIfNotModifiedSinceCondition(ancientTime), null),
                    "IfNotModifiedSince conditional on past time should throw",
                    HttpStatusCode.PreconditionFailed,
                    "ConditionNotMet");

                dir2.delete(AccessCondition.GenerateIfNotModifiedSinceCondition(currentModifiedTime), null);
            }
        }
        finally
        {
            share.delete();
        }
    }
     */

    /**
     * Ensures you cannot create a file without first creating the directory it is located within.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryFileCreateWithoutDirectory() throws URISyntaxException, StorageException {
        CloudFileDirectory dir = this.share.getRootDirectoryReference().getDirectoryReference("Dir1");
        CloudFile file = dir.getFileReference("file1");

        try {
            file.create(0);
            fail("File shouldn't be created in a directory that wasn't created.");
        }
        catch (StorageException e) {
            assertEquals(e.getErrorCode(), "ParentNotFound");
            assertEquals(e.getHttpStatusCode(), 404);
            assertEquals(e.getMessage(), "The specified parent path does not exist.");
        }

        // File creation directly in the share should pass.
        CloudFile file2 = this.share.getRootDirectoryReference().getFileReference("file2");
        file2.create(0);

        dir.create();
        file.create(0);
    }

    /**
     * Ensures we cannot create a directory without creating its parent directory first.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryCreateDirectoryWithoutParent() throws URISyntaxException, StorageException {
        CloudFileDirectory dir1 = this.share.getRootDirectoryReference().getDirectoryReference("Dir1");
        CloudFileDirectory dir2 = this.share.getRootDirectoryReference().getDirectoryReference("Dir1/Dir2");
        try {
            dir2.create();
            fail("Directory shouldn't be created when the parent directory wasn't created.");
        }
        catch (StorageException e) {
            assertEquals(e.getErrorCode(), "ParentNotFound");
            assertEquals(e.getHttpStatusCode(), 404);
            assertEquals(e.getMessage(), "The specified parent path does not exist.");
        }

        dir1.create();
        dir2.create();
    }

    /**
     * Tests directory hierarchy navigation.
     * 
     * @throws StorageException
     * @throws URISyntaxException
     */
    public void testCloudFileDirectoryGetParent() throws StorageException, URISyntaxException {
        CloudFile file = this.share.getRootDirectoryReference().getDirectoryReference("Dir1")
                .getFileReference("File1");
        assertEquals("File1", file.getName());

        // get the file's parent
        CloudFileDirectory parent = file.getParent();
        assertEquals(parent.getName(), "Dir1");

        // get share as parent
        CloudFileDirectory root = parent.getParent();
        assertEquals(root.getName(), "");

        // make sure the parent of the share dir is null
        CloudFileDirectory empty = root.getParent();
        assertNull(empty);

        // from share, get directory reference to share
        root = this.share.getRootDirectoryReference();
        assertEquals("", root.getName());
        assertEquals(this.share.getUri(), root.getUri());

        // make sure the parent of the share dir is null
        empty = root.getParent();
        assertNull(empty);
    }

    /**
     * Tests directory hierarchy navigation.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryGetSubdirectoryAndTraverseBackToParent() throws URISyntaxException, StorageException {
        CloudFileDirectory directory = this.share.getRootDirectoryReference().getDirectoryReference("TopDir1");
        CloudFileDirectory subDirectory = directory.getDirectoryReference("MidDir1");
        CloudFileDirectory parent = subDirectory.getParent();
        assertEquals(parent.getName(), directory.getName());
        assertEquals(parent.getUri(), directory.getUri());
    }

    /**
     * Tests directory hierarchy navigation.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryGetParentOnRoot() throws URISyntaxException, StorageException {
        CloudFileDirectory root = this.share.getRootDirectoryReference().getDirectoryReference("TopDir1");
        CloudFileDirectory parent = root.getParent();
        assertNotNull(parent);

        CloudFileDirectory empty = parent.getParent();
        assertNull(empty);
    }

    /**
     * Tests directory hierarchy navigation.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryHierarchicalTraversal() throws URISyntaxException, StorageException {
        ////Traverse hierarchically starting with length 1
        CloudFileDirectory directory1 = this.share.getRootDirectoryReference().getDirectoryReference("Dir1");
        CloudFileDirectory subdir1 = directory1.getDirectoryReference("Dir2");
        CloudFileDirectory parent1 = subdir1.getParent();
        assertEquals(parent1.getName(), directory1.getName());

        CloudFileDirectory subdir2 = subdir1.getDirectoryReference("Dir3");
        CloudFileDirectory parent2 = subdir2.getParent();
        assertEquals(parent2.getName(), subdir1.getName());

        CloudFileDirectory subdir3 = subdir2.getDirectoryReference("Dir4");
        CloudFileDirectory parent3 = subdir3.getParent();
        assertEquals(parent3.getName(), subdir2.getName());

        CloudFileDirectory subdir4 = subdir3.getDirectoryReference("Dir5");
        CloudFileDirectory parent4 = subdir4.getParent();
        assertEquals(parent4.getName(), subdir3.getName());
    }

    /**
     * Tests directory hierarchy navigation.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryFileParentValidate() throws URISyntaxException, StorageException {
        CloudFile file = this.share.getRootDirectoryReference().getDirectoryReference("TopDir1")
                .getDirectoryReference("MidDir1").getDirectoryReference("EndDir1").getFileReference("EndFile1");
        CloudFileDirectory directory = file.getParent();
        assertEquals(directory.getName(), "EndDir1");
        assertEquals(directory.getUri().toString(), this.share.getUri() + "/TopDir1/MidDir1/EndDir1");
    }

    /**
     * Tests directory creation with empty names.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryGetEmptySubDirectory() throws URISyntaxException, StorageException {
        CloudFileDirectory root = this.share.getRootDirectoryReference().getDirectoryReference("TopDir1");
        try {
            root.getDirectoryReference("");
            fail("Subdirectory references shouldn't work with empty strings.");
        }
        catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "The argument must not be null or an empty string. Argument name: itemName.");
        }
    }

    /**
     * Tests directory creation with absolute uris.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     */
    public void testCloudFileDirectoryAbsoluteUriAppended() throws URISyntaxException, StorageException {
        CloudFileDirectory dir = this.share.getRootDirectoryReference().getDirectoryReference(
                this.share.getUri().toString());
        assertEquals(PathUtility.appendPathToUri(this.share.getStorageUri(), this.share.getUri().toString())
                .getPrimaryUri(), dir.getUri());
        assertEquals(new URI(this.share.getUri() + "/" + this.share.getUri()), dir.getUri());
        dir = this.share.getRootDirectoryReference().getDirectoryReference(this.share.getUri() + "/TopDir1");
        assertEquals(
                PathUtility.appendPathToUri(this.share.getStorageUri(), this.share.getUri().toString() + "/TopDir1")
                        .getPrimaryUri(), dir.getUri());
    }

    /**
     * Test specific deleteIfExists case.
     * 
     * @throws URISyntaxException
     * @throws StorageException
     * @throws IOException
     */
    public void testCloudFileDirectoryDeleteIfExistsErrorCode() throws URISyntaxException, StorageException, IOException {
        final CloudFileDirectory directory = this.share.getRootDirectoryReference().getDirectoryReference(
                "directory");

        try {
            directory.delete();
            fail("Directory should not already exist.");
        }
        catch (StorageException e) {
            assertEquals(StorageErrorCodeStrings.RESOURCE_NOT_FOUND, e.getErrorCode());
        }

        assertFalse(directory.exists());
        assertFalse(directory.deleteIfExists());

        directory.create();
        assertTrue(directory.exists());

        assertTrue(directory.deleteIfExists());
        assertFalse(directory.deleteIfExists());

        // check if second condition works in delete if exists
        OperationContext ctx = new OperationContext();
        ctx.getSendingRequestEventHandler().addListener(new StorageEvent<SendingRequestEvent>() {

            @Override
            public void eventOccurred(SendingRequestEvent eventArg) {
                if (((HttpURLConnection) eventArg.getConnectionObject()).getRequestMethod().equals("DELETE")) {
                    try {
                        directory.delete();
                        assertFalse(directory.exists());
                    }
                    catch (StorageException e) {
                        fail("Delete should succeed.");
                    }
                }
            }
        });

        directory.create();
        assertFalse(directory.deleteIfExists(null, null, ctx));
    }
}
