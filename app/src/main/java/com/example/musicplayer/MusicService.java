package com.example.musicplayer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import com.example.musicplayer.model.Song;

import java.util.ArrayList;

public class MusicService extends Service {
    public static final String TAG = MusicService.class.getSimpleName();
    private ArrayList<Song> listSong;

    private MediaPlayer player;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        readMediaFile();
        player = MediaPlayer.create(this,R.raw.onrainyday_beats);
        player.setLooping(true);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
    }
    private void readMediaFile() {
        ContentResolver resolver = getContentResolver();
        Uri musicContent = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(musicContent, null, null, null, null);
        Log.d(TAG, "readMediaFile: " + cursor.getCount());


        if (cursor != null) {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {

                String path = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Audio.Media.TITLE));


                if (path != null && path.endsWith(".mp3")) {
                    Song song = new Song(title, path);
                    Log.d(TAG, "readMediaFile: add song");
                    listSong.add(song);
                }
            }
        }
//        String test = "";
//        for (int i = 0; i < 10; i++) {
//            test = test + " " + listSong.get(i).getName();
//        }
        Log.d(TAG, "readMediaFile: " + listSong.size());

    }
}
