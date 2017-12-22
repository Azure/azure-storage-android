package com.microsoft.azure.storage.samples;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.microsoft.azure.storage.samples.blob.BlobGettingStartedTask;
import com.microsoft.azure.storage.samples.queue.QueueGettingStartedTask;
import com.microsoft.azure.storage.samples.table.TableGettingStartedTask;
import com.microsoft.azure.storage.samples.table.TablePayloadFormatTask;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends Activity {

    /**
     * MODIFY THIS!
     * 
     * Stores the storage connection string.
     * Only use Shared Key authentication (Account Key) for testing purposes! 
     * Your account name and account key, which give full read/write access to the associated Storage account, 
     * will be distributed to every person that downloads your app. 
     * This is not a good practice as you risk having your key compromised by untrusted clients. 
     * Please consult following documents to understand and use Shared Access Signatures instead. 
     * https://docs.microsoft.com/en-us/rest/api/storageservices/delegating-access-with-a-shared-access-signature
     * https://docs.microsoft.com/en-us/azure/storage/common/storage-dotnet-shared-access-signature-part-1
     */
    public static final String storageConnectionString = "DefaultEndpointsProtocol=https;"
            + "AccountName=[MY_ACCOUNT_NAME];"
            + "AccountKey=[MY_ACCOUNT_KEY]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Runs the blob getting started sample.
     */
    public void runBlobGettingStartedSample(View view) {
        new BlobGettingStartedTask(this, (TextView) findViewById(R.id.textView))
                .execute();
    }

    /**
     * Runs the queue getting started sample.
     */
    public void runQueueGettingStartedSample(View view) {
        new QueueGettingStartedTask(this,
                (TextView) findViewById(R.id.textView)).execute();
    }

    /**
     * Runs the table getting started sample.
     */
    public void runTableGettingStartedSample(View view) {
        new TableGettingStartedTask(this,
                (TextView) findViewById(R.id.textView)).execute();
    }

    /**
     * Runs the table payload format sample.
     */
    public void runTablePayloadFormatSample(View view) {
        new TablePayloadFormatTask(this, (TextView) findViewById(R.id.textView))
                .execute();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            return rootView;
        }
    }

    /**
     * Prints the specified text value to the view and to LogCat.
     * 
     * @param view
     *            The view to print to.
     * @param value
     *            The value to print.
     */
    public void outputText(final TextView view, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.append(value + "\n");
                System.out.println(view);
            }
        });
    }

    /**
     * Clears the text from the specified view.
     * 
     * @param view
     *            The view to clear.
     */
    public void clearText(final TextView view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText("");
            }
        });
    }

    /**
     * Prints out the exception information .
     */
    public void printException(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        outputText(
                (TextView) findViewById(R.id.textView),
                String.format(
                        "Got an exception from running samples. Exception details:\n%s\n",
                        stringWriter.toString()));
    }

    /**
     * Prints out the sample start information .
     */
    public void printSampleStartInfo(String sampleName) {
        TextView view = (TextView) findViewById(R.id.textView);
        clearText(view);
        outputText(view, String.format(
                "The Azure storage client library sample %s is starting...",
                sampleName));
    }

    /**
     * Prints out the sample complete information .
     */
    public void printSampleCompleteInfo(String sampleName) {
        outputText((TextView) findViewById(R.id.textView), String.format(
                "The Azure storage client library sample %s completed.\n",
                sampleName));
    }

}
