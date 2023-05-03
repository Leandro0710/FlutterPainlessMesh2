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

  /** Mesh name == Mesh SSID */
  static late String meshName;
  /** Mesh password == Mesh network password */
  static late String meshPw;
  /** Mesh port == TCP port number */
  static late int meshPort;

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


  @override
  void initState() {
    super.initState();
    startTimer();
    // Inicialización de objetos que necesitan configuración
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    MeshActivity.wifi = NetworkInfo();

    // Manejo de cualquier cambio en las dependencias del Widget
  }

  @override
  Widget build(BuildContext context) {
    // Construcción y renderizado del Widget
    return Container(
      // ...
    );
  }

  @override
  void dispose() {
    // Liberación de recursos utilizados por el Widget
    super.dispose();
  }

  void startTimer() async {
    bool timeForNodeReq = true;
    Timer.periodic(Duration(seconds: 10), (timer) {
      if (MeshCommunicator.isConnected()) {
        if (timeForNodeReq) {
          MeshHandler.sendNodeSyncRequest();
          timeForNodeReq = false;
        } else {
          //MeshHandler.sendTimeSyncRequest();
          timeForNodeReq = true;
        }
      }
    });
  }

  void handleConnection() {
    if (!MeshActivity.isConnected) {
      if (MeshActivity.tryToConnect) {
        stopConnection();
      } else {
        startConnectionRequest();
      }
    } else {
      stopConnection();
    }
  }

  void startConnectionRequest() async {
    MeshActivity.tryToConnect = true;
    MeshActivity.userDisConRequest = false;

    // Get current active WiFi AP
    String oldAPName = "";

    // Get current WiFi connection
    var connectivityResult = await (Connectivity().checkConnectivity());
    if (connectivityResult == ConnectivityResult.wifi) {
      var wifiInfo = await (NetworkInfo().getWifiName());
      if (wifiInfo != null && wifiInfo.isNotEmpty) {
        oldAPName = wifiInfo;
      }
    }
    PluginWifiConnect.deactivateWifi();
    PluginWifiConnect.activateWifi();
    PluginWifiConnect.connectToSecureNetwork(MeshActivity.meshName, MeshActivity.meshPw);
  }

  void stopConnection(){
    if (MeshCommunicator.isConnected()) {
      MeshCommunicator.Disconnect();}

  }
}