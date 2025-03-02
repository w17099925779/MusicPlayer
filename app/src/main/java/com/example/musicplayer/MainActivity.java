package com.example.musicplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 权限请求码
    private static final int REQUEST_PERMISSION_CODE = 1;
    
    // 播放器相关
    private MediaPlayer mediaPlayer;
    private MusicItem currentMusic;
    private final List<MusicItem> musicList = new ArrayList<>();
    private MusicAdapter adapter;
    
    // UI组件
    private SeekBar seekBar;
    private TextView currentTime;
    private TextView totalTime;
    private TextView currentTrack;
    
    // 控制逻辑
    private final Handler handler = new Handler();
    private boolean isLooping = false;
    private boolean isSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化所有视图组件
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        currentTrack = findViewById(R.id.currentTrack);
        Button loopButton = findViewById(R.id.loopButton);

        // 配置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MusicAdapter(this, musicList, new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MusicItem item) {
                currentTrack.setVisibility(View.VISIBLE);
                currentTrack.setText("正在播放: " + item.getTitle());
                playMusic(item);
            }
        });
        recyclerView.setAdapter(adapter);

        // 播放按钮事件
        findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }
        });

        // 暂停按钮事件
        findViewById(R.id.pauseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
        });

        // 停止按钮事件
        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMusic();
                currentTrack.setVisibility(View.GONE);
                adapter.setSelectedPosition(-1);
            }
        });

        // 循环模式切换
        loopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLooping = !isLooping;
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(isLooping);
                }
                ((Button)v).setText(isLooping ? "单曲循环" : "单次播放");
            }
        });

        // 进度条控制
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
                handler.removeCallbacks(updateProgress);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    handler.post(updateProgress);
                }
            }
        });

        checkPermission(); // 权限检查
    }

    // 权限检查方法
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE
            );
        } else {
            loadMusic();
        }
    }

    // 权限回调处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic();
            } else {
                Toast.makeText(this, "需要存储权限才能扫描音乐文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 加载音乐文件
    private void loadMusic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                musicList.clear();
                String[] projection = {
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA
                };

                android.database.Cursor cursor = getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        musicList.add(new MusicItem(
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2))
                        );
                    }
                    cursor.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    // 播放音乐核心方法
    private void playMusic(MusicItem item) {
        try {
            stopMusic(); // 停止当前播放
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(item.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentMusic = item;

            // 更新选中状态
            int position = musicList.indexOf(item);
            adapter.setSelectedPosition(position);

            // 初始化进度条
            seekBar.setMax(mediaPlayer.getDuration());
            totalTime.setText(formatTime(mediaPlayer.getDuration()));
            handler.post(updateProgress);

            // 播放完成监听
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!isLooping) {
                        stopMusic();
                        currentTrack.setVisibility(View.GONE);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 进度更新任务
    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && !isSeeking) {
                int current = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(current);
                currentTime.setText(formatTime(current));
                handler.postDelayed(this, 1000);
            }
        }
    };

    // 时间格式化方法
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 停止播放
    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            handler.removeCallbacks(updateProgress);
            seekBar.setProgress(0);
            currentTime.setText("00:00");
        }
    }

    // 生命周期管理
    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateProgress);
        stopMusic();
        super.onDestroy();
    }
}