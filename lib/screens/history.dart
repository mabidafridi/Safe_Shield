import 'package:flutter/material.dart';
import '../services/native_api.dart';

class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Map<String, dynamic>>>(
      future: NativeApi.getHistory(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
        final logs = snapshot.data!.reversed.toList();

        if (logs.isEmpty) {
          return const Center(child: Text("No Activity Yet", style: TextStyle(color: Colors.white54)));
        }

        return Column(
          children: [
            TextButton.icon(
              icon: const Icon(Icons.delete, color: Colors.redAccent),
              label: const Text("Clear History", style: TextStyle(color: Colors.redAccent)),
              onPressed: () async {
                await NativeApi.clearHistory();
                (context as Element).markNeedsBuild();
              },
            ),
            Expanded(
              child: ListView.builder(
                itemCount: logs.length,
                itemBuilder: (context, index) {
                  final log = logs[index];
                  final isBlocked = log['action'].toString().contains("BLOCKED");
                  return ListTile(
                    leading: Icon(isBlocked ? Icons.block : Icons.check_circle, color: isBlocked ? Colors.red : Colors.green),
                    title: Text(log['url'], style: const TextStyle(color: Colors.white)),
                    subtitle: Text("${log['timestamp']} • Score: ${log['score']}", style: const TextStyle(color: Colors.grey)),
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}