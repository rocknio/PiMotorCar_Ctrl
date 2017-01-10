package com.syn.pimotorcar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.util.EncodingUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MotorCarActivity extends AppCompatActivity {
    final private static String settings_filename = "pimotorcar.cfg";
    private WebSocketClient ws_client;
    private JSONObject settings = new JSONObject();
    private RadioButton rb_forward;
    private RadioButton rb_astern;
    private ToggleButton btn_start_stop;
    private VideoView camera_video;

    //读数据
    public String readFile(String fileName) throws IOException {
        String res = "";
        try
        {
            FileInputStream fin = openFileInput(fileName);
            int length = fin.available();
            byte[] buffer = new byte[length];
            int read_result = fin.read(buffer);
            if ( read_result > 0 )
            {
                res = EncodingUtils.getString(buffer, "UTF-8");
            }
            fin.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return res;
    }

    private void refresh_settings() {
        // 载入配置参数
        try {
            String str_settings = readFile(settings_filename);
            JSONObject tmp_setting = new JSONObject(str_settings);
            settings.put("host_ip", tmp_setting.getString("host_ip"));
            settings.put("host_port", tmp_setting.getString("host_port"));
            settings.put("camera_uri", tmp_setting.getString("camera_uri"));
            Log.i("Config", str_settings);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LibsChecker.checkVitamioLibs(this))
            return;

        setContentView(R.layout.activity_motor_car);

        // 初始化界面元素
        InitView();

        refresh_settings();
    }

    private void sendFollowCommand() {
        if ( rb_forward.isChecked() ) {
            Log.i("CMD", "forward");
            sendCommand("forward");
        }
        else if ( rb_astern.isChecked()) {
            Log.i("CMD", "astern");
            sendCommand("astern");
        }
    }

    private void sendCommand(String cmd) {
        if ( ws_client == null )
        {
            return;
        }

        if ( ws_client.getReadyState() == WebSocket.READYSTATE.OPEN ) {
            Log.i("CMD", "send websocket command:" + cmd);
            ws_client.send(cmd);
        }
    }

    private void startGetCameraVideo() {
        String path;
        try {
            path = settings.getString("camera_uri");
        } catch (JSONException e) {
            e.printStackTrace();
            path = "";
        }

        if (!path.equals("")) {
            camera_video.setVideoPath(path);
            camera_video.setMediaController(new MediaController(this));
            camera_video.requestFocus();

            // 设置播放速率，自动播放
            camera_video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setPlaybackSpeed(1.0f);
                    mp.start();
                }
            });
        }
    }

    private void InitView()
    {
        camera_video = (VideoView) findViewById(R.id.wv_camera);

        rb_forward = (RadioButton) findViewById(R.id.rb_forward);
        rb_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("CMD", "forward");
                sendCommand("forward");
            }
        });

        rb_astern = (RadioButton) findViewById(R.id.rb_astern);
        rb_astern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("CMD", "astern");
                sendCommand("astern");
            }
        });

        // 左转弯
        Button btn_turn_left = (Button) findViewById(R.id.btn_turnleft);
        btn_turn_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) { // 按下
                    Log.i("CMD", "turn_left");
                    sendCommand("turn_left");
                } else if (action == MotionEvent.ACTION_UP) { // 松开
                    Log.i("CMD", "stop_left");
                    sendCommand("stop_left");
                    sendFollowCommand();
                }
                return false;
            }
        });

        // 右转弯
        Button btn_turn_right = (Button) findViewById(R.id.btn_turnright);
        btn_turn_right.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) { // 按下
                    Log.i("CMD", "turn_right");
                    sendCommand("turn_right");
                } else if (action == MotionEvent.ACTION_UP) { // 松开
                    Log.i("CMD", "stop_right");
                    sendCommand("stop_right");
                    sendFollowCommand();
                }
                return false;
            }
        });

        // 启动、停止
        btn_start_stop = (ToggleButton) findViewById(R.id.toggleButton);
        btn_start_stop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if ( b ){
                    try {
                        // 连接控制端
                        String str_host = "ws://" + settings.getString("host_ip") + ":" + settings.getString("host_port") + "/ws";
                        ws_client = new WebSocketClient(new URI(str_host)) {
                            @Override
                            public void onOpen(ServerHandshake handshakedata) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i("WebSocket", "connected!");
                                        Toast.makeText(getApplicationContext(), "连接成功!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                sendFollowCommand();

                                // 控制端链接成功，播放视频
                                startGetCameraVideo();
                            }

                            @Override
                            public void onMessage(final String message) {
                                Log.i("WebSocket", "Recv = " + message);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String msg = "Recv = " + message;
                                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                Log.i("WebSocket", "disconnected!");
                                ws_client.close();

                                // 断开控制端链接，停止视频播放
                                if (camera_video.isPlaying()) {
                                    camera_video.stopPlayback();
                                }
                            }

                            @Override
                            public void onError(Exception ex) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "连接失败!", Toast.LENGTH_SHORT).show();
                                        btn_start_stop.setChecked(false);
                                    }
                                });

                                // 控制端链接异常，停止视频播放
                                if (camera_video.isPlaying()) {
                                    camera_video.stopPlayback();
                                }
                            }
                        };

                        ws_client.connect();
                    } catch (JSONException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    if (ws_client.getReadyState() == WebSocket.READYSTATE.OPEN ) {
                        sendCommand("stop_all");
                        ws_client.close();
                    }

                    // 主动断开连接，停止播放视频
                    if (camera_video.isPlaying()) {
                        camera_video.stopPlayback();
                    }
                }
            }
        });

        // 设置
        Button btn_setting = (Button) findViewById(R.id.btn_settings);
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 打开设置activity
                Intent settings_intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settings_intent);
            }
        });
    }
}
