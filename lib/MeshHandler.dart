import 'dart:convert';
import 'dart:isolate';
import 'package:comunicacionnativo3/MeshActivity.dart';
import 'package:flutter/material.dart';

import 'MeshCommunicator.dart';

class MeshHandler extends MeshActivity{
  /* Debug tag */
  final String DBG_TAG = "MeshHandler";

  // List of currently known nodes
  static late List<int> nodesList;

  static int createMeshID(String macAddress) {
    int calcNodeId = -1;
    List<String> macAddressParts = macAddress.split(':');
    if (macAddressParts.length == 6) {
      try {
        int number = int.parse('0x${macAddressParts[2]}');
        if (number < 0) {
          number *= -1;
        }
        calcNodeId = number * 256 * 256 * 256;
        number = int.parse('0x${macAddressParts[3]}');
        if (number < 0) {
          number *= -1;
        }
        calcNodeId += number * 256 * 256;
        number = int.parse('0x${macAddressParts[4]}');
        if (number < 0) {
          number *= -1;
        }
        calcNodeId += number * 256;
        number = int.parse('0x${macAddressParts[5]}');
        if (number < 0) {
          number *= -1;
        }
        calcNodeId += number;
      } catch (_) {
    calcNodeId = -1;
    }
  }
    return calcNodeId;
  }

  static String getWifiMACAddress(){
    return "01:02:03:04:05:06";
  }

  static void sendNodeSyncRequest(){
    if(MeshCommunicator.isConnected()) {
      Map<String, dynamic> nodeMessage = {};
      List<dynamic> subsArray = [];
      try {
        nodeMessage['dest'] = MeshActivity.apNodeId;
        nodeMessage['from'] = MeshActivity.myNodeId;
        nodeMessage['type'] = 5;
        nodeMessage['subs'] = subsArray;
        String msg = jsonEncode(nodeMessage);
        List<int> data = utf8.encode(msg);
        MeshCommunicator.writeData(data);
        print('Sending node sync request $msg');
      } catch (e) {
        print('Error: $e');
      }
    }
  }

  static void sendTimeSyncRequest(){
    Map<String, dynamic> nodeMessage = new Map();
    Map<String, dynamic> typeObject = new Map();
    nodeMessage["dest"] = MeshActivity.apNodeId;
    nodeMessage["from"] = MeshActivity.myNodeId;
    nodeMessage["type"] = 4;
    typeObject["type"] = 0;
    nodeMessage["msg"] = typeObject;
    String msg = json.encode(nodeMessage);
    List<int> data = utf8.encode(msg);
    MeshCommunicator.writeData(data);
  }




}
