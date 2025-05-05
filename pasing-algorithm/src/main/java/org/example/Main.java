package org.example;

import java.util.List;

import static org.example.Tokenizer.tokenize;

public class Main {
    public static void main(String[] args) {
        String code = """
        #include <iostream>
        using namespace std;

        int add(int a, int b) {
            return a + b;
        }

        int main() {
            int x = 5;
            int y = 10;
            int sum = add(x, y);

            if (sum > 10) {
                cout << "Sum is greater than 10" << endl;
            } else {
                cout << "Sum is 10 or less" << endl;
            }

            for (int i = 0; i < 3; i++) {
                cout << i << " ";
            }

            return 0;
        }
        """;

        List<Tokenizer.Token> tokens = tokenize(code);
        for (Tokenizer.Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("Hello world!");
    }
}