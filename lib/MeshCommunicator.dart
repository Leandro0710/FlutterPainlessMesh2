import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:isolate';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class MeshCommunicator
{
  static String DBG_TAG = "MeshCommunicator";

  //Acción para los datos MESH
  static String MESH_DATA_RECVD = "";

  //Accion para el error del socket
  static String MESH_SOCKET_ERR = "DISCON";

  //Accion para el éxito de la conexión
  static String MESH_CONNECTED = "CON";

  //Accion para la lista de nodos
  static String MESH_NODES = "NODE";

  //Local copy of the mesh network AP gateway address */
  static String serverIp = "10.183.133.1";
  // Local copy of the mesh network communication port */
  static int serverPort = 5555;

  static late Socket? connectionSocket = null;

  var streamController = StreamController<String>();
  var data = StringBuffer();


  //Verificar el socket si esta conectado
  static bool isConnected(){
    return (connectionSocket == null);
  }
  //Abrir conexion de nodo
  static Future<void> Connect(String ip, int port) async {
    serverIp = ip;
    serverPort = port;
    await Isolate.spawn(ConnectRunnableRun,"Funcionando ConnectRunnable");
  }

  static void Disconnect(){
    try{
      connectionSocket?.destroy();
    }catch(e){
      print(e);
    }
  }

  static Future<void> writeData(List<int> data) async {

    if (isConnected()) {
      print("Antes");
      //await Isolate.spawn(sendSocketData,data);
      MeshCommunicator.sendSocketData(data);
      print("Despues await");
    }

  }

  static ConnectRunnableRun(dynamic message) async {
    print(message);

    InternetAddress? serverAddr = InternetAddress.tryParse(serverIp);
    if (serverAddr == null) {
      print('Dirección IP no válida');
      return;
    }

    try {
      connectionSocket = await Socket.connect(serverAddr, serverPort);
      connectionSocket?.listen(dataHandler,
          onError: errorHandler,
          onDone: doneHandler,
          cancelOnError: false);
      print("Conectado!");

    } catch (e) {
      print(e);
    }

  }
  static void dataHandler(data) {
    MESH_DATA_RECVD += utf8.decode(data);

    if (MESH_DATA_RECVD.contains('}')) {
      int realLen = MESH_DATA_RECVD.lastIndexOf("}");
      MESH_DATA_RECVD = MESH_DATA_RECVD.substring(0, realLen + 1);
      handleReceivedMessage(MESH_DATA_RECVD);

      MESH_DATA_RECVD = '';
    }
  }

  static void handleReceivedMessage(String message) {
    // Process the received message
    print("Datos a llegar: "+ MESH_DATA_RECVD);
  }

  static void errorHandler(error, StackTrace trace){
    print("Tuviste un error" + error);
  }

  static void doneHandler(){
    print("Desconexion");
    connectionSocket?.destroy();
  }

  Stream<String> receiveSocketData(Socket socket) async* {
    var streamController = StreamController<String>();
    var data = StringBuffer();

    socket.listen((List<int> bytes) {
      var chunk = String.fromCharCodes(bytes);
      data.write(chunk);

      // Busca un delimitador que indique el final de un mensaje
      var delimiterIndex = data.toString().indexOf('\n');
      while (delimiterIndex != -1) {
        var message = data.toString().substring(0, delimiterIndex);
        streamController.add(message);
        data = StringBuffer(data.toString().substring(delimiterIndex + 1));
        delimiterIndex = data.toString().indexOf('\n');
      }
    });

    await for (var message in streamController.stream) {
      yield message;
    }
  }

  static Stream<void> sendSocketData(List<int> data) async* {
    String msg = json.encode(data);
    print("msg: " + msg);
    try{
      connectionSocket?.add(data);
      connectionSocket?.add([0]);
      connectionSocket?.flush();
    }
    catch(e){
      print(e);
    }

  }




}



