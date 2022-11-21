package cn.dolphinstar.demo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.dolphinstar.demo.toolkit.QRHelper;
import cn.dolphinstar.lib.player.core.MYOUPlayer;
import cn.dolphinstar.lib.player.core.StartUpCfg;
import cn.dolphinstar.lib.wozkit.NetHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends Activity {

    //二维码图片
    private ImageView qrImageView;
    //二维码文件路径
    private String qrImagePath;

    //wifi名称
    private String wifiName;
    //名称
    private String playerName;

    @SuppressLint({"CheckResult", "ShowToast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrImageView = findViewById(R.id.iv_qrcode);
        qrImagePath = newQRCodeFileName();

        if (Build.VERSION.SDK_INT >= 23) {
            checkAndRequestPermission();
        } else {
            appStartUp();
        }
    }

    @SuppressLint({"CheckResult", "ShowToast", "SetTextI18n"})
    private void appStartUp() {

        NetHelper netHelper = new NetHelper(getApplicationContext());
        int netType = netHelper.getConnectedType();
        if (netType == -1) {
            wifiName = "未连接网络!";
            Toast.makeText(getApplicationContext(), "未连接网络，投屏服务未启动!", Toast.LENGTH_LONG);
        } else {
            if (netType == 1) {
                wifiName = netHelper.WifiName();
            } else {
                wifiName = "有线网络";
            }
            ((TextView)findViewById(R.id.tv_wifi)).setText("网络:"+wifiName);
            playerName="海豚星空TV-" +(int) (Math.random() * 900 + 100);

            //应该在启动配置
            StartUpCfg cfg = new StartUpCfg();
            cfg.PlayerName = playerName;
            cfg.IsShowLogger = BuildConfig.DEBUG;
            cfg.AppSecret = "";  //应用的Secret
            //启动服务
            MYOUPlayer.of(MainActivity.this)
                    .StartService(cfg)
                    .subscribe(s -> {
                        //投屏服务启动成功
                        Log.e("MainActivity","投屏服务启动成功");
                        onSuccess();
                    }, e -> {
                        //投屏服务启动失败
                        String msg = "投屏服务启动失败: "+ e.getMessage();
                        ((TextView)findViewById(R.id.tv_cast_code)).setText(msg);
                        Log.e("MainActivity",msg);
                    });
        }
    }

    @SuppressLint("CheckResult")
    private void onSuccess() {
        //获取二维码链接并显示
        buildQRcode(MYOUPlayer.of(MainActivity.this).GetQrUrl());

        //获取投屏码并显示
        MYOUPlayer.of(MainActivity.this)
                .GetScreenCode()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(code -> {
                    ((TextView)findViewById(R.id.tv_cast_code)).setText("投屏码:"+code);
                });
        //playerName = MYOUPlayer.of(MainActivity.this).getMediaRenderName();
        ((TextView)findViewById(R.id.tv_name)).setText(playerName);

    }

    protected void onDestroy() {

        MYOUPlayer.of(MainActivity.this).Close();

        //删除二维码文件
        File file = new File(qrImagePath);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        super.onDestroy();
    }


    //region 处理二维码部分
    //创建二维码把并显示到图片上
    private void buildQRcode(String url) {
        final String text = url;

        LinearLayout line = findViewById(R.id.layout_linear);
        int ih = line.getHeight() * 2 / 3;

        boolean success = new QRHelper().BuildQRCode(text, ih, ih, qrImagePath);
        if (success) {
            runOnUiThread(() -> qrImageView.setImageBitmap(BitmapFactory.decodeFile(qrImagePath)));
        }
    }

    //获取本次二维码文件路径
    private String newQRCodeFileName() {
        String rootPath = getApplicationContext().getFilesDir().getAbsolutePath();
        String fileName = rootPath + File.separator
                + "qr_" + System.currentTimeMillis() + ".jpg";
        return fileName;
    }

    //endregion

    //region 动态权限申请

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission() {

        List<String> lackedPermission = new ArrayList<>();

        if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // 如果需要的权限都已经有了，那么直接调用SDK
        if (lackedPermission.size() == 0) {
            appStartUp();
        } else {
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            requestPermissions(requestPermissions, 1024);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1024) {
            appStartUp();
        }
    }

    //endregion



}
