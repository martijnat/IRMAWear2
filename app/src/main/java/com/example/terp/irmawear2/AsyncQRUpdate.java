package com.example.terp.irmawear2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import static android.content.ContentValues.TAG;
import static android.content.Context.WIFI_SERVICE;
import static com.example.terp.irmawear2.MainActivity.PORT;


public class AsyncQRUpdate  extends AsyncTask<String, Void, String> {

    private MainActivity mMainActivity;
    public String qrtext;
    public Bitmap bitmap;

    public AsyncQRUpdate(MainActivity m) {
        mMainActivity = m;
    }

    public String MyIPAdress()
    {
        WifiManager wm = (WifiManager) mMainActivity.getApplicationContext().getSystemService(WIFI_SERVICE);
        int ip = wm.getConnectionInfo().getIpAddress();

        int ipbyte1 = (ip & (0xff <<  0)) >>  0;
        int ipbyte2 = (ip & (0xff <<  8)) >>  8;
        int ipbyte3 = (ip & (0xff << 16)) >> 16;
        int ipbyte4 = (ip & (0xff << 24)) >> 24;
        return ipbyte1 + "." + ipbyte2 + "." + ipbyte3 + "." + ipbyte4;
    }

    protected String doInBackground(String... arguments) {
        if (mMainActivity.QRcreated)
        {
            qrtext = mMainActivity.qrcode;
            return "QR already generated";
        }
        Log.i(TAG, "started async QR");
        qrtext = MyIPAdress() + " " + Integer.toString(PORT) + "\n\n\n";
        Log.i(TAG, "QR: generated ip string");
        BitMatrix result;
        bitmap=null;
        int qrsize = 512;
        try {
            result = new MultiFormatWriter().encode(qrtext, BarcodeFormat.QR_CODE, qrsize, qrsize, null);
            int w = result.getWidth();
            int h = result.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = result.get(x, y) ?
                                         0xff172C73:
                                         0x00ffffff;
                }
            }
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
            bitmap.setPixels(pixels, 0, qrsize, 0, 0, w, h);
            Log.i(TAG, "QR: generated matrix");
        } catch (WriterException e) {
            e.printStackTrace();
            Log.i(TAG, "Exception: " + e.toString());
        }
        Log.i(TAG, "QR: Finished");
        return "Finished";
    }

    protected void onPostExecute(String result) {
        if (mMainActivity.QRcreated)
        {
            return;
        }
        mMainActivity.QRcreated = true;
        mMainActivity.qrcode = qrtext;
        Log.i(TAG, "QR: F1");
        mMainActivity.LogUI(qrtext);
        Log.i(TAG, "QR: F2");
        mMainActivity.SetQRsubtext(qrtext);
        Log.i(TAG, "QR: F3");
        mMainActivity.SetQRImage(bitmap);
        Log.i(TAG, "QR: F4");
    }
}
