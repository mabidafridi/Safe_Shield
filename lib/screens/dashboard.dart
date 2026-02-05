import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import '../services/native_api.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> with SingleTickerProviderStateMixin {
  bool isProtected = true;
  bool isLoading = true;
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _loadState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);
    _scaleAnimation = Tween<double>(begin: 1.0, end: 1.1).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _loadState() async {
    final state = await NativeApi.getProtectionState();
    if (mounted) {
      setState(() {
        isProtected = state;
        isLoading = false;
        if (!isProtected) _controller.stop();
      });
    }
  }

  Future<void> _toggleProtection() async {
    if (await Permission.ignoreBatteryOptimizations.status.isDenied) {
       await Permission.ignoreBatteryOptimizations.request();
    }
    setState(() => isProtected = !isProtected);
    await NativeApi.setProtectionState(isProtected);

    if (isProtected) {
      _controller.repeat(reverse: true);
    } else {
      _controller.stop();
      _controller.value = 1.0;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) return const Center(child: CircularProgressIndicator());

    Color primaryColor = isProtected ? const Color(0xFF00FFC2) : const Color(0xFFFF3B3B);
    String statusText = isProtected ? "PROTECTION ACTIVE" : "PROTECTION PAUSED";

    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            statusText,
            style: TextStyle(
              color: primaryColor,
              fontSize: 24,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.5,
            ),
          ),
          const SizedBox(height: 50),
          GestureDetector(
            onTap: _toggleProtection,
            child: AnimatedBuilder(
              animation: _scaleAnimation,
              builder: (context, child) {
                return Transform.scale(
                  scale: isProtected ? _scaleAnimation.value : 1.0,
                  child: Container(
                    height: 220,
                    width: 220,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: const Color(0xFF1E1E1E),
                      border: Border.all(color: primaryColor, width: 5),
                      boxShadow: [
                        BoxShadow(
                          color: primaryColor.withOpacity(0.4),
                          blurRadius: isProtected ? 50 : 10,
                          spreadRadius: isProtected ? 10 : 2,
                        )
                      ],
                    ),
                    child: Icon(Icons.power_settings_new, size: 100, color: primaryColor),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}