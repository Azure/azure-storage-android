If you intend to contribute to the project, please make sure you've followed the instructions provided in the [Azure Projects Contribution Guidelines](http://azure.github.io/guidelines/).
## Project Setup
The Azure Storage development team uses Android Studio so instructions will be tailored to that preference. However, any preferred IDE or other toolset should be usable.

### Install
* Java SE 6+
* [Android](https://developer.android.com/studio/index.html)
* [Maven](https://maven.apache.org/install.html)
* [Jackson-Core](https://github.com/FasterXML/jackson-core) is used for JSON parsing. 
* Clone the source code from GitHub

### Open Solution
Open the project from Android Studio using File->Open and navigating to the azure-storage-android folder.

## Tests

### Configuration
The only step to configure testing is to populate the accountName, accountKey, and service endpoints in TestHelper.java.

### Running
To actually run tests, right click on the test class in the Package Explorer or the individual test in the Outline and select Run -> Run. All tests or tests grouped by service can be run using the test runners in the com.microsoft.azure.storage package TestRunners file. Running all tests from the top of the package explorer will result in each test being run multiple times as the package explorer will also run every test runner.
If a test run failed to test runner not found, go to Run -> Edig Configurations... -> Specifc instrumentation runner (optional) -> ... ->AndroidJUnitRunner (android.support.test.runner). Then rerun the test.

### Testing Features
As you develop a feature, you'll need to write tests to ensure quality. You should also run existing tests related to your change to address any unexpected breaks.

## Pull Requests

### Guidelines
The following are the minimum requirements for any pull request that must be met before contributions can be accepted.
* Make sure you've signed the CLA before you start working on any change.
* Discuss any proposed contribution with the team via a GitHub issue **before** starting development.
* Code must be professional quality
	* No style issues
	* You should strive to mimic the style with which we have written the library
	* Clean, well-commented, well-designed code
	* Try to limit the number of commits for a feature to 1-2. If you end up having too many we may ask you to squash your changes into fewer commits.
* [ChangeLog.md](ChangeLog.md) needs to be updated describing the new change
* Thoroughly test your feature

### Branching Policy
Changes should be based on the **dev** branch, not master as master is considered publicly released code. If after discussion with us breaking changes are considered for the library, we will create a **dev_breaking** branch based on dev which can be used to store these changes until the next breaking release. Each breaking change should be recorded in [BreakingChanges.md](BreakingChanges.md). 

### Adding Features for Java 6+
We strive to release each new feature in a backward compatible manner. Therefore, we ask that all contributions be written to work in Java 6, 7 and 8.

### Review Process
We expect all guidelines to be met before accepting a pull request. As such, we will work with you to address issues we find by leaving comments in your code. Please understand that it may take a few iterations before the code is accepted as we maintain high standards on code quality. Once we feel comfortable with a contribution, we will validate the change and accept the pull request.


Thank you for any contributions! Please let the team know if you have any questions or concerns about our contribution policy.