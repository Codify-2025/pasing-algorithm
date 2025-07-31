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

    //연산자 탐색 함수 (리스트) 나중에 deque로 변경
    public static ASTNode buildExpressionTreeList(Deque<ASTNode> tempDeque) {
        Deque<ASTNode> nodeDeque = new ArrayDeque<>();
        Deque<ASTNode> opDeque = new ArrayDeque<>();
        int length = tempDeque.size();
        for (int i = 0; i < length; i++) {
            ASTNode stmtNode = tempDeque.removeLast();
            //if-else 문으로 연산자의 우선순위에 따라 stack에 넣음
            if (isOperator(stmtNode.value)) { //stmtNode가 연산자라면
                while (!opDeque.isEmpty() && precedence(opDeque.peek().value) >= precedence(stmtNode.value)) { //연산자의 우선순위 파악
                    //operatorNode pop
                    ASTNode op = opDeque.pop();
                    op.type = "Operator";

                    ASTNode right = nodeDeque.pop();
                    ASTNode left = nodeDeque.pop();
                    ASTNode opNode;

                    //나중에 복합 연산자 확장
                    if (op.value.equals("=")) {
                        opNode = new ASTNode("InitExpr", null, stmtNode.line);
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
                    case "SYMBOL" -> {
                        if (isOperator(stmtNode.value)) {
                            stmtNode.type = "Operator";
                        }
                    }
                }


                nodeDeque.push(stmtNode);
                ASTNode store = nodeDeque.peek();
            }

        }

        while (!opDeque.isEmpty()) {
            ASTNode op = opDeque.pop();
            op.type = "Operator";
            ASTNode right = nodeDeque.pop();
            ASTNode left = nodeDeque.pop();
            ASTNode opNode;
            if (op.value.equals("=")) {
                opNode = new ASTNode("InitExpr", null, left.line);
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

//    public static ASTNode buildInitExprTree(Stack<ASTNode> tempStack) {
//        Stack<ASTNode> reversed = new Stack<>(); //reversed stack 생성
//        while (!tempStack.isEmpty()) reversed.push(tempStack.pop()); //tempstack에 있는걸 reversed stack으로 Push -> 뒤집어짐
//
//        Stack<ASTNode> leftStack = new Stack<>();
//        Stack<ASTNode> rightStack = new Stack<>();
//        boolean foundEqual = false; //원소가 = 인지 아닌지 판별하는 boolean variable
//
//        while (!reversed.isEmpty()) { //reversed stack이 빌 때까지 반복
//            ASTNode node = reversed.pop(); //원소 하나 pop
//            if (!foundEqual && node.value.equals("=")) { // foundEqual이 false고 연산자가 = 이라면
//                foundEqual = true; //foundEqlal을 true로 변경
//            } else if (!foundEqual) {
//                leftStack.push(node); // foundEqual이 false라면 원소들을 left stack에 저장 -> =연산자를 만나기 전의 원소들을 leftStack에 저장
//            } else {
//                rightStack.push(node); // Expression after '=' -> = 연산자를 만난 후 foundEqual = true -> 이 이후의 원소들은 rightStack에 저장
//            }
//        }
//
//        ASTNode left = leftStack.pop();
//        ASTNode rightExpr = buildExpressionTree(rightStack);
//        left.type = "VariableName";
//        ASTNode assign = new ASTNode("InitExpr", "=", left.line);
//        assign.addChild(left);
//        assign.addChild(rightExpr);
//
//
//        return assign;
//    }

    //{}안의 토큰들을 파싱하는 함수
    public static ParseResult buildBlockNode(List<Tokenizer.Token> tokens, int startIndex) {
        System.out.println("blockNode 함수 실행\n");
        Deque<ASTNode> blockDeque = new ArrayDeque<>();
        ASTNode blockNode = new ASTNode("BlockStmt", null, tokens.get(startIndex).line);
        int i = startIndex + 1;
        // } 토큰을 만나면 while loop에서 빠져나감
        while (i < tokens.size()) {
            Tokenizer.Token innerToken = tokens.get(i);
            String innerKey = innerToken.type;

            if (innerToken.type.equals("SYMBOL")) innerKey += ":" + innerToken.value;
            String innerAction = parsingTable.getOrDefault(innerKey, "ERROR");

            if (innerToken.value.equals("}")) {
                break;
            }

            //3-1. S action -> tempstack에 token push
            if (innerAction.equals("S")) {
                blockDeque.push(new ASTNode(innerToken.type, innerToken.value, innerToken.line));
                i++;
            } else if (innerAction.equals("R_Stmt")) {//3-2. ; 을 만난경우 tempStack에 쌓인 토큰들을 파싱 후 blockNode의 child로 추가
                // token의 정보 확인 후 변수 선언인지, 변수 호출인지 확인
                // 함수 호출, 선언은 나중에 추가
                ASTNode firstNode = blockDeque.removeLast();
                ASTNode secondNode = blockDeque.getLast();
                blockDeque.offerLast(firstNode);
                //변수 선언이 대부분 제일 많이 존재해 Default로 가정. 다른 조건을 보고 해당하지 않는다면 variableName
                ASTNode innerParent = new ASTNode("VariableDeclaration", null, firstNode.line);

                if (firstNode.type.equals("IDENT") && isUnaryExpr(secondNode.value)) {
                    innerParent.type = "UnaryExpr";
                } else if (firstNode.type.equals("IDENT")) {
                    innerParent.type = "VariableName";
                } else if (firstNode.value.equals("return")) {
                    innerParent.type = "ReturnStmt";
                }

                // tempStack에 들어있는 토큰들(;토큰 만나기 전 내용들) 파싱 후 innerParentNode의 child로 추가
                // = 이전 인덱스에 해당하는 토큰 -> left stack, = 이후 인덱스에 해당하는 토큰 -> right stack
                //rightStack은 InitExpr의 childNode
                Stack<ASTNode> reversed = new Stack<>();

                //tempStack의 내용을 reversed Stack으로 push -> 뒤집어서 다시 left,right stack에 넣어야 tempStack에 넣어진 순서 유지 가능
                while (!blockDeque.isEmpty()) {
                    reversed.push(blockDeque.pop());
                }
                // = 토큰 기준으로 leftStack, rightStack으로 나누는 로직
                Deque<ASTNode> leftDeque = new ArrayDeque<>();
                Deque<ASTNode> rightDeque = new ArrayDeque<>();
                boolean foundEqual = false;

                while (!reversed.isEmpty()) {
                    ASTNode reverseNode = reversed.pop();
                    if (!foundEqual && reverseNode.value.equals("=")) {
                        foundEqual = true;
                    } else if (!foundEqual) { //= 만나기 이전
                        leftDeque.push(reverseNode);
                    } else {
                        rightDeque.push(reverseNode); //= 만난 이후
                    }
                }


                if (!rightDeque.isEmpty()) {
                    //rightStack을 이용하여 연산자의 우선순위를 고려한 tree 생성 -> buildExpressionTree method 정의해서 사용
                    ASTNode rightExpr = buildExpressionTree(rightDeque);

                    //InitExpr의 childNode로 rightExpr 추가
                    ASTNode assign = new ASTNode("InitExpr", "=", rightExpr.line);
                    assign.addChild(rightExpr);

                    // leftStack의 내용을 꺼내 innerParent의 child로 추가
                    int length = leftDeque.size();
                    for (int l = 0; l < length; l++) {
                        ASTNode leftStackNode = leftDeque.removeLast();
                        String type = leftStackNode.type;

                        switch (type) {
                            case "IDENT" -> {
                                leftStackNode.type = "VariableName";
                            }
                            case "NUMBER" -> {
                                leftStackNode.type = "Literal";
                            }
                            case "TYPE" -> {
                                leftStackNode.type = "Type";
                            }
                        }
                        innerParent.addChild(leftStackNode);
                    }

                    //InitExpr tree를 innerParent의 childNode로 추가
                    innerParent.addChild(assign);

                } else {
                    //System.out.println("rightStack이 비어 있는 경우");
                    int length = leftDeque.size();
                    for (int l = 0; l < length; l++) {
                        ASTNode leftStackNode = leftDeque.removeLast();
                        String type = leftStackNode.type;

                        switch (type) {
                            case "IDENT" -> {
                                leftStackNode.type = "VariableName";
                            }
                            case "NUMBER" -> {
                                leftStackNode.type = "Literal";
                            }
                            case "TYPE" -> {
                                leftStackNode.type = "Type";
                            }
                            case "SYMBOL" -> {
                                if (isOperator(leftStackNode.value)) {
                                    leftStackNode.type = "Operator";
                                }
                            }
                        }
                        innerParent.addChild(leftStackNode);
                    }

                }

                System.out.println("innerParent를 blockNode의 child로 추가\n");
                //innerParent를 blockNode의 child로 추가
                blockNode.addChild(innerParent);
                blockDeque.clear();
                i++;
                System.out.println("함수 실행 끝\n");
            }
        }
        return new ParseResult(blockNode, i);
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
                    ifChild = buildExpressionTreeList(tempDeque);
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
                    whileChild = buildExpressionTreeList(tempDeque);
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
                Deque<ASTNode> forTempDeque = new ArrayDeque<>();
                for (int l = 0; l < length; l++) {
                    ASTNode node = tempDeque.removeLast();
                    if (node.value.equals(";")) {
                        ASTNode forChild = buildExpressionTreeList(forTempDeque);
                        declaration.addChild(forChild);
                        forTempDeque.clear();
                    } else {
                        forTempDeque.addLast(node);
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
                        ASTNode forChild = buildExpressionTreeList(forTempDeque);
                        declaration.addChild(forChild);
                    }
                    forTempDeque.clear();
                }
            }
            default -> {
//                ASTNode methodDeclaration = new ASTNode("MethodDeclaration", null, declaration.line);

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
                            ASTNode paramChild = buildExpressionTreeList(methodTempDeque);
                            param.addChild(paramChild);
                            paramList.addChild(param);
                            methodTempDeque.clear();
                        } else {
                            methodTempDeque.addLast(node);
                        }
                    }
                    if (!methodTempDeque.isEmpty()) {
                        ASTNode param = new ASTNode("Parameter", null, paramList.line);
                        ASTNode paramChild = buildExpressionTreeList(methodTempDeque);
                        param.addChild(paramChild);
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
//                case "R_Close" -> {
//                    //()안의 토큰들 임시 저장 리스트 content
//                    //추후에 리스트가 아니라 stack으로 리팩토링 후 연산자 함수 buildExpressionTree 활용하기 or stack이 아닌 deque로 리팩토링..
//                    List<ASTNode> content = new ArrayList<>();
//                    //소괄호 안의 내용들 파싱 후 함수, 조건문의 케이스에 따라 각각의 작업 수행
//                    while (true) {
//                        ASTNode node = tempDeque.pop();
//
//                        // ( 토큰을 만나면 ( 바로 이전 인덱스의 토큰 확인
//                        if (node.value.equals("(")) { // ( 만나면 (앞의 토큰 확인
//                            ASTNode prev = tempDeque.peek(); //스택의 가장 위 element 반환
//                            String prevValue = prev.value;
//                            ASTNode DeclarationNode;
//
//                            //A. ( 앞의 토큰이 조건문에 해당한다면
//                            if (prev.type.equals("CONTROL")) {
//                                switch (prevValue) {
//                                    //1. ( 앞의 token.value에 따라 DeclarationNode 객체 생성 후 ()안의 조건 토큰들을 child로 추가
//                                    case "if" -> {
//                                        DeclarationNode = new ASTNode("IfStmt", null, token.line);
//                                        System.out.println("ifStmt 파싱 시작" + "\n");
//                                        tempDeque.pop();
//                                        ASTNode ifChild;
//                                        int j = 0;
//                                        System.out.println("if ()안의 토큰들 파싱 시작" + "\n");
//                                        if (content.size() > 1) {
//                                            ifChild = buildExpressionTreeList(content);
//                                        } else {
//                                            ifChild = new ASTNode("VariableName", content.get(j).value, content.get(j).line);
//                                        }
//                                        System.out.println("if ()안의 토큰들 파싱 완료" + "\n");
//                                        DeclarationNode.addChild(ifChild);
//                                        tempDeque.clear();
//                                        //2. {}안의 내용 파싱
//                                        Tokenizer.Token nextToken = tokens.get(i + 1);
//                                        if (nextToken.value.equals("{")) {
//                                            System.out.println("if {} 안의 토큰들 파싱 시작" + "\n");
//                                            ParseResult blockNode = buildBlockNode(tokens, i + 1);
//                                            DeclarationNode.addChild(blockNode.astNode);
//                                            System.out.println("if {} 안의 토큰들 파싱 완료" + "\n");
//                                            i = blockNode.index;
//                                            System.out.println("index 갱신" + "\n");
//                                            astList.add(DeclarationNode);
//                                        } else {
//                                            astList.add(DeclarationNode);
//                                        }
//                                        tempDeque.clear();
//                                        //3. else를 만났을 때의 파싱
//                                        if (i + 1 < tokens.size()) {
//                                            Tokenizer.Token elseToken = tokens.get(i + 1);
//                                            if (elseToken.value.equals("else")) {
//                                                ASTNode elseDeclarationNode = new ASTNode("ElseStmt", null, elseToken.line);
//                                                ParseResult blockNode = buildBlockNode(tokens, i + 2);
//                                                elseDeclarationNode.addChild(blockNode.astNode);
//                                                i = blockNode.index;
//                                                astList.add(elseDeclarationNode);
//                                            }
//                                            tempDeque.clear();
//                                        }
//                                    }
//                                    case "for" -> {
//                                        //1. for loop의 ()안의 token들 파싱 후 Forstmt의 child로 추가
//                                        Collections.reverse(content);
//                                        System.out.println("Forstmt 조건 파싱 시작\n");
//                                        DeclarationNode = new ASTNode("ForStmt", null, token.line);
//                                        tempDeque.pop();
//                                        List<ASTNode> tempContent = new ArrayList<>();
//                                        for (int j = 0; j < content.size(); j++) {
//                                            if (content.get(j).value.equals(";")) {
//                                                System.out.println("tempContentSize: " + tempContent.size() + "\n");
//                                                Collections.reverse(tempContent);
//                                                ASTNode forChildNode = buildExpressionTreeList(tempContent);
//                                                DeclarationNode.addChild(forChildNode);
//                                                tempContent.clear();
//                                            } else {
//                                                tempContent.add(content.get(j));
//                                            }
//                                        }
//
//                                        System.out.println("tempContent의 크기: " + tempContent.size() + "\n");
//                                        if (!tempContent.isEmpty()) {
//                                            System.out.println("buildExpressionTreeList함수 실행\n");
//                                            ASTNode forChildNode;
//                                            if (tempContent.size() == 2) {
//                                                forChildNode = new ASTNode("UnaryExpr", null, tempContent.get(0).line);
//                                                for (int j = 0; j < 2; j++) {
//                                                    ASTNode tempNode = tempContent.get(j);
//                                                    if (tempNode.type.equals("IDENT")) {
//                                                        tempNode.type = "VariableName";
//                                                    } else {
//                                                        tempNode.type = "Operator";
//                                                    }
//                                                    forChildNode.addChild(tempNode);
//                                                }
//                                            } else {
//                                                Collections.reverse(tempContent);
//                                                forChildNode = buildExpressionTreeList(tempContent);
//                                            }
//                                            DeclarationNode.addChild(forChildNode);
//                                        }
//
//                                        tempDeque.clear();
//                                        System.out.println("forstmt 조건 파싱 완료\n");
//
//                                        //2. {}안의 내용들 파싱
//                                        Tokenizer.Token nextToken = tokens.get(i + 1);
//
//                                        if (nextToken.value.equals("{")) {
//                                            System.out.println("{ 안의 요소들 파싱 시작\n");
//                                            ParseResult blockNode = buildBlockNode(tokens, i + 1);
//                                            DeclarationNode.addChild(blockNode.astNode);
//                                            i = blockNode.index; //리팩토링 후 i index 갱신
//                                            astList.add(DeclarationNode);
//                                            System.out.println("{ 안의 토큰 파싱 완료\n");
//
//                                        } else {
//                                            astList.add(DeclarationNode);
//                                        }
//                                        tempDeque.clear();
//                                    }
//                                    case "while" -> {
//                                        DeclarationNode = new ASTNode("WhileStmt", null, token.line);
//                                        tempDeque.pop();
//
//                                        // 2.()안의 조건 토큰들 파싱하고 whileChdild의 child로 추가
//                                        //; 기준이 아닌 ( 토큰 만날 때 까지 파싱 -> ()안의 임시 저장 리스트 content의 토큰들 파싱
//                                        //괄호와 같이 묶이는 경우는 일단 패스..
//                                        ASTNode whileChild;
//                                        int j = 0;
//                                        // while 문의 조건이 한 개의 원소로 이루어져 있지 않다면 -> symbol 등의 연산자를 통해 파싱
//                                        if (content.size() > 1) {
//                                            System.out.println("buildExpressionTreeList 함수 실행 시작\n");
//                                            ASTNode whileChildNode = buildExpressionTreeList(content);
//                                            System.out.println("buildExpressionTreeList 함수 실행 완료\n");
//                                            DeclarationNode.addChild(whileChildNode);
//
//                                        } else {
//                                            whileChild = new ASTNode("VariableName", content.get(j).value, content.get(j).line);
//                                            DeclarationNode.addChild(whileChild);
//                                        }
//                                        tempDeque.clear();
//
//                                        //3. While 이후 {}안의 내용들 파싱
//                                        Tokenizer.Token nextToken = tokens.get(i + 1);
//                                        // 다음 토큰이 { 이라면 { 안의 토큰들 파싱
//                                        if (nextToken.value.equals("{")) {
//
//                                            System.out.println("buildBlockNode 함수 실행 시작\n");
//                                            ParseResult blockNode = buildBlockNode(tokens, i + 1);
//                                            System.out.println("buildBlockNode 함수 실행 완료\n");
//                                            DeclarationNode.addChild(blockNode.astNode);
//                                            i = blockNode.index; //리팩토링 후 i index 갱신
//                                            astList.add(DeclarationNode);
//                                            System.out.println("{ 안의 토큰 파싱 완료\n");
//
//                                        } else {
//                                            astList.add(DeclarationNode);
//                                        }
//                                    }
//                                }
//                                tempDeque.clear();
//                            } else { //B. ( 앞의 토큰이 조건문에 해당하지 않는다면 함수
//                                //1. 소괄호 안의 토큰(함수의 파라미터)들 파싱
//                                //소괄호 토큰 ( 이후 인덱스에 해당하는 토큰들(파라미터)을 paramList ASTNode객체의 children으로 추가
//                                //소괄호 안의 함수의 파라미터들은 함수의 인수 -> type-value 형태로 정해져 있음
//                                //함수 호출의 경우 추후에 따로 케이스 나누기. 현재는 함수의 정의라 생각하고 진행
//
//                                //2. 함수의 타입, 이름 파싱
//                                //소괄호 토큰 ( 이전 인덱스에 해당하는 토큰들 -> 함수의 type, name 토큰들로 tempStack에 담겨 있음
//                                ASTNode methodName = tempDeque.pop();
//                                methodName.type = "MethodName";
//                                ASTNode methodType = tempDeque.pop();
//                                methodType.type = "Type";
//
//                                if (!content.isEmpty()) {
//                                    ASTNode paramList = new ASTNode("ParameterList", null, token.line);
//                                    Collections.reverse(content);
//                                    for (int j = 0; j < content.size() - 1; j += 2) {
//                                        ASTNode typeNode = content.get(j);
//                                        ASTNode identNode = content.get(j + 1);
//
//                                        typeNode.type = "Type";
//                                        identNode.type = "VariableName";
//
//                                        ASTNode param = new ASTNode("Parameter", null, token.line);
//                                        param.addChild(typeNode);
//                                        param.addChild(identNode);
//                                        paramList.addChild(param);
//                                    }
//                                    //methodDeclaration 노드의 child로 parameterList, MethodName, MethodType 추가
//                                    DeclarationNode = new ASTNode("MethodDeclaration", null, token.line);
//                                    DeclarationNode.addChild(methodType);       // Type
//                                    DeclarationNode.addChild(methodName);       // Name
//                                    DeclarationNode.addChild(paramList);        // ParameterList
//                                } else {
//                                    DeclarationNode = new ASTNode("MethodDeclaration", null, token.line);
//                                    DeclarationNode.addChild(methodType);       // Type
//                                    DeclarationNode.addChild(methodName);       // Name
//                                }
//
//                                //MethodDeclaration 관련 작업 완료했으니 tempStack을 비우기
//                                tempDeque.clear();
//
//                                //3. MethodDeclaration 이후 {}안의 내용들 파싱
//                                Tokenizer.Token nextToken = tokens.get(i + 1);
//                                // 다음 토큰이 { 이라면 { 안의 토큰들 파싱
//                                if (nextToken.value.equals("{")) {
//                                    //input -> i, tempstack, tokens
//                                    //들어가는 tokens ->
//                                    ParseResult blockNode = buildBlockNode(tokens, i + 1);
//                                    DeclarationNode.addChild(blockNode.astNode);
//                                    i = blockNode.index; //리팩토링 후 i index 갱신
//                                    astList.add(DeclarationNode);
//
//                                } else {
//                                    astList.add(DeclarationNode);
//                                }
//
//                            }
//
//                            tempDeque.clear(); //tempStack 비움
//                            break;
//                        }
//                        if (node.value.equals(",")) {
//                            continue; // 쉼표는 파라미터 구분용이므로 AST에는 포함하지 않음
//                        }
//                        content.add(node);
//                    }
//                    i++;
//                }
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

