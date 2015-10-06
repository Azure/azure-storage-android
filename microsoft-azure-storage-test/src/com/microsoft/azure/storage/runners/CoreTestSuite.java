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
package com.microsoft.azure.storage.runners;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.microsoft.azure.storage.AccountSasTests;
import com.microsoft.azure.storage.EventFiringTests;
import com.microsoft.azure.storage.GenericTests;
import com.microsoft.azure.storage.MaximumExecutionTimeTests;
import com.microsoft.azure.storage.SecondaryTests;
import com.microsoft.azure.storage.ServicePropertiesTests;
import com.microsoft.azure.storage.StorageAccountTests;
import com.microsoft.azure.storage.StorageUriTests;

public class CoreTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("CoreTestSuite");
        suite.addTestSuite(AccountSasTests.class);
        suite.addTestSuite(EventFiringTests.class);
        suite.addTestSuite(GenericTests.class);
        suite.addTestSuite(MaximumExecutionTimeTests.class);
        suite.addTestSuite(SecondaryTests.class);
        suite.addTestSuite(ServicePropertiesTests.class);
        suite.addTestSuite(StorageAccountTests.class);
        suite.addTestSuite(StorageUriTests.class);
        return suite;
    }
}
