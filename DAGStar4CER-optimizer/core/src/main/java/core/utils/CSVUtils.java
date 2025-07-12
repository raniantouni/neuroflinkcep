package core.utils;

import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CSVUtils {
    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public static String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(CSVUtils::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

}
