package com.example.terp.irmawear2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import android.bluetooth.*;

import static android.content.ContentValues.TAG;


public class AsyncQRUpdate  extends AsyncTask<String, Void, String> {

    private MainActivity mMainActivity;

    public AsyncQRUpdate(MainActivity m) {
        mMainActivity = m;
    }

    protected String doInBackground(String... arguments) {
        return "";
    }

    protected void onPostExecute(String result) {
    }
}
