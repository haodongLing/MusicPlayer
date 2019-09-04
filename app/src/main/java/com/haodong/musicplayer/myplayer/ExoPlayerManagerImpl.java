package com.haodong.musicplayer.myplayer;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.util.Util;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * description 工具类实现
 * 2019-05-17
 * linghailong
 */
public class ExoPlayerManagerImpl extends ExoPlayerManager {
    private static final String TAG = "lhl->ExoPlayerManager";

    private String currentUri;
    /*自定义进度监听*/
    private long[] adGroupTimesMs;
    private boolean[] playedAdGroups;
    private long[] extraAdGroupTimesMs;
    private boolean[] extraPlayedAdGroups;


    @Override
    public void addListener(@Nullable Player.EventListener listener) {
        if (checkExoPlayerIsInited())
            if (listener == null) {
                mSimpleExoPlayer.addListener(new Player.EventListener() {
                    @Override
                    public void onLoadingChanged(boolean isLoading) {
                        if (isLoading) {
                            startListenProgress();
                        }
                    }

                    @Override
                    public void onPlayerError(ExoPlaybackException error) {

                    }

                    @Override
                    public void onSeekProcessed() {

                    }

                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

                        if (playWhenReady && playbackState == Player.STATE_READY) {
                            LogUtil.i("onPlayerStateChanged: --->setPlayWhenReady");
                        }
                        if (playWhenReady && playbackState == Player.STATE_ENDED) {
                            mSimpleExoPlayer.seekTo(0);
                        }
                    }
                });
            } else {
                mSimpleExoPlayer.addListener(listener);
            }

    }

    @Override
    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    @Override
    public void init(@NonNull Context pContext) {
        super.init(pContext);

        adGroupTimesMs = new long[0];
        playedAdGroups = new boolean[0];
        extraAdGroupTimesMs = new long[0];
        extraPlayedAdGroups = new boolean[0];
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
    }

    @Override
    public void addMediaListener(@Nullable MediaControlListener listener) {
        this.mediaControlListener = listener;
    }

    @Override
    public void startListenProgress() {
        mHandler.post(loadStatusRunable);
    }


    @Override
    public void releasePlayer() {
        LogUtil.i("exo-->releasePlayer");
        if (checkExoPlayerIsInited()) {
            mHandler.removeCallbacks(loadStatusRunable);
            mHandler = null;
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
        }
        if (mContext != null) {
            mContext = null;
        }
    }

    @Override
    public void startRadio(String uri) {
        if (checkExoPlayerIsInited()) {
            mSimpleExoPlayer.stop(true);
            mSimpleExoPlayer.prepare(buildMediaSource(Uri.parse(uri)));
            mSimpleExoPlayer.setPlayWhenReady(true);
            currentUri = uri;
        }
    }

    //重新播放
    @Override
    public void reStart() {
        if (checkExoPlayerIsInited()) {
            if (isPaused) {
                resumeRadio();
            } else {
                mSimpleExoPlayer.setPlayWhenReady(true);
                mSimpleExoPlayer.seekTo(0);
            }

        }

    }

    @Override
    public void stopRadio() {
        if (checkExoPlayerIsInited()) {
            mSimpleExoPlayer.stop();
        }

    }


    @Override
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

    @Override
    public boolean isCurrentWindowSeekable() {
        Timeline timeline = mSimpleExoPlayer.getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(mSimpleExoPlayer.getCurrentWindowIndex(), window).isSeekable;
    }

    @Override
    public void pauseRadio() {
        if (controlDispatcher != null && mSimpleExoPlayer != null) {
            controlDispatcher.dispatchSetPlayWhenReady(mSimpleExoPlayer, false);
            setPaused(true);
        }
    }

    @Override
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

    @Override
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
