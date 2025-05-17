package org.example;

import java.util.*;

public class Tokenizer {

    //예약어 사전
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            //type
            "int", "char", "float", "double", "bool", "void",

            //조건, 반복, 결과
            "if", "else", "while", "for", "do", "switch", "case", "default",
            "break", "continue", "return", "goto",

            //c++ 관련
            "class", "struct", "public", "private", "protected",
            "new", "delete", "this", "namespace", "using",
            "try", "catch", "throw", "const", "static", "virtual",

            //입출력
            "cin", "cout", "endl",

            //기타
            "true", "false", "sizeof", "include", "typedef", "inline", "enum"
    ));

    //단어가 예약어인지 확인하는 method -> KEYWORD에 포함되어 있다면 true, 없다면 false
    private static boolean isKeyword(String word) {
        return KEYWORDS.contains(word);
    }


    //문자열이 symbol인지 확인 -> symbol 이라면 true, 아니라면 false
    private static boolean isSymbol(char c) {
        return ";(){}[]+-*/=<>,.&|!".indexOf(c) != -1;
    }

    //token화 이후 결과를 저장하기 위한 Token class
    //토큰화 결과 json으로 출력
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
        //Token 객체들을 저장할 list 생성
        List<Token> tokens = new ArrayList<>();
        //현재 위치
        int current = 0;
        //코드 행(row)
        int line = 1;
        //코드 열
        int column = 0;

        //code 길이만큼 반복
        while (current < code.length()) {

            //현재 위치 index의 문자열을 c에 저장
            char c = code.charAt(current);

            //char 기반 분기 -> switch로 처리
            //나머지 조건문은 if-else로 처리
            switch (c) {

                //공백과 탭
                //다음 열로 이동
                case ' ':
                case '\t': {
                    current++;
                    column++;
                    break;
                }

                //줄바꿈
                //다음 줄로 이동하고 column 초기화
                case '\n': {
                    current++;
                    line++;
                    column = 0;
                    break;
                }

                //주석 또는 symbol
                case '/': {
                    if (current + 1 < code.length() && code.charAt(current + 1) == '/') {
                        int startColumn = column;
                        StringBuilder sb = new StringBuilder();
                        sb.append("//");
                        current += 2;
                        column += 2;

                        while (current < code.length() && code.charAt(current) != '\n') {
                            sb.append(code.charAt(current));
                            current++;
                            column++;
                        }
                        tokens.add(new Token("COMMENT", sb.toString(), line, startColumn));
                        break;
                    }

                    if (current + 1 < code.length() && code.charAt(current + 1) == '*') {
                        int startColumn = column;
                        int startLine = line;
                        StringBuilder sb = new StringBuilder();
                        sb.append("/*");
                        current += 2;
                        column += 2;

                        while (current + 1 < code.length()) {
                            char ch = code.charAt(current);
                            char next = code.charAt(current + 1);

                            sb.append(ch);
                            current++;
                            column++;

                            if (ch == '\n') {
                                line++;
                                column = 0;
                            }

                            if (ch == '*' && next == '/') {
                                sb.append('/');
                                current++;
                                column++;
                                break;
                            }
                        }
                        tokens.add(new Token("COMMENT", sb.toString(), startLine, startColumn));
                    }
                    break;
                }

                //문자열 리터럴
                //시작 문자~끝 문자를 StringBuilder객체에 붙여서 저장
                case '"': {
                    int startColumn = column;
                    current++;
                    column++;
                    StringBuilder sb = new StringBuilder();
                    while (current < code.length() && code.charAt(current) != '"') {
                        sb.append(code.charAt(current));
                        current++;
                        column++;
                    }
                    current++;
                    column++;
                    tokens.add(new Token("STRING", sb.toString(), line, startColumn));
                    break;
                }

                default:
                    //숫자
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

                    //키워드, 식별자
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

                    //기호
                    if (isSymbol(c)) {
                        tokens.add(new Token("SYMBOL", Character.toString(c), line, column));
                        current++;
                        column++;
                        continue;
                    }

                    current++;
                    column++;
            }
        }

        return tokens;
    }

}
