package com.example.evbel.improof;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DetailActivity extends AppCompatActivity{

    private String fileName;
    private File recordingFile;
    private LinkedList<File> projectFiles;

    private ViewFlipper viewFlipper;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private SeekBar seekBar;
    private TextView recordingName;
    private EditText notes;
    private Spinner changeProjectSpinner;
    private ImageButton deleteButton;

    private ChangeProjectAdapter cpa;

    private MediaPlayer mPlayer;
    private boolean paused = false;
    private Activity activity = this;

    private Runnable updater = new Runnable() {
        public void run() {
            double duration = 1;
            try{
                duration = mPlayer.getDuration();
            }catch (Exception e){
                e.printStackTrace();
            }
            double pct = mPlayer.getCurrentPosition()/duration;
            final int position = (int)(pct * 1000);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    seekBar.setProgress(position);
                }
            });
        }
    };
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        fileName = intent.getStringExtra(RecordingListAdapter.EXTRA_ITEM);
        this.setTitle(fileName);
        recordingFile = getRecordingFile();

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        playButton = (ImageButton) findViewById(R.id.playButton);
        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        recordingName = (TextView) findViewById(R.id.recordingName);
        notes = (EditText) findViewById(R.id.notes);
        changeProjectSpinner = (Spinner) findViewById(R.id.changeProjectSpinner);
        deleteButton = (ImageButton) findViewById(R.id.deleteButton);

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(recordingFile.getAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                play();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pause();
            }
        });

        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)
                    seek(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        recordingName.setText(recordingFile.getName());

        loadNotes();

        cpa = new ChangeProjectAdapter(this,changeProjectSpinner);

        //------save and load notes
        //------close stuff
        //handle sliding
        //------handle deleting
        //handle changing project

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        });

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                seekBar.setProgress(0);
            }
        });

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(updater, 1, 1, TimeUnit.SECONDS);
    }

    private void delete() {
        closeStuff();
        recordingFile.delete();
        String filepath = recordingFile.getAbsolutePath();
        filepath = filepath.replace("\\.3gp",".txt");
        File notesFile = new File(filepath);
        if(notesFile.exists() && notesFile.isFile())
            notesFile.delete();

        if(projectFiles != null){
            recreate();
        }else{
            finish();
        }
    }


    private void play() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(recordingFile.getAbsolutePath().toString());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pause(){
        try {
            if (paused)
                mPlayer.start();
            else
                mPlayer.pause();
            paused = !paused;
        }catch(IllegalStateException e){
            e.printStackTrace();
        }
    }

    private void seek(int pos){
        double pctPos = pos/((double) 1000);
        if(mPlayer.isPlaying()){
            int position = (int) (pctPos * mPlayer.getDuration());
            mPlayer.seekTo(position);
        }else{
            play();
            pause();
            int position = (int) (pctPos * mPlayer.getDuration());
            mPlayer.seekTo(position);
        }
    }

    private File getRecordingFile() {
        if(fileName.contains("PROJECT: ")){
            String str = fileName;
            str = str.replace("PROJECT: ", "");
            File f = new File(MainActivity.ROOT + str + "/");
            if(f.listFiles().length > 0){
                TreeMap<Long,File> filesInDirectory = new TreeMap<Long,File>();
                long currentTime = System.currentTimeMillis();
                for(File file : f.listFiles()){
                    filesInDirectory.put(currentTime - file.lastModified(),file);
            }
                projectFiles = new LinkedList<File>();
                projectFiles.addAll(filesInDirectory.values());
                return projectFiles.getFirst();
            }else{
                projectFiles = null;
                return null;
            }
        }else{
            projectFiles = null;
            return new File(MainActivity.ROOT + fileName);
        }
    }

    @Override
    public void onBackPressed(){
        saveNotes();
        closeStuff();
        super.onBackPressed();
    }

    private void saveNotes(){
        String filepath = recordingFile.getAbsolutePath();
        filepath = filepath.replace("\\.3gp",".txt");
        File notesFile = new File(filepath);
        FileOutputStream outputStream;
        try{
            outputStream = new FileOutputStream(notesFile);
            String text = notes.getText().toString();
            BufferedReader reader = new BufferedReader(new StringReader(text));
            String line = null;
            while((line = reader.readLine()) != null){
                outputStream.write(line.getBytes());
                outputStream.write("\n".getBytes());
            }
            outputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadNotes(){
        String filepath = recordingFile.getAbsolutePath();
        filepath = filepath.replace("\\.3gp",".txt");
        File notesFile = new File(filepath);
        if(notesFile.exists() && notesFile.isFile()){
            FileInputStream inputstream;
            try{
                inputstream = new FileInputStream(notesFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream));
                String line = null;
                String text = "";
                while((line = reader.readLine()) != null){
                    text += line += "\n";
                }
                notes.setText(text);
            }catch(Exception e){

            }
        }

    }

    private void closeStuff(){
        if(mPlayer.isPlaying())
            mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        service.shutdownNow();
    }



}
