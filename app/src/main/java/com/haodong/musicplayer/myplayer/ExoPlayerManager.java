package com.haodong.musicplayer.myplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;

import androidx.annotation.NonNull;


/**
 * description:
 * 2019-05-17
 * linghailong
 */
public abstract class ExoPlayerManager {
    protected Context mContext;
    protected SimpleExoPlayer mSimpleExoPlayer;
    protected DataSource.Factory dataSourceFactory;
    private String userAgent = "exoplayer-codelab";

    protected ExoPlayerManager() {
    }

    protected Timeline.Window window;
    protected Timeline.Period period;
    protected Handler mHandler;
    protected MediaControlListener mediaControlListener;
    protected DefaultControlDispatcher controlDispatcher;
    protected StringBuilder formatBuilder;
    protected Formatter formatter;

    protected boolean isPaused = false;

    private static final class Holder {
        private static final ExoPlayerManager sInstance = new ExoPlayerManagerImpl();
    }

    public static ExoPlayerManager getDefault() {
        return Holder.sInstance;
    }

    public StringBuilder getFormatBuilder() {
        return formatBuilder;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public abstract void setPaused(boolean paused);

    /**
     * @param pContext        A valid context of the calling application.
     *  "Blah" String that will be prefix'ed to the generated user agent.
     */
    public void init(@NonNull Context pContext) {
        /*如果mContext!=null,那么说明已经实例化*/
        if (mContext != null) {
            return;
        }
        mContext = pContext;
         TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        // 创建轨道选择器实例
        String applicationName="appname";
         TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);
        userAgent = Util.getUserAgent(mContext, applicationName.replace("ExoPlayerLib", "Blah"));
        dataSourceFactory = new DefaultDataSourceFactory(mContext, userAgent, new TransferListener() {
            @Override
            public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {

            }

            @Override
            public void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {

            }

            @Override
            public void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {

            }

            @Override
            public void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {

            }
        });
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        if (mHandler == null)
            mHandler = new Handler(Looper.getMainLooper());
        controlDispatcher = new DefaultControlDispatcher();

    }

    public abstract void addMediaListener(MediaControlListener listener);

    public abstract void startListenProgress();

    /**
     * 添加Player的listener
     *
     * @param listener
     */
    public abstract void addListener(Player.EventListener listener);

    /**
     * 释放Player
     */
    public abstract void releasePlayer();


    public abstract void startRadio(String uri);

    /**
     * 重新开始
     */
    public abstract void reStart();

    /**
     * 停止
     */
    public abstract void stopRadio();


    /**
     * 重启
     */
    public abstract void resumeRadio();

    public abstract boolean isCurrentWindowSeekable();

    /**
     * 暂停
     */
    public abstract void pauseRadio();


    /**
     * 检查当亲Player是否被实例化
     *
     * @return
     */
    public abstract boolean checkExoPlayerIsInited();


    public abstract void seekToTimeBarPosition(long positionMs);

    public interface MediaControlListener {
        void setCurPositionTime(long curPositionTime);

        void setDurationTime(long durationTime);

        void setBufferedPositionTime(long bufferedPosition);

        void setCurTimeString(String curTimeString);

        void setDurationTimeString(String durationTimeString);
    }
}
