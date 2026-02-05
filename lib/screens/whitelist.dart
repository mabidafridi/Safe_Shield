import 'package:flutter/material.dart';
import '../services/native_api.dart';

class WhitelistScreen extends StatefulWidget {
  const WhitelistScreen({super.key});
  @override
  State<WhitelistScreen> createState() => _WhitelistScreenState();
}

class _WhitelistScreenState extends State<WhitelistScreen> {
  final TextEditingController _controller = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _controller,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(hintText: "example.com", filled: true, fillColor: Colors.black12),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.add, color: Colors.cyanAccent),
                onPressed: () async {
                  if (_controller.text.isNotEmpty) {
                    await NativeApi.addToWhitelist(_controller.text);
                    _controller.clear();
                    setState(() {});
                  }
                },
              )
            ],
          ),
        ),
        Expanded(
          child: FutureBuilder<List<String>>(
            future: NativeApi.getWhitelist(),
            builder: (context, snapshot) {
              if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
              return ListView.builder(
                itemCount: snapshot.data!.length,
                itemBuilder: (ctx, i) => ListTile(
                  title: Text(snapshot.data![i], style: const TextStyle(color: Colors.white)),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red),
                    onPressed: () async {
                      await NativeApi.removeFromWhitelist(snapshot.data![i]);
                      setState(() {});
                    },
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}