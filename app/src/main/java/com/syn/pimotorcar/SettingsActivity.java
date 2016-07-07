package com.syn.pimotorcar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.util.EncodingUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SettingsActivity extends AppCompatActivity {
    final private static String settings_filename = "pimotorcar.cfg";
    private JSONObject settings = new JSONObject();
    private EditText host_ip;
    private EditText host_port;
    private WebSocketClient ws_client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化界面元素
        InitView();

        // 载入配置参数
        try {
            String str_settings = readFile(settings_filename);
            JSONObject tmp_setting = new JSONObject(str_settings);
            settings.put("host_ip", tmp_setting.getString("host_ip"));
            settings.put("host_port", tmp_setting.getString("host_port"));

            host_ip.setText(settings.getString("host_ip"));
            host_port.setText(settings.getString("host_port"));

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    // 初始化界面元素
    private void InitView() {
        host_ip = (EditText) findViewById(R.id.edit_ipaddress);
        host_port = (EditText) findViewById(R.id.edit_port);

        Button btn_test_connection = (Button) findViewById(R.id.btn_connect);
        btn_test_connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String str_host = "ws://" + settings.getString("host_ip") + ":" + settings.getString("host_port");
                    ws_client = new WebSocketClient(new URI(str_host)) {
                        @Override
                        public void onOpen(ServerHandshake handshakedata) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "连接成功!", Toast.LENGTH_SHORT).show();
                                    ws_client.close();
                                }
                            });
                        }

                        @Override
                        public void onMessage(String message) {

                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {

                        }

                        @Override
                        public void onError(Exception ex) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "连接失败!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    };
                } catch (JSONException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn_setting_save = (Button) findViewById(R.id.btn_save);
        btn_setting_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str_host_ip = host_ip.getText().toString();
                String str_host_port = host_port.getText().toString();

                if ( str_host_ip.equals("") || str_host_port.equals("") ) {
                    Toast.makeText(getApplicationContext(), "请配置服务端IP，端口!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 保存配置到文件，文件内容json格式
                try {
                    settings.put("host_ip", str_host_ip);
                    settings.put("host_port", str_host_port);

                    String str_settings = settings.toString();
                    Log.e("CONFIG", str_settings);
                    writeFile(settings_filename, str_settings);
                    Toast.makeText(getApplicationContext(), "配置保存成功!", Toast.LENGTH_SHORT).show();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //读写/data/data/<应用程序名>目录上的文件:
    //写数据
    public void writeFile(String fileName, String writestr) throws IOException {
        try
        {
            FileOutputStream file_out = openFileOutput(fileName, MODE_PRIVATE);
            byte[] bytes = writestr.getBytes();
            file_out.write(bytes);
            file_out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

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
}
