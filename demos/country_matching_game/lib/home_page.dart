import 'package:country_matching_game/models/item_model.dart';
import 'package:flutter/material.dart';
import 'package:tolgee/tolgee.dart';

class HomePage extends StatefulWidget {
  final List<Locale> supportedLocales;

  const HomePage({super.key, required this.supportedLocales});

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<ItemModel>? items;
  List<ItemModel>? items2;

  int score = 0;
  bool gameOver = false;
  String? matchDescription;

  @override
  void initState() {
    super.initState();
    initGame();
    Future.delayed(Duration.zero, () {
      _showHowToPlayDialog();
    });
  }

  void _showHowToPlayDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const TranslationText('How to Play'),
          content: const TranslationText('Instruction'),
          actions: <Widget>[
            ElevatedButton(
              child: const TranslationText('got it'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  initGame() {
    gameOver = false;
    score = 0;
    matchDescription = null; // Reset match description on new game
    items = [
      ItemModel(
          image: Image.asset('assets/icons/eagle.png',
              height: 50, width: 50, fit: BoxFit.contain),
          name: "country_USA",
          value: "USA",
          description: 'USA'),
      ItemModel(
          image: Image.asset('assets/icons/eiffel-tower.png',
              height: 50, width: 50, fit: BoxFit.contain),
          name: "country_france",
          value: "France",
          description: 'France'),
      ItemModel(
          image: Image.asset('assets/icons/head.png',
              height: 50, width: 50, fit: BoxFit.contain),
          name: "country_china",
          value: "China",
          description: 'China'),
      ItemModel(
          image: Image.asset('assets/icons/lotus-flower.png',
              height: 50, width: 50, fit: BoxFit.contain),
          name: "country_india",
          value: "India",
          description: 'india'),
      ItemModel(
          image: Image.asset("assets/icons/matryoshka-doll.png",
              height: 50, width: 50, fit: BoxFit.contain),
          name: "country_russia",
          value: "Russia",
          description: 'russia'),
    ];
    items2 = List<ItemModel>.from(items!);
    items!.shuffle();
    items2!.shuffle();
  }

  void checkGameOver() {
    if (items2!.isEmpty) {
      setState(() {
        gameOver = true;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.amber,
      appBar: AppBar(
        centerTitle: true,
        title: const Text('Country Matching Game'),
        actions: [
          IconButton(
            icon: const Icon(Icons.help_outline),
            onPressed: () {
              _showHowToPlayDialog();
            },
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: DropdownButton<Locale>(
                value:
                    Tolgee.currentLocale, // Set the currently selected locale
                onChanged: (Locale? newLocale) {
                  if (newLocale != null) {
                    setState(() {
                      Tolgee.setCurrentLocale(newLocale);
                    });
                  }
                },
                items: widget.supportedLocales.map((Locale locale) {
                  return DropdownMenuItem<Locale>(
                    value: locale,
                    child: Text(locale.languageCode),
                  );
                }).toList(),
              ),
            ),
            Text.rich(TextSpan(children: [
              const TextSpan(text: "Score: "),
              TextSpan(
                  text: "$score",
                  style: const TextStyle(
                    color: Colors.green,
                    fontWeight: FontWeight.bold,
                    fontSize: 30.0,
                  ))
            ])),
            if (matchDescription != null)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 10.0),
                child: TranslationWidget(
                  builder: (context, translationGetter) => Text(
                    translationGetter(matchDescription!),
                    style: const TextStyle(
                      fontSize: 16.0,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            if (!gameOver)
              Row(
                children: <Widget>[
                  Column(
                      children: items!.map((item) {
                    return Container(
                      margin: const EdgeInsets.all(8.0),
                      child: Draggable<ItemModel>(
                        data: item,
                        childWhenDragging: item.image,
                        feedback: item.image,
                        child: item.image,
                      ),
                    );
                  }).toList()),
                  const Spacer(),
                  Column(
                      children: items2!.map((item) {
                    return DragTarget<ItemModel>(
                      onAcceptWithDetails: (receivedItem) {
                        if (item.value == receivedItem.data.value) {
                          setState(() {
                            items!.remove(receivedItem);
                            items2!.remove(item);
                            score += 10;
                            matchDescription =
                                receivedItem.data.description.toString();
                            item.accepting = false;
                          });
                        } else {
                          setState(() {
                            score -= 5;
                            item.accepting = false;
                          });
                        }
                        checkGameOver(); // Check game over after each match
                      },
                      onLeave: (receivedItem) {
                        setState(() {
                          item.accepting = false;
                        });
                      },
                      onWillAcceptWithDetails: (receivedItem) {
                        setState(() {
                          item.accepting = true;
                        });
                        return true;
                      },
                      builder: (context, acceptedItems, rejectedItem) =>
                          Container(
                        color: item.accepting ? Colors.red : Colors.teal,
                        height: 50,
                        width: 100,
                        alignment: Alignment.center,
                        margin: const EdgeInsets.all(8.0),
                        child: TranslationWidget(
                          builder: (context, translationGetter) => Text(
                            translationGetter(item.name),
                            style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 18.0),
                          ),
                        ),
                      ),
                    );
                  }).toList()),
                ],
              ),
            if (gameOver)
              Column(
                children: [
                  TranslationWidget(
                    builder: (context, translationGetter) => Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text(
                          score > 0
                              ? translationGetter('congrats', {'score': score})
                              : translationGetter('loose', {'score': score}),
                          style: TextStyle(
                            color: score > 0 ? Colors.green : Colors.red,
                            fontWeight: FontWeight.bold,
                            fontSize: 24.0,
                          ),
                          textAlign: TextAlign.center,
                        )),
                  ),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    style: ButtonStyle(
                        textStyle: MaterialStateProperty.all(
                            const TextStyle(color: Colors.white)),
                        backgroundColor:
                            MaterialStateProperty.all(Colors.pink)),
                    child: const Text("New Game"),
                    onPressed: () {
                      initGame();
                      setState(() {});
                    },
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
