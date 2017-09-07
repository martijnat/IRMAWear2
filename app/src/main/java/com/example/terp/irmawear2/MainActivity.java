package com.example.terp.irmawear2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.irmacard.api.common.IrmaQr;
import org.irmacard.api.common.util.GsonUtil;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class MainActivity extends WearableActivity {


    public TextView mStatus;
    public ProgressBar mProgressbar;
    public Button mButton;

    private BackgroundClient mBackgroundClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mStatus = (TextView) findViewById(R.id.status);
        mProgressbar = (ProgressBar) findViewById(R.id.progressBar);
        mButton = (Button) findViewById(R.id.connectButton);

    }

    public void ClickConnect(View view) {
        mProgressbar.setVisibility(View.VISIBLE);
        mButton.setVisibility(View.GONE);
        mStatus.setText("Waiting ...");
        mBackgroundClient = new BackgroundClient(MainActivity.this);
        mBackgroundClient.execute();
        //Toast.makeText(MainActivity.this, "Pressed (?) test", Toast.LENGTH_SHORT).show();
    }

    public void ResetConnect(){
        mProgressbar.setVisibility(View.GONE);
        mButton.setVisibility(View.VISIBLE);
        //        mProgressbar.setVisibility(View.VISIBLE);
//        mButton.setVisibility(View.GONE);
//        mBackgroundClient = new BackgroundClient(MainActivity.this);
//        mBackgroundClient.execute();
    }

    public String WifiInput(String result)
    {
        mStatus.setText(result);

        try {
            JSONObject jObject = new JSONObject(result);
            String qrUrl       = jObject.getString("u");
            String qrVersion   = jObject.getString("v");
            String qrVersionMax= jObject.getString("vmax");
            String qrType      = jObject.getString("irmaqr");
            mStatus.setText(qrUrl + "\n---\n" + qrVersion + "\n---\n" + qrVersionMax +"\n---\n"+qrType);

            return "Received: " + result + "\n";
        } catch(Exception e) {
            return e.toString();
        }



    }

}
