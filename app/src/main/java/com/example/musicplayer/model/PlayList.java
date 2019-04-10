package com.example.musicplayer.model;



import java.io.Serializable;
import java.util.ArrayList;

public class PlayList implements Serializable {

    private int mCurrentSong = 0;
    private ArrayList<Song> mListSong;

    public PlayList(ArrayList<Song> listSong) {
        this.mListSong = listSong;
        mCurrentSong = 0;
    }

    public ArrayList<Song> getListSong() {
        return mListSong;
    }

    public void setListSong(ArrayList<Song> listSong) {
        this.mListSong = listSong;
    }

    public int getCurrentSong() {
        return mCurrentSong;
    }

    public void setCurrentSong(int currentSong) {
        this.mCurrentSong = currentSong;
    }
}
