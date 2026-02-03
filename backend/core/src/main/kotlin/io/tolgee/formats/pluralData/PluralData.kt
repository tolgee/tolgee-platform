package io.tolgee.formats.pluralData

class PluralData {
  companion object {
    val DATA =
      mapOf<String, PluralLanguage>(
        "ach" to
          PluralLanguage(
            tag = "ach",
            name = "Acholi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "af" to
          PluralLanguage(
            tag = "af",
            name = "Afrikaans",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ak" to
          PluralLanguage(
            tag = "ak",
            name = "Akan",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "am" to
          PluralLanguage(
            tag = "am",
            name = "Amharic",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "an" to
          PluralLanguage(
            tag = "an",
            name = "Aragonese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ar" to
          PluralLanguage(
            tag = "ar",
            name = "Arabic",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 0,
                ),
                PluralExample(
                  plural = 1,
                  sample = 1,
                ),
                PluralExample(
                  plural = 2,
                  sample = 2,
                ),
                PluralExample(
                  plural = 3,
                  sample = 3,
                ),
                PluralExample(
                  plural = 4,
                  sample = 11,
                ),
                PluralExample(
                  plural = 5,
                  sample = 100,
                ),
              ),
            pluralsText = "nplurals=6; plural=(n == 0 ? 0 : n == 1 ? 1 : n == 2 ? 2 : n % 100 >= 3 && n % 100 <= 10 ? 3 : n % 100 >= 11 ? 4 : 5)",
            nplurals = 6,
            pluralsFunc = {
                n ->
              if (n == 0) {
                0
              } else if (n == 1) {
                1
              } else if (n == 2) {
                2
              } else if (n % 100 >= 3 && n % 100 <= 10) {
                3
              } else if (n % 100 >= 11) {
                4
              } else {
                5
              }
            },
          ),
        "arn" to
          PluralLanguage(
            tag = "arn",
            name = "Mapudungun",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "ast" to
          PluralLanguage(
            tag = "ast",
            name = "Asturian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ay" to
          PluralLanguage(
            tag = "ay",
            name = "Aymará",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "az" to
          PluralLanguage(
            tag = "az",
            name = "Azerbaijani",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "be" to
          PluralLanguage(
            tag = "be",
            name = "Belarusian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "bg" to
          PluralLanguage(
            tag = "bg",
            name = "Bulgarian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "bn" to
          PluralLanguage(
            tag = "bn",
            name = "Bengali",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "bo" to
          PluralLanguage(
            tag = "bo",
            name = "Tibetan",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "br" to
          PluralLanguage(
            tag = "br",
            name = "Breton",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "brx" to
          PluralLanguage(
            tag = "brx",
            name = "Bodo",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "bs" to
          PluralLanguage(
            tag = "bs",
            name = "Bosnian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "ca" to
          PluralLanguage(
            tag = "ca",
            name = "Catalan",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "cgg" to
          PluralLanguage(
            tag = "cgg",
            name = "Chiga",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "cs" to
          PluralLanguage(
            tag = "cs",
            name = "Czech",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if ((n >= 2 && n <= 4)) {
                1
              } else {
                2
              }
            },
          ),
        "csb" to
          PluralLanguage(
            tag = "csb",
            name = "Kashubian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 1 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n == 1) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "cy" to
          PluralLanguage(
            tag = "cy",
            name = "Welsh",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 3,
                ),
                PluralExample(
                  plural = 3,
                  sample = 8,
                ),
              ),
            pluralsText = "nplurals=4; plural=(n == 1 ? 0 : n == 2 ? 1 : (n != 8 && n != 11) ? 2 : 3)",
            nplurals = 4,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if (n == 2) {
                1
              } else if ((n != 8 && n != 11)) {
                2
              } else {
                3
              }
            },
          ),
        "da" to
          PluralLanguage(
            tag = "da",
            name = "Danish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "de" to
          PluralLanguage(
            tag = "de",
            name = "German",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "doi" to
          PluralLanguage(
            tag = "doi",
            name = "Dogri",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "dz" to
          PluralLanguage(
            tag = "dz",
            name = "Dzongkha",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "el" to
          PluralLanguage(
            tag = "el",
            name = "Greek",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "en" to
          PluralLanguage(
            tag = "en",
            name = "English",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "eo" to
          PluralLanguage(
            tag = "eo",
            name = "Esperanto",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "es" to
          PluralLanguage(
            tag = "es",
            name = "Spanish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "et" to
          PluralLanguage(
            tag = "et",
            name = "Estonian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "eu" to
          PluralLanguage(
            tag = "eu",
            name = "Basque",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "fa" to
          PluralLanguage(
            tag = "fa",
            name = "Persian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "ff" to
          PluralLanguage(
            tag = "ff",
            name = "Fulah",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "fi" to
          PluralLanguage(
            tag = "fi",
            name = "Finnish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "fil" to
          PluralLanguage(
            tag = "fil",
            name = "Filipino",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "fo" to
          PluralLanguage(
            tag = "fo",
            name = "Faroese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "fr" to
          PluralLanguage(
            tag = "fr",
            name = "French",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "fur" to
          PluralLanguage(
            tag = "fur",
            name = "Friulian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "fy" to
          PluralLanguage(
            tag = "fy",
            name = "Frisian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ga" to
          PluralLanguage(
            tag = "ga",
            name = "Irish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 3,
                ),
                PluralExample(
                  plural = 3,
                  sample = 7,
                ),
                PluralExample(
                  plural = 4,
                  sample = 11,
                ),
              ),
            pluralsText = "nplurals=5; plural=(n == 1 ? 0 : n == 2 ? 1 : n < 7 ? 2 : n < 11 ? 3 : 4)",
            nplurals = 5,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if (n == 2) {
                1
              } else if (n < 7) {
                2
              } else if (n < 11) {
                3
              } else {
                4
              }
            },
          ),
        "gd" to
          PluralLanguage(
            tag = "gd",
            name = "Scottish Gaelic",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 3,
                ),
                PluralExample(
                  plural = 3,
                  sample = 20,
                ),
              ),
            pluralsText = "nplurals=4; plural=((n == 1 || n == 11) ? 0 : (n == 2 || n == 12) ? 1 : (n > 2 && n < 20) ? 2 : 3)",
            nplurals = 4,
            pluralsFunc = {
                n ->
              if ((n == 1 || n == 11)) {
                0
              } else if ((n == 2 || n == 12)) {
                1
              } else if ((n > 2 && n < 20)) {
                2
              } else {
                3
              }
            },
          ),
        "gl" to
          PluralLanguage(
            tag = "gl",
            name = "Galician",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "gu" to
          PluralLanguage(
            tag = "gu",
            name = "Gujarati",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "gun" to
          PluralLanguage(
            tag = "gun",
            name = "Gun",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "ha" to
          PluralLanguage(
            tag = "ha",
            name = "Hausa",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "he" to
          PluralLanguage(
            tag = "he",
            name = "Hebrew",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "hi" to
          PluralLanguage(
            tag = "hi",
            name = "Hindi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "hne" to
          PluralLanguage(
            tag = "hne",
            name = "Chhattisgarhi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "hr" to
          PluralLanguage(
            tag = "hr",
            name = "Croatian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "hu" to
          PluralLanguage(
            tag = "hu",
            name = "Hungarian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "hy" to
          PluralLanguage(
            tag = "hy",
            name = "Armenian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "id" to
          PluralLanguage(
            tag = "id",
            name = "Indonesian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "is" to
          PluralLanguage(
            tag = "is",
            name = "Icelandic",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n % 10 != 1 || n % 100 == 11)",
            nplurals = 2,
            pluralsFunc = { n -> if (n % 10 != 1 || n % 100 == 11) 0 else 1 },
          ),
        "it" to
          PluralLanguage(
            tag = "it",
            name = "Italian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ja" to
          PluralLanguage(
            tag = "ja",
            name = "Japanese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "jbo" to
          PluralLanguage(
            tag = "jbo",
            name = "Lojban",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "jv" to
          PluralLanguage(
            tag = "jv",
            name = "Javanese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 0,
                ),
                PluralExample(
                  plural = 1,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 0)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 0) 0 else 1 },
          ),
        "ka" to
          PluralLanguage(
            tag = "ka",
            name = "Georgian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "kk" to
          PluralLanguage(
            tag = "kk",
            name = "Kazakh",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "km" to
          PluralLanguage(
            tag = "km",
            name = "Khmer",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "kn" to
          PluralLanguage(
            tag = "kn",
            name = "Kannada",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ko" to
          PluralLanguage(
            tag = "ko",
            name = "Korean",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "ku" to
          PluralLanguage(
            tag = "ku",
            name = "Kurdish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "kw" to
          PluralLanguage(
            tag = "kw",
            name = "Cornish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 3,
                ),
                PluralExample(
                  plural = 3,
                  sample = 4,
                ),
              ),
            pluralsText = "nplurals=4; plural=(n == 1 ? 0 : n == 2 ? 1 : n == 3 ? 2 : 3)",
            nplurals = 4,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if (n == 2) {
                1
              } else if (n == 3) {
                2
              } else {
                3
              }
            },
          ),
        "ky" to
          PluralLanguage(
            tag = "ky",
            name = "Kyrgyz",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "lb" to
          PluralLanguage(
            tag = "lb",
            name = "Letzeburgesch",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ln" to
          PluralLanguage(
            tag = "ln",
            name = "Lingala",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "lo" to
          PluralLanguage(
            tag = "lo",
            name = "Lao",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "lt" to
          PluralLanguage(
            tag = "lt",
            name = "Lithuanian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 10,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "lv" to
          PluralLanguage(
            tag = "lv",
            name = "Latvian",
            examples =
              listOf(
                PluralExample(
                  plural = 2,
                  sample = 0,
                ),
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n != 0 ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = { n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n != 0) {
                1
              } else {
                2
              }
            },
          ),
        "mai" to
          PluralLanguage(
            tag = "mai",
            name = "Maithili",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "mfe" to
          PluralLanguage(
            tag = "mfe",
            name = "Mauritian Creole",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "mg" to
          PluralLanguage(
            tag = "mg",
            name = "Malagasy",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "mi" to
          PluralLanguage(
            tag = "mi",
            name = "Maori",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "mk" to
          PluralLanguage(
            tag = "mk",
            name = "Macedonian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n == 1 || n % 10 == 1 ? 0 : 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n == 1 || n % 10 == 1) 0 else 1 },
          ),
        "ml" to
          PluralLanguage(
            tag = "ml",
            name = "Malayalam",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "mn" to
          PluralLanguage(
            tag = "mn",
            name = "Mongolian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "mni" to
          PluralLanguage(
            tag = "mni",
            name = "Manipuri",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "mnk" to
          PluralLanguage(
            tag = "mnk",
            name = "Mandinka",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 0,
                ),
                PluralExample(
                  plural = 1,
                  sample = 1,
                ),
                PluralExample(
                  plural = 2,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 0 ? 0 : n == 1 ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = { n ->
              if (n == 0) {
                0
              } else if (n == 1) {
                1
              } else {
                2
              }
            },
          ),
        "mr" to
          PluralLanguage(
            tag = "mr",
            name = "Marathi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ms" to
          PluralLanguage(
            tag = "ms",
            name = "Malay",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "mt" to
          PluralLanguage(
            tag = "mt",
            name = "Maltese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 11,
                ),
                PluralExample(
                  plural = 3,
                  sample = 20,
                ),
              ),
            pluralsText = "nplurals=4; plural=(n == 1 ? 0 : n == 0 || ( n % 100 > 1 && n % 100 < 11) ? 1 : (n % 100 > 10 && n % 100 < 20 ) ? 2 : 3)",
            nplurals = 4,
            pluralsFunc = {
                n ->
              if (n == 1) {
                0
              } else if (n == 0 || (n % 100 > 1 && n % 100 < 11)) {
                1
              } else if ((n % 100 > 10 && n % 100 < 20)) {
                2
              } else {
                3
              }
            },
          ),
        "my" to
          PluralLanguage(
            tag = "my",
            name = "Burmese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "nah" to
          PluralLanguage(
            tag = "nah",
            name = "Nahuatl",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "nap" to
          PluralLanguage(
            tag = "nap",
            name = "Neapolitan",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "nb" to
          PluralLanguage(
            tag = "nb",
            name = "Norwegian Bokmal",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ne" to
          PluralLanguage(
            tag = "ne",
            name = "Nepali",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "nl" to
          PluralLanguage(
            tag = "nl",
            name = "Dutch",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "nn" to
          PluralLanguage(
            tag = "nn",
            name = "Norwegian Nynorsk",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "no" to
          PluralLanguage(
            tag = "no",
            name = "Norwegian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "nso" to
          PluralLanguage(
            tag = "nso",
            name = "Northern Sotho",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "oc" to
          PluralLanguage(
            tag = "oc",
            name = "Occitan",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "or" to
          PluralLanguage(
            tag = "or",
            name = "Oriya",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "pa" to
          PluralLanguage(
            tag = "pa",
            name = "Punjabi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "pap" to
          PluralLanguage(
            tag = "pap",
            name = "Papiamento",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "pl" to
          PluralLanguage(
            tag = "pl",
            name = "Polish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 1 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n == 1) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "pms" to
          PluralLanguage(
            tag = "pms",
            name = "Piemontese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ps" to
          PluralLanguage(
            tag = "ps",
            name = "Pashto",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "pt" to
          PluralLanguage(
            tag = "pt",
            name = "Portuguese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "rm" to
          PluralLanguage(
            tag = "rm",
            name = "Romansh",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ro" to
          PluralLanguage(
            tag = "ro",
            name = "Romanian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 20,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 1 ? 0 : (n == 0 || (n % 100 > 0 && n % 100 < 20)) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if ((n == 0 || (n % 100 > 0 && n % 100 < 20))) {
                1
              } else {
                2
              }
            },
          ),
        "ru" to
          PluralLanguage(
            tag = "ru",
            name = "Russian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "rw" to
          PluralLanguage(
            tag = "rw",
            name = "Kinyarwanda",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sah" to
          PluralLanguage(
            tag = "sah",
            name = "Yakut",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "sat" to
          PluralLanguage(
            tag = "sat",
            name = "Santali",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sco" to
          PluralLanguage(
            tag = "sco",
            name = "Scots",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sd" to
          PluralLanguage(
            tag = "sd",
            name = "Sindhi",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "se" to
          PluralLanguage(
            tag = "se",
            name = "Northern Sami",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "si" to
          PluralLanguage(
            tag = "si",
            name = "Sinhala",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sk" to
          PluralLanguage(
            tag = "sk",
            name = "Slovak",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = { n ->
              if (n == 1) {
                0
              } else if ((n >= 2 && n <= 4)) {
                1
              } else {
                2
              }
            },
          ),
        "sl" to
          PluralLanguage(
            tag = "sl",
            name = "Slovenian",
            examples =
              listOf(
                PluralExample(
                  plural = 1,
                  sample = 1,
                ),
                PluralExample(
                  plural = 2,
                  sample = 2,
                ),
                PluralExample(
                  plural = 3,
                  sample = 3,
                ),
                PluralExample(
                  plural = 0,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=4; plural=(n % 100 == 1 ? 1 : n % 100 == 2 ? 2 : n % 100 == 3 || n % 100 == 4 ? 3 : 0)",
            nplurals = 4,
            pluralsFunc = {
                n ->
              if (n % 100 == 1) {
                1
              } else if (n % 100 == 2) {
                2
              } else if (n % 100 == 3 || n % 100 == 4) {
                3
              } else {
                0
              }
            },
          ),
        "so" to
          PluralLanguage(
            tag = "so",
            name = "Somali",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "son" to
          PluralLanguage(
            tag = "son",
            name = "Songhay",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sq" to
          PluralLanguage(
            tag = "sq",
            name = "Albanian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sr" to
          PluralLanguage(
            tag = "sr",
            name = "Serbian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "su" to
          PluralLanguage(
            tag = "su",
            name = "Sundanese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "sv" to
          PluralLanguage(
            tag = "sv",
            name = "Swedish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "sw" to
          PluralLanguage(
            tag = "sw",
            name = "Swahili",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "ta" to
          PluralLanguage(
            tag = "ta",
            name = "Tamil",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "te" to
          PluralLanguage(
            tag = "te",
            name = "Telugu",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "tg" to
          PluralLanguage(
            tag = "tg",
            name = "Tajik",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "th" to
          PluralLanguage(
            tag = "th",
            name = "Thai",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "ti" to
          PluralLanguage(
            tag = "ti",
            name = "Tigrinya",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "tk" to
          PluralLanguage(
            tag = "tk",
            name = "Turkmen",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "tr" to
          PluralLanguage(
            tag = "tr",
            name = "Turkish",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "tt" to
          PluralLanguage(
            tag = "tt",
            name = "Tatar",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "ug" to
          PluralLanguage(
            tag = "ug",
            name = "Uyghur",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "uk" to
          PluralLanguage(
            tag = "uk",
            name = "Ukrainian",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
                PluralExample(
                  plural = 2,
                  sample = 5,
                ),
              ),
            pluralsText = "nplurals=3; plural=(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
            nplurals = 3,
            pluralsFunc = {
                n ->
              if (n % 10 == 1 && n % 100 != 11) {
                0
              } else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) {
                1
              } else {
                2
              }
            },
          ),
        "ur" to
          PluralLanguage(
            tag = "ur",
            name = "Urdu",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "uz" to
          PluralLanguage(
            tag = "uz",
            name = "Uzbek",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "vi" to
          PluralLanguage(
            tag = "vi",
            name = "Vietnamese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "wa" to
          PluralLanguage(
            tag = "wa",
            name = "Walloon",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n > 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n > 1) 0 else 1 },
          ),
        "wo" to
          PluralLanguage(
            tag = "wo",
            name = "Wolof",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
        "yo" to
          PluralLanguage(
            tag = "yo",
            name = "Yoruba",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
                PluralExample(
                  plural = 1,
                  sample = 2,
                ),
              ),
            pluralsText = "nplurals=2; plural=(n != 1)",
            nplurals = 2,
            pluralsFunc = { n -> if (n != 1) 0 else 1 },
          ),
        "zh" to
          PluralLanguage(
            tag = "zh",
            name = "Chinese",
            examples =
              listOf(
                PluralExample(
                  plural = 0,
                  sample = 1,
                ),
              ),
            pluralsText = "nplurals=1; plural=0",
            nplurals = 1,
            pluralsFunc = { n -> 0 },
          ),
      )
  }
}
