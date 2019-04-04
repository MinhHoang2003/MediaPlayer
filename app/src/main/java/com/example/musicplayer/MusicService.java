package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.musicplayer.model.PlayList;
import com.example.musicplayer.model.Song;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener {

    //action get from activity and notification
    public static final String SUPPLY = "com.example.musicplayer.supply";
    public static final String NEXT = "com.example.musicplayer.next";
    public static final String PAUSE = "com.example.musicplayer.pause";
    public static final String PREVIOUS = "com.example.musicplayer.previous";
    public static final String PREPARED = "com.example.musicplayer.prepared";
    public static final String PLAY = "com.example.musicplayer.play";
    public static final String UPDATE_TIMER = "com.example.musicplayer.currenttime";
    public static final String PLAY_ONCLICK = "com.example.musicplayer.playonclickitem";

    //key
    public static final String CURRENT_POSITION = "com.example.musicplayer.currentpositon";
    public static final String DURATION_SONG = "com.example.musicplayer.durration";
    public static final String SEEK_TO = "com.example.musicplayer.seekto";
    public static final int UPDATE_TIME = 100;

    // to update timer
    private Handler mHandler = new Handler();


    //notification
    public static final String CHANEL_ID = "background_music";
    public static final int NOTIFICATION_ID = 2;
    private NotificationManager mManager;

    //music data and player
    private PlayList mPlayList;
    private MediaPlayer player;

    public enum MusicPlayerState {
        Stopped, Playing, Paused
    }

    public static MusicPlayerState sMusicPlayerState;

    //get intent from activity main and notification
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NEXT)) {
                nextSong();
            } else if (intent.getAction().equals(PAUSE)) {
                pause();
            } else if (intent.getAction().equals(PREVIOUS)) {
                previous();
            } else if (intent.getAction().equals(SEEK_TO)) {
                int seekToPosition = intent.getIntExtra(SEEK_TO, 0);
                if(player!=null) player.seekTo(seekToPosition);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sMusicPlayerState = MusicPlayerState.Stopped;
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.NEXT);
        filter.addAction(MusicService.PAUSE);
        filter.addAction(MusicService.PREVIOUS);
        filter.addAction(SEEK_TO);
        filter.addAction(PLAY_ONCLICK);
        registerReceiver(mReceiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(SUPPLY)) {
            mPlayList = (PlayList) intent.getSerializableExtra(MainActivity.LIST_SONG);
        } else if (intent.getAction().equals(PLAY)) {
            int position = intent.getIntExtra(PLAY_ONCLICK,-1);
            if(position>=0){
                mPlayList.setCurrentSong(position);
            }
            Song song = mPlayList.getListSong().get(mPlayList.getCurrentSong());
            play(song);
            startForeground(NOTIFICATION_ID,
                    createNotification(mPlayList.getListSong().get(mPlayList.getCurrentSong()).getName()));

        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if(player!=null) player.stop();
        player.release();
    }


    private Notification createNotification(String name) {
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(CHANEL_ID,
                    "Music Control",
                    NotificationManager.IMPORTANCE_HIGH);
            mManager.createNotificationChannel(channel);
        }

        RemoteViews customNotification = new RemoteViews(getPackageName(),
                R.layout.custom_notification);
        customNotification.setTextViewText(R.id.textSongTitle, name);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANEL_ID)
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(customNotification)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        setListener(customNotification);

        return builder.build();
    }

    private void setListener(RemoteViews view) {
        Intent next = new Intent(NEXT);
        Intent pause = new Intent(PAUSE);
        Intent previous = new Intent(PREVIOUS);


        PendingIntent intentNext = PendingIntent.getBroadcast(getApplicationContext(), 0, next, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnNext, intentNext);

        PendingIntent intentPause = PendingIntent.getBroadcast(getApplicationContext(), 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPause, intentPause);

        PendingIntent intentPrevious = PendingIntent.getBroadcast(getApplicationContext(), 0, previous, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPrevious, intentPrevious);


    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        Intent sendMessage = new Intent(PREPARED);
        sendMessage.putExtra(DURATION_SONG, mp.getDuration());
        bm.sendBroadcast(sendMessage);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        stopSelf();
    }

    private void play(Song song) {
        if(player!=null){
            player.reset();
        }
        if(sMusicPlayerState.equals(MusicPlayerState.Paused)){
            player.start();
        }
        player = MediaPlayer.create(getApplicationContext(), Uri.parse(song.getPath()));
        player.setOnPreparedListener(this);
        mHandler.postDelayed(mUpdateCurrentTime, 100);
        updateNotification(song.getName());
        sMusicPlayerState = MusicPlayerState.Playing;
        player.start();

    }

    private Runnable mUpdateCurrentTime = new Runnable() {
        @Override
        public void run() {
            if (player.isPlaying()) {
                int position = player.getCurrentPosition();
                int duration = player.getDuration();
                Intent intent = new Intent(UPDATE_TIMER);
                intent.putExtra(CURRENT_POSITION, position);
                intent.putExtra(DURATION_SONG, duration);
                LocalBroadcastManager bm = LocalBroadcastManager.getInstance(MusicService.this);
                bm.sendBroadcast(intent);

            }
            mHandler.postDelayed(mUpdateCurrentTime, UPDATE_TIME);
        }
    };

    public void nextSong() {
        if (mPlayList != null) {
            int position = mPlayList.getCurrentSong();
            player.reset();
            if (position < mPlayList.getListSong().size() - 1) {
                mPlayList.setCurrentSong(mPlayList.getCurrentSong() + 1);
                play(mPlayList.getListSong().get(mPlayList.getCurrentSong()));
            }
        }
    }

    public void pause() {
        if (mPlayList != null) {
            if (player.isPlaying()) {
                sMusicPlayerState = MusicPlayerState.Paused;
                player.pause();
            }
        }
    }

    public void previous() {
        if (mPlayList != null) {
            player.reset();
            int position = mPlayList.getCurrentSong();
            if (position > 0) {
                mPlayList.setCurrentSong(mPlayList.getCurrentSong() - 1);
                play(mPlayList.getListSong().get(mPlayList.getCurrentSong()));
            }
        }
    }

    public void updateNotification(String songTitle) {
        if (mManager != null) {
            mManager.notify(NOTIFICATION_ID, createNotification(songTitle));
        }
    }
}
