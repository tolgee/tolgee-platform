package io.tolgee.helpers;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class TextHelper {
    public static ArrayList<String> splitOnNonEscapedDelimiter(String string, char delimiter) {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (character == delimiter && !TextHelper.isCharEscaped(i, string)) {
                result.add(TextHelper.removeEscapes(actual.toString()));
                actual = new StringBuilder();
                continue;
            }
            actual.append(string.charAt(i));
        }
        result.add(TextHelper.removeEscapes(actual.toString()));
        return result;
    }

    public static boolean isCharEscaped(int position, String fullString) {
        var escapeCharsCount = 0;
        while (position > -1 && fullString.charAt(position - 1) == '\\') {
            escapeCharsCount++;
            position--;
        }
        return escapeCharsCount % 2 == 1;
    }

    public static String removeEscapes(String text) {
        return Pattern.compile("\\\\?\\\\?").matcher(text).replaceAll(
                match -> {
                    if (match.group().equals("\\\\")) {
                        //this seems strange. We need to escape it once more for the replace logic
                        return "\\\\";
                    }
                    return "";
                }
        );
    }
}



