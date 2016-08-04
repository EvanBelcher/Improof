package com.example.evbel.improof;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String ROOT = Environment.getExternalStorageDirectory().toString() + "/Improof/";

    private static MediaRecorder mRecorder = null;
    private static String mFileName = null;

    private static int minutes = 0;
    private static int seconds = 0;
    private static TextView textView;
    private static ListView listView;
    private Activity mActivity = this;
    RecordingListAdapter rla;
    SelectProjectAdapter spa;
    private Runnable updater = new Runnable() {
        public void run() {
            seconds++;
            if (seconds == 60) {
                seconds = 0;
                minutes++;
            }
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    DecimalFormat df = new DecimalFormat("00");
                    String s = minutes + ":" + df.format(seconds);

                    textView.setText(s);
                }
            });
        }
    };
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getPermissions();

        mFileName = ROOT;
        File rootDir = new File(mFileName);
        rootDir.mkdirs();
        File nomedia = new File(mFileName+".nomedia");
        try {
            nomedia.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ToggleButton toggle = (ToggleButton) findViewById(R.id.recordButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    //Start recording
                    Log.d("app", "Start recording");
                    startRecording();
                    service = Executors.newSingleThreadScheduledExecutor();
                    service.scheduleWithFixedDelay(updater, 1, 1, TimeUnit.SECONDS);
                    textView.setText("0:00");
                } else {
                    //Stop recording
                    Log.d("app", "Stop recording");
                    stopRecording();
                    service.shutdownNow();
                    rla.refreshList();
                }
            }
        });

        textView = (TextView) findViewById(R.id.recordTimer);

        Button button = (Button) findViewById(R.id.newProject);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setTitle("What is your new project called?");
                alert.setMessage("Enter your new project name.");

                // Set an EditText view to get user input
                final EditText input = new EditText(mActivity);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String project = input.getText().toString();
                        if(!project.isEmpty()) {
                            createProject(project);
                            rla.refreshList();
                            spa.refreshList();
                        }
                    }
                });

                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                alert.show();
            }
        });

        listView = (ListView) findViewById(R.id.projectList);
        rla = new RecordingListAdapter(this,listView);

        Spinner spinner = (Spinner) findViewById(R.id.selectProject);
        spa = new SelectProjectAdapter(this,spinner);
    }

    private void createProject(String project) {
        String directory = ROOT;
        directory += project.trim();
        directory += "/";
        File dir = new File(directory);
        dir.mkdirs();
    }

    public void startRecording() {
        changeFileName();
        minutes = 0;
        seconds = 0;

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("app", "prepare() failed");
        }

        mRecorder.start();
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    public void changeFileName() {
        DateFormat dateFormat = new SimpleDateFormat().getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.LONG);
        TimeZone tz = TimeZone.getDefault();
        Calendar c = Calendar.getInstance(tz);

        mFileName = Environment.getExternalStorageDirectory().toString();
        mFileName += "/Improof/";
        if (!spa.getSelectedProject().trim().isEmpty()) {
            mFileName += spa.getSelectedProject().trim();
            mFileName += "/";
        }

        mFileName += dateFormat.format(c.getTime()).replaceAll(":", "-");
        mFileName += ".3gp";
        File file = new File(mFileName);

        Log.d("app", mFileName);

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.d("app", "yes");
        }
        file.getParentFile().mkdirs();

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(updater, 1, 1, TimeUnit.SECONDS);

        int i = 1;
        while(file.exists()){
            if(i == 1)
                mFileName = mFileName.replaceAll(".3gp"," (" + i++ + ").3gp");
            else
                mFileName = mFileName.replaceAll("\\(" + (i - 1) + "\\)", "(" + i + ")");
            Log.d("app",mFileName);
            file = new File(mFileName);
        }

    }

    private static String TAG = "PermissionDemo";
    private static final int IMPROOF_REQUESTS = 333;

    private void getPermissions(){
        getWritePermission();
        getReadPermission();
        getRecordPermission();
    }

    private void getWritePermission() {
        //ask for the permission in android M
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to write to the SD Card is required for this app to save your recordings.")
                        .setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "Clicked");
                        makeWriteRequest();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                makeWriteRequest();
            }
        }


    }

    private void getReadPermission(){
        //ask for the permission in android M
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to read to the SD Card is required for this app to play your recordings.")
                        .setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "Clicked");
                        makeWriteRequest();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                makeReadRequest();
            }
        }
    }

    private void getRecordPermission(){
        //ask for the permission in android M
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to record audio is required for this app to record your takes.")
                        .setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "Clicked");
                        makeWriteRequest();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                makeRecordRequest();
            }
        }
    }

    protected void makeWriteRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                IMPROOF_REQUESTS);
    }

    protected void makeReadRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                IMPROOF_REQUESTS);
    }

    protected void makeRecordRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                IMPROOF_REQUESTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case IMPROOF_REQUESTS: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");

                } else {

                    Log.i(TAG, "Permission has been granted by user");

                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
