package com.haodong.musicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.util.Util;
import com.haodong.musicplayer.myplayer.Chapter;
import com.haodong.musicplayer.myplayer.ExoPlayerManager;
import com.haodong.musicplayer.myplayer.ExoPlayerService;
import com.haodong.musicplayer.myplayer.LogUtil;
import com.haodong.musicplayer.permission.FloatWindowManager;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity implements ExoPlayerService.OnProgressLis {
    @BindView(R.id.iv_music_previous)
    ImageView ivPrevious;
    @BindView(R.id.iv_music_next)
    ImageView ivNext;
    @BindView(R.id.tv_start_time)
    TextView tvStartTime;
    @BindView(R.id.tv_end_time)
    TextView tvEndTime;
    @BindView(R.id.iv_cover)
    ImageView ivCover;
    @BindView(R.id.iv_audio_switch)
    ImageView ivStop;
    @BindView(R.id.exo_progress)
    DefaultTimeBar timeBar;
    private List<Chapter> chapterList = new ArrayList<>();
    @BindView(R.id.btn_show_floating)
    Button btnFloating;
    Unbinder unbinder;
    private ExoPlayerService.MusicBinder myBinder;
    private ExoPlayerService mService;

    private Intent serviceIntent;
    private ServiceConnection serviceConnection;

    @OnClick({R.id.iv_music_previous, R.id.iv_music_next, R.id.iv_audio_switch, R.id.btn_show_floating})
    void onBtnClick(View v) {
        final int id = v.getId();
        if (id == R.id.iv_music_next) {

        } else if (id == R.id.iv_music_previous) {


        } else if (id == R.id.iv_audio_switch) {
            if (!ExoPlayerManager.getDefault().isPaused()) {
                ivStop.setImageResource(R.mipmap.ic_knowledge_audio_suspended);
                ExoPlayerManager.getDefault().pauseRadio();
            } else {
                ivStop.setImageResource(R.mipmap.ic_knowledge_audio_play);
                ExoPlayerManager.getDefault().resumeRadio();
            }
        } else if (id == R.id.btn_show_floating) {
            LogUtil.i();
            if (mService != null) {
                if (FloatWindowManager.getInstance().applyOrShowFloatWindow(this) && !ExoPlayerManager.getDefault().isStoped()) {
                   mService.showWindow();
                    // todo
                } else if (!mService.isWindowDismiss()) {
                    mService.dismissWindow();
                }
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knowledge_music);
        unbinder = ButterKnife.bind(this);
        initWidget();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        /*这是重点*/
        moveTaskToBack(true);
    }

    private void initWidget() {
        ExoPlayerManager.getDefault().init(this);
        bindService();

        timeBar.addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                tvStartTime.setText(Util.getStringForTime(ExoPlayerManager.getDefault().getFormatBuilder()
                        , ExoPlayerManager.getDefault().getFormatter(), position));
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                ExoPlayerManager.getDefault().seekToTimeBarPosition(position);
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {

            }
        });


    }

    private void bindService() {
        LogUtil.i();
        serviceIntent = new Intent(MainActivity.this, ExoPlayerService.class);
        if (serviceConnection == null) {
            serviceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mService = ((ExoPlayerService.MusicBinder) service).getService();
//                    String uri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3";
                    mService.setOnProgressLis(MainActivity.this).initListener();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            /*模仿网络请求*/
                            LogUtil.d("发送请求");
                            String uri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3";
                            ExoPlayerManager.getDefault().startRadio(uri);
                        }
                    }, 1000);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
            startService(serviceIntent);
        }
    }

    private void unbindService() {
        if (null != serviceConnection) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService();
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    public void onPositionChanged(long curPositionTime) {
        if (timeBar != null)
            timeBar.setPosition(curPositionTime);
    }

    @Override
    public void onDurationChanged(long durationTime) {
        if (timeBar != null)
            timeBar.setDuration(durationTime);
    }

    @Override
    public void onBufferedPositionChanged(long bufferedPosition) {
        if (timeBar != null)
            timeBar.setBufferedPosition(bufferedPosition);
    }

    @Override
    public void onCurTimeStringChanged(String curTimeString) {
        if (tvStartTime != null) {
            tvStartTime.setText(curTimeString);
        }

    }

    @Override
    public void onDurationTimeStringChanged(String durationTimeString) {
        if (tvEndTime != null) {
            tvEndTime.setText(durationTimeString);
        }

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        LogUtil.i("onPlayerStateChanged");
        if (playWhenReady) {
            switch (playbackState) {
                case Player.STATE_READY:
                    LogUtil.d("----time5");
                    ivStop.setImageResource(R.mipmap.ic_knowledge_audio_play);
                    ExoPlayerManager.getDefault().setPaused(false);
                    break;
                case Player.STATE_ENDED:
                    /*如果是节，播放下一首*/
                    LogUtil.i("播放下一首");
                    break;
                default:
                    ivStop.setImageResource(R.mipmap.ic_knowledge_audio_suspended);
                    ExoPlayerManager.getDefault().setPaused(true);
                    break;

            }
        } else {
            ivStop.setImageResource(R.mipmap.ic_knowledge_audio_suspended);
            ExoPlayerManager.getDefault().setPaused(true);
        }
    }
}
