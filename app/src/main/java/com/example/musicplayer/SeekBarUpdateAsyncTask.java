package com.example.musicplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;

public class SeekBarUpdateAsyncTask extends AsyncTask<Void,Integer,Void> {

    private Context context;

    public SeekBarUpdateAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MediaPlayer player = MediaPlayer.create(context,R.raw.onrainyday_beats);
        player.setLooping(true);

        player.start();

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);


    }
}
