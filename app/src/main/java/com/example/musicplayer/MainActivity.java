package com.example.musicplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicplayer.model.PlayList;
import com.example.musicplayer.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String LIST_SONG = "com.example.musicplayer.listsong";
    public static final int REQUEST_CODE = 1;
    public static final int UPDATE_TIME = 100;
    private TextView mTextViewProcess, mTextViewDuration;
    private ListView mListView;

    private PlayList mPlayList;
    private ArrayList<String> mListSongName = new ArrayList<>();

    private AppCompatSeekBar mSeekBar;
    private ImageButton mButtonPlay;
    private LocalBroadcastManager localBroadcastManager;

    private MusicService mMusicService;
    private boolean isBound = false;
    private Handler mHandler;

    private Runnable mUpdateCurrentTime = new Runnable() {
        @Override
        public void run() {
            if (isBound) {
                int songDuration = mMusicService.getSongDuration();
                int currentPosition = mMusicService.getCurrentSongPosition();
                mSeekBar.setMax(songDuration);
                mSeekBar.setProgress(currentPosition);
                mTextViewProcess.setText(formatTime(currentPosition));
                mTextViewDuration.setText(formatTime(songDuration));
                Log.d("Test", "run: ");
            }
            mHandler.postDelayed(mUpdateCurrentTime, UPDATE_TIME);
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.TimeCurrentBinder binder = (MusicService.TimeCurrentBinder) service;
            mMusicService = binder.getMusicService();
            isBound = true;
            mHandler.postDelayed(mUpdateCurrentTime,UPDATE_TIME);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MusicService.PREPARED)){
                int time = intent.getIntExtra(MusicService.DURATION_SONG,0);
                mSeekBar.setMax(time);
                mTextViewDuration.setText(formatTime(time));
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionRequest();
        setListSongName();
        getView();
        mHandler = new Handler();
        //send playlist to service
        if(mPlayList !=null){
            Intent intent = new Intent(this,MusicService.class);
            intent.setAction(MusicService.SUPPLY);
            intent.putExtra(LIST_SONG, mPlayList);
            startService(intent);
        }

        //bind data to list view
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,mListSongName);
        mListView.setAdapter(arrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent playOnClick = new Intent(MainActivity.this,MusicService.class);
                playOnClick.setAction(MusicService.PLAY);
                playOnClick.putExtra(MusicService.PLAY_ONCLICK,position);
                startService(playOnClick);
            }
        });

        // set seek to in music player
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    Intent seekTo = new Intent(MusicService.SEEK_TO);
                    seekTo.putExtra(MusicService.SEEK_TO,progress);
                    sendBroadcast(seekTo);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


    }
    private void getView(){
        mButtonPlay = findViewById(R.id.btn_play);
        mTextViewProcess = findViewById(R.id.txt_time);
        mTextViewDuration = findViewById(R.id.txt_duration);
        mSeekBar = findViewById(R.id.seekBar);
        mListView = findViewById(R.id.ListView);
    }

    private String formatTime(int millis) {
       String time = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return time;
    }

    @Override
    protected void onStart() {
        super.onStart();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PREPARED);
        localBroadcastManager.registerReceiver(mBroadcastReceiver,filter);

        if(!isBound){
            Intent bindServiceIntent = new Intent(MainActivity.this,MusicService.class);
            bindService(bindServiceIntent,mConnection,Context.BIND_AUTO_CREATE);
        }
    }

    private void permissionRequest() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        } else {
            mPlayList = readMediaFile();
        }
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager mn = LocalBroadcastManager.getInstance(this);
        mn.unregisterReceiver(mBroadcastReceiver);
        unbindService(mConnection);
        super.onStop();
    }

    private PlayList readMediaFile(){
        PlayList playList;
        ArrayList<Song> listSong = new ArrayList<Song>();
        ArrayList<File> listFile = findSong(Environment.getExternalStorageDirectory());
        for(File file:listFile){
            Song song  = new Song(file.getName(),file.getPath());
            listSong.add(song);
        }
        playList = new PlayList(listSong);
        return playList;

    }
    public ArrayList<File> findSong(File root){
        ArrayList<File> list = new ArrayList<File>();
        for(File file:root.listFiles()){
            if(file.isDirectory()&&!file.isHidden()){
                list.addAll(findSong(file));
            }else {
                if(file.getName().endsWith(".mp3")||file.getName().endsWith(".MP3")){
                    list.add(file);
                }
            }
        }
        return list;
    }

    public void setListSongName(){
        if(mPlayList!=null){
            for(Song song:mPlayList.getListSong()){
                mListSongName.add(song.getName());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE){
            mPlayList = readMediaFile();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_play:
                if(!MusicService.sMusicPlayerState.equals(MusicService.MusicPlayerState.Playing)){
                    Intent play = new Intent(MainActivity.this,MusicService.class);
                    play.setAction(MusicService.PLAY);
                    startService(play);
                }
                break;
            case R.id.btn_pause:
                Intent pause = new Intent(MusicService.PAUSE);
                sendBroadcast(pause);
                break;
            case R.id.btn_previous:
                Intent previous = new Intent(MusicService.PREVIOUS);
                sendBroadcast(previous);
                break;
            case R.id.btn_nextTo:
                Intent next = new Intent(MusicService.NEXT);
                sendBroadcast(next);
                break;

        }
    }
}
