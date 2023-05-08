import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:js_interop';

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
  static String serverIp = "192.168.0.2";
  // Local copy of the mesh network communication port */
  static int serverPort = 1234;

  static late Socket connectionSocket;

  var streamController = StreamController<String>();
  var data = StringBuffer();


  //Verificar el socket si esta conectado
  static bool isConnected(){
    return false;
  }
  //Abrir conexion de nodo
  static void Connect(String ip, int port) {
    serverIp = ip;
    serverPort = port;
  }

  static void Disconnect(){
    stopStream();
    try{
      connectionSocket.destroy();
    }catch(e){
      print(e);
    }
  }

  static void writeData(List<int> data) {
    if (isConnected()) {
      sendSocketData(data);
    }

  }

  static void startSending() {
    //sendRunnable = new SendRunnable(connectionSocket);
    //sendThread = new Thread(sendRunnable);
    //sendThread.start();
  }


  static void stopStream() {
    /*if (receiveThread != null)
      receiveThread.interrupt();

    if (sendThread != null)
      sendThread.interrupt();

     */
  }


  ConnectRunnableRun(dynamic message) async {

    InternetAddress? serverAddr = InternetAddress.tryParse(serverIp);
    if (serverAddr == null) {
      print('Dirección IP no válida');
      return;
    }

    try {
      connectionSocket = await Socket.connect(serverAddr, serverPort);
      connectionSocket.setOption(SocketOption.tcpNoDelay, true);
      connectionSocket.listen(dataHandler,
          onError: errorHandler,
          onDone: doneHandler,
          cancelOnError: false);
      print("Conectado!");

    } catch (e) {
      print(e);
    }

    stdin.listen((data) =>
        connectionSocket.write(
            new String.fromCharCodes(data).trim() + '\n'));
  }
  void dataHandler(data) {
    MESH_DATA_RECVD += utf8.decode(data);

    if (MESH_DATA_RECVD.contains('}')) {
      int realLen = MESH_DATA_RECVD.lastIndexOf("}");
      MESH_DATA_RECVD = MESH_DATA_RECVD.substring(0, realLen + 1);

      handleReceivedMessage(MESH_DATA_RECVD);

      MESH_DATA_RECVD = '';
    }
  }

  void handleReceivedMessage(String message) {
    // Process the received message
    print(MESH_DATA_RECVD);
  }

  void errorHandler(error, StackTrace trace){
    print(error);
  }

  void doneHandler(){
    connectionSocket.destroy();
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

  static Stream<String> sendSocketData(List<int> data) async* {
    String msg = json.encode(data);


    connectionSocket.add(data);
    connectionSocket.add([0]);
    connectionSocket.flush();

    yield msg;
    await Future.delayed(Duration(seconds: 1));
  }




}



