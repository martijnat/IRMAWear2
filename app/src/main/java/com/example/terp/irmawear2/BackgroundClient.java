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


public class BackgroundClient  extends AsyncTask<String, Void, String> {

    public String message = "";
    private MainActivity mMainActivity;

    public BackgroundClient(MainActivity m) {
        mMainActivity = m;
    }

    protected String doInBackground(String... arguments) {
        int PORT = 8080;
        String message = "";
        String line = "";
        String str = "";
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));

            // listen for incoming clients
            Socket socket = serverSocket.accept();


            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            str = in.readLine();
            out.write(str);
            out.flush();

            out.close();
            in.close();
            socket.close();
            serverSocket.close();

            return str;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return message;
    }

    protected void onPostExecute(String result) {
        mMainActivity.WifiInput(result);
    }
}
