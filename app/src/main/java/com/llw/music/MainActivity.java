package com.llw.music;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.llw.music.adapter.MusicListAdapter;
import com.llw.music.model.Song;
import com.llw.music.utils.MusicUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    @BindView(R.id.rv_music)
    RecyclerView rvMusic;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.scan_lay)
    LinearLayout scanLay;
    @BindView(R.id.tv_clear_list)
    TextView tvClearList;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_play_time)
    TextView tvPlayTime;
    @BindView(R.id.time_seekBar)
    SeekBar timeSeekBar;
    @BindView(R.id.tv_total_time)
    TextView tvTotalTime;
    @BindView(R.id.btn_previous)
    ImageView btnPrevious;
    @BindView(R.id.btn_play_or_pause)
    ImageView btnPlayOrPause;
    @BindView(R.id.btn_play_mode)
    ImageView btnPlayMode;
    @BindView(R.id.btn_next)
    ImageView btnNext;
    @BindView(R.id.btn_refresh)
    ImageView btnRefresh;
    public final static String MUSIC_DATA_FIRST = "musicDataFirst";
    private MusicListAdapter mAdapter;//歌曲适配器
    private List<Song> mList;//歌曲列表
    private RxPermissions rxPermissions;//权限请求
    private MediaPlayer mediaPlayer;//音频播放器
    private String musicData = null;
    private boolean modeIsSeqFlag = true;
    private Random random = new Random();
    @BindView(R.id.tv_play_song_info)
    TextView tvPlaySongInfo;
    @BindView(R.id.play_state_img)
    ImageView playStateImg;
    @BindView(R.id.play_state_lay)
    LinearLayout playStateLay;
    // 记录当前播放歌曲的位置
    public int mCurrentPosition;
    private static final int INTERNAL_TIME = 1000;// 音乐进度间隔时间

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            // 展示给进度条和当前时间
            int progress = mediaPlayer.getCurrentPosition();
            timeSeekBar.setProgress(progress);
            tvPlayTime.setText(parseTime(progress));
            // 继续定时发送数据
            updateProgress();
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        rxPermissions = new RxPermissions(this);//使用前先实例化
        timeSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);//滑动条监听
        musicData = getString(MUSIC_DATA_FIRST, "yes", this);


        if (musicData.equals("no")) {//说明是第一次打开APP，未进行扫描
            scanLay.setVisibility(View.GONE);
            initMusic();
        } else {
            scanLay.setVisibility(View.VISIBLE);
        }
    }

    private void permissionRequest() {
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {//请求成功之后开始扫描
                        initMusic();
                    } else {//失败时给一个提示
                        Toast.makeText(MainActivity.this, "未授权",Toast.LENGTH_LONG).show();
                    }
                });
    }

    //获取音乐列表
    private void initMusic() {
        mList = new ArrayList<>();//实例化

        //数据赋值
        mList = MusicUtils.getMusicData(this);//将扫描到的音乐赋值给音乐列表
        if (!mList.isEmpty() && mList != null) {
            scanLay.setVisibility(View.GONE);
            putString(MUSIC_DATA_FIRST, "no", this);
        }
        mAdapter = new MusicListAdapter(R.layout.item_music_rv_list, mList);//指定适配器的布局和数据源
        rvMusic.setLayoutManager(new LinearLayoutManager(this));
        //设置适配器
        rvMusic.setAdapter(mAdapter);

        //item的点击事件
        mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.item_music) {
                    mCurrentPosition = position;
                    changeMusic(mCurrentPosition);
                }
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.item_music) {
                    DeleteDialog(position);
                    mCurrentPosition = position++;
                    changeMusic(mCurrentPosition);

                }
                return true;
            }
        });
    }

    private void DeleteDialog(int index) {
        new AlertDialog.Builder(this).setTitle("删除歌曲")
                .setMessage("是否真的删除歌曲?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mList.remove(index);
                        mAdapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).create().show();
    }

    @OnClick({R.id.tv_clear_list, R.id.btn_scan, R.id.btn_previous, R.id.btn_play_or_pause, R.id.btn_next,
            R.id.btn_play_mode,R.id.btn_refresh})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_clear_list://清空数据
                mList.clear();
                mAdapter.notifyDataSetChanged();
                putString(MUSIC_DATA_FIRST, "yes", this);
                scanLay.setVisibility(View.VISIBLE);
                toolbar.setBackgroundColor(getResources().getColor(R.color.white));
                tvTitle.setTextColor(getResources().getColor(R.color.black));
                tvClearList.setTextColor(getResources().getColor(R.color.black));
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnCompletionListener(this);//监听音乐播放完毕事件，自动下一曲
                }
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaPlayer.reset();
                }
                break;
            case R.id.btn_scan://扫描本地歌曲
                permissionRequest();
                break;
            case R.id.btn_refresh:
                permissionRequest();
                break;
            case R.id.btn_previous://上一曲
                if(modeIsSeqFlag)
                    changeMusic(--mCurrentPosition);//当前歌曲位置减1
                else
                    changeMusic(random.nextInt(mList.size()));
                break;
            case R.id.btn_play_or_pause://播放或者暂停
                // 首次点击播放按钮，默认播放第0首，下标从0开始
                if (mediaPlayer == null) {
                    changeMusic(0);
                } else {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_pause));
                        playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_play_state));

                    } else {
                        mediaPlayer.start();
                        btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_play));
                        playStateImg.setBackground(getResources().getDrawable(R.mipmap.list_pause_state));
                    }
                }
                break;
            case R.id.btn_play_mode://切换随机播放还是顺序播放
                if(modeIsSeqFlag){
                    btnPlayMode.setBackground(getResources().getDrawable(R.mipmap.icon_random_play));
                    modeIsSeqFlag=false;
                }else{
                    btnPlayMode.setBackground(getResources().getDrawable(R.mipmap.icon_list_cycle));
                    modeIsSeqFlag=true;
                }
                break;
            case R.id.btn_next://下一曲
                if (modeIsSeqFlag)
                    changeMusic(++mCurrentPosition);//当前歌曲位置加1
                else
                    changeMusic(random.nextInt(mList.size()));
                break;
        }
    }

    //切歌
    private void changeMusic(int position) {
        Log.e("MainActivity", "position:" + position);
        if (position < 0) {
            mCurrentPosition = position = mList.size() - 1;
            Log.e("MainActivity", "mList.size:" + mList.size());
        } else if (position > mList.size() - 1) {
            mCurrentPosition = position = 0;
        }
        Log.e("MainActivity", "position:" + position);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);//监听音乐播放完毕事件，自动下一曲
        }

        try {
            // 切歌之前先重置，释放掉之前的资源
            mediaPlayer.reset();
            // 设置播放源
            Log.d("Music", mList.get(position).path);
            mediaPlayer.setDataSource(mList.get(position).path);
            tvPlaySongInfo.setText("歌名： " + mList.get(position).song +
                    "  歌手： " + mList.get(position).singer);
            tvPlaySongInfo.setSelected(true);//跑马灯效果
            playStateLay.setVisibility(View.VISIBLE);
            // 开始播放前的准备工作，加载多媒体资源，获取相关信息
            mediaPlayer.prepare();
            // 开始播放
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 切歌时重置进度条并展示歌曲时长
        timeSeekBar.setProgress(0);
        timeSeekBar.setMax(mediaPlayer.getDuration());
        tvTotalTime.setText(parseTime(mediaPlayer.getDuration()));

        updateProgress();
        if (mediaPlayer.isPlaying()) {
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_play));

        } else {
            btnPlayOrPause.setBackground(getResources().getDrawable(R.mipmap.icon_pause));

        }
    }

    private void updateProgress() {
        // 使用Handler每间隔1s发送一次空消息，通知进度条更新
        Message msg = Message.obtain();// 获取一个现成的消息
        // 使用MediaPlayer获取当前播放时间除以总时间的进度
        int progress = mediaPlayer.getCurrentPosition();
        msg.arg1 = progress;
        mHandler.sendMessageDelayed(msg, INTERNAL_TIME);
    }

    //滑动条监听
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        // 当手停止拖拽进度条时执行该方法
        // 获取拖拽进度
        // 将进度对应设置给MediaPlayer
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            mediaPlayer.seekTo(progress);
        }
    };

    //播放完成之后自动下一曲
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (modeIsSeqFlag)
        changeMusic(++mCurrentPosition);
        else
            changeMusic(random.nextInt(mList.size()));
    }
    public  String parseTime(int oldTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");// 时间格式
        String newTime = sdf.format(new Date(oldTime));
        return newTime;
    }
    public static void putString(String key, String value, Context context) {
        SharedPreferences sp = context.getSharedPreferences("config",
                Context.MODE_PRIVATE);
        sp.edit().putString(key, value).commit();
    }

    public static String getString(String key, String defValue, Context context) {
        if(context!=null){
            SharedPreferences sp = context.getSharedPreferences("config",
                    Context.MODE_PRIVATE);
            return sp.getString(key, defValue);
        }
        return "";

    }
}
