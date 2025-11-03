import 'package:flutter/material.dart';
import 'package:surveysdk_flutter/surveysdk_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _surveysdkFlutterPlugin = SurveySdkFlutter();
  String _sdkStatus = 'Not initialized';
  List<dynamic> _surveyIds = [];
  bool _configLoaded = false;

  @override
  void initState() {
    super.initState();
  }

  Future<void> initializeSDK() async {
    try {
      setState(() {
        _sdkStatus = 'Initializing...';
      });
      
      await _surveysdkFlutterPlugin.initialize('test-api-key');
      
      final isConfigLoaded = await _surveysdkFlutterPlugin.isConfigurationLoaded();
      final surveyIds = await _surveysdkFlutterPlugin.getSurveyIds();
      
      setState(() {
        _sdkStatus = 'Initialized ✅';
        _configLoaded = isConfigLoaded;
        _surveyIds = surveyIds;
      });
      
      _showSnackBar('SDK Initialized! Found ${surveyIds.length} surveys');
      
    } catch (e) {
      setState(() {
        _sdkStatus = 'Initialization Failed ❌';
      });
      _showSnackBar('Error: $e');
    }
  }

  Future<void> showAutoSurvey() async {
    try {
      await _surveysdkFlutterPlugin.showSurvey();
      _showSnackBar('Survey shown!');
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  Future<void> showSpecificSurvey(String surveyId) async {
    try {
      await _surveysdkFlutterPlugin.showSurveyById(surveyId);
      _showSnackBar('Survey $surveyId shown!');
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  Future<void> getDebugInfo() async {
    try {
      final debugStatus = await _surveysdkFlutterPlugin.getDebugStatus();
      _showDialog('Debug Status', debugStatus);
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  void _showDialog(String title, String content) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: Text(content),
          ),
          actions: [
            TextButton(
              child: const Text('OK'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Survey SDK Flutter Example'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Status Section
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('SDK Status: $_sdkStatus', 
                          style: const TextStyle(fontWeight: FontWeight.bold)),
                      Text('Config Loaded: ${_configLoaded ? '✅' : '❌'}'),
                      Text('Available Surveys: ${_surveyIds.length}'),
                    ],
                  ),
                ),
              ),
              
              const SizedBox(height: 16),
              
              // Controls Section
              Column(
                children: [
                  ElevatedButton(
                    onPressed: initializeSDK,
                    child: const Text('1. Initialize SDK'),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton(
                    onPressed: showAutoSurvey,
                    child: const Text('2. Show Auto Survey'),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton(
                    onPressed: getDebugInfo,
                    child: const Text('3. Get Debug Status'),
                  ),
                ],
              ),
              
              // Survey List
              if (_surveyIds.isNotEmpty) ...[
                const SizedBox(height: 16),
                const Text('Available Surveys:', 
                    style: TextStyle(fontWeight: FontWeight.bold)),
                ..._surveyIds.map((surveyId) => Card(
                  child: ListTile(
                    title: Text(surveyId.toString()),
                    trailing: IconButton(
                      icon: const Icon(Icons.play_arrow),
                      onPressed: () => showSpecificSurvey(surveyId.toString()),
                    ),
                  ),
                )).toList(),
              ],
            ],
          ),
        ),
      ),
    );
  }
}