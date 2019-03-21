package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.musicplayer.model.Song;

import java.util.ArrayList;

public class MusicService extends Service {
    public static final String TAG = MusicService.class.getSimpleName();
    public static final String CHANEL_ID = "background_music";
    private ArrayList<Song> listSong;

    private MediaPlayer player;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
      //  readMediaFile();

        player = MediaPlayer.create(this,R.raw.onrainyday_beats);
        player.setLooping(true);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        startForeground(2,createNotification());
//        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        manager.notify(1,createNotification());
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

    }

    private Notification createNotification(){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){

            NotificationChannel channel = new NotificationChannel(CHANEL_ID,
                    "Music Control",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        RemoteViews customNoti = new RemoteViews(getPackageName(),R.layout.custom_notification);

        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANEL_ID)
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(customNoti)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        return builder.build();
    }

}
