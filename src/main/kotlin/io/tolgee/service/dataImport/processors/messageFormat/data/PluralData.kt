package io.tolgee.service.dataImport.processors.messageFormat.data

class PluralData {
    companion object {
        val DATA = mapOf<String, PluralLanguage>(
                "ach" to PluralLanguage(
                        abbreviation = "ach",
                        name = "Acholi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "af" to PluralLanguage(
                        abbreviation = "af",
                        name = "Afrikaans",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ak" to PluralLanguage(
                        abbreviation = "ak",
                        name = "Akan",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "am" to PluralLanguage(
                        abbreviation = "am",
                        name = "Amharic",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "an" to PluralLanguage(
                        abbreviation = "an",
                        name = "Aragonese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ar" to PluralLanguage(
                        abbreviation = "ar",
                        name = "Arabic",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 0
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 4,
                                        sample = 11
                                ),
                                PluralExample(
                                        plural = 5,
                                        sample = 100
                                )
                        ),
                        pluralsText = "nplurals = 6; plural = (n === 0 ? 0 : n === 1 ? 1 : n === 2 ? 2 : n % 100 >= 3 && n % 100 <= 10 ? 3 : n % 100 >= 11 ? 4 : 5)",
                        nplurals = 6,
                        pluralsFunc = { n -> if (n == 0) 0 else if (n == 1) 1 else if (n == 2) 2 else if (n % 100 >= 3 && n % 100 <= 10) 3 else if (n % 100 >= 11) 4 else 5 }
                ),
                "arn" to PluralLanguage(
                        abbreviation = "arn",
                        name = "Mapudungun",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "ast" to PluralLanguage(
                        abbreviation = "ast",
                        name = "Asturian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ay" to PluralLanguage(
                        abbreviation = "ay",
                        name = "Aymará",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "az" to PluralLanguage(
                        abbreviation = "az",
                        name = "Azerbaijani",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "be" to PluralLanguage(
                        abbreviation = "be",
                        name = "Belarusian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "bg" to PluralLanguage(
                        abbreviation = "bg",
                        name = "Bulgarian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "bn" to PluralLanguage(
                        abbreviation = "bn",
                        name = "Bengali",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "bo" to PluralLanguage(
                        abbreviation = "bo",
                        name = "Tibetan",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "br" to PluralLanguage(
                        abbreviation = "br",
                        name = "Breton",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "brx" to PluralLanguage(
                        abbreviation = "brx",
                        name = "Bodo",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "bs" to PluralLanguage(
                        abbreviation = "bs",
                        name = "Bosnian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "ca" to PluralLanguage(
                        abbreviation = "ca",
                        name = "Catalan",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "cgg" to PluralLanguage(
                        abbreviation = "cgg",
                        name = "Chiga",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "cs" to PluralLanguage(
                        abbreviation = "cs",
                        name = "Czech",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 1) 0 else if ((n >= 2 && n <= 4)) 1 else 2 }
                ),
                "csb" to PluralLanguage(
                        abbreviation = "csb",
                        name = "Kashubian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 1 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "cy" to PluralLanguage(
                        abbreviation = "cy",
                        name = "Welsh",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 8
                                )
                        ),
                        pluralsText = "nplurals = 4; plural = (n === 1 ? 0 : n === 2 ? 1 : (n !== 8 && n !== 11) ? 2 : 3)",
                        nplurals = 4,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n == 2) 1 else if ((n != 8 && n != 11)) 2 else 3 }
                ),
                "da" to PluralLanguage(
                        abbreviation = "da",
                        name = "Danish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "de" to PluralLanguage(
                        abbreviation = "de",
                        name = "German",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "doi" to PluralLanguage(
                        abbreviation = "doi",
                        name = "Dogri",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "dz" to PluralLanguage(
                        abbreviation = "dz",
                        name = "Dzongkha",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "el" to PluralLanguage(
                        abbreviation = "el",
                        name = "Greek",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "en" to PluralLanguage(
                        abbreviation = "en",
                        name = "English",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "eo" to PluralLanguage(
                        abbreviation = "eo",
                        name = "Esperanto",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "es" to PluralLanguage(
                        abbreviation = "es",
                        name = "Spanish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "et" to PluralLanguage(
                        abbreviation = "et",
                        name = "Estonian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "eu" to PluralLanguage(
                        abbreviation = "eu",
                        name = "Basque",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "fa" to PluralLanguage(
                        abbreviation = "fa",
                        name = "Persian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "ff" to PluralLanguage(
                        abbreviation = "ff",
                        name = "Fulah",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "fi" to PluralLanguage(
                        abbreviation = "fi",
                        name = "Finnish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "fil" to PluralLanguage(
                        abbreviation = "fil",
                        name = "Filipino",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "fo" to PluralLanguage(
                        abbreviation = "fo",
                        name = "Faroese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "fr" to PluralLanguage(
                        abbreviation = "fr",
                        name = "French",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "fur" to PluralLanguage(
                        abbreviation = "fur",
                        name = "Friulian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "fy" to PluralLanguage(
                        abbreviation = "fy",
                        name = "Frisian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ga" to PluralLanguage(
                        abbreviation = "ga",
                        name = "Irish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 7
                                ),
                                PluralExample(
                                        plural = 4,
                                        sample = 11
                                )
                        ),
                        pluralsText = "nplurals = 5; plural = (n === 1 ? 0 : n === 2 ? 1 : n < 7 ? 2 : n < 11 ? 3 : 4)",
                        nplurals = 5,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n == 2) 1 else if (n < 7) 2 else if (n < 11) 3 else 4 }
                ),
                "gd" to PluralLanguage(
                        abbreviation = "gd",
                        name = "Scottish Gaelic",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 20
                                )
                        ),
                        pluralsText = "nplurals = 4; plural = ((n === 1 || n === 11) ? 0 : (n === 2 || n === 12) ? 1 : (n > 2 && n < 20) ? 2 : 3)",
                        nplurals = 4,
                        pluralsFunc = { n -> if ((n == 1 || n == 11)) 0 else if ((n == 2 || n == 12)) 1 else if ((n > 2 && n < 20)) 2 else 3 }
                ),
                "gl" to PluralLanguage(
                        abbreviation = "gl",
                        name = "Galician",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "gu" to PluralLanguage(
                        abbreviation = "gu",
                        name = "Gujarati",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "gun" to PluralLanguage(
                        abbreviation = "gun",
                        name = "Gun",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "ha" to PluralLanguage(
                        abbreviation = "ha",
                        name = "Hausa",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "he" to PluralLanguage(
                        abbreviation = "he",
                        name = "Hebrew",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "hi" to PluralLanguage(
                        abbreviation = "hi",
                        name = "Hindi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "hne" to PluralLanguage(
                        abbreviation = "hne",
                        name = "Chhattisgarhi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "hr" to PluralLanguage(
                        abbreviation = "hr",
                        name = "Croatian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "hu" to PluralLanguage(
                        abbreviation = "hu",
                        name = "Hungarian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "hy" to PluralLanguage(
                        abbreviation = "hy",
                        name = "Armenian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "id" to PluralLanguage(
                        abbreviation = "id",
                        name = "Indonesian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "is" to PluralLanguage(
                        abbreviation = "is",
                        name = "Icelandic",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n % 10 !== 1 || n % 100 === 11)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n % 10 != 1 || n % 100 == 11) 0 else 1 }
                ),
                "it" to PluralLanguage(
                        abbreviation = "it",
                        name = "Italian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ja" to PluralLanguage(
                        abbreviation = "ja",
                        name = "Japanese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "jbo" to PluralLanguage(
                        abbreviation = "jbo",
                        name = "Lojban",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "jv" to PluralLanguage(
                        abbreviation = "jv",
                        name = "Javanese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 0
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 0)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 0) 0 else 1 }
                ),
                "ka" to PluralLanguage(
                        abbreviation = "ka",
                        name = "Georgian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "kk" to PluralLanguage(
                        abbreviation = "kk",
                        name = "Kazakh",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "km" to PluralLanguage(
                        abbreviation = "km",
                        name = "Khmer",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "kn" to PluralLanguage(
                        abbreviation = "kn",
                        name = "Kannada",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ko" to PluralLanguage(
                        abbreviation = "ko",
                        name = "Korean",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "ku" to PluralLanguage(
                        abbreviation = "ku",
                        name = "Kurdish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "kw" to PluralLanguage(
                        abbreviation = "kw",
                        name = "Cornish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 4
                                )
                        ),
                        pluralsText = "nplurals = 4; plural = (n === 1 ? 0 : n === 2 ? 1 : n === 3 ? 2 : 3)",
                        nplurals = 4,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n == 2) 1 else if (n == 3) 2 else 3 }
                ),
                "ky" to PluralLanguage(
                        abbreviation = "ky",
                        name = "Kyrgyz",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "lb" to PluralLanguage(
                        abbreviation = "lb",
                        name = "Letzeburgesch",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ln" to PluralLanguage(
                        abbreviation = "ln",
                        name = "Lingala",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "lo" to PluralLanguage(
                        abbreviation = "lo",
                        name = "Lao",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "lt" to PluralLanguage(
                        abbreviation = "lt",
                        name = "Lithuanian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 10
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "lv" to PluralLanguage(
                        abbreviation = "lv",
                        name = "Latvian",
                        examples = listOf(
                                PluralExample(
                                        plural = 2,
                                        sample = 0
                                ),
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n !== 0 ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n != 0) 1 else 2 }
                ),
                "mai" to PluralLanguage(
                        abbreviation = "mai",
                        name = "Maithili",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "mfe" to PluralLanguage(
                        abbreviation = "mfe",
                        name = "Mauritian Creole",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "mg" to PluralLanguage(
                        abbreviation = "mg",
                        name = "Malagasy",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "mi" to PluralLanguage(
                        abbreviation = "mi",
                        name = "Maori",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "mk" to PluralLanguage(
                        abbreviation = "mk",
                        name = "Macedonian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n === 1 || n % 10 === 1 ? 0 : 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n == 1 || n % 10 == 1) 0 else 1 }
                ),
                "ml" to PluralLanguage(
                        abbreviation = "ml",
                        name = "Malayalam",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "mn" to PluralLanguage(
                        abbreviation = "mn",
                        name = "Mongolian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "mni" to PluralLanguage(
                        abbreviation = "mni",
                        name = "Manipuri",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "mnk" to PluralLanguage(
                        abbreviation = "mnk",
                        name = "Mandinka",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 0
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 0 ? 0 : n === 1 ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 0) 0 else if (n == 1) 1 else 2 }
                ),
                "mr" to PluralLanguage(
                        abbreviation = "mr",
                        name = "Marathi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ms" to PluralLanguage(
                        abbreviation = "ms",
                        name = "Malay",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "mt" to PluralLanguage(
                        abbreviation = "mt",
                        name = "Maltese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 11
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 20
                                )
                        ),
                        pluralsText = "nplurals = 4; plural = (n === 1 ? 0 : n === 0 || ( n % 100 > 1 && n % 100 < 11) ? 1 : (n % 100 > 10 && n % 100 < 20 ) ? 2 : 3)",
                        nplurals = 4,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n == 0 || (n % 100 > 1 && n % 100 < 11)) 1 else if ((n % 100 > 10 && n % 100 < 20)) 2 else 3 }
                ),
                "my" to PluralLanguage(
                        abbreviation = "my",
                        name = "Burmese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "nah" to PluralLanguage(
                        abbreviation = "nah",
                        name = "Nahuatl",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "nap" to PluralLanguage(
                        abbreviation = "nap",
                        name = "Neapolitan",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "nb" to PluralLanguage(
                        abbreviation = "nb",
                        name = "Norwegian Bokmal",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ne" to PluralLanguage(
                        abbreviation = "ne",
                        name = "Nepali",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "nl" to PluralLanguage(
                        abbreviation = "nl",
                        name = "Dutch",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "nn" to PluralLanguage(
                        abbreviation = "nn",
                        name = "Norwegian Nynorsk",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "no" to PluralLanguage(
                        abbreviation = "no",
                        name = "Norwegian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "nso" to PluralLanguage(
                        abbreviation = "nso",
                        name = "Northern Sotho",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "oc" to PluralLanguage(
                        abbreviation = "oc",
                        name = "Occitan",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "or" to PluralLanguage(
                        abbreviation = "or",
                        name = "Oriya",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "pa" to PluralLanguage(
                        abbreviation = "pa",
                        name = "Punjabi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "pap" to PluralLanguage(
                        abbreviation = "pap",
                        name = "Papiamento",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "pl" to PluralLanguage(
                        abbreviation = "pl",
                        name = "Polish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 1 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 1) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "pms" to PluralLanguage(
                        abbreviation = "pms",
                        name = "Piemontese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ps" to PluralLanguage(
                        abbreviation = "ps",
                        name = "Pashto",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "pt" to PluralLanguage(
                        abbreviation = "pt",
                        name = "Portuguese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "rm" to PluralLanguage(
                        abbreviation = "rm",
                        name = "Romansh",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ro" to PluralLanguage(
                        abbreviation = "ro",
                        name = "Romanian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 20
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 1 ? 0 : (n === 0 || (n % 100 > 0 && n % 100 < 20)) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 1) 0 else if ((n == 0 || (n % 100 > 0 && n % 100 < 20))) 1 else 2 }
                ),
                "ru" to PluralLanguage(
                        abbreviation = "ru",
                        name = "Russian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "rw" to PluralLanguage(
                        abbreviation = "rw",
                        name = "Kinyarwanda",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sah" to PluralLanguage(
                        abbreviation = "sah",
                        name = "Yakut",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "sat" to PluralLanguage(
                        abbreviation = "sat",
                        name = "Santali",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sco" to PluralLanguage(
                        abbreviation = "sco",
                        name = "Scots",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sd" to PluralLanguage(
                        abbreviation = "sd",
                        name = "Sindhi",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "se" to PluralLanguage(
                        abbreviation = "se",
                        name = "Northern Sami",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "si" to PluralLanguage(
                        abbreviation = "si",
                        name = "Sinhala",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sk" to PluralLanguage(
                        abbreviation = "sk",
                        name = "Slovak",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n == 1) 0 else if ((n >= 2 && n <= 4)) 1 else 2 }
                ),
                "sl" to PluralLanguage(
                        abbreviation = "sl",
                        name = "Slovenian",
                        examples = listOf(
                                PluralExample(
                                        plural = 1,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 3,
                                        sample = 3
                                ),
                                PluralExample(
                                        plural = 0,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 4; plural = (n % 100 === 1 ? 1 : n % 100 === 2 ? 2 : n % 100 === 3 || n % 100 === 4 ? 3 : 0)",
                        nplurals = 4,
                        pluralsFunc = { n -> if (n % 100 == 1) 1 else if (n % 100 == 2) 2 else if (n % 100 == 3 || n % 100 == 4) 3 else 0 }
                ),
                "so" to PluralLanguage(
                        abbreviation = "so",
                        name = "Somali",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "son" to PluralLanguage(
                        abbreviation = "son",
                        name = "Songhay",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sq" to PluralLanguage(
                        abbreviation = "sq",
                        name = "Albanian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sr" to PluralLanguage(
                        abbreviation = "sr",
                        name = "Serbian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "su" to PluralLanguage(
                        abbreviation = "su",
                        name = "Sundanese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "sv" to PluralLanguage(
                        abbreviation = "sv",
                        name = "Swedish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "sw" to PluralLanguage(
                        abbreviation = "sw",
                        name = "Swahili",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "ta" to PluralLanguage(
                        abbreviation = "ta",
                        name = "Tamil",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "te" to PluralLanguage(
                        abbreviation = "te",
                        name = "Telugu",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "tg" to PluralLanguage(
                        abbreviation = "tg",
                        name = "Tajik",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "th" to PluralLanguage(
                        abbreviation = "th",
                        name = "Thai",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "ti" to PluralLanguage(
                        abbreviation = "ti",
                        name = "Tigrinya",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "tk" to PluralLanguage(
                        abbreviation = "tk",
                        name = "Turkmen",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "tr" to PluralLanguage(
                        abbreviation = "tr",
                        name = "Turkish",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "tt" to PluralLanguage(
                        abbreviation = "tt",
                        name = "Tatar",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "ug" to PluralLanguage(
                        abbreviation = "ug",
                        name = "Uyghur",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "uk" to PluralLanguage(
                        abbreviation = "uk",
                        name = "Ukrainian",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                ),
                                PluralExample(
                                        plural = 2,
                                        sample = 5
                                )
                        ),
                        pluralsText = "nplurals = 3; plural = (n % 10 === 1 && n % 100 !== 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2)",
                        nplurals = 3,
                        pluralsFunc = { n -> if (n % 10 == 1 && n % 100 != 11) 0 else if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) 1 else 2 }
                ),
                "ur" to PluralLanguage(
                        abbreviation = "ur",
                        name = "Urdu",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "uz" to PluralLanguage(
                        abbreviation = "uz",
                        name = "Uzbek",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "vi" to PluralLanguage(
                        abbreviation = "vi",
                        name = "Vietnamese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "wa" to PluralLanguage(
                        abbreviation = "wa",
                        name = "Walloon",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n > 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n > 1) 0 else 1 }
                ),
                "wo" to PluralLanguage(
                        abbreviation = "wo",
                        name = "Wolof",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
                "yo" to PluralLanguage(
                        abbreviation = "yo",
                        name = "Yoruba",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                ),
                                PluralExample(
                                        plural = 1,
                                        sample = 2
                                )
                        ),
                        pluralsText = "nplurals = 2; plural = (n !== 1)",
                        nplurals = 2,
                        pluralsFunc = { n -> if (n != 1) 0 else 1 }
                ),
                "zh" to PluralLanguage(
                        abbreviation = "zh",
                        name = "Chinese",
                        examples = listOf(
                                PluralExample(
                                        plural = 0,
                                        sample = 1
                                )
                        ),
                        pluralsText = "nplurals = 1; plural = 0",
                        nplurals = 1,
                        pluralsFunc = { n -> 0 }
                ),
        )

    }
}
