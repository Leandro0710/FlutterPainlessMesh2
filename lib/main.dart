import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a blue toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {

  StreamSubscription<dynamic>? streamSubscription;
  final eventChannel = const EventChannel('platform.testing/datos');
  final messageChannel = const BasicMessageChannel<String>(
      'platform.testing/message', StringCodec());
  static const platform = MethodChannel('samples.flutter.dev/battery');
  // Get battery level.

  final String ssid = "whateverYouLike";
  final String contrasena = "somethingSneaky";
  final int puerto = 5555;

  String nodo = " ";
  bool nodoanalisis = false;
  String meshIP = " ";

  List _lista = [];

  @override
  void dispose() {
    // Limpia el controlador cuando el Widget se descarte
    /*
    ssid.dispose();
    contrasena.dispose();
    puerto.dispose();

     */
    streamSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            /*
            TextField(
              showCursor: true,
              controller: ssid,
              decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: 'Nombre del wifi'
              ),
            ),
            TextField(
              showCursor: true,
              controller: contrasena,
              decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: 'clave'
              ),
            ),
            TextField(
              keyboardType: TextInputType.number,
              controller: puerto,
              decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: 'puerto'
              ),
            ),
             */
            ElevatedButton(
              onPressed: (){sendToAndroid();},
              child: const Text('Conexion wifi mesh'),
            ),
            SingleChildScrollView(
              child: Column(
                children: [
                  Text("===Datos===="),
                  Text(meshIP),
                Text(nodo),
              ],
              )
            )
          ],
        ),
      ),
    );
  }


  /*
  Future<void> _wifinodomesh() async {
    try {
      streamSubscription =
          eventChannel.receiveBroadcastStream().listen((event) {
            print('Conexion: $event');
            if (!mounted) return;
            setState(() {
              nodo = event;
              nodoanalisis = true;
            });
          }, onError: (error) {
            print(error);
          });

    } on Error catch (e) {
      nodo = "Failed";
    }

    setState(() {
    });
  }
*/
  Future<void> sendToAndroid() async {
    _lista.add(ssid);
    _lista.add(contrasena);
    _lista.add(puerto);
    try {
      int _meshIPint = await platform.invokeMethod('llegadadatos', [nodoanalisis, _lista] );
      //Vemos si esta haciendo el analisis o no

        if (!nodoanalisis){
          print("Analisis empezando");
          nodoanalisis = !nodoanalisis;
        }else{
          nodo = " ";
          streamSubscription?.cancel();
          streamSubscription = null;
          print("detener analisis");
          nodoanalisis = !nodoanalisis;
        }
      setState(() {meshIP= _meshIPint.toString(); });
    } catch (e) {
      print(e);
    }

  }


}
