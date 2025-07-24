package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import static org.example.Parser.parse;
import static org.example.Tokenizer.tokenize;

public class Main {

    public static void printAST(Parser.ASTNode node, String indent) {
        System.out.println(indent + "Type: " + node.type + ", Value: " + node.value);
        for (Parser.ASTNode child : node.childern) {
            printAST(child, indent + "  "); // 들여쓰기 증가
        }
    }
    public static void main(String[] args) {
        String code = """
        #include <iostream>
        using namespace std;

        int add(int a, int b) {
            return a + b;
        }
        //주석 1
        
        /*
        주석 2
        */
        
        int main() {
            int x = 5;
            int y = 10;
            int sum = add(x, y);

            if (sum > 10) {
                cout << "Sum is greater than 10" << endl;
            } else {
                cout << "Sum is 10 or less" << endl;
            }

            for (int i = 0; i < 3; i += 2) {
                cout << i << " ";
            }

            return 0;
        }
        """;

        String code2 = """
        int add(int a, int b) {
            int k = 0;
            k = 1 + 2;
        }
        """;

        String code3 = """
        while (a > 0 && b < 10) {
            k++;
            return k;
        }
        int k;
        """;

        String code4 = """
        
        int add(int a, int b) {
            return a + b;
        }
        
        int main() {
            int x = 5;
            int y = 10;
            int sum = add(x, y);

            while(x < 10) {
                x++;
                y++;
            }

            return 0;
        }
        """;

        String code5 = """
        
        int add(int a, int b) {
            return a + b;
        }
        
        while (a > 0 && b < 10) {
            int k;
            k++;
        }
        
        """;

        String code6 = """
        
        int add(int a, int b) {
            while (a > 0 && b < 10) {
                int k;
                k++;
            }  
            return a + b;
        }
        """;

        String code7 = """  
        int k = 0;
        for(int i=0; i < 3 ; i++) {
            k++;
            }
        """;

        List<Tokenizer.Token> tokens = tokenize(code7);
        for (Tokenizer.Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("parsing");

        Parser.ASTNode astNode = parse(tokens);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(astNode);

        System.out.println(json);

        System.out.println("=== 최종 AST 구조 ===");
        printAST(astNode, "");

    }
}