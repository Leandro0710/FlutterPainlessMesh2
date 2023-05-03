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

  static void sendNodeSyncRequest() async {
    if(MeshCommunicator.isConnected()) {
      try{
      Map<String, dynamic> nodeMessage = {
        'dest': MeshActivity.apNodeId,
        'from': MeshActivity.myNodeId,
        'type': 5,
        'subs': [],
      };
      String msg = json.encode(nodeMessage);
      List<int> data = utf8.encode(msg);
      MeshCommunicator.writeData(data);

      print('Sending node sync request: $msg');
    }
    catch(_){
        print("Error sending node sync request");
    }
    }
  }


}
