package comunicacionnativo3;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.net.wifi.WifiNetworkSpecifier;
import io.flutter.BuildConfig;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

import android.os.PatternMatcher;
import android.net.MacAddress;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.net.wifi.aware.PeerHandle;


/**
 * Mesh activity, handles the UI
 */
public class MainActivity extends FlutterActivity {
    private String APPCHANNEL = "platform.testing/";

    private static final String CHANNEL = "samples.flutter.dev/battery";

    /** Tag for debug messages of service*/
    private static final String DBG_TAG = "MeshActivity";

    /** Flag if we try to connect to Mesh */
    private static boolean tryToConnect = false;
    /** Flag if connection to Mesh was started */
    private static boolean isConnected = false;
    /** Flag when user stops connection */
    private static boolean userDisConRequest = false;

    /** WiFi manager to connect to Mesh network */
    private WifiNetworkSpecifier wifiMgr;

    /** Mesh name == Mesh SSID */
    private String meshName;
    /** Mesh password == Mesh network password */
    private String meshPw;
    /** Mesh port == TCP port number */
    private static int meshPort;

    /** WiFi AP to which device was connected before connecting to mesh */
    private String oldAPName = "";
    /** Mesh network entry IP */
    private static String meshIP;

    /** My Mesh node id */
    static long myNodeId = 0;
    /** The node id we connected to */
    static long apNodeId = 0;

    /** Filter for incoming messages */
    private long filterId = 0;

    /** View for connection status */
    private TextView tv_mesh_conn;
    /** View for errors */
    private TextView tv_mesh_err;
    /** View for received MESH messages */
    private TextView tv_mesh_msgs;
    /** View for connect button */
    private MenuItem mi_mesh_conn_bt;

    /** Predefined message 1 */
    private String predMsg1;
    /** Predefined message 2 */
    private String predMsg2;
    /** Predefined message 3 */
    private String predMsg3;
    /** Predefined message 4 */
    private String predMsg4;
    /** Predefined message 5 */
    private String predMsg5;

    /** Flag if log file should be written */
    private static boolean doLogging = true;
    /** For log file of data */
    static BufferedWriter out = null;
    /** Path to storage folder */

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
    }

    private long llegadadatos(boolean on, List datos) {
        boolean encendido = on;
        if (!encendido) {
            meshName = (String) datos.get(0);
            meshPw = (String) datos.get(1);
            meshPort = (int) datos.get(2);
            handleConnection();
            encendido = !encendido;
            if (isConnected){
                return myNodeId;
            }
            return myNodeId;
        } else {
            encendido = !encendido;
            return myNodeId;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventChannel eventChannel = new EventChannel(
                getFlutterEngine().getDartExecutor().getBinaryMessenger(),
                APPCHANNEL + "datos"
        );

        if (getFlutterEngine() != null) {
            GeneratedPluginRegistrant.registerWith(getFlutterEngine());
        }

        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                                ///Getting the arguments array passed to the invokemethod from dart
                                ArrayList args = (ArrayList) call.arguments;

                                // This method is invoked on the main thread.
                                if (call.method.equals("llegadadatos")) {
                                    myNodeId = llegadadatos((Boolean) args.get(0), (List) args.get(1));
                                    result.success(myNodeId);
                                }else{
                                    result.error("UNAVAILABLE", "Battery level not available.", "F");
                                }
                        }

                );

        // Start handler to send node sync request and time sync request every 5 seconds
        // Keeps socket connection more stable
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            boolean timeForNodeReq = true;

            public void run() {
                if (MeshCommunicator.isConnected()) {
                    if (timeForNodeReq) {
                        MainHandler.sendNodeSyncRequest();
                        timeForNodeReq = false;
                    } else {
                        MainHandler.sendTimeSyncRequest();
                        timeForNodeReq = true;
                    }

                }
                handler.postDelayed(this, 10000);
            }
        }, 10000);


    }

    @SuppressLint({"DefaultLocale", "ClickableViewAccessibility"})
    @Override
    public void onResume() {
        // Get pointer to shared preferences
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get predefined messages
        predMsg1 = mPrefs.getString("msg_1", "");
        predMsg2 = mPrefs.getString("msg_2", "");
        predMsg3 = mPrefs.getString("msg_3", "");
        predMsg4 = mPrefs.getString("msg_4", "");
        predMsg5 = mPrefs.getString("msg_5", "");

        // Ask for permissions if necessary
        ArrayList<String> arrPerm = new ArrayList<>();
        // On newer Android versions it is required to get the permission of the user to
        // get the location of the device. This is necessary to do a WiFi scan for APs.
        // I am not sure at all what that has to be with
        // the permission to use Bluetooth or BLE, but you need to get it anyway

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 0);
        }

        // Enable access to connectivity
        // ThreadPolicy to get permission to access connectivity
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Enable sharing of files
        // VmPolicy to get permission to share files
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Get the wifi manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiMgr = new WifiNetworkSpecifier.Builder()
                    .setSsidPattern(new PatternMatcher("test", PatternMatcher.PATTERN_PREFIX))
                    .setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .build();
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(wifiMgr)
                    .build();
        }



        // Register Mesh events
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MeshCommunicator.MESH_DATA_RECVD);
        intentFilter.addAction(MeshCommunicator.MESH_SOCKET_ERR);
        intentFilter.addAction(MeshCommunicator.MESH_CONNECTED);
        intentFilter.addAction(MeshCommunicator.MESH_NODES);
        intentFilter.addAction(MeshCommunicator.MESH_OTA);
        intentFilter.addAction(MeshCommunicator.MESH_OTA_REQ);
        // Register network change events
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // Register receiver
        registerReceiver(localBroadcastReceiver, intentFilter);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MeshCommunicator.isConnected()) {
            MeshCommunicator.Disconnect();
        }
        stopLogging();
        // unregister the broadcast receiver
        unregisterReceiver(localBroadcastReceiver);
    }

    /**
     * Apply or remove filter to show only messages from a specific node
     */

    private void handleFilterRequest() {
        if (MeshCommunicator.isConnected()) {
            ArrayList<String> nodesListStr = new ArrayList<>();

            ArrayList<Long> tempNodesList = new ArrayList<>(MainHandler.nodesList);

            tempNodesList.add(0L);
            Collections.sort(tempNodesList);

            for (int idx = 0; idx < tempNodesList.size(); idx++) {
                nodesListStr.add(String.valueOf(tempNodesList.get(idx)));
            }
            ///nodesListStr.set(0,getString(R.string.mesh_filter_clear));

            ArrayAdapter<String> nodeListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nodesListStr);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
			/*builder.setTitle(getString(R.string.mesh_list_header))
					.setInverseBackgroundForced(true)
					.setNegativeButton(getString(android.R.string.cancel),
							(dialog, which) -> {
								// Do something here if you want
								dialog.dismiss();
							})
					.setAdapter(nodeListAdapter,
							(dialog, which) -> {
								filterId = tempNodesList.get(which);
								dialog.dismiss();
							});
			builder.create();
			builder.show();
			*/
        } else {
            //showToast(getString(R.string.mesh_no_connection), Toast.LENGTH_SHORT);
        }
    }


    /**
     * Handle connect action events.
     * Depending on current status
     * - Start connection request
     * - Cancel connection request if pending
     * - Stop connection to the mesh network
     */
    private void handleConnection() {
        if (!isConnected) {
            if (tryToConnect) {
                stopConnection();
            } else {
                startConnectionRequest();
            }
        } else {
            stopConnection();
        }
    }

    /**
     * Add mesh network AP to the devices list of Wifi APs
     * Enable mesh network AP to initiate connection to the mesh AP
     */
    private void startConnectionRequest() {
        tryToConnect = true;
        userDisConRequest = false;

        // Get current active WiFi AP
        oldAPName = "";

        // Get current WiFi connection
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                final WifiInfo connectionInfo = wifiMgr.getConnectionInfo();
                if (connectionInfo != null && !connectionInfo.getSSID().isEmpty()) {
                    oldAPName = connectionInfo.getSSID();
                }
            }
        }

        // Add device AP to network list and enable it
        WifiConfiguration meshAPConfig = new WifiConfiguration();
        meshAPConfig.SSID = "\"" + meshName + "\"";
        meshAPConfig.preSharedKey = "\"" + meshPw + "\"";
        int newId = wifiMgr.addNetwork(meshAPConfig);
        if (BuildConfig.DEBUG) Log.i(DBG_TAG, "Result of addNetwork: " + newId);
        wifiMgr.disconnect();
        wifiMgr.enableNetwork(newId, true);
        wifiMgr.reconnect();
        System.out.println("ayuda");
    }

    /**
     * Stop connection to the mesh network
     * Disable the mesh AP in the device Wifi AP list so that
     * the device reconnects to its default AP
     */
    private void stopConnection() {

        if (MeshCommunicator.isConnected()) {
            MeshCommunicator.Disconnect();
        }
        isConnected = false;
        tryToConnect = false;
        userDisConRequest = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<WifiConfiguration> availAPs = wifiMgr.getConfiguredNetworks();

        if (oldAPName.isEmpty()) {
            for (int index = 0; index < availAPs.size(); index++) {
                if (availAPs.get(index).SSID.equalsIgnoreCase("\"" + meshName + "\"")) {
                    wifiMgr.disconnect();
                    wifiMgr.disableNetwork(availAPs.get(index).networkId);
                    if (BuildConfig.DEBUG) Log.d(DBG_TAG, "Disabled: " + availAPs.get(index).SSID);
                    wifiMgr.reconnect();
                    break;
                }
            }
        } else {
            for (int index = 0; index < availAPs.size(); index++) {
                if (availAPs.get(index).SSID.equalsIgnoreCase(oldAPName)) {
                    wifiMgr.disconnect();
                    wifiMgr.enableNetwork(availAPs.get(index).networkId, true);
                    if (BuildConfig.DEBUG)
                        Log.d(DBG_TAG, "Re-enabled: " + availAPs.get(index).SSID);
                    wifiMgr.reconnect();
                    break;
                }
            }
        }
        stopLogging();
    }

    /**
     * Show known mesh nodes as preparation of sending a message
     * Uses a temporary nodes list in case the nodes list is refreshed
     * while this dialog is still open
     * Adds a BROADCAST node to enable sending broadcast messages to
     * the mesh network
     */
    private void selectNodesForSending() {
        ArrayList<String> nodesListStr = new ArrayList<>();

        ArrayList<Long> tempNodesList = new ArrayList<>(MainHandler.nodesList);

        tempNodesList.add(0L);
        Collections.sort(tempNodesList);

        for (int idx = 0; idx < tempNodesList.size(); idx++) {
            nodesListStr.add(String.valueOf(tempNodesList.get(idx)));
        }
        ///nodesListStr.set(0,getString(R.string.mesh_send_broadcast));

        ArrayAdapter<String> nodeListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nodesListStr);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		/*builder.setTitle(getString(R.string.mesh_list_header))
				.setInverseBackgroundForced(true)
				.setNegativeButton(getString(android.R.string.cancel),
						(dialog, which) -> {
							// Do something here if you want
							dialog.dismiss();
						})
				.setAdapter(nodeListAdapter,
						(dialog, which) -> showSendDialog(tempNodesList.get(which)));
		builder.create();
		builder.show();
		 */
    }


    /**
     * Show the dialog to send a message to the mesh network
     * Options
     * - Send a time sync request
     * - Send a node sync request
     * - Send a user message
     * @param selectedNode nodeID that the message should be sent to
     */
    private void showSendDialog(long selectedNode) {
        // Get dialog layout
        LayoutInflater li = LayoutInflater.from(this);
        ///@SuppressLint("InflateParams") View sendDialogView = li.inflate(R.layout.send_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

		/*
		TextView nodeToSendTo  = sendDialogView.findViewById(R.id.send_node_id);

		if (selectedNode == 0) {
			nodeToSendTo.setText(getString(R.string.mesh_send_broadcast));
		} else {
			nodeToSendTo.setText(String.valueOf(selectedNode));
		}

		final EditText msgToSend = sendDialogView.findViewById(R.id.send_node_msg);
		final long rcvNodeId = selectedNode;
		// set prompts.xml to alert dialog builder
		alertDialogBuilder.setView(sendDialogView)
				.setNegativeButton(getString(android.R.string.cancel),
						(dialog, which) -> {
							// Do something here if you want
							dialog.dismiss();
						})
				.setPositiveButton(getString(R.string.mesh_send_button),
						(dialog, which) -> {
							MeshHandler.sendNodeMessage(rcvNodeId, msgToSend.getText().toString());
							// Do something here if you want
							dialog.dismiss();
						});
		// create alert dialog
		final AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();

		// Set the button functions
		final Button msg1Sel = sendDialogView.findViewById(R.id.bt_pred_msg_1);
		msg1Sel.setOnClickListener(v12 -> msgToSend.setText(predMsg1));
		final Button msg2Sel = sendDialogView.findViewById(R.id.bt_pred_msg_2);
		msg2Sel.setOnClickListener(v12 -> msgToSend.setText(predMsg2));
		final Button msg3Sel = sendDialogView.findViewById(R.id.bt_pred_msg_3);
		msg3Sel.setOnClickListener(v12 -> msgToSend.setText(predMsg3));
		final Button msg4Sel = sendDialogView.findViewById(R.id.bt_pred_msg_4);
		msg4Sel.setOnClickListener(v12 -> msgToSend.setText(predMsg4));
		final Button msg5Sel = sendDialogView.findViewById(R.id.bt_pred_msg_5);
		msg5Sel.setOnClickListener(v12 -> msgToSend.setText(predMsg5));
		 */
    }


    /**
     * Share current content of the message list through available
     * share resources (email/gmail/sms/facebook/...)
     */
    private void handleShareRequest() {
        if (doLogging) {
            // Still logging, cannot share logs yet
            ///showToast(getString(R.string.error_share), Toast.LENGTH_SHORT);
        } else {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

            MimeTypeMap map = MimeTypeMap.getSingleton();
            String ext = MimeTypeMap.getFileExtensionFromUrl(logFilePath);
            String type = map.getMimeTypeFromExtension(ext);

            File logFile = new File(logFilePath);
            Uri data = Uri.fromFile(logFile);

            sharingIntent.setDataAndType(data, type);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, data);
            // TODO find out why the email address would be the filename if we do not set it here to ""
            sharingIntent.putExtra(Intent.EXTRA_EMAIL, "receiver@google.com");

            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, meshName);

            startActivity(Intent.createChooser(sharingIntent, "Share collected messages using"));
        }
    }


    /**
     * Show dialog to select OTA file and node type and initiate advertise of OTA
     */

    /**
     * Scroll the text view with the received messages to the bottom
     */
    private void scrollViewDown() {
        ///final ScrollView scrollview = findViewById(R.id.sv_msgs);
        ///scrollview.post(() -> scrollview.fullScroll(ScrollView.FOCUS_DOWN));
    }
    /**
     * Start logging the received messages
     * TODO double check this works for all Android versions
     */
    @SuppressLint("DefaultLocale")
    private void startLogging() {
        if (doLogging) {
            stopLogging();
        }
		/*
		DateTime now = new DateTime();
		String logTitle = String.format ("Log created: %02d/%02d/%02d %02d:%02d\n\n", now.getYear()-2000, now.getMonthOfYear(),
		0		now.getDayOfMonth(), now.getHourOfDay(), now.getMinuteOfHour());

		 */
        /* Name of the log */
        String logName = "meshLogFile.txt";

        // Create folder for this data set
        try {
            File appDir = new File(sdcardPath);
            boolean exists = appDir.exists();
            if (!exists) {
                boolean result = appDir.mkdirs();
                if (!result) {
                    Log.d(DBG_TAG, "Failed to create log folder");
                }
            }
        } catch (Exception exc) {
            Log.e(DBG_TAG, "Failed to create log folder: " + exc);
        }

        // If file is still open for writing, close it first
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException exc) {
                Log.e(DBG_TAG, "Failed to close log file: " + exc);
            }
        }

        // TODO find a better solution to handle the log files
        // For now delete the old log file to avoid getting a too large file
        boolean result = new File(sdcardPath + logName).exists();
        if (result) {
            result = new File(sdcardPath + logName).delete();
            if (!result) {
                Log.d(DBG_TAG, "Failed to delete the old logfile");
            }
        }

        try {
            logFilePath = sdcardPath + logName;
            FileWriter newFile = new FileWriter(logFilePath);
            out = new BufferedWriter(newFile);
            ///out.append(logTitle);
        } catch (IOException exc) {
            Log.e(DBG_TAG, "Failed to open log file for writing: " + exc);
        }

        doLogging = true;
    }

    /**
     * Stop logging of the received messages
     */
    private void stopLogging() {
        if (doLogging) {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException exc) {
                    Log.e(DBG_TAG, "Failed to close log file: " + exc);
                }
                out = null;
            }
            doLogging = false;
            String[] toBeScannedStr = new String[1];
            toBeScannedStr[0] = sdcardPath + "*";
            MediaScannerConnection.scanFile(this, toBeScannedStr, null, (path, uri) -> System.out.println("SCAN COMPLETED: " + path));
        }
    }

    /**
     * Local broadcast receiver
     * Registered for
     * - WiFi connection change events
     * - Mesh network data events
     * - Mesh network error events
     */
    private final BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(final Context context, Intent intent) {
            // Connection change
            String intentAction = intent.getAction();
            //Log.d(DBG_TAG, "Received broadcast: " + intentAction);
            // WiFi events
            if (isConnected) {
                // Did we loose connection to the mesh network?
                /* Access to connectivity manager */
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                /* WiFi connection information  */
                NetworkInfo wifiOn;
                if (cm != null) {
                    wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (!wifiOn.isConnected()) {
                        isConnected = false;
                        runOnUiThread(() -> stopConnection());
                    }
                }
            }
            if (tryToConnect && (intentAction != null) && (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION))) {
                /* Access to connectivity manager */
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                /* WiFi connection information  */
                NetworkInfo wifiOn;
                if (cm != null) {
                    wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (wifiOn.isConnected()) {
                        if (tryToConnect) {
                            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                            if (wifiInfo.getSSID().equalsIgnoreCase("\"" + meshName + "\"")) {
                                Log.d(DBG_TAG, "Connected to Mesh network " + wifiOn.getExtraInfo());
                                // Get the gateway IP address
                                DhcpInfo dhcpInfo;
                                if (wifiMgr != null) {
                                    // Create the mesh AP node ID from the AP MAC address
                                    apNodeId = MainHandler.createMeshID(wifiInfo.getBSSID());

                                    dhcpInfo = wifiMgr.getDhcpInfo();
                                    // Get the mesh AP IP
                                    int meshIPasNumber = dhcpInfo.gateway;
                                    meshIP = ((meshIPasNumber & 0xFF) + "." +
                                            ((meshIPasNumber >>>= 8) & 0xFF) + "." +
                                            ((meshIPasNumber >>>= 8) & 0xFF) + "." +
                                            (meshIPasNumber >>> 8 & 0xFF));

                                    // Create our node ID
                                    myNodeId = MainHandler.createMeshID(MainHandler.getWifiMACAddress());
                                } else {
                                    // We are screwed. Tell user about the problem
                                    Log.e(DBG_TAG, "Critical Error -- cannot get WifiManager access");
                                }
                                // Rest has to be done on UI thread
                                runOnUiThread(() -> {
                                    tryToConnect = false;

                                    String connMsg = "ID: " + myNodeId + " on " + meshName;
                                    tv_mesh_conn.setText(connMsg);

                                    // Set flag that we are connected
                                    isConnected = true;

                                    startLogging();

                                    // Connected to the Mesh network, start network task now
                                    MeshCommunicator.Connect(meshIP, meshPort, getApplicationContext());

                                });
                            } else {

                                @SuppressLint("MissingPermission") List<WifiConfiguration> availAPs = wifiMgr.getConfiguredNetworks();

                                for (int index = 0; index < availAPs.size(); index++) {
                                    if (availAPs.get(index).SSID.equalsIgnoreCase("\""+meshName+"\"")) {
                                        wifiMgr.disconnect();
                                        wifiMgr.enableNetwork(availAPs.get(index).networkId, true);
                                        if (BuildConfig.DEBUG) Log.d(DBG_TAG, "Retry to enable: " + availAPs.get(index).SSID);
                                        wifiMgr.reconnect();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            String dataSet = null;
			/*
			DateTime now = new DateTime();
			dataSet = String.format ("[%02d:%02d:%02d:%03d] ",
					now.getHourOfDay(),
					now.getMinuteOfHour(),
					now.getSecondOfMinute(),
					now.getMillisOfSecond());

			 */

            // Mesh events
            if (MeshCommunicator.MESH_DATA_RECVD.equals(intentAction)) {
                String rcvdMsg = intent.getStringExtra("msg");
                String oldText;
                try {
                    JSONObject rcvdJSON = new JSONObject(rcvdMsg);
                    int msgType = rcvdJSON.getInt("type");
                    long fromNode = rcvdJSON.getLong("from");
                    switch (msgType) {
                        case 3: // TIME_DELAY
                            ///tv_mesh_err.setText(getString(R.string.mesh_event_time_delay));
                            dataSet += "Received TIME_DELAY\n";
                            break;
                        case 4: // TIME_SYNC
                            ///tv_mesh_err.setText(getString(R.string.mesh_event_time_sync));
                            dataSet += "Received TIME_SYNC\n";
                            break;
                        case 5: // NODE_SYNC_REQUEST
                        case 6: // NODE_SYNC_REPY
                            if (msgType != 5) {
                                ///tv_mesh_err.setText(getString(R.string.mesh_event_node_reply));
                                dataSet += "Received NODE_SYNC_REPLY\n";
                            } else {
                                ///tv_mesh_err.setText(getString(R.string.mesh_event_node_req));
                                dataSet += "Received NODE_SYNC_REQUEST\n";
                            }
                            // Generate known nodes list
                            final String nodesListString = rcvdMsg;
                            final Handler handler = new Handler();
                            handler.post(() -> MainHandler.generateNodeList(nodesListString));
                            break;
                        case 7: // CONTROL ==> deprecated
                            dataSet += "Received CONTROL\n";
                            break;
                        case 8: // BROADCAST
                            dataSet += "Broadcast:\n" + rcvdJSON.getString("msg") + "\n";
                            if (filterId != 0) {
                                if (fromNode != filterId) {
                                    return;
                                }
                            }
                            oldText = "BC from " + fromNode + "\n\t" + rcvdJSON.getString("msg") + "\n";

                            tv_mesh_msgs.append(oldText);
                            break;
                        case 9: // SINGLE
                            dataSet += "Single Msg:\n" + rcvdJSON.getString("msg") + "\n";
                            // Check if the message is a OTA req message
                            JSONObject rcvdData = new JSONObject(rcvdJSON.getString("msg"));
                            String dataType = rcvdData.getString("plugin");
                            if ((dataType != null) && dataType.equalsIgnoreCase("ota")) {
                                dataType = rcvdData.getString("type");
                                if (dataType != null) {
                                    if (dataType.equalsIgnoreCase("version")) {
                                        // We received a OTA advertisment!
                                        ///tv_mesh_err.setText(getString(R.string.mesh_event_ota_adv));
                                        return;
                                    } else if (dataType.equalsIgnoreCase("request")) {
                                        // We received a OTA block request
                                        MainHandler.sendOtaBlock(fromNode, rcvdData.getLong("partNo"));
                                        ///tv_mesh_err.setText(getString(R.string.mesh_event_ota_req));
                                    }
                                }
                            }
                            if (filterId != 0) {
                                if (fromNode != filterId) {
                                    return;
                                }
                            }
                            oldText = "SM from " + fromNode + "\n\t" + rcvdJSON.getString("msg") + "\n";
                            tv_mesh_msgs.append(oldText);

                            break;
                    }
                } catch (JSONException e) {
                    Log.d(DBG_TAG, "Received message is not a JSON Object!");
                    oldText = "E: " + intent.getStringExtra("msg") + "\n";
                    tv_mesh_msgs.append(oldText);
                    dataSet += "ERROR INVALID DATA:\n" + intent.getStringExtra("msg") + "\n";
                }
                if (out != null) {
                    try {
                        out.append(dataSet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                scrollViewDown();
            } else if (MeshCommunicator.MESH_SOCKET_ERR.equals(intentAction)) {
                if (MainHandler.nodesList != null) {
                    MainHandler.nodesList.clear();
                }
                if (!userDisConRequest) {
                    ///showToast(getString(R.string.mesh_lost_connection), Toast.LENGTH_LONG);
                    MeshCommunicator.Connect(meshIP, meshPort, getApplicationContext());
                    tv_mesh_err.setText(intent.getStringExtra("msg"));
                }
            } else if (MeshCommunicator.MESH_CONNECTED.equals(intentAction)) {
                userDisConRequest = false;
            } else if (MeshCommunicator.MESH_NODES.equals(intentAction)) {
                String oldText = intent.getStringExtra("msg") + "\n";
                tv_mesh_msgs.append(oldText);
                scrollViewDown();
                dataSet += intent.getStringExtra("msg") + "\n";
                if (out != null) {
                    try {
                        out.append(dataSet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            String oldText = "hola";
            // Check if the text is getting too long
            if (oldText.length() > 16535) {
                // Quite long, remove the first 20 lines  from the text
                int indexOfCr = 0;
                for (int lines=0; lines < 20; lines++) {
                    indexOfCr = oldText.indexOf("\n", indexOfCr+1);
                }
                oldText = oldText.substring(indexOfCr+1);
                tv_mesh_msgs.setText(oldText);
            }
        }
    };
}

