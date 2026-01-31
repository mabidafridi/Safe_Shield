import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool isActive = false;
  final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();

  // DUMMY LIST for Time Being
  final List<String> unsafeLinks = [
    "http://malicious.com",
    "www.phishing-bank.com",
    "http://bad-site.org"
  ];

  final List<String> safeLinks = [
    "https://google.com",
    "www.flutter.dev",
    "http://github.com"
  ];

  @override
  void initState() {
    super.initState();
    _initNotifications();
  }

  // 1. Setup Notifications
  void _initNotifications() async {
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');
    const InitializationSettings initializationSettings =
        InitializationSettings(android: initializationSettingsAndroid);
    await flutterLocalNotificationsPlugin.initialize(initializationSettings);
  }

  // 2. Toggle Logic
  void _toggleProtection() {
    setState(() {
      isActive = !isActive;
    });

    if (isActive) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text("Protection Enabled. Running in Background...")));
      // Later we will add the code here to open Accessibility Settings
    }
  }

  // 3. Logic to check the link (Called when link is detected)
  Future<void> _analyzeUrl(String url) async {
    if (!isActive) return;

    bool isUnsafe = unsafeLinks.any((link) => url.contains(link));
    bool isSafe = safeLinks.any((link) => url.contains(link));

    String title = "URL Detected";
    String body = "Analyzing: $url";

    if (isUnsafe) {
      title = "⚠️ UNSAFE LINK DETECTED";
      body = "Probability: 95% Unsafe. Do not proceed.";
    } else if (isSafe) {
      title = "✅ Safe Link";
      body = "Probability: 99% Safe.";
    } else {
      title = "❓ Unknown Link";
      body = "Be careful. Probability: 50% Safe.";
    }

    _showNotification(title, body);
  }

  Future<void> _showNotification(String title, String body) async {
    const AndroidNotificationDetails androidPlatformChannelSpecifics =
        AndroidNotificationDetails(
      'url_channel_id',
      'URL Detection',
      channelDescription: 'Notifications for URL safety',
      importance: Importance.max,
      priority: Priority.high,
      showWhen: true,
    );
    const NotificationDetails platformChannelSpecifics =
        NotificationDetails(android: androidPlatformChannelSpecifics);
    await flutterLocalNotificationsPlugin.show(
        0, title, body, platformChannelSpecifics);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1E1E1E),
      appBar: AppBar(
        title: const Text("URL Shield", style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Status Text
            Text(
              isActive ? "PROTECTION ACTIVE" : "PROTECTION DISABLED",
              style: TextStyle(
                color: isActive ? Colors.greenAccent : Colors.redAccent,
                fontWeight: FontWeight.bold,
                letterSpacing: 1.2,
                fontSize: 18,
              ),
            ),
            const SizedBox(height: 40),

            // VPN Style Button
            GestureDetector(
              onTap: _toggleProtection,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                height: 200,
                width: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isActive
                      ? Colors.cyanAccent.withOpacity(0.2)
                      : Colors.grey.withOpacity(0.1),
                  border: Border.all(
                    color: isActive ? Colors.cyanAccent : Colors.grey,
                    width: 4,
                  ),
                  boxShadow: isActive
                      ? [
                          BoxShadow(
                              color: Colors.cyanAccent.withOpacity(0.5),
                              blurRadius: 30,
                              spreadRadius: 10)
                        ]
                      : [],
                ),
                child: Icon(
                  Icons.power_settings_new,
                  size: 80,
                  color: isActive ? Colors.cyanAccent : Colors.grey,
                ),
              ),
            ),

            const SizedBox(height: 50),

            // Testing Console (Only visible when Active)
            if (isActive) ...[
              const Text("Simulation Console",
                  style: TextStyle(color: Colors.white54)),
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ElevatedButton(
                    style:
                        ElevatedButton.styleFrom(backgroundColor: Colors.red),
                    onPressed: () => _analyzeUrl("http://malicious.com"),
                    child: const Text("Test Unsafe"),
                  ),
                  const SizedBox(width: 10),
                  ElevatedButton(
                    style:
                        ElevatedButton.styleFrom(backgroundColor: Colors.green),
                    onPressed: () => _analyzeUrl("https://google.com"),
                    child: const Text("Test Safe"),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}