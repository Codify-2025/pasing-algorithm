package org.example;
import java.util.*;

public class Tokenizer {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            // 기본형
            "int", "char", "float", "double", "bool", "void",

            // 제어 흐름
            "if", "else", "while", "for", "do", "switch", "case", "default",
            "break", "continue", "return", "goto",

            // C++ 관련
            "class", "struct", "public", "private", "protected",
            "new", "delete", "this", "namespace", "using",
            "try", "catch", "throw", "const", "static", "virtual",

            // 입출력
            "cin", "cout", "endl",

            // 기타
            "true", "false", "sizeof", "include", "typedef", "inline", "enum"
    ));

    //단어가 예약어인지 확인
    //예약어 사전에 포함되어 있다면 true, 없다면 false
    private static boolean isKeyword(String word) {
        return KEYWORDS.contains(word);
    }

    //c가 symbol인지 확인
    //해당 문자열에 symbol이 있다면 true 반환
    private static boolean isSymbol(char c) {
        return ";(){}[]+-*/=<>,.&|!".indexOf(c) != -1;
    }

    //token class 생성
    //json으로 출력
    public static class Token {
        String type;
        String value;
        int line;
        int column;

        public Token(String type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return String.format("{\"type\": \"%s\", \"value\": \"%s\", \"line\": %d, \"column\": %d}",
                    type, value, line, column);
        }
    }

    public static List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        int current = 0;
        int line = 1;
        int column = 0;

        while (current < code.length()) {
            char c = code.charAt(current);

            // 공백과 탭
            if (c == ' ' || c == '\t') {
                current++;
                column++;
                continue;
            }

            // 줄바꿈
            if (c == '\n') {
                current++;
                line++;
                column = 0;
                continue;
            }

            // 주석 (//)
            if (c == '/' && current + 1 < code.length() && code.charAt(current + 1) == '/') {
                while (current < code.length() && code.charAt(current) != '\n') {
                    current++;
                }
                continue;
            }

            // 문자열 리터럴
            if (c == '"') {
                int startColumn = column;
                current++;
                column++;
                StringBuilder sb = new StringBuilder();
                while (current < code.length() && code.charAt(current) != '"') {
                    sb.append(code.charAt(current));
                    current++;
                    column++;
                }
                current++; // closing "
                column++;
                tokens.add(new Token("STRING", sb.toString(), line, startColumn));
                continue;
            }

            // 숫자
            if (Character.isDigit(c)) {
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (current < code.length() && Character.isDigit(code.charAt(current))) {
                    sb.append(code.charAt(current));
                    current++;
                    column++;
                }
                tokens.add(new Token("NUMBER", sb.toString(), line, startColumn));
                continue;
            }

            // 키워드/식별자
            if (Character.isLetter(c) || c == '_') {
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (current < code.length() && (Character.isLetterOrDigit(code.charAt(current)) || code.charAt(current) == '_')) {
                    sb.append(code.charAt(current));
                    current++;
                    column++;
                }
                String value = sb.toString();
                String type = isKeyword(value) ? "KEYWORD" : "IDENT";
                tokens.add(new Token(type, value, line, startColumn));
                continue;
            }

            // 기호
            if (isSymbol(c)) {
                tokens.add(new Token("SYMBOL", Character.toString(c), line, column));
                current++;
                column++;
                continue;
            }

            // 기타 문자 처리
            current++;
            column++;
        }

        return tokens;
    }

}
