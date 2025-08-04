package org.example;

import java.util.*;

public class Parser {

    //파싱 후 AST 를 저장하기 위한 ASTNode class
    //토큰화 후 json 형태로 출력
    public static class ASTNode {
        String type;
        String value;
        int line;
        List<ASTNode> childern = new ArrayList<>(); //AST Node의 자식 노드를 list에 저장 -> 근데 arrayList가 더 낫지 않나..?

        //constructor
        ASTNode(String type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        //children Node를 쌓는 method
        void addChild(ASTNode child) {
            childern.add(child);
        }
    }

    //astNode와 index를 동시에 출력하기 위한 객체
    public static class ParseResult {
        public ASTNode astNode;
        public int index;

        public ParseResult(ASTNode astNode, int index) {
            this.astNode = astNode;
            this.index = index;
        }
    }

    //parsingTable
    //key -> 상태:tokentype , value -> 다음 상태(s -> shift, r -> reduce, a -> 완료(accept), error -> 실패)
    static Map<String, String> parsingTable = new HashMap<>(); //Map vs Set,private static final로 보호해 줄 필요가 있을것 같은데..

    static {
        parsingTable.put("TYPE", "S");
        parsingTable.put("CONTROL", "S");
        parsingTable.put("IO", "S");
        parsingTable.put("ACCESS", "S");
        parsingTable.put("CXX", "S");
        parsingTable.put("EXCEPTION", "S");
        parsingTable.put("OTHERS", "S");
        parsingTable.put("IDENT", "S"); //함수명, 변수명
        parsingTable.put("NUMBER", "S");
        parsingTable.put("STRING", "S");
        parsingTable.put("SYMBOL:(", "S"); //소괄호 열기 -> 함수 정의 or 함수 call
        parsingTable.put("SYMBOL:)", "S"); //소괄호 닫기
        parsingTable.put("SYMBOL:{", "R_Block"); //대괄호 열기
        parsingTable.put("SYMBOL:}", "R_Block_Close"); //대괄호 닫기
        parsingTable.put("SYMBOL:;", "R_Stmt"); //세미콜론
        parsingTable.put("SYMBOL:=", "S");
        parsingTable.put("SYMBOL:+", "S");
        parsingTable.put("SYMBOL:++", "S");
        parsingTable.put("SYMBOL:-", "S");
        parsingTable.put("SYMBOL:--", "S");
        parsingTable.put("SYMBOL:=", "S");
        parsingTable.put("SYMBOL:+=", "S");
        parsingTable.put("SYMBOL:-=", "S");
        parsingTable.put("SYMBOL:==", "S");
        parsingTable.put("SYMBOL:>", "S");
        parsingTable.put("SYMBOL:>=", "S");
        parsingTable.put("SYMBOL:<", "S");
        parsingTable.put("SYMBOL:<=", "S");
        parsingTable.put("SYMBOL:&&", "S");
        parsingTable.put("SYMBOL:,", "S");
        parsingTable.put("KEYWORD:if", "S");
        parsingTable.put("KEYWORD:for", "S");
        parsingTable.put("EOF", "ACCEPT");
    }

    //method

    //산술 연산자 우선순위 함수
    public static int precedence(String op) {
        return switch (op) {
            case "=" -> 1;
            case "||" -> 2;
            case "&&" -> 3;
            case "==", "!=", "<", ">", "<=", ">=" -> 4;
            case "+", "-", "++", "--", "-=", "+=" -> 5;
            case "*", "/" -> 6;
            default -> 0;
        };
    }

    //연산자 판별 함수
    public static boolean isOperator(String value) {
        return value.equals("=") || value.equals("+") || value.equals("-") || value.equals("++") || value.equals("--")
                || value.equals("*") || value.equals("/")
                || value.equals("||") || value.equals("&&")
                || value.equals("==") || value.equals("!=") || value.equals("<") || value.equals(">") || value.equals("<=") || value.equals(">=")
                || value.equals("+=") || value.equals("-=");
    }

    public static boolean isUnaryExpr(String value) {
        return value.equals("++") || value.equals("--") || value.equals("!");
    }

    //연산자 탐색 함수
    public static ASTNode buildExpressionTree(Deque<ASTNode> tempDeque) {
        Deque<ASTNode> nodeDeque = new ArrayDeque<>();
        Deque<ASTNode> opDeque = new ArrayDeque<>();

        while (!tempDeque.isEmpty()) {
            ASTNode stmtNode = tempDeque.pop(); //deque의 원소 pop
            //if-else 문으로 연산자의 우선순위에 따라 stack에 넣음
            if (isOperator(stmtNode.value)) { //stmtNode가 연산자라면
                while (!opDeque.isEmpty() && precedence(opDeque.peek().value) >= precedence(stmtNode.value)) { //연산자의 우선순위 파악
                    //operatorNode pop
                    ASTNode op = opDeque.pop();
                    op.type = "Operator";

                    ASTNode right = nodeDeque.pop();
                    ASTNode left = nodeDeque.pop();
                    ASTNode opNode;

                    if (op.value.equals("=")) {
                        opNode = new ASTNode("InitExpr", op.value, stmtNode.line);
                    } else if (op.value.equals("!") || op.value.equals("++") || op.value.equals("--")) {
                        opNode = new ASTNode("UnaryExpr", null, stmtNode.line);
                    } else {
                        opNode = new ASTNode("BinaryExpr", null, stmtNode.line);
                    }

                    opNode.addChild(right);
                    opNode.addChild(op);
                    opNode.addChild(left);
                    nodeDeque.push(opNode);
                }
                opDeque.push(stmtNode);
            } else {

                String stmtType = stmtNode.type;

                switch (stmtType) {
                    case "IDENT" -> {
                        stmtNode.type = "VariableName";
                    }
                    case "NUMBER" -> {
                        stmtNode.type = "Literal";
                    }
                    case "TYPE" -> {
                        stmtNode.type = "Type";
                    }
                }

                nodeDeque.push(stmtNode);
            }
        } //연산자 파악 후 노드스택에 넣는건 문제 x

            while (!opDeque.isEmpty()) {
                ASTNode op = opDeque.pop();
                op.type = "Operator";
                ASTNode right = nodeDeque.pop();
                ASTNode left = nodeDeque.pop();
                ASTNode opNode;
                if (op.value.equals("=")) {
                    opNode = new ASTNode("InitExpr", op.value, left.line);
                } else if (op.value.equals("!") || op.value.equals("++") || op.value.equals("--")) {
                    opNode = new ASTNode("UnaryExpr", null, left.line);
                } else {
                    opNode = new ASTNode("BinaryExpr", null, left.line);
                }
                opNode.addChild(right);
                opNode.addChild(op);
                opNode.addChild(left);
                nodeDeque.push(opNode);
            }
        //nodeStack을 ASTNode형태로 리턴하면 끝날듯...
        return nodeDeque.pop();
    }


    //{ 만났을때 파싱하는 함수
    public static ParseResult buildBlock(Deque<ASTNode> tempDeque, List<Tokenizer.Token> tokens, int index) {
        //선언부 파싱
        ASTNode declaration = tempDeque.removeLast();

        String declarationType = declaration.value;
        switch (declaration.value) {
            case "if" -> {
                declaration.type = "IfStmt";
                //()파싱 -> tempDeque에서 ()제거
                tempDeque.removeLast();
                tempDeque.removeFirst();
                ASTNode ifChild;
                if (tempDeque.size() > 1) {
                    ifChild = buildExpressionTree(tempDeque);
                } else {
                    //true, false에 따른 처리 또 필요할듯..
                    ifChild = new ASTNode("VariableName", tempDeque.pop().value, tempDeque.pop().line);
                }
                declaration.addChild(ifChild);
            }
            case "else" -> {
                declaration.type = "ElseStmt";
            }
            case "while" -> {
                declaration.type = "WhileStmt";

                tempDeque.removeLast();
                tempDeque.removeFirst();
                ASTNode whileChild;
                if (tempDeque.size() > 1) {
                    whileChild = buildExpressionTree(tempDeque);
                } else {
                    whileChild = new ASTNode("VariableName", tempDeque.pop().value, tempDeque.pop().line);
                }
                declaration.addChild(whileChild);
            }
            case "for" -> {
                declaration.type = "ForStmt";

                tempDeque.removeLast();
                tempDeque.removeFirst();
                int length = tempDeque.size();
                boolean isequal = false;
                Deque<ASTNode> forTempDeque = new ArrayDeque<>();
                for (int l = 0; l < length; l++) {
                    ASTNode node = tempDeque.removeLast();

                    if (node.value.equals("=")) {
                        isequal = true;
                    }
                    if (node.value.equals(";")) {
                        ASTNode forChild;
                        if (isequal) {
                            forChild = buildStmtNode(forTempDeque, index).astNode;
                        } else {
                            forChild = buildExpressionTree(forTempDeque);
                        }
                        declaration.addChild(forChild);
                        forTempDeque.clear();
                    } else {
                        forTempDeque.addFirst(node);
                    }
                }
                if (!forTempDeque.isEmpty()) {
                    if (forTempDeque.size() == 2) {
                        ASTNode var = forTempDeque.removeFirst();
                        var.type = "VariableName";
                        ASTNode op = forTempDeque.removeLast();
                        op.type = "Operator";
                        ASTNode forChild = new ASTNode("UnaryExpr", null, var.line);
                        forChild.addChild(var);
                        forChild.addChild(op);
                        declaration.addChild(forChild);
                    } else {
                        ASTNode forChild = buildExpressionTree(forTempDeque);
                        declaration.addChild(forChild);
                    }
                    forTempDeque.clear();
                }
            }
            default -> {
                ASTNode methodType = new ASTNode("Type", declaration.value, declaration.line);
                ASTNode methodName = tempDeque.removeLast();
                methodName.type = "MethodName";
                ASTNode paramList = new ASTNode("ParameterList", null, methodName.line);

                declaration.type = "MethodDeclaration";


                tempDeque.removeLast();
                tempDeque.removeFirst();
                if (!tempDeque.isEmpty()) {
                    int length = tempDeque.size();
                    Deque<ASTNode> methodTempDeque = new ArrayDeque<>();
                    for (int l = 0; l < length; l++) {
                        ASTNode node = tempDeque.removeLast();
                        if (node.value.equals(",")) {
                            ASTNode param = new ASTNode("Parameter", null, node.line);

                            ASTNode type = methodTempDeque.pop();
                            type.type = "Type";
                            ASTNode variable = methodTempDeque.pop();
                            variable.type = "VariableName";

                            param.addChild(type);
                            param.addChild(variable);

                            paramList.addChild(param);
                            methodTempDeque.clear();
                        } else {
                            methodTempDeque.addLast(node);
                        }
                    }
                    if (!methodTempDeque.isEmpty()) {
                        ASTNode param = new ASTNode("Parameter", null, paramList.line);

                        ASTNode type = methodTempDeque.pop();
                        type.type = "Type";
                        ASTNode variable = methodTempDeque.pop();
                        variable.type = "VariableName";

                        param.addChild(type);
                        param.addChild(variable);

                        paramList.addChild(param);
                    }
                    declaration.addChild(methodType);
                    declaration.addChild(methodName);
                    declaration.addChild(paramList);
                } else {
                    declaration.addChild(methodType);
                    declaration.addChild(methodName);
                }
            }
        }
        declaration.value = null;
        tempDeque.clear();

        //{뒤쪽 파싱 시작
        ASTNode blockStmt = new ASTNode("BlockStmt", null, tokens.get(index).line);
        boolean breakpoint = true;
        int i = index+1;
        while (breakpoint) {
            Tokenizer.Token token = tokens.get(i);
            String action = token.value;
            switch (action) {
                case ";" -> {
                    ParseResult result = buildStmtNode(tempDeque, index);
                    blockStmt.addChild(result.astNode);
                    tempDeque.clear();
                    i++;
                }
                case "{" -> {
                    ParseResult reResult = buildBlock(tempDeque, tokens, i);
                    blockStmt.addChild(reResult.astNode);
                    i = reResult.index+1;
                    tempDeque.clear();
                }
                case "}" -> {
                    breakpoint = false;
                }
                default -> {
                    tempDeque.push(new ASTNode(token.type, token.value, token.line));
                    i++;
                }
            }
        }
        declaration.addChild(blockStmt);
        ParseResult buildBlockResult = new ParseResult(declaration, i);

        return buildBlockResult;
    }

    //;만났을때 파싱하는 함수
    public static ParseResult buildStmtNode(Deque<ASTNode> tempDeque, int index) {
        ASTNode firstNode = tempDeque.removeLast();
        ASTNode secondNode = tempDeque.getLast();
        tempDeque.offerLast(firstNode);
        ASTNode lastNode = tempDeque.getFirst();

        ASTNode innerParent = new ASTNode("VariableDeclaration", null, firstNode.line);

        if (firstNode.type.equals("IDENT") && isUnaryExpr(secondNode.value)) {
            innerParent.type = "UnaryExpr";
        } else if (lastNode.value.equals(")")) {
            innerParent.type = "MethodDeclaration";
        }

        Deque<ASTNode> leftDeque = new ArrayDeque<>();
        Deque<ASTNode> rightDeque = new ArrayDeque<>();
        boolean foundEqual = false;

        while (!tempDeque.isEmpty()) {
            ASTNode tempNode = tempDeque.removeLast();
            if (!foundEqual && tempNode.value.equals("=")) {
                foundEqual = true;
            } else if (!foundEqual) { // = 만나기 이전
                leftDeque.push(tempNode);
            }else
                rightDeque.push(tempNode);
        }

        if (foundEqual) {
            ASTNode rightExpr = buildExpressionTree(rightDeque);

            ASTNode assign = new ASTNode("InitExpr", "=", rightExpr.line);
            assign.addChild(rightExpr);

            int length = leftDeque.size();
            for (int l = 0; l < length; l++) {
                ASTNode leftNode = leftDeque.removeLast();
                String type = leftNode.type;

                switch (type) {
                    case "IDENT" -> {
                        leftNode.type = "VariableName";
                    }
                    case "NUMBER" -> {
                        leftNode.type = "Literal";
                    }
                    case "TYPE" -> {
                        leftNode.type = "Type";
                    }
                }
                innerParent.addChild(leftNode);
            }
            innerParent.addChild(assign);
        } else {
            int length = leftDeque.size();
            for (int l = 0; l < length; l++) {
                ASTNode leftNode = leftDeque.removeLast();
                String type = leftNode.type;

                switch (type) {
                    case "IDENT" -> {
                        leftNode.type = "VariableName";
                    }
                    case "NUMBER" -> {
                        leftNode.type = "Literal";
                    }
                    case "TYPE" -> {
                        leftNode.type = "Type";
                    }
                    case "SYMBOL" -> {
                        if(isOperator(leftNode.value)) {
                            leftNode.type = "Operator";
                        }
                    }
                }
                innerParent.addChild(leftNode);
            }
        }
        return new ParseResult(innerParent, index+1);
    }


    public static ASTNode parse(List<Tokenizer.Token> tokens) {

        List<ASTNode> astList = new ArrayList<>(); // AST tree 저장 stack
        Deque<ASTNode> tempDeque = new ArrayDeque<>(); // 임시 저장 스택. R을 만나면 규칙에 따라 node 들을 처리 후 AST Stack에 push
        int i = 0;

        //token의 길이 만큼 순회
        while (i < tokens.size()) {
            // i index의 token의 type을 가져와 parsingTable의 어떤 value와 key 값이 일치하는지 확인
            Tokenizer.Token token = tokens.get(i);
            String key = token.type;
            if (token.type.equals("SYMBOL")) key += ":" + token.value;
            String action = parsingTable.getOrDefault(key, "ERROR");

            switch (action) {
                case "S" -> {
                    tempDeque.push(new ASTNode(token.type, token.value, token.line)); //tempstack에 token push
                    i++;
                }
                case "R_Stmt" -> {
                    ASTNode firstNode = tempDeque.removeLast();
                    ASTNode secondNode = tempDeque.getLast();
                    tempDeque.offerLast(firstNode);
                    if (firstNode.value.equals("for")) {
                        tempDeque.push(new ASTNode(token.type, token.value, token.line));
                        i++;
                    } else {
                        ParseResult parseResult = buildStmtNode(tempDeque, i);
                        astList.add(parseResult.astNode);
                        i = parseResult.index;
                        tempDeque.clear();
                    }
                }
                case "R_Block" -> {
                    ParseResult parseResult = buildBlock(tempDeque, tokens, i);
                    astList.add(parseResult.astNode);
                    i = parseResult.index;
                    tempDeque.clear();
                    i++;
                }
                case "ERROR" -> {
                    ASTNode errorNode = new ASTNode("ErrorNode", token.value, token.line);
                    astList.add(errorNode);
                    i++;
                }
                case "ACCEPT" -> {
                    break;
                }
                default -> throw new RuntimeException("Unexpected parsing action at line " + token.line + ": " + token.value);
            }
        }
        ASTNode root = new ASTNode("CompilationUnit", null, 0);
        for (int t = 0; t < astList.size(); t++) {
            root.addChild(astList.get(t));
        }
        return root;
    }
}

