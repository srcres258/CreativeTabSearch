package top.srcres.mods.creativetabsearch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchHelper {
    public static boolean validateSearchCommand(String command) {
        command = command.trim();
        Pattern pattern = Pattern.compile("@(?<cmd>\\w+)( +(?<arg>\\w+))*");
        Matcher matcher = pattern.matcher(command);
        return matcher.matches();
    }

    public static String cleanSearchCommand(String command) {
        if (command.length() < 2) {
            return command;
        }
        command = command.trim();
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < command.length(); i++) {
            if (command.charAt(i) == ' ' && command.charAt(i - 1) == ' ') {
                continue;
            }
            result.append(command.charAt(i));
        }
        return result.toString();
    }
}
