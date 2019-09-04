package com.haodong.musicplayer.myplayer;

import java.io.Serializable;

/**
 * created by linghaoDo on 2019-09-01
 * <p>
 * description:
 */
public class Chapter implements Serializable {
    private String title;
    private String cover;
    private String musicUrl;

    public Chapter() {
    }

    public Chapter(String title, String cover, String musicUrl) {
        this.title = title;
        this.cover = cover;
        this.musicUrl = musicUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }
}
