import 'package:flutter/material.dart';
import '../services/native_api.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool? autoBlock;
  bool? showSafe;
  double? threshold;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final s = await NativeApi.getSettings();
    setState(() {
      autoBlock = s['autoBlock'];
      showSafe = s['showSafe'];
      threshold = (s['threshold'] as int).toDouble();
    });
  }

  Future<void> _save() async {
    await NativeApi.updateSettings(autoBlock: autoBlock!, showSafe: showSafe!, threshold: threshold!.toInt());
  }

  @override
  Widget build(BuildContext context) {
    if (autoBlock == null) return const Center(child: CircularProgressIndicator());
    return ListView(
      children: [
        SwitchListTile(
          title: const Text("Auto-Block High Threats", style: TextStyle(color: Colors.white)),
          value: autoBlock!,
          onChanged: (v) { setState(() => autoBlock = v); _save(); },
        ),
        SwitchListTile(
          title: const Text("Safe Link Toasts", style: TextStyle(color: Colors.white)),
          value: showSafe!,
          onChanged: (v) { setState(() => showSafe = v); _save(); },
        ),
        ListTile(
          title: Text("Sensitivity: ${threshold!.toInt()}%", style: const TextStyle(color: Colors.white)),
          subtitle: Slider(
            value: threshold!, min: 50, max: 100, divisions: 10,
            onChanged: (v) => setState(() => threshold = v),
            onChangeEnd: (v) => _save(),
          ),
        )
      ],
    );
  }
}