import 'dart:async';
import 'dart:core';
import 'dart:ffi';

import 'package:comunicacionnativo3/MeshCommunicator.dart';
import 'package:comunicacionnativo3/MeshHandler.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:plugin_wifi_connect/plugin_wifi_connect.dart';

class MeshActivity extends StatefulWidget {
  static final String DBG_TAG = "MeshActivity";

  /** Flag if we try to connect to Mesh */
  static bool tryToConnect = false;
  /** Flag if connection to Mesh was started */
  static bool isConnected = false;

  /** Flag when user stops connection */
  static bool userDisConRequest = false;

  /** Mesh name == Mesh SSI */
  static late String meshName = "whateverYouLike";
  /** Mesh password == Mesh network password */
  static late String meshPw = "somethingSneaky";
  /** Mesh port == TCP port number */
  static late int meshPort = 5555;

  /** WiFi AP to which device was connected before connecting to mesh */
  static String oldAPName = "";
  /** Mesh network entry IP */
  static late String meshIP;

  /** My Mesh node id */
  static late int myNodeId = 0;
  /** The node id we connected to */
  static late int apNodeId = 0;
  //** Tag for debug messages of service*/

  static late var wifi;

  @override

  State<MeshActivity>  createState() => _MeshActivity();
}

class _MeshActivity  extends State<MeshActivity> {
  List<String> _messages = [];

  @override
  void initState() {
    super.initState();
    NetworkInfo();
    // Inicialización de objetos que necesitan configuración
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    //broadcast();
    // Manejo de cualquier cambio en las dependencias del Widget
  }

  @override
  Widget build(BuildContext context) {
    // Construcción y renderizado del Widget
    return Scaffold(
        floatingActionButton: FloatingActionButton(
         onPressed: () async {
           await MeshHandler.sendNodeSyncRequest();
      // Add your onPressed code here!
      },
         backgroundColor: Colors.green,
     child: const Icon(Icons.navigation),
      ),
      appBar: AppBar(
        title: Text('Socket Messages'),
      ),
      body: Column(
        children: [
          TextButton(onPressed: () async {
            NetworkInfo wifiInfo = NetworkInfo();
            String? wifi= await wifiInfo.getWifiBSSID();
            MeshActivity.apNodeId = MeshHandler.createMeshID(wifi!);

            MeshActivity.meshIP = await wifiInfo.getWifiGatewayIP() as String;

            // Create our node ID
            MeshActivity.myNodeId = MeshHandler.createMeshID(await MeshHandler.getWifiMACAddress());

            MeshCommunicator.Connect(MeshActivity.meshIP,MeshActivity.meshPort);
          }, child: Text(
            'Presionar',
            style: TextStyle(
              fontSize: 20,
              color: Colors.blue,
            ),
          ),
          ),
          /*
          Container(
            child: ListView.builder(
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                // Construir un widget para mostrar cada mensaje
                return ListTile(
                  title: Text(_messages[index]),
                );
              },
            ),
          )

           */
        ],
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();

    // Liberación de recursos utilizados por el Widget
  }

  void startTimer() async {
    bool timeForNodeReq = true;
    Timer.periodic(Duration(seconds: 10), (timer) {
      if (MeshCommunicator.isConnected()) {
        if (timeForNodeReq) {
          MeshHandler.sendNodeSyncRequest();
          timeForNodeReq = false;
        } else {
          MeshHandler.sendTimeSyncRequest();
          timeForNodeReq = true;
        }
      }
    });
  }

  void handleConnection() {
    if (!MeshActivity.isConnected) {
      if (MeshActivity.tryToConnect) {
        print("Start detener");
        stopConnection();
      } else {
        print("Start Conexion");
        startConnectionRequest();
      }
    } else {
      print("Conexion detener");
      stopConnection();
    }
  }

  void startConnectionRequest() async {
    MeshActivity.tryToConnect = true;
    MeshActivity.userDisConRequest = false;

    // Get current active WiFi AP
    MeshActivity.oldAPName = "";

    // Get current WiFi connection
    var connectivityResult = await (Connectivity().checkConnectivity());
    if (connectivityResult == ConnectivityResult.wifi) {
      var wifiInfo = await (NetworkInfo().getWifiName());
      if (wifiInfo != null && wifiInfo.isNotEmpty) {
        MeshActivity.oldAPName = wifiInfo;
      }
    }

    PluginWifiConnect.deactivateWifi();
    PluginWifiConnect.activateWifi();
    PluginWifiConnect.connectToSecureNetwork(
        MeshActivity.meshName, MeshActivity.meshPw);
  }

  static void stopConnection() {
    if (MeshCommunicator.isConnected()) {
      MeshCommunicator.Disconnect();
    }


  }


  static void broadcast() async {
    MeshActivity.tryToConnect = true;
    MeshActivity.userDisConRequest = false;
    // WiFi events
    /*
    if (MeshActivity.isConnected) {
      // Did we lose connection to the mesh network?
      var connectivityResult = await (Connectivity().checkConnectivity());
      if (connectivityResult == ConnectivityResult.wifi) {
        if (!await PluginWifiConnect.isEnabled) {
          MeshActivity.isConnected = false;
          stopConnection();
        }
      }
    }

     */
    if (MeshActivity.tryToConnect) {
      /* Access to connectivity manager */
      var connectivityResult = await (Connectivity().checkConnectivity());
      /* WiFi connection information  */
      NetworkInfo wifiInfo = NetworkInfo();
      if (connectivityResult == ConnectivityResult.wifi) {

        //if (await PluginWifiConnect.isEnabled) {
          if (MeshActivity.tryToConnect ) {
            print("Conectado al wifi");
            // Create the mesh AP node ID from the AP MAC address
            String? wifi= await wifiInfo.getWifiBSSID();
            MeshActivity.apNodeId = MeshHandler.createMeshID(wifi!);

            MeshActivity.meshIP = await wifiInfo.getWifiGatewayIP() as String;

            // Create our node ID
            MeshActivity.myNodeId = MeshHandler.createMeshID(await MeshHandler.getWifiMACAddress());

            // Rest has to be done on UI thread
            await runInBackground();
          }
        ///}
      }
    }
  }

  static Future<void> runInBackground() async {
    MeshActivity.tryToConnect = false;
    String connMsg = "ID:${MeshActivity.myNodeId} on ${MeshActivity.meshName}";

    // Set flag that we are connected
    MeshActivity.isConnected = true;
    print(connMsg);

    // Connected to the Mesh network, start network task now
    MeshCommunicator.Connect(MeshActivity.meshIP,MeshActivity.meshPort);
  }

}

