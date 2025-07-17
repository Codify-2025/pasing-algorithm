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
        parsingTable.put("SYMBOL:)", "R_Close"); //소괄호 닫기
        parsingTable.put("SYMBOL:{", "R_Block"); //대괄호 열기
        parsingTable.put("SYMBOL:}", "R_Block_Close"); //대괄호 닫기
        parsingTable.put("SYMBOL:;", "R_Stmt"); //세미콜론
        parsingTable.put("SYMBOL:=", "S");
        parsingTable.put("SYMBOL:+", "S");
        parsingTable.put("SYMBOL:==", "S");
        parsingTable.put("SYMBOL:,", "S");
        parsingTable.put("KEYWORD:if", "S");
        parsingTable.put("KEYWORD:for", "S");
        parsingTable.put("EOF", "ACCEPT");
    }

    //method

    //연산자 우선순위 함수
    public static int precedence(String op) {
        switch (op) {
            case "=": return 1;
            case "+": case "-": return 2;
            case "*": case "/": return 3;
            default: return 0;
        }
    }

    //연산자 판별 함수
    public static boolean isOperator(String value) {
        return value.equals("=") || value.equals("+") || value.equals("-")
                || value.equals("*") || value.equals("/");
    }

    //연산자 탐색 함수
    public static ASTNode buildExpressionTree(Stack<ASTNode> tempStack) {
        Stack<ASTNode> nodeStack = new Stack<>();
        Stack<String> opStack = new Stack<>();

        while (!tempStack.isEmpty()) {
            ASTNode stmtNode = tempStack.pop(); //stack의 원소 pop
            if (isOperator(stmtNode.value)) { //stmtNode가 연산자라면
                while (!opStack.isEmpty() && precedence(opStack.peek()) >= precedence(stmtNode.value)) { //연산자의 우선순위 파악
                    String op = opStack.pop();
                    ASTNode right= nodeStack.pop();
                    ASTNode left = nodeStack.pop();
                    ASTNode opNode;

                    //나중에 복합 연산자 확장
                    if (op.equals("=")) {
                        opNode = new ASTNode("InitExpr", op, stmtNode.line);
                    }else{
                        opNode = new ASTNode("BinaryExpr", op, stmtNode.line);
                    }

                    opNode.addChild(left);
                    opNode.addChild(right);
                    nodeStack.push(opNode);
                }
                opStack.push(stmtNode.value);
            }else{

                System.out.println("stmtNode.value = " + stmtNode.value + ", type = " + stmtNode.type);
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


                nodeStack.push(stmtNode);
                ASTNode store = nodeStack.peek();
                System.out.println("nodeStack.value = " + store.value + ", type = " + store.type);
            }
        } //연산자 파악 후 노드스택에 넣는건 문제 x

        while (!opStack.isEmpty()) {
            String op = opStack.pop();
            ASTNode right = nodeStack.pop();
            ASTNode left = nodeStack.pop();
            ASTNode opNode;
            if (op.equals("=")) {
                opNode = new ASTNode("InitExpr", op, left.line);
            }else{
                opNode = new ASTNode("BinaryExpr", op, left.line);
            }
            opNode.addChild(left);
            opNode.addChild(right);
            nodeStack.push(opNode);
        }
        //nodeStack을 ASTNode형태로 리턴하면 끝날듯...
        return nodeStack.pop();
    }

    public static ASTNode buildInitExprTree(Stack<ASTNode> tempStack) {
        Stack<ASTNode> reversed = new Stack<>(); //reversed stack 생성
        while (!tempStack.isEmpty()) reversed.push(tempStack.pop()); //tempstack에 있는걸 reversed stack으로 Push -> 뒤집어짐

        Stack<ASTNode> leftStack = new Stack<>();
        Stack<ASTNode> rightStack = new Stack<>();
        boolean foundEqual = false; //원소가 = 인지 아닌지 판별하는 boolean variable

        while (!reversed.isEmpty()) { //reversed stack이 빌 때까지 반복
            ASTNode node = reversed.pop(); //원소 하나 pop
            if (!foundEqual && node.value.equals("=")) { // foundEqual이 false고 연산자가 = 이라면
                foundEqual = true; //foundEqlal을 true로 변경
            } else if (!foundEqual) {
                leftStack.push(node); // foundEqual이 false라면 원소들을 left stack에 저장 -> =연산자를 만나기 전의 원소들을 leftStack에 저장
            } else {
                rightStack.push(node); // Expression after '=' -> = 연산자를 만난 후 foundEqual = true -> 이 이후의 원소들은 rightStack에 저장
            }
        }

        ASTNode left = leftStack.pop();
        ASTNode rightExpr = buildExpressionTree(rightStack);
        left.type = "VariableName";
        ASTNode assign = new ASTNode("InitExpr", "=", left.line);
        assign.addChild(left);
        assign.addChild(rightExpr);


        return assign;
    }



    public static ASTNode parse(List<Tokenizer.Token> tokens) {

        Stack<ASTNode> astStack = new Stack<>(); // AST tree 저장 stack
        Stack<ASTNode> tempStack = new Stack<>(); // 임시 저장 스택. R을 만나면 규칙에 따라 node 들을 처리 후 AST Stack에 push
        int i = 0;

        while (i < tokens.size()) { //token의 길이 만큼 순회
            Tokenizer.Token token = tokens.get(i);
            String key = token.type; //token의 type을 가져와 parsingTable에 존재하는지 확인
            if (token.type.equals("SYMBOL")) key += ":" + token.value;
            String action = parsingTable.getOrDefault(key, "ERROR");

            if (action.equals("S")) {
                tempStack.push(new ASTNode(token.type, token.value, token.line)); //tempstack에 token push
                i++;
            } else if (action.equals("R_Close")) { // 소괄호 닫기에 해당하는 ) 토큰이 나왔을 경우의 동작
                //()안의 토큰들 임시 저장 리스트 content
                List<ASTNode> content = new ArrayList<>();

                //소괄호 안의 내용들 파싱 후 함수, 조건문의 케이스에 따라 각각의 작업 수행
                while (true) {
                    ASTNode node = tempStack.pop();

                    // ( 토큰을 만나면 ( 바로 이전 인덱스의 토큰 확인
                    if (node.value.equals("(")) { // ( 만나면 (앞의 토큰 확인
                        ASTNode prev = tempStack.peek(); //스택의 가장 위 element 반환
                        ASTNode DeclarationNode;

                        //A. ( 앞의 토큰이 조건문에 해당한다면
                        if (prev.type.equals("CONTROL") && (prev.value.equals("if") || prev.value.equals("for") || prev.value.equals("while"))) {
                            DeclarationNode = new ASTNode(prev.value.substring(0,1).toUpperCase() + prev.value.substring(1)+ "Stmt", null, token.line);
                            tempStack.pop();
                        }else { //B. ( 앞의 토큰이 조건문에 해당하지 않는다면 함수
                            //1. 소괄호 안의 토큰(함수의 파라미터)들 파싱
                            //소괄호 토큰 ( 이후 인덱스에 해당하는 토큰들(파라미터)을 paramList ASTNode객체의 children으로 추가
                            //소괄호 안의 함수의 파라미터들은 함수의 인수 -> type-value 형태로 정해져 있음
                            //함수 호출의 경우 추후에 따로 케이스 나누기. 현재는 함수의 정의라 생각하고 진행
                            ASTNode paramList = new ASTNode("ParameterList", null, token.line);
                            Collections.reverse(content);
                            for (int j = 0; j < content.size() - 1; j += 2) {
                                ASTNode typeNode = content.get(j);
                                ASTNode identNode = content.get(j + 1);

                                typeNode.type = "Type";
                                identNode.type = "VariableName";

                                ASTNode param = new ASTNode("Parameter", null, token.line);
                                param.addChild(typeNode);
                                param.addChild(identNode);
                                paramList.addChild(param);
                            }
                            //2. 함수의 타입, 이름 파싱
                            //소괄호 토큰 ( 이전 인덱스에 해당하는 토큰들 -> 함수의 type, name 토큰들로 tempStack에 담겨 있음
                            ASTNode methodName = tempStack.pop();
                            methodName.type = "MethodName";
                            ASTNode methodType = tempStack.pop();
                            methodType.type = "Type";

                            //methodDeclaration 노드의 child로 parameterList, MethodName, MethodType 추가
                            DeclarationNode = new ASTNode("MethodDeclaration", null, token.line);
                            DeclarationNode.addChild(methodType);       // Type
                            DeclarationNode.addChild(methodName);       // Name
                            DeclarationNode.addChild(paramList);        // ParameterList

                            //MethodDeclaration 관련 작업 완료했으니 tempStack을 비우기
                            tempStack.clear();

                            //3. MethodDeclaration 이후 {}안의 내용들 파싱
                            Tokenizer.Token nextToken = tokens.get(i + 1);
                            // 다음 토큰이 { 이라면 { 안의 토큰들 파싱
                            if (nextToken.value.equals("{")) {
                                ASTNode blockNode = new ASTNode("BlockStmt", null, nextToken.line);
                                i += 2;

                                while (i < tokens.size()) {
                                    Tokenizer.Token innerToken = tokens.get(i);
                                    String innerKey = innerToken.type;
                                    // } 토큰을 만나면 while loop에서 빠져나감
                                    if (innerToken.type.equals("SYMBOL")) innerKey += ":" + innerToken.value;
                                    String innerAction = parsingTable.getOrDefault(innerKey, "ERROR");

                                    if (innerToken.value.equals("}")) {
                                        break;
                                    }

                                    //3-1. S action -> tempstack에 token push
                                    if (innerAction.equals("S")) {
                                        tempStack.push(new ASTNode(innerToken.type, innerToken.value, innerToken.line)); //tempstack에 token push
                                        i++;
                                    }else if (innerAction.equals("R_Stmt")){ //3-2. ; 을 만난경우 tempStack에 쌓인 토큰들을 파싱 후 blockNode의 child로 추가

                                        // token의 정보 확인 후 변수 선언인지, 변수 호출인지 확인
                                        // 함수 호출, 선언은 나중에 추가
                                        ASTNode firstNode = tempStack.get(0);
                                        ASTNode secondNode = tempStack.get(1);

                                        //변수 선언이 대부분 제일 많이 존재해 Default로 가정. 다른 조건을 보고 해당하지 않는다면 variableName
                                        ASTNode innerParent = new ASTNode("VariableDeclaration",null, firstNode.line);

                                        if (firstNode.type == "STRING" || firstNode.type == "IDENT") {
                                            innerParent.type = "variableName";
                                        }
                                        // tempStack에 들어있는 토큰들(;토큰 만나기 전 내용들) 파싱 후 innerParentNode의 child로 추가
                                        // = 이전 인덱스에 해당하는 토큰 -> left stack, = 이후 인덱스에 해당하는 토큰 -> right stack
                                        //rightStack은 InitExpr의 childNode
                                        Stack<ASTNode> reversed = new Stack<>();

                                        //tempStack의 내용을 reversed Stack으로 push -> 뒤집어서 다시 left,right stack에 넣어야 tempStack에 넣어진 순서 유지 가능
                                        while (!tempStack.isEmpty()) {
                                            reversed.push(tempStack.pop());
                                        }
                                        // = 토큰 기준으로 leftStack, rightStack으로 나누는 로직
                                        Stack<ASTNode> leftStack = new Stack<>();
                                        Stack<ASTNode> rightStack = new Stack<>();
                                        boolean foundEqual = false;

                                        while (!reversed.isEmpty()) {
                                            ASTNode reverseNode = reversed.pop();
                                            if (!foundEqual && reverseNode.value.equals("=")) {
                                                foundEqual = true;
                                            } else if (!foundEqual) { //= 만나기 이전
                                                leftStack.push(reverseNode);
                                            }else{
                                                rightStack.push(reverseNode); //= 만난 이후
                                            }
                                        }

                                        //rightStack을 이용하여 연산자의 우선순위를 고려한 tree 생성 -> buildExpressionTree method 정의해서 사용
                                        ASTNode rightExpr = buildExpressionTree(rightStack);

                                        //InitExpr의 childNode로 rightExpr 추가
                                        ASTNode assign = new ASTNode("InitExpr", "=", rightExpr.line);
                                        assign.addChild(rightExpr);

                                        // leftStack의 내용을 꺼내 innerParent의 child로 추가
                                        for (int l = 0; l < leftStack.size(); l++) {
                                            ASTNode leftStackNode = leftStack.get(l);
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
                                        //innerParent를 blockNode의 child로 추가
                                        blockNode.addChild(innerParent);
                                        tempStack.clear();
                                        i++;
                                    }
                                }

                                DeclarationNode.addChild(blockNode);
                                astStack.push(DeclarationNode);

                            }else{
                                astStack.push(DeclarationNode);
                            }

//                            if (parentNode.type.equals("BlockStmt")) {
//                                parentNode.addChild(DeclarationNode);
//                                astStack.push(parentNode);
//                            }else{
//                                astStack.push(DeclarationNode);
//                            }

                        }
//                        Collections.reverse(content);
//                        content.forEach(parentNode::addChild); //content list안의 모든 요소(child)를 newNode의 children으로 추가
//                        astStack.push(parentNode); //astStack에 push
                        tempStack.clear(); //tempStack 비움
                        break;
                    }
                    if (node.type.equals("SYMBOL") && node.value.equals(",")) {
                        continue; // 쉼표는 파라미터 구분용이므로 AST에는 포함하지 않음
                    }
                    content.add(node);
                }
                i++;
            }else if (action.equals("ERROR")) {
                ASTNode errorNode = new ASTNode("ErrorNode", token.value, token.line);
                astStack.push(errorNode);
                i++;
                continue; // 다음 토큰으로
            } else if (action.startsWith("R")) {
                String rule = action.substring(2);
                ASTNode newNode = null;
                switch (rule) {
                    case "Block":
//                        List<ASTNode> block = new ArrayList<>();
//                        while (!tempStack.isEmpty()) {
//                            ASTNode node = tempStack.pop();
//                            if(node.value.equals("{")) break;
//                            block.add(0, node);
//                        }
//                        ASTNode blockNode = new ASTNode("BlockStmt", null, token.line);
////                        newNode = new ASTNode("BlockStmt", null, token.line);
//                        block.forEach(blockNode::addChild);
//                        if (!astStack.isEmpty()) {
//                            ASTNode parent = astStack.pop();
//                            parent.addChild(blockNode);
//                            astStack.push(parent);
//                        }else{
//                            astStack.push(blockNode);
//                        }
                        ASTNode node = tempStack.pop();
                        if (node.value.equals("{")) {
                            ASTNode parent = astStack.peek();
                            ASTNode blockNode = new ASTNode("BlockStmt", null, token.line);
                            blockNode.addChild(parent);
                        }else{
                            System.out.println("parsing error at the Block");
                        }

                        //exception
                        if (tempStack.empty()) {
                            System.out.println("tempstack is empty.");
                        }else{
                            System.out.println("error: tempstack is not empty");
                        }
                        break;

                    case "Block_Close":
                        break;

                    case "Stmt" :
                        List<ASTNode> content = new ArrayList<>();
                        while (!tempStack.isEmpty()) {
                            ASTNode stmtNode = tempStack.pop();

                            if (stmtNode.type.equals("SYMBOL") && stmtNode.value.equals(",")) {
                                continue; // 쉼표는 파라미터 구분용이므로 AST에는 포함하지 않음
                            }
                            content.add(stmtNode);
                        }
                        ASTNode stmt = tempStack.pop();
                        newNode = new ASTNode("Statement", null, token.line);
                        newNode.addChild(stmt);
                        break;
                }
                astStack.push(newNode);
                i++;
            } else if (action.equals("ACCEPT")) {
                break;
            }else {
                System.out.println("Parsing error at line " + token.line + ": " + token.value);
                return null;
            }
        }
        ASTNode root = new ASTNode("CompilationUnit", null, 0);
        while (!astStack.isEmpty()) root.addChild(astStack.remove(0));
        return root;
    }


}
