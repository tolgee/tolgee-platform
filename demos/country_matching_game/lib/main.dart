import 'package:country_matching_game/home_page.dart';
import 'package:flutter/material.dart';
import 'package:tolgee/tolgee.dart';

void main() async {
  await Tolgee.init(
      // apiKey: 'YOUR_API_KEY',
      // apiUrl: 'https://app.tolgee.io/v2',
      );
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    List<Locale> supportedLocales = Tolgee.supportedLocales.toList();

    return MaterialApp(
      title: Tolgee.baseLocale.languageCode,
      localizationsDelegates: Tolgee.localizationDelegates,
      supportedLocales: supportedLocales,
      locale: supportedLocales.first,
      debugShowCheckedModeBanner: false,
      home: HomePage(supportedLocales: supportedLocales),
    );
  }
}
