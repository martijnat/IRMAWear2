package com.example.terp.irmawear2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// import org.irmacard.api.common.IrmaQr;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.encoder.QRCode;

import org.irmacard.api.common.IrmaQr;
import org.irmacard.api.common.SchemeManagerQr;
import org.irmacard.api.common.exceptions.ApiErrorMessage;
import org.irmacard.api.common.util.GsonUtil;
import org.irmacard.cardemu.ExpandableCredentialsAdapter;
import org.irmacard.cardemu.IRMApp;
import org.irmacard.cardemu.SecureSSLSocketFactory;
import org.irmacard.cardemu.credentialdetails.CredentialDetailActivity;
import org.irmacard.cardemu.credentialdetails.CredentialDetailFragment;
import org.irmacard.cardemu.identifiers.IdemixCredentialIdentifier;
import org.irmacard.cardemu.irmaclient.IrmaClient;
import org.irmacard.cardemu.irmaclient.IrmaClientHandler;
import org.irmacard.cardemu.preferences.IRMAPreferenceActivity;
import org.irmacard.cardemu.store.AndroidFileReader;
import org.irmacard.cardemu.store.CredentialManager;
import org.irmacard.cardemu.store.SchemeManagerHandler;
import org.irmacard.cardemu.store.StoreManager;
import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.info.IdemixKeyStore;
import org.irmacard.credentials.idemix.info.IdemixKeyStoreDeserializer;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.DescriptionStoreDeserializer;
import org.irmacard.credentials.info.FileReader;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.credentials.info.SchemeManager;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.net.ssl.SSLSocketFactory;

import static com.example.terp.irmawear2.R.color.black;
import static com.example.terp.irmawear2.R.color.white;
import static com.example.terp.irmawear2.R.id.qrdisplay;

public class MainActivity extends WearableActivity {


    public TextView mStatus;
    public TextView mError;
    public TextView mLog;
    public Button mButton;
    public static int PORT = 9090;
    public Boolean QRcreated = false;
    public String qrcode = "";
    private BackgroundClient mBackgroundClient;
    private AsyncQRUpdate mQRupdater;

    public static Boolean DEBUGUI = true;

    // variable from android app
    private static final String TAG = "SmartWatchMainActivity";
    private static final String SETTINGS = "cardemu";
    public static final int PERMISSION_REQUEST_CAMERA = 1;

    /**
     * {@link MainActivity} UI states
     */
    enum State {
        LOADING(true),
        CREDENTIALS_LOADED(true),
        DESCRIPTION_STORE_LOADED(true),
        KEY_STORE_LOADED(true),
        IDLE(false),
        CONNECTED(false),
        READY(false),
        COMMUNICATING(false);

        private boolean booting;
        State(boolean booting) {
            this.booting = booting;
        }
        /** Whether this state is still part of the app boot process */
        public boolean isBooting() {
            return booting;
        }
    }

    private State state = State.LOADING;

    private SharedPreferences settings;

    // Previewed list of credentials
    private ExpandableCredentialsAdapter credentialListAdapter;

    // Timer for briefly displaying feedback messages on CardEmu
    private CountDownTimer cdt;
    private static final int FEEDBACK_SHOW_DELAY = 10000;
    private boolean showingFeedback = false;

    // Keep track of last verification url to ensure we handle it only once
    private String currentSessionUrl = "()";
    private boolean launchedFromBrowser;
    private boolean onlineEnrolling;

    // Keep track of how far we are in the app boot process
    private boolean credentialsLoaded = false;
    private boolean descriptionStoreLoaded = false;
    private boolean keyStoreLoaded = false;

    private IrmaClientHandler irmaClientHandler = new ClientHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();


        mStatus = findViewById(R.id.status);
        mError = findViewById(R.id.error);
        mLog = findViewById(R.id.log);
        mButton = findViewById(R.id.connectButton);


        mLog.setVisibility(DEBUGUI?View.VISIBLE:View.GONE);

        super.onCreate(savedInstanceState);

//        // Disable screenshots if we should
//        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("allow_screenshots", false))
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        settings = getSharedPreferences(SETTINGS, 0);

        ExpandableListView credentialList = (ExpandableListView) findViewById(R.id.listView);
        credentialListAdapter = new ExpandableCredentialsAdapter(this);
        credentialList.setAdapter(credentialListAdapter);

        new CredentialsLoader().execute();
        new StoreLoader().execute();
    }

    public void ExitApp(View view)
    {
        System.exit(0);
    }

    public void DisplayQR()
    {
        findViewById(R.id.qrlayout).setVisibility(View.VISIBLE);
        findViewById(R.id.normallayout).setVisibility(View.GONE);
        MyUpdateQR();
    }

    public void MyUpdateQR()
    {
        mQRupdater = new AsyncQRUpdate(MainActivity.this);
        mQRupdater.execute();
    }

    public void SetQRsubtext(String qrtext)
    {
        TextView qrsubtext = findViewById(R.id.qrdusplaysubtext);
        qrsubtext.setText(qrtext);
    }

    public void SetQRImage(Bitmap bitmap)
    {
        ImageView qrdisplay = findViewById(R.id.qrdisplay);
        qrdisplay.setImageBitmap(bitmap);
    }

    public void ClickConnect(View view) {
        mButton.setVisibility(View.INVISIBLE);

        SetError("");
        SetStatus("Switching to QR display");
        DisplayQR();
//        start listening on socket.
        mBackgroundClient = new BackgroundClient(MainActivity.this);
        mBackgroundClient.execute();

    }

    public void QuitConnect(View view)
    {
        if (mBackgroundClient != null)
        {
            mBackgroundClient.cancel(true);
        }
        if (mQRupdater != null)
        {
            mQRupdater.cancel(true);
        }
        findViewById(R.id.qrlayout).setVisibility(View.GONE);
        findViewById(R.id.normallayout).setVisibility(View.VISIBLE);
        SetStatus("Canceled QR Display");
        mButton.setVisibility(View.VISIBLE);
    }

    public void LogUI(String str) {
        String sep = "\n";
        mLog.setText(mLog.getText()+sep + str);

    }
    public void SetStatus(String str) {
        mStatus.setText(str);

    }

    public void SetError(String str) {
        if (str.length()>0) {
            mError.setText(str);
            mError.setVisibility(View.VISIBLE);
        }
        else {
            mError.setVisibility(View.GONE);
        }

    }

    public String WifiInput(String result)
    {
        findViewById(R.id.qrlayout).setVisibility(View.GONE);
        findViewById(R.id.normallayout).setVisibility(View.VISIBLE);
        SetStatus("Processing input ...");

        try {
            LogUI("Processing input as JSON");
            JSONObject jObject = new JSONObject(result);
            LogUI("Creating new IrmaClient Object");

            LogUI("Turning on WiFi");
            WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);

            new IrmaClient(result, irmaClientHandler);
        } catch(Exception e) {
            LogUI("Exception while processing input in WifiInput()\n" + e.toString());
            SetStatus(e.toString());
        }

        LogUI("Finished processing WifiInput()\n");

        return "Received: " + result + "\n";




    }
    // Classes and functions copied from android app
    private void showErrorDialog(final String title, final String message, final String techInfo) {
        showErrorDialog(title, message, techInfo, false);
    }

    private void showErrorDialog(final String title, final String message,
                                 final String techInfo, final boolean showingTechInfo) {
        LogUI("showErrorDialog: [title] " + title);
        LogUI("showErrorDialog: [message] " + message);
        LogUI("showErrorDialog: [techInfo] " + techInfo);
        LogUI("showErrorDialog: [showingTechInfo] " + Boolean.toString(showingTechInfo));
        SetError(title + ": " + message + "\n" + techInfo);
    }

    private State getState() {
        return state;
    }

    private void setState(State state) {
        Log.i(TAG, "Set state: " + state);
        this.state = state;

        switch (state) {
        case CREDENTIALS_LOADED:
            credentialsLoaded = true;
            break;
        case DESCRIPTION_STORE_LOADED:
            descriptionStoreLoaded = true;
            break;
        case KEY_STORE_LOADED:
            keyStoreLoaded = true;
            break;
        }

        updateCredentialList();
        setUIForState();

        if (state.isBooting() && credentialsLoaded && descriptionStoreLoaded && keyStoreLoaded) {
            setState(State.IDLE);
        }
    }

    private void setUIForState() {
        int imageResource = 0;
        int statusTextResource = 0;

        switch (getState()) {
        case LOADING:
        case CREDENTIALS_LOADED:
        case DESCRIPTION_STORE_LOADED:
        case KEY_STORE_LOADED:
            imageResource = R.drawable.irma_icon_place_card_520px;
            statusTextResource = R.string.loading;
            LogUI("statusTextResource = R.string.loading;");
            SetStatus("Key Store Loaded");
            break;
        case IDLE:
            imageResource = R.drawable.irma_icon_place_card_520px;
            statusTextResource = R.string.status_idle;
            LogUI("statusTextResource = R.string.status_idle;");
            SetStatus("");
            mButton.setVisibility(View.VISIBLE);
            break;
        case CONNECTED:
            imageResource = R.drawable.irma_icon_place_card_520px;
            statusTextResource = R.string.status_connected;
            LogUI("statusTextResource = R.string.status_connected;");
            SetStatus("Connected");
            break;
        case READY:
            imageResource = R.drawable.irma_icon_card_found_520px;
            statusTextResource = R.string.status_ready;
            LogUI("statusTextResource = R.string.status_ready;");
            SetStatus("Ready");
            mButton.setVisibility(View.VISIBLE);
            break;
        case COMMUNICATING:
            imageResource = R.drawable.irma_icon_card_found_520px;
            statusTextResource = R.string.status_communicating;
            LogUI("statusTextResource = R.string.status_communicating;");
            SetStatus("Communicating ...");
            break;
        default:
            break;
        }

        // ((TextView) findViewById(R.id.status_text)).setText(statusTextResource);
        LogUI("Line 294: "+Integer.toString(statusTextResource));
        // if (!showingFeedback)
        // 	((ImageView) findViewById(R.id.statusimage)).setImageResource(imageResource);
    }

    public void setFeedback(String message, String state) {
        setUIForState();
        LogUI(state + ":" + message);
    }

    private void clearFeedback() {
        LogUI("Line 338: clearfeedback()");
        SetStatus("");
        setUIForState();
    }

    protected void deleteAllCredentials(View v) {
        if (getState() != State.IDLE)
            return;
        LogUI("Deleting credentials (Without confirmation)");
        CredentialManager.deleteAll();
        updateCredentialList();
    }

    protected void tryDeleteCredential(int hashCode) {
        if (getState() != State.IDLE) {
            Log.i(TAG, "Delete long-click ignored in non-idle mode");
            return;
        }

        IdemixCredentialIdentifier ici = CredentialManager.findCredential(hashCode);
        if (ici == null)
            return;

        Log.w(TAG, "Deleting credential " + ici.toString());
        CredentialManager.delete(ici);
        updateCredentialList();
    }

    /**
     * Update the list of credentials. (Note: this method does nothing if the activity is not in
     * an appropriate state.)
     */
    protected void updateCredentialList() {
        updateCredentialList(true);
    }

    /**
     * Update the list of credentials if the activity is in the appropriate state
     * @param tryDownloading Whether to update the description store and keystore in advance
     */
    protected void updateCredentialList(boolean tryDownloading) {
        if (!credentialsLoaded || !descriptionStoreLoaded
                || (!getState().isBooting() && getState() != State.IDLE))
            return;

        if (tryDownloading) {
            CredentialManager.updateStores(new StoreManager.DownloadHandler() {
                @Override public void onSuccess() {
                    updateCredentialList(false);
                }
                @Override public void onError(Exception e) {
                    setFeedback(getString(R.string.downloading_credential_info_failed), "warning");
                    updateCredentialList(false);
                }
            });
        }

        LinkedHashMap<IdemixCredentialIdentifier, Attributes> credentials = CredentialManager.getAllAttributes();
        credentialListAdapter.updateData(credentials);

        TextView noCredsText = (TextView) findViewById(R.id.no_credentials_text);
        int visibility = credentials.isEmpty() ? View.VISIBLE : View.INVISIBLE;

        if (noCredsText != null)
            noCredsText.setVisibility(visibility);
    }


    // @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CredentialDetailActivity.ACTIVITY_CODE && resultCode == CredentialDetailActivity.RESULT_DELETE) {
            int hashCode = data.getIntExtra(CredentialDetailActivity.ARG_RESULT_DELETE, 0);
            if (hashCode != 0)
                tryDeleteCredential(hashCode);
        }

        else if (requestCode == IRMAPreferenceActivity.ACTIVITY_CODE) {
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("allow_screenshots", false))
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            else
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        else { // Must be from the QR scanner
            IntentResult scanResult = IntentIntegrator
                                      .parseActivityResult(requestCode, resultCode, data);

            // Process the results from the QR-scanning activity
            if (scanResult == null)
                return;
            String contents = scanResult.getContents();
            if (contents == null)
                return;

            IrmaQr qr;
            try {
                qr = GsonUtil.getGson().fromJson(contents, IrmaQr.class);
            } catch(Exception e) {
                irmaClientHandler.onFailure(IrmaClient.Action.UNKNOWN, "Not an IRMA session", null, "Content: " + contents);
                return;
            }

            String qrType = qr.getType();
            if (qrType == null) qrType = "";
            switch (qrType) {
            case "schememanager":
                Log.i(TAG, "Adding new scheme manager from qr code!");
                SchemeManagerHandler.confirmAndDownloadManager(
                    GsonUtil.getGson().fromJson(contents, SchemeManagerQr.class).getUrl(), this,
                new Runnable() {
                    @Override public void run() {
                        updateCredentialList(false);
                    }
                });
                break;
            case "disclosing":
            case "signing":
            case "issuing":
            default:
                launchedFromBrowser = false;
                onlineEnrolling = false;
                new IrmaClient(contents, irmaClientHandler);
                break;
            }
        }
    }

    private void showAboutDialog() {
        String version = BuildConfig.VERSION_NAME;
        if (BuildConfig.DEBUG)
            version += " (debug build)";

        new AlertDialog.Builder(this)
        .setTitle(R.string.about_app_title)
        .setMessage(getString(R.string.about_app_text, version))
        .setPositiveButton(R.string.dismiss, null)
        .show();
    }

    public void initialRegistration() {
        ArrayList<SchemeManager> managers = CredentialManager.getUnEnrolledKSSes();
        if (managers!=null && !managers.isEmpty()) {
            for (final SchemeManager m:managers) {
                SchemeManagerHandler.getKeyserverEnrollInput(MainActivity.this, m, new SchemeManagerHandler.KeyserverInputHandler() {
                    @Override
                    public void done(String email, String pin) {
                        SchemeManagerHandler.enrollKeyshareServer(
                            m.getName(), m.getKeyshareServer(), email, pin, MainActivity.this, null);
                    }
                });
            }
        }
    }

    // Classes handling (integration with) other components of the app

    /**
     * Reports session info to the user using {@link #setFeedback(String, String)} and updates
     * the activity state
     */
    private class ClientHandler extends IrmaClientHandler {
        public ClientHandler() {
            super(MainActivity.this);
        }

        @Override public void onStatusUpdate(IrmaClient.Action action, IrmaClient.Status status) {
            switch (status) {
            case COMMUNICATING:
                setState(State.COMMUNICATING);
                break;
            case CONNECTED:
                setState(State.CONNECTED);
                break;
            case DONE:
                setState(State.IDLE);
                break;
            }
        }

        @Override public void onSuccess(IrmaClient.Action action) {
            switch (action) {
            case DISCLOSING:
                setFeedback(getString(R.string.disclosure_successful), "success");
                break;
            case SIGNING:
                setFeedback(getString(R.string.signing_successful), "success");
                break;
            case ISSUING:
                setFeedback(getString(R.string.issuing_succesful), "success");
                break;
            }
            finish(true);
        }

        @Override public void onCancelled(IrmaClient.Action action) {
            switch (action) {
            case DISCLOSING:
                setFeedback(getString(R.string.disclosure_cancelled), "warning");
                break;
            case SIGNING:
                setFeedback(getString(R.string.signing_cancelled), "warning");
                break;
            case ISSUING:
                setFeedback(getString(R.string.issuing_cancelled), "warning");
                break;
            }
            finish(true);
        }

        @Override public void onFailure(IrmaClient.Action action, String message, ApiErrorMessage error, final String techInfo) {
            final String title;
            switch (action) {
            case DISCLOSING:
                title = getString(R.string.disclosure_failed);
                break;
            case SIGNING:
                title = getString(R.string.signing_failed);
                break;
            case ISSUING:
                title = getString(R.string.issuing_failed);
                break;
            case UNKNOWN:
            default:
                title = getString(R.string.failed);
                break;
            }

            final String feedback = title + ": " + message;
            setFeedback(title, "failure");
            finish(false);

            showErrorDialog(title, feedback, techInfo);
        }

        private void finish(boolean returnToBrowser) {
            setState(State.IDLE);

            if (!onlineEnrolling && launchedFromBrowser && returnToBrowser)
                onBackPressed();

            onlineEnrolling = false;
            launchedFromBrowser = false;
        }
    }

    /**
     * Initializes {@link CredentialManager} asynchroniously.
     */
    private class CredentialsLoader extends AsyncTask<Void,Void,Exception> {
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Log.i(TAG, "Loading credentials and logs");
                CredentialManager.init(settings);
                return null;
            } catch (CredentialsException e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            Log.i(TAG, "Finished loading credentials and logs");
            if (e == null)
                setState(State.CREDENTIALS_LOADED);
            else {
                // In this case the app would at some point erase the unserializable attributes by
                // overwriting them, so we should give the user a chance to bail out
                new AlertDialog.Builder(MainActivity.this)
                .setIcon(R.drawable.irma_error)
                .setTitle(R.string.cantreadattributes)
                .setMessage(R.string.cantreadattributes_long)
                .setNeutralButton(R.string.se_continue, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        settings.edit().remove(CredentialManager.CREDENTIAL_STORAGE).apply();
                        try {
                            CredentialManager.init(settings);
                            updateCredentialList();
                        } catch (CredentialsException e1) {
                            // This couldn't possibly happen, but if it does, let's be safe
                            throw new RuntimeException(e1);
                        }
                    }
                })
                .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        //Process.killProcess(Process.myPid());
                        System.exit(1);
                    }
                })
                .show();
            }
        }
    }

    /**
     * Loads {@link DescriptionStore} and {@link IdemixKeyStore} asynchroniously, reporting back
     * to the activity in between
     */
    private class StoreLoader extends AsyncTask<Void,Void,Exception> {
        @Override
        protected Exception doInBackground(Void... voids) {
            Log.i(TAG, "Loading DescriptionStore and IdemixKeyStore");
            FileReader reader = new AndroidFileReader(MainActivity.this);
            SSLSocketFactory socketFactory = null;
            if (Build.VERSION.SDK_INT >= 21) // 20 = 4.4 Kitkat, 21 = 5.0 Lollipop
                socketFactory = new SecureSSLSocketFactory();

            try {
                DescriptionStore.initialize(new DescriptionStoreDeserializer(reader), IRMApp.getStoreManager(), socketFactory);
                Log.i(TAG, "Loaded DescriptionStore");
                publishProgress();

                IdemixKeyStore.initialize(new IdemixKeyStoreDeserializer(reader), IRMApp.getStoreManager());
                Log.i(TAG, "Loaded IdemixKeyStore");
                return null;
            } catch (InfoException e) {
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            setState(State.DESCRIPTION_STORE_LOADED);
        }

        @Override
        protected void onPostExecute(Exception e) {
            Log.i(TAG, "Finished loading DescriptionStore and IdemixKeyStore");
            if (e != null)
                throw new RuntimeException(e);
            else
                setState(State.KEY_STORE_LOADED);


            initialRegistration();
        }
    }

}
