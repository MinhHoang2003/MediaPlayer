package com.example.musicplayer.model;



import java.io.Serializable;
import java.util.ArrayList;

public class PlayList implements Serializable {

    private int currentSong = 0;
    private ArrayList<Song> listSong;

    public PlayList(ArrayList<Song> listSong) {
        this.listSong = listSong;
        currentSong = 0;
    }

    public ArrayList<Song> getListSong() {
        return listSong;
    }

    public void setListSong(ArrayList<Song> listSong) {
        this.listSong = listSong;
    }

    public int getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(int currentSong) {
        this.currentSong = currentSong;
    }
}
