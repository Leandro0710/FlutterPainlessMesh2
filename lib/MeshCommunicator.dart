import 'dart:async';
import 'dart:io';
import 'dart:js_interop';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class MeshCommunicator
{
  static String DBG_TAG = "MeshCommunicator";

  //Acción para los datos MESH
  static String MESH_DATA_RECVD = "DATA";

  //Accion para el error del socket
  static String MESH_SOCKET_ERR = "DISCON";

  //Accion para el éxito de la conexión
  static String MESH_CONNECTED = "CON";

  //Accion para la lista de nodos
  static String MESH_NODES = "NODE";

  //Local copy of the mesh network AP gateway address */
  late String serverIp = "192.168.0.2";
  // Local copy of the mesh network communication port */
  static late int serverPort = 1234;

  static late Socket connectionSocket;

  //Verificar el socket si esta conectado
  static bool isConnected(){
    return !connectionSocket.isUndefinedOrNull && connectionSocket.done!=null;
  }
  //Abrir conexion de nodo
  void Connect(String ip, int port) {
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
      startSending();
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
  void dataHandler(data){
    MESH_DATA_RECVD = String.fromCharCodes(data).trim();

  }

  void errorHandler(error, StackTrace trace){
    print(error);
  }

  void doneHandler(){
    connectionSocket.destroy();
  }



}

class SendRunnable {

  static late List<int> dataa;
  static late StreamSink out;
  static bool hasMessage = false;

  SendRunnable(Socket server) {
    server.listen((data) {out = data as StreamSink;});
  }

  /*
   * Send data as bytes to the server
   * @param bytes Data to send
   */
  void Send(List<int> bytes) {
  SendRunnable.dataa = bytes;
  SendRunnable.hasMessage = true;
  }
  /*

  Future<void> run() async {
  print("Sending started");
  if (SendRunnable.hasMessage) {
  try {
  //Send the data
    SendRunnable.out.addStream(data, 0, data.length);
    SendRunnable.out.write(0);
  //Flush the stream to be sure all bytes has been written out
    SendRunnable.out.flush();
  } catch (IOException e) {
  Log.e(DBG_TAG, "Sending failed: " + e.getMessage());
  Disconnect(); //Gets stuck in a loop if we don't call this on error!
  sendMyBroadcast(MESH_SOCKET_ERR, e.getMessage());
  }
  this.hasMessage = false;
  this.data = null;
  Log.i(DBG_TAG, "Command has been sent!");
  }
  Log.i(DBG_TAG, "Sending stopped");
  }

   */

}


