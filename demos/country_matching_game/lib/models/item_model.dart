import 'package:flutter/material.dart';

class ItemModel {
  final String name;
  final String value;
  final String description;
  final Image image;
  bool accepting;

  ItemModel({
    required this.name,
    required this.value,
    required this.description,
    required this.image,
    this.accepting = false,
  });
}