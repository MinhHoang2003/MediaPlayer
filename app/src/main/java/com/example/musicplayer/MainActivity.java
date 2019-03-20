package com.example.musicplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private TextView mTextViewProcess, mTextViewDuration;

    private List<Song> listSong = new ArrayList<>();

    private AppCompatSeekBar seekBar;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewProcess = findViewById(R.id.txt_time);
        mTextViewDuration = findViewById(R.id.txt_duration);

        seekBar = findViewById(R.id.seekBar);
        permissionRequest();

        handler = new Handler();

//        final MediaPlayer mediaPlayer;
//        readMediaFile();
//        if (listSong.size() > 0) {
//            Log.d(TAG, "onCreate: play in list");
//            Uri uri = Uri.parse(listSong.get(0).getPath());
//            mediaPlayer = MediaPlayer.create(this, uri);
//        } else mediaPlayer = MediaPlayer.create(this, R.raw.onrainyday_beats);
//
//
//        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                seekBar.setMax(mp.getDuration());
//                mTextViewDuration.setText(formatTime(mp.getDuration()));
//            }
//        });
//        mediaPlayer.setLooping(true);
//       // mediaPlayer.setVolume(0, 0);
//        mediaPlayer.start();
//
//
//        MainActivity.this.runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//
//                if (mediaPlayer != null) {
//
//                    int mCurrentPosition = mediaPlayer.getCurrentPosition();
//                    seekBar.setProgress(mCurrentPosition);
//                    mTextViewProcess.setText(formatTime(mCurrentPosition));
//
//                }
//                handler.postDelayed(this, 100);
//            }
//        });
//
//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    mediaPlayer.seekTo(progress);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });

        Intent musicIntent = new Intent(this,MusicService.class);
        startService(musicIntent);
    }

    private String formatTime(int millis) {

        String time = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return time;
    }



    private void permissionRequest() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                Log.d(TAG, "permissionRequest: need reques");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            Log.d(TAG, "permissionRequest: had request");
        }
    }

}
