import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

// Main entry point of the application
void main() {
  runApp(const MyApp());
}

// The main application widget
class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'URL Shortener', // Title of the application
      theme: ThemeData(
        textTheme: GoogleFonts.josefinSansTextTheme(), // Custom text theme
      ),
      home: const UrlShortenerScreen(), // Home screen
    );
  }
}

// The main screen for URL shortening
class UrlShortenerScreen extends StatefulWidget {
  const UrlShortenerScreen({Key? key}) : super(key: key);

  @override
  State<UrlShortenerScreen> createState() => _UrlShortenerScreenState();
}

// State for UrlShortenerScreen
class _UrlShortenerScreenState extends State<UrlShortenerScreen> {
  final TextEditingController _urlController =
      TextEditingController(); // Controller for URL input
  String _shortenedUrl = ""; // Holds the shortened URL
  String _selectedLanguage = 'en'; // Selected language for localization
  Map<String, String> _localizedText = {}; // Map for localized texts

  @override
  void initState() {
    super.initState();
    _loadLocalizedText(); // Load localized texts on initialization
  }

  // Load localized text from JSON file
  Future<void> _loadLocalizedText() async {
    try {
      // Attempt to load the localized JSON based on the selected language
      String jsonString =
          await rootBundle.loadString('assets/i18n/$_selectedLanguage.json');
      setState(() {
        _localizedText = Map<String, String>.from(
            json.decode(jsonString)); // Parse and store localized text
      });
    } catch (e) {
      // Fallback to English if the selected language file doesn't exist
      String jsonString = await rootBundle.loadString('assets/i18n/en.json');
      setState(() {
        _localizedText = Map<String, String>.from(
            json.decode(jsonString)); // Parse and store English text
      });
    }
  }

  // API call to shorten the URL
  Future<void> _shortenUrl() async {
    final String longUrl =
        _urlController.text; // Get the URL from the input field
    final url = Uri.parse(
        'https://cleanuri.com/api/v1/shorten'); // URL for shortening service

    final response = await http.post(
      url,
      body:
          jsonEncode({'url': longUrl}), // Send the long URL in the request body
      headers: {"Content-Type": "application/json"}, // Set content type to JSON
    );

    if (response.statusCode == 200) {
      final jsonResponse = jsonDecode(response.body); // Parse the response
      setState(() {
        _shortenedUrl = jsonResponse['result_url']; // Store the shortened URL
      });
    } else {
      setState(() {
        _shortenedUrl = _translate(
            'failedToShorten'); // Show error message if URL shortening fails
      });
    }
  }

  // Translate the given key to the corresponding localized text
  String _translate(String key) {
    return _localizedText[key] ??
        key; // Return the localized text or the key if not found
  }

  // Show a dialog to select the language
  Future<void> _showLanguageDialog() async {
    return showDialog<void>(
      context: context,
      barrierDismissible: true, // Dismiss the dialog on tap outside
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(_translate("selectLang")), // Dialog title
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                ListTile(
                  leading: const Text("🇬🇧"), // English flag
                  title: const Text("English"), // English option
                  onTap: () {
                    setState(() {
                      _selectedLanguage = 'en'; // Update selected language
                    });
                    _loadLocalizedText(); // Load localized text
                    Navigator.of(context).pop(); // Close dialog
                  },
                ),
                ListTile(
                  leading: const Text("🇮🇳"), // Hindi flag
                  title: const Text("Hindi"), // Hindi option
                  onTap: () {
                    setState(() {
                      _selectedLanguage = 'hi'; // Update selected language
                    });
                    _loadLocalizedText(); // Load localized text
                    Navigator.of(context).pop(); // Close dialog
                  },
                ),
                ListTile(
                  leading: const Text("🇨🇳"), // Chinese flag
                  title: const Text("Chinese"), // Chinese option
                  onTap: () {
                    setState(() {
                      _selectedLanguage = 'zh'; // Update selected language
                    });
                    _loadLocalizedText(); // Load localized text
                    Navigator.of(context).pop(); // Close dialog
                  },
                ),
                ListTile(
                  leading: const Text("🇪🇸"), // Spanish flag
                  title: const Text("Spanish"), // Spanish option
                  onTap: () {
                    setState(() {
                      _selectedLanguage = 'es'; // Update selected language
                    });
                    _loadLocalizedText(); // Load localized text
                    Navigator.of(context).pop(); // Close dialog
                  },
                ),
                // Add more languages here
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        elevation: 1,
        backgroundColor: Theme.of(context).primaryColor,
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const SizedBox(width: 56), // Spacer for layout
            Center(
              child: Text(
                _translate("appTitle"), // Application title
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.language,
                  color: Colors.white), // Language selection icon
              onPressed: _showLanguageDialog, // Open language selection dialog
            ),
          ],
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0), // Padding for content
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              TextField(
                autofocus: true, // Focus on this field on load
                controller: _urlController, // Controller for the input field
                decoration: InputDecoration(
                  labelText:
                      _translate('enterUrl'), // Label for the input field
                  border: const OutlineInputBorder(), // Border styling
                ),
              ),
              const SizedBox(height: 25), // Spacer
              FilledButton.icon(
                onPressed: _shortenUrl, // Call shorten URL method on press
                icon: const Icon(Icons.link), // Icon for the button
                label: Text(
                  _translate('shorten'), // Button label
                  style: GoogleFonts.josefinSans(
                      fontSize: 24, fontWeight: FontWeight.bold),
                ),
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 25, vertical: 15), // Button padding
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.0), // Button shape
                  ),
                ),
              ),
              const SizedBox(height: 20), // Spacer
              if (_shortenedUrl
                  .isNotEmpty) // Show results only if URL is shortened
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _translate('result'), // Result title
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                    const SizedBox(height: 5), // Spacer
                    GestureDetector(
                      onTap: () {
                        if (_shortenedUrl.isNotEmpty) {
                          Clipboard.setData(ClipboardData(
                              text:
                                  _shortenedUrl)); // Copy shortened URL to clipboard
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                                content: Text(_translate(
                                    "urlCopied"))), // Show confirmation
                          );
                        }
                      },
                      child: Text(
                        _shortenedUrl, // Display the shortened URL
                        style: const TextStyle(
                          color: Colors.blue,
                          fontSize: 16,
                          decoration: TextDecoration
                              .underline, // Underline for clickable text
                        ),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}
