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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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
    public static final String PLAY_ONCLICK = "com.example.musicplayer.playonclickitem";

    //key
    public static final String DURATION_SONG = "com.example.musicplayer.durration";
    public static final String SEEK_TO = "com.example.musicplayer.seekto";

    // to update timer
    private TimeCurrentBinder mTimeCurrentBinder;



    //notification
    public static final String CHANEL_ID = "background_music";
    public static final int NOTIFICATION_ID = 2;
    private NotificationManager mManager;

    //music data and mPlayer
    private PlayList mPlayList;
    private MediaPlayer mPlayer;

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
                if(mPlayer !=null) mPlayer.seekTo(seekToPosition);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mTimeCurrentBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTimeCurrentBinder = new TimeCurrentBinder();
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
        if(mPlayer !=null) mPlayer.stop();
        mPlayer.release();
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
        if(mPlayer !=null){
            mPlayer.reset();
        }
        if(sMusicPlayerState.equals(MusicPlayerState.Paused)){
            mPlayer.start();
        }
        mPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(song.getPath()));
        mPlayer.setOnPreparedListener(this);
        updateNotification(song.getName());
        sMusicPlayerState = MusicPlayerState.Playing;
        mPlayer.start();

    }

    public void nextSong() {
        if (mPlayList != null) {
            int position = mPlayList.getCurrentSong();
            mPlayer.reset();
            if (position < mPlayList.getListSong().size() - 1) {
                mPlayList.setCurrentSong(mPlayList.getCurrentSong() + 1);
                play(mPlayList.getListSong().get(mPlayList.getCurrentSong()));
            }
        }
    }

    public void pause() {
        if (mPlayList != null) {
            if (mPlayer.isPlaying()) {
                sMusicPlayerState = MusicPlayerState.Paused;
                mPlayer.pause();
                sMusicPlayerState = MusicPlayerState.Paused;
            }
        }
    }

    public void previous() {
        if (mPlayList != null) {
            mPlayer.reset();
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
    public int getCurrentSongPosition(){
        if(mPlayer !=null)  return mPlayer.getCurrentPosition();
        return 0;
    }
    public int getSongDuration(){
        if(mPlayer !=null) return mPlayer.getDuration();
        return 0;
    }
    public class TimeCurrentBinder extends Binder{
        public MusicService getMusicService(){
            return MusicService.this;
        }
    }
}
