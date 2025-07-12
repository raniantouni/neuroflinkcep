package utils;

import java.util.Map;
import java.util.TreeMap;

public class MapUtils {
    public static String write(Map<String, String> map) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                b.append(',');
            }
            b.append('"');
            writeEscaped(b, e.getKey());
            b.append("\":");
            if (e.getValue() == null) {
                b.append("null");
            } else {
                b.append('"');
                writeEscaped(b, e.getValue());
                b.append('"');
            }
        }
        b.append('}');
        return b.toString();
    }

    public static void writeEscaped(StringBuilder b, String str) {
        for (int i = 0, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    b.append('\\').append(c);
                    break;
                case '\b':
                    b.append("\\b");
                    break;
                case '\f':
                    b.append("\\f");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c <= 0x1f) {
                        // escape all control characters
                        b.append(encodeEscape(c));
                    } else {
                        b.append(c);
                    }
                    break;
            }
        }
    }

    public static Map<String, String> read(String str) {
        Map<String, String> result = new TreeMap<String, String>();
        int state = 0;
        StringBuilder key = new StringBuilder();
        StringBuilder val = new StringBuilder();
        for (int i = 0, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            switch (state) {
                case 0:
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b') {
                        continue;
                    }
                    if (c != '{') {
                        throw new RuntimeException("Malformed");
                    }
                    state = 1;
                    break;
                case 1:
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b') {
                        continue;
                    }
                    if (c == '}') {
                        state = 9;
                    } else if (c != '\"') {
                        throw new RuntimeException("Malformed");
                    } else {
                        state = 2;
                    }
                    break;
                case 2:
                    if (c == '\\') {
                        state = 3;
                    } else if (c == '\"') {
                        state = 4;
                    } else {
                        key.append(c);
                    }
                    break;
                case 3:
                    i = decodeEscape(key, c, str, i, n);
                    state = 2;
                    break;
                case 4:
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b') {
                        continue;
                    }
                    if (c == 'n' && i + 3 < n && str.charAt(i + 1) == 'u' && str.charAt(i + 2) == 'l'
                            && str.charAt(i + 3) == 'l') {
                        result.put(key.toString(), null);
                        key.setLength(0);
                        state = 8;
                        break;
                    }
                    if (c != ':') {
                        throw new RuntimeException("Malformed " + c + " @ " + i);
                    }
                    state = 5;
                    break;
                case 5:
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b') {
                        continue;
                    }
                    if (c == 'n' && i + 3 < n && str.charAt(i + 1) == 'u' && str.charAt(i + 2) == 'l'
                            && str.charAt(i + 3) == 'l') {
                        i += 3;
                        result.put(key.toString(), null);
                        key.setLength(0);
                        state = 8;
                        break;
                    }
                    if (c != '\"') {
                        throw new RuntimeException("Malformed " + c + " @ " + i);
                    }
                    state = 6;
                    break;
                case 6:
                    if (c == '\\') {
                        state = 7;
                    } else if (c == '\"') {
                        result.put(key.toString(), val.toString());
                        key.setLength(0);
                        val.setLength(0);
                        state = 8;
                    } else {
                        val.append(c);
                    }
                    break;
                case 7:
                    i = decodeEscape(val, c, str, i, n);
                    state = 6;
                    break;
                case 8:
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b') {
                        continue;
                    }
                    if (c == '}') {
                        state = 9;
                    } else if (c == ',') {
                        state = 1;
                    } else {
                        throw new RuntimeException("Malformed " + c + " @ " + i);
                    }
                    break;
                case 9:
                    if (!(c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b')) {
                        throw new RuntimeException("Malformed " + c + " @ " + i);
                    }
                    break;
            }
        }
        return result;
    }

    private static int decodeEscape(StringBuilder val, char c, String str, int i, int n) {
        switch (c) {
            case '\"':
            case '\\':
            case '/':
                val.append(c);
                return i;
            case 'b':
                val.append('\b');
                return i;
            case 'f':
                val.append('\f');
                return i;
            case 'n':
                val.append('\n');
                return i;
            case 'r':
                val.append('\r');
                return i;
            case 't':
                val.append('\t');
                return i;
            case 'u':
                if (i + 4 < n) {
                    int n1 = Character.digit(str.charAt(i + 1), 16);
                    int n2 = Character.digit(str.charAt(i + 2), 16);
                    int n3 = Character.digit(str.charAt(i + 3), 16);
                    int n4 = Character.digit(str.charAt(i + 4), 16);
                    if (n1 == -1 || n2 == -1 || n3 == -1 || n4 == -1) {
                        throw new RuntimeException("Malformed");
                    }
                    val.append((char) ((n1 << 12) | (n2 << 8) | (n3 << 4) | n4));
                    return i + 4;
                } else {
                    throw new RuntimeException("Malformed");
                }
            default:
                throw new RuntimeException("Malformed");
        }

    }

    public static String encodeEscape(int c) {
        return "\\u" + Character.forDigit((c >> 12) & 15, 16)
                + Character.forDigit((c >> 8) & 15, 16)
                + Character.forDigit((c >> 4) & 15, 16)
                + Character.forDigit(c & 15, 16);
    }
}
