package com.haodong.musicplayer.myplayer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.TimeBar;
import com.haodong.musicplayer.R;
import com.haodong.musicplayer.permission.AVCallFloatView;
import com.haodong.musicplayer.permission.FloatWindowManager;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.Nullable;


/**
 * created by linghaoDo on 2019-08-25
 * <p>
 * description:
 */
public class ExoPlayerService extends Service implements FloatWindowManager.OnWindowLis {
    private static final String TAG = "lhl-->ExoPlayerService";
    private ImageView ivCover;
    private CircleTimeBar timeBar;
    private ImageView ivClose;
    private boolean isWindowDismiss = true;
    private WindowManager windowManager = null;
    private WindowManager.LayoutParams mParams = null;
    private AVCallFloatView floatView = null;
    private OnProgressLis onProgressLis;
    private String mCoverUrl;
    private boolean isListenerIniteed = false;

    public boolean isWindowDismiss() {
        return isWindowDismiss;
    }

    private static final String ACTIVITY_NAME="com.maxwon.mobile.module.business.activities.knowledge.KnowledgeMusicActivity";

    public OnProgressLis getOnProgressLis() {
        return onProgressLis;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true, priority = 8)
    public ExoPlayerService setOnProgressLis(OnProgressLis onProgressLis) {
        this.onProgressLis = onProgressLis;
        LogUtil.i("setOnProgressLis()");
        return this;
    }
    public class MusicBinder extends Binder {
        public ExoPlayerService getService() {
            return ExoPlayerService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.i("执行了onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
        LogUtil.i("onCreate");
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void initListener() {
        ExoPlayerManager.getDefault().addMediaListener(new ExoPlayerManager.MediaControlListener() {
            @Override
            public void setCurPositionTime(long curPositionTime) {
                if (timeBar != null)
                    timeBar.setPosition(curPositionTime);
                if (onProgressLis != null) {
                    onProgressLis.onPositionChanged(curPositionTime);
                }
            }

            @Override
            public void setDurationTime(long durationTime) {
                if (timeBar != null)
                    timeBar.setDuration(durationTime);
                if (onProgressLis != null)
                    onProgressLis.onDurationChanged(durationTime);
            }

            @Override
            public void setBufferedPositionTime(long bufferedPosition) {
                if (onProgressLis != null) {
                    onProgressLis.onBufferedPositionChanged(bufferedPosition);
                }

            }

            @Override
            public void setCurTimeString(String curTimeString) {
                if (onProgressLis != null) {
                    onProgressLis.onCurTimeStringChanged(curTimeString);
                }
            }

            @Override
            public void setDurationTimeString(String durationTimeString) {
                if (onProgressLis != null) {
                    onProgressLis.onDurationTimeStringChanged(durationTimeString);
                }
            }
        });
        ExoPlayerManager.getDefault().addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.i(TAG,  "playWhenReady-->" + playWhenReady + "playbackState-->" + playbackState);
                if (onProgressLis != null) {
                    onProgressLis.onPlayerStateChanged(playWhenReady, playbackState);
                }
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                if (isLoading) {
                    ExoPlayerManager.getDefault().startListenProgress();
                }
            }
        });
    }


    @Subscribe(sticky = true)
    public void doStart(DoStartEvent startEvent) {
        mCoverUrl = startEvent.getImgUrl();
        if (!isListenerIniteed)
            initListener();
        ExoPlayerManager.getDefault().startRadio(startEvent.getMusicUrl());
    }

    public void show() {
        LogUtil.i("show");
        initFloatingWindow();
    }

    public interface OnProgressLis {
        void onPositionChanged(long curPositionTime);

        void onDurationChanged(long durationTime);

        void onBufferedPositionChanged(long bufferedPosition);

        void onCurTimeStringChanged(String curTimeString);

        void onDurationTimeStringChanged(String durationTimeString);

        void onPlayerStateChanged(boolean playWhenReady, int playbackState);
    }


    @Override
    public void onDestroy() {
        LogUtil.i("music service destoryed");
        EventBus.getDefault().unregister(this);
        ExoPlayerManager.getDefault().releasePlayer();
        super.onDestroy();
    }

    private void initFloatingWindow() {
        if (!isWindowDismiss) {
            Log.e(TAG, "view is already added here");
            return;
        }
        isWindowDismiss = false;
        if (windowManager == null) {
            windowManager = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        mParams = new WindowManager.LayoutParams();
        mParams.packageName = this.getPackageName();
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        int mType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        }
        mParams.type = mType;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        mParams.x = screenWidth - dp2px(this, 100);
        mParams.y = screenHeight - dp2px(this, 171);
        floatView = new AVCallFloatView(this.getApplicationContext());
        floatView.setParams(mParams);
        floatView.setIsShowing(true);
        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = null;
                try {
                    intent = new Intent(ExoPlayerService.this.getApplicationContext(),Class.forName(ACTIVITY_NAME));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                PendingIntent pendingIntent =
                        PendingIntent.getActivity(ExoPlayerService.this.getApplicationContext(), 0, intent, 0);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });
        /*init*/
        ivClose = floatView.findViewById(R.id.iv_stop);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExoPlayerManager.getDefault().pauseRadio();
                dismissWindow();
            }
        });
        ivCover = floatView.findViewById(R.id.iv_cover);
        timeBar = floatView.findViewById(R.id.circle_time_bar);
        timeBar.addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                ExoPlayerManager.getDefault().seekToTimeBarPosition(position);
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {

            }
        });
        windowManager.addView(floatView, mParams);
        initListener();
    }

    @Override
    public void showWindow() {
        show();
    }

    @Override
    public void dismissWindow() {
        if (isWindowDismiss) {
            Log.e(TAG, "window can not be dismiss cause it has not been added");
            return;
        }
        isWindowDismiss = true;
        floatView.setIsShowing(false);
        if (windowManager != null && floatView != null) {
            windowManager.removeViewImmediate(floatView);
        }
    }


    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

}
