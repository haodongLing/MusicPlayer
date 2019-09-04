package com.haodong.musicplayer.myplayer;

/**
 * created by linghaoDo on 2019-09-02
 * <p>
 * description:
 */
public class DoStartEvent {
    private String imgUrl;
    private String musicUrl;

    public DoStartEvent(String imgUrl, String musicUrl) {
        this.imgUrl = imgUrl;
        this.musicUrl = musicUrl;
    }

    public DoStartEvent() {
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getMusicUrl() {
        return musicUrl;
    }
}
