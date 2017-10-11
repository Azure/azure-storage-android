# Microsoft Azure Storage SDK for Android

This project provides a client library for Android that makes it easy to consume Microsoft Azure Storage services. For documentation please see the [AndroidDocs](http://azure.github.io/azure-storage-android/).

> If you are looking for the Azure Storage Java SDK, please visit [https://github.com/Azure/azure-storage-java](https://github.com/Azure/azure-storage-java).

# Features
  * Blob
      * Create/Read/Update/Delete containers
      * Create/Read/Update/Delete blobs
      * Advanced Blob Operations
  * Queue
      * Create/Delete Queues
      * Insert/Peek Queue Messages
      * Advanced Queue Operations
  * Table
      * Create/Read/Update/Delete tables
      * Create/Read/Update/Delete entities
      * Batch operations
      * Advanced Table Operations

# Getting Started

## Download
### Option 1: Source Zip

To download a copy of the source code, click "Download ZIP" on the right side of the page or click [here](https://github.com/Azure/azure-storage-android/archive/master.zip). Unzip and navigate to the microsoft-azure-storage folder.

### Option 2: Source Via Git

To get the source code of the SDK via git just type:

    git clone git://github.com/Azure/azure-storage-android.git
    cd ./azure-storage-android/microsoft-azure-storage

### Option 3: aar via Gradle

To get the binaries of this library as distributed by Microsoft, ready for use within your project, you can use Gradle.

First, add mavenCentral to your repositories by adding the following to your gradle build file:

    repositories {
        mavenCentral()
    }

Then, add a dependency by adding the following to your gradle build file:

    dependencies {
        compile 'com.microsoft.azure.android:azure-storage-android:2.0.0@aar'
    }

### Option 4: aar via Maven

To get the binaries of this library as distributed by Microsoft, ready for use within your project, you can use Maven.

```xml
<dependency>
	<groupId>com.microsoft.azure.android</groupId>
	<artifactId>azure-storage-android</artifactId>
	<version>2.0.0</version>
	<type>aar</type>
</dependency>
```

## Minimum Requirements and Setup
* [Jackson-Core](https://github.com/FasterXML/jackson-core) is used for JSON parsing. 
* Android 4.0/15+
* (Optional) Gradle or Maven

This library is currently tested to work on Android versions 4.0+. Compatibility with older versions is not guaranteed.

## Usage

To use this SDK to call Microsoft Azure storage services, you need to first [create an account](https://account.windowsazure.com/signup). 

Samples are provided in the microsoft-azure-storage-samples folder. The unit tests in microsoft-azure-storage-test can also be helpful.

Make sure the storage client library is added as a project dependency. From Android Studio, go to File -> Project Structure. Click on the module (either sample or test). If 'microsoft-azure-storage' is not listed as a dependy, click the '+' sign. Then click module dependency and select 'microsoft-azure-storage'.

If using Maven or Gradle, Jackson-Core should be automatically added to the build path. Otherwise, please download the jar and add it to your build path. Also, please make sure that the jar will be added to your project's apk. To do this in Android Studio, go to File -> Project Structure -> Modules. Click the Dependencies tab. Click the '+' sign and click 'File Dependency'. Navigate the .jar.

## Code Samples

Runnable samples for blob, queue, and table may be found in the microsoft-azure-storage-samples directory. To run these samples, specify a connection string in the MainActivity class and add a dependency on the Android client library. For additional information on using the Android client library, the Java [general documentation](http://azure.microsoft.com/en-us/develop/java/) and Java How To guides for [blobs](http://azure.microsoft.com/en-us/documentation/articles/storage-java-how-to-use-blob-storage/), [queues](http://azure.microsoft.com/en-us/documentation/articles/storage-java-how-to-use-queue-storage/), [tables](http://azure.microsoft.com/en-us/documentation/articles/storage-java-how-to-use-table-storage/) may be helpful.

# Need Help?

Be sure to check out the Azure [Developer Forums on MSDN](http://social.msdn.microsoft.com/Forums/windowsazure/en-US/home?forum=windowsazuredata) or the [Developer Forums on Stack Overflow](http://stackoverflow.com/questions/tagged/azure+windows-azure-storage) if you have trouble with the provided code.

# Contribute Code or Provide Feedback

If you would like to become an active contributor to this project please follow the instructions provided in [Azure Projects Contribution Guidelines](http://azure.github.io/guidelines/).

If you encounter any bugs with the library please file an issue in the [Issues](https://github.com/Azure/azure-storage-android/issues) section of the project.

# Learn More
* [Azure Storage Service](http://azure.microsoft.com/en-us/documentation/services/storage/)
* [Azure Storage Team Blog](http://blogs.msdn.com/b/windowsazurestorage/)
* [AndroidDocs](http://azure.github.io/azure-storage-android/)
