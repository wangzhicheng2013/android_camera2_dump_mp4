package com.example.camera2dumpmp4;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "MainActivity";
    private TextureView mTextureView;
    private BasicCamera2 mBasicCamera2;
    private Button mQuitButton;
    private Button mDumpMP4Button;
    public static boolean mStartDumpMP4 = false;
    public static String mOutputPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (false == checkPermissions()) {
            return;
        }
        initCamera2();
    }
    private void initView() {
        mTextureView = (TextureView) findViewById(R.id.tv_camera);
        mQuitButton = findViewById(R.id.quit_btn);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                System.exit(0);
            }
        });
        mDumpMP4Button = findViewById(R.id.dump_mp4_btn);
        mDumpMP4Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (false == mStartDumpMP4) {
                    mStartDumpMP4 = true;
                    mDumpMP4Button.setText("停止录制");
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    mOutputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/" + timeStamp + "_privacymosaic.mp4";
                    Toast.makeText(view.getContext(), "MP4文件是:" + mOutputPath, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "mp4:" + mOutputPath);
                }
                else {
                    mStartDumpMP4 = false;
                    mDumpMP4Button.setText("开始录制");
                    mOutputPath = "";
                }
            }
        });
    }
    private void initCamera2() {
        mBasicCamera2 = new BasicCamera2(this, mTextureView);
    }
    private void requestCameraPermission() {
        requestPermissions(new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CAMERA_PERMISSION);
    }
    public boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请授予相机权限！", Toast.LENGTH_SHORT).show();
            } else {
                initCamera2();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mBasicCamera2.initCamera("1");
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }
}