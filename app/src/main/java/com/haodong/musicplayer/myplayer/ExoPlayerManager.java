package com.haodong.musicplayer.myplayer;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * description:exoplayer 抽象类
 * 2019-05-17
 * linghailong
 */
public class ExoPlayerManager {
    private Context mContext;
    private SimpleExoPlayer mSimpleExoPlayer;
    // 创建轨道选择器实例
    private TrackSelector trackSelector;
    private DataSource.Factory dataSourceFactory;
    private String userAgent = "exoplayer-codelab";
    private boolean haveBuy;


    /*自定义进度监听*/
    private long[] adGroupTimesMs;
    private boolean[] playedAdGroups;
    private long[] extraAdGroupTimesMs;
    private boolean[] extraPlayedAdGroups;

    private ExoPlayerManager() {
    }

    private Timeline.Window window;
    private Timeline.Period period;
    private Handler mHandler;
    private MediaControlListener mediaControlListener;
    private DefaultControlDispatcher controlDispatcher;
    private StringBuilder formatBuilder;
    private Formatter formatter;
    private String currentUri;
    /*为暂停而记录*/
    private boolean isPaused = false;
    /*记录是否已经停止播放*/
    private boolean isStoped = false;
    private boolean activityIsShowing;
    // 记录当前暂停位置；
    private long mWindowIndex;
    private String mChapterId = "";
    private String mProductId = "";
    private Player.EventListener mPreListener;
    private AudioManager mAudioManager;


    /**
     * 注意内存泄漏，context只能使用application
     * 或者在单个activity中使用完成之后移除。
     */
    private static final class Holder {
        private static final ExoPlayerManager sInstance = new ExoPlayerManager();
    }

    public String getCurrentUri() {
        return currentUri;
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

    public boolean isStoped() {
        return isPaused && !mSimpleExoPlayer.getPlayWhenReady();
    }

    public long getWindowIndex() {
        if (checkExoPlayerIsInited()) {
            LogUtil.d("windowIndex-->" + mSimpleExoPlayer.getCurrentPosition());
            return mSimpleExoPlayer.getCurrentPosition();
        }
        return 0;
    }

    public String getChapterId() {
        return mChapterId;
    }

    public void setChapterId(String mChapterId) {
        this.mChapterId = mChapterId;
    }

    public Player.EventListener getmPreListener() {
        return mPreListener;
    }

    public void setmPreListener(Player.EventListener mPreListener) {
        this.mPreListener = mPreListener;
    }

    /**
     * @param pContext A valid context of the calling application.
     */
    public void init(@NonNull Context pContext) {
        /*如果mContext!=null,那么说明已经实例化*/
        if (mContext != null) {
            return;
        }
        mContext = pContext;
        String applicationName = "appname";

        //1 初始化AudioManager对象
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        //2 申请焦点
        mAudioManager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
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

        adGroupTimesMs = new long[0];
        playedAdGroups = new boolean[0];
        extraAdGroupTimesMs = new long[0];
        extraPlayedAdGroups = new boolean[0];
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

    }


    /**
     * 添加Player的listener
     *
     * @param listener
     */

    public void addListener(@NonNull Player.EventListener listener) {
        if (checkExoPlayerIsInited()) {
            if (mPreListener != null) {
                mSimpleExoPlayer.removeListener(mPreListener);
            }
            mSimpleExoPlayer.addListener(listener);
            mPreListener = listener;
        }
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                    //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                    //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                    LogUtil.d("AUDIOFOCUS_LOSS");
                    pauseRadio();
                    //释放焦点，该方法可根据需要来决定是否调用
                    //若焦点释放掉之后，将不会再自动获得
                    mAudioManager.abandonAudioFocus(mAudioFocusChange);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                    //会触发此回调事件，例如播放短视频，拨打电话等。
                    //通常需要暂停音乐播放
                    pauseRadio();
                    LogUtil.d("AUDIOFOCUS_LOSS_TRANSIENT");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    LogUtil.d("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //当其他应用申请焦点之后又释放焦点会触发此回调
                    //可重新播放音乐
                    LogUtil.d("AUDIOFOCUS_GAIN");
                        reStart();
                    break;
            }
        }
    };


    /**
     * 检查当亲Player是否被实例化
     *
     * @return
     */


    public interface MediaControlListener {
        void setCurPositionTime(long curPositionTime);

        void setDurationTime(long durationTime);

        void setBufferedPositionTime(long bufferedPosition);

        void setCurTimeString(String curTimeString);

        void setDurationTimeString(String durationTimeString);
    }

    public void removeListener(@Nullable Player.EventListener listener) {
        mSimpleExoPlayer.removeListener(listener);
    }


    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }


    public void addMediaListener(@Nullable MediaControlListener listener) {
        this.mediaControlListener = listener;
    }


    public void removeMediaListener(@Nullable MediaControlListener listener) {
        this.mediaControlListener = null;
    }


    public void startListenProgress() {
        mHandler.post(loadStatusRunable);
    }


    public void releasePlayer() {
        LogUtil.i("exo-->releasePlayer");
        if (checkExoPlayerIsInited()) {
            mHandler.removeCallbacks(loadStatusRunable);
            mHandler = null;
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
            trackSelector = null;
        }
        if (mContext != null) {
            mContext = null;
        }
    }


    public void startRadio(String uri) {
        if (checkExoPlayerIsInited()) {
            mSimpleExoPlayer.stop(true);
            mSimpleExoPlayer.prepare(buildMediaSource(Uri.parse(uri)));
            mSimpleExoPlayer.setPlayWhenReady(true);
            currentUri = uri;
            isPaused = false;
        }
    }

    //重新播放

    public void reStart() {
        if (checkExoPlayerIsInited()) {
            if (isPaused) {
                resumeRadio();
            } else {
                mSimpleExoPlayer.setPlayWhenReady(true);
                mSimpleExoPlayer.seekTo(0);
                isPaused = false;
            }

        }

    }


    public void stopRadio() {
        if (checkExoPlayerIsInited()) {
            mSimpleExoPlayer.stop();
        }

    }


    public void resumeRadio() {
        if (controlDispatcher != null && checkExoPlayerIsInited()) {
            /**
             * The player does not have any media to play.
             */
            if (mSimpleExoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                LogUtil.i("加载失败");

            } else if (mSimpleExoPlayer.getPlaybackState() == Player.STATE_ENDED) {
                //重新播放
                controlDispatcher.dispatchSeekTo(mSimpleExoPlayer, mSimpleExoPlayer.getCurrentWindowIndex(), C.TIME_UNSET);
                mSimpleExoPlayer.setPlayWhenReady(true);
//                mSimpleExoPlayer.seekTo(0);
            }
            controlDispatcher.dispatchSetPlayWhenReady(mSimpleExoPlayer, true);
            setPaused(false);
        }
    }

//    /**
//     * @param windowIndex
//     */
//    public void seekTo(int windowIndex) {
//        FFLog.i("seekTo---->" + windowIndex);
//        controlDispatcher.dispatchSeekTo(mSimpleExoPlayer, windowIndex, C.TIME_UNSET);
//        mSimpleExoPlayer.setPlayWhenReady(true);
//        isPaused = false;
//    }


    public boolean isCurrentWindowSeekable() {
        Timeline timeline = mSimpleExoPlayer.getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(mSimpleExoPlayer.getCurrentWindowIndex(), window).isSeekable;
    }


    public void pauseRadio() {
        if (controlDispatcher != null && mSimpleExoPlayer != null) {
            mWindowIndex = (int) mSimpleExoPlayer.getCurrentPosition();
            LogUtil.d("mWindowIndex-->" + mWindowIndex);
            LogUtil.d("getCurrentPosition-->" + mSimpleExoPlayer.getCurrentPosition());
            controlDispatcher.dispatchSetPlayWhenReady(mSimpleExoPlayer, false);
            setPaused(true);
        }
    }

    public void pauseRadioBackground() {
        pauseRadio();
    }


    public boolean checkExoPlayerIsInited() {
        return mSimpleExoPlayer != null;
    }


    public MediaSource buildMediaSource(Uri uri) {
        return buildMediaSource(uri, null);
    }

    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    public void seekToTimeBarPosition(long positionMs) {
        Timeline timeline = mSimpleExoPlayer.getCurrentTimeline();
        int windowIndex;
        if (!timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
            while (true) {
                long windowDurationMs = timeline.getWindow(windowIndex, window).getDurationMs();
                if (positionMs < windowDurationMs) {
                    break;
                } else if (windowIndex == windowCount - 1) {
                    // Seeking past the end of the last window should seek to the end of the timeline.
                    positionMs = windowDurationMs;
                    break;
                }
                positionMs -= windowDurationMs;
                windowIndex++;
            }
        } else {
            windowIndex = mSimpleExoPlayer.getCurrentWindowIndex();
        }
        boolean dispatched = controlDispatcher.dispatchSeekTo(mSimpleExoPlayer, windowIndex, positionMs);
        if (!dispatched) {
            mHandler.post(loadStatusRunable);
        }
    }

    public Runnable loadStatusRunable = new Runnable() {
        @Override
        public void run() {
            long durationUs = 0;
            int adGroupCount = 0;
            long currentWindowTimeBarOffsetMs = 0;
            Timeline currentTimeline = mSimpleExoPlayer.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                int currentWindowIndex = mSimpleExoPlayer.getCurrentWindowIndex();


                int firstWindowIndex = currentWindowIndex;
                int lastWindowIndex = currentWindowIndex;
                for (int i = firstWindowIndex; i <= lastWindowIndex; i++) {
                    if (i == currentWindowIndex) {
                        currentWindowTimeBarOffsetMs = C.usToMs(durationUs);
                    }
                    currentTimeline.getWindow(i, window);
                    if (window.durationUs == C.TIME_UNSET) {
                        break;
                    }
                    for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                        currentTimeline.getPeriod(j, period);
                        int periodAdGroupCount = period.getAdGroupCount();
                        for (int adGroupIndex = 0; adGroupIndex < periodAdGroupCount; adGroupIndex++) {
                            long adGroupTimeInPeriodUs = period.getAdGroupTimeUs(adGroupIndex);
                            if (adGroupTimeInPeriodUs == C.TIME_END_OF_SOURCE) {
                                if (period.durationUs == C.TIME_UNSET) {
                                    continue;
                                }
                                adGroupTimeInPeriodUs = period.durationUs;
                            }
                            long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + period.getPositionInWindowUs();
                            if (adGroupTimeInWindowUs >= 0 && adGroupTimeInWindowUs <= window.durationUs) {
                                if (adGroupCount == adGroupTimesMs.length) {
                                    int newLength = adGroupTimesMs.length == 0 ? 1 : adGroupTimesMs.length * 2;
                                    adGroupTimesMs = Arrays.copyOf(adGroupTimesMs, newLength);
                                    playedAdGroups = Arrays.copyOf(playedAdGroups, newLength);
                                }
                                adGroupTimesMs[adGroupCount] = C.usToMs(durationUs + adGroupTimeInWindowUs);
                                playedAdGroups[adGroupCount] = period.hasPlayedAdGroup(adGroupIndex);
                                adGroupCount++;
                            }
                        }
                    }
                    durationUs += window.durationUs;
                }
            }

            durationUs = C.usToMs(window.durationUs);
            long curtime = currentWindowTimeBarOffsetMs + mSimpleExoPlayer.getContentPosition();
            long bufferedPosition = currentWindowTimeBarOffsetMs + mSimpleExoPlayer.getContentBufferedPosition();

            if (mediaControlListener != null) {
                mediaControlListener.setCurTimeString("" + Util.getStringForTime(formatBuilder, formatter, curtime));
                //  > 1000 ? durationUs - 1000 : durationUs
                mediaControlListener.setDurationTimeString("" + Util.getStringForTime(formatBuilder, formatter, durationUs));
                mediaControlListener.setBufferedPositionTime(bufferedPosition);
                mediaControlListener.setCurPositionTime(curtime);
                mediaControlListener.setDurationTime(durationUs);
            }

            mHandler.removeCallbacks(loadStatusRunable);
            int playbackState = mSimpleExoPlayer == null ? Player.STATE_IDLE : mSimpleExoPlayer.getPlaybackState();

            // 播放器未开始播放后者播放器播放结束
            if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
                long delayMs = 0;
                // 当正在播放状态时
                if (mSimpleExoPlayer.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                    float playBackSpeed = mSimpleExoPlayer.getPlaybackParameters().speed;
                    if (playBackSpeed <= 0.1f) {
                        delayMs = 1000;
                    } else if (playBackSpeed <= 5f) {
                        // 中间更新周期时间
                        long mediaTimeUpdatePeriodMs = 1000 / Math.max(1, Math.round(1 / playBackSpeed));
                        // 当前进度时间与中间更新周期之间的多出的不足一个中间更新周期时长的时间
                        long surplusTimeMs = curtime % mediaTimeUpdatePeriodMs;
                        // 播放延迟时间
                        long mediaTimeDelayMs = mediaTimeUpdatePeriodMs - surplusTimeMs;
                        if (mediaTimeDelayMs < (mediaTimeUpdatePeriodMs / 5)) {
                            mediaTimeDelayMs += mediaTimeUpdatePeriodMs;
                        }
                        delayMs = playBackSpeed == 1 ? mediaTimeDelayMs : (long) (mediaTimeDelayMs / playBackSpeed);
                    } else {
                        delayMs = 200;
                    }
                } else {
                    // 当暂停状态时
                    delayMs = 1000;
                }
                mHandler.postDelayed(this, delayMs);
            }
        }
    };
}
