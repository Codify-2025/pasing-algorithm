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
            } else if (action.equals("R_Close")) {
                //stack에 저장된 keyword가 있는지 없는지 보고 결정
                List<ASTNode> content = new ArrayList<>(); //괄호 안 내용 임시 저장 리스트
                //break를 통해 제어
                //추후에 true를 바꿀 수도 있음
                while (true) {
                    ASTNode node = tempStack.pop();
                    if (node.value.equals("(")) { // ( 만나면 (앞의 토큰 확인
                        ASTNode prev = tempStack.peek(); //스택의 가장 위 element 반환
                        ASTNode DeclarationNode;
                        //( 앞의 토큰이 조건문이라면
                        if (prev.type.equals("CONTROL") && (prev.value.equals("if") || prev.value.equals("for") || prev.value.equals("while"))) {
                            DeclarationNode = new ASTNode(prev.value.substring(0,1).toUpperCase() + prev.value.substring(1)+ "Stmt", null, token.line);
                            tempStack.pop();
                        }else { // 아니라면 함수
                            // ( 이전 내용들을 paramList 객체 children으로 저장
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

                            ASTNode methodName = tempStack.pop();
                            methodName.type = "MethodName";
                            ASTNode methodType = tempStack.pop();
                            methodType.type = "Type";

//                            ASTNode parentNode = astStack.peek();

                            DeclarationNode = new ASTNode("MethodDeclaration", null, token.line);
                            DeclarationNode.addChild(methodType);       // Type
                            DeclarationNode.addChild(methodName);       // Name
                            DeclarationNode.addChild(paramList);        // ParameterList

                            //MethodDeclaration 관련 작업 완료했으니 tempStack을 비우기
                            tempStack.clear();

                            //MethodDeclaration 이후 괄호 안 함수 정의한 내용들 파싱하는 로직
                            Tokenizer.Token nextToken = tokens.get(i + 1);
                            if (nextToken.value.equals("{")) {
                                ASTNode blockNode = new ASTNode("BlockStmt", null, nextToken.line);
                                i += 2;

//                                List<ASTNode> statementContent = new ArrayList<>();
                                while (i < tokens.size()) {
                                    Tokenizer.Token innerToken = tokens.get(i);
                                    String innerKey = innerToken.type;
                                    if (innerToken.type.equals("SYMBOL")) innerKey += ":" + innerToken.value;
                                    String innerAction = parsingTable.getOrDefault(innerKey, "ERROR");

                                    if (innerToken.value.equals("}")) {
                                        break;
                                    }

                                    if (innerAction.equals("S")) {
                                        tempStack.push(new ASTNode(innerToken.type, innerToken.value, innerToken.line)); //tempstack에 token push
                                        i++;
                                    }else if (innerAction.equals("R_Stmt")){
                                        // 먼저 인덱싱을 통해 스택의 정보 확인하여 어떤 parentNode로 파싱할지 결정
                                        ASTNode firstNode = tempStack.get(0);
                                        ASTNode secondNode = tempStack.get(1);

                                        //기본적으로는 변수 선언이 젤 많을듯
                                        //TYPE에 해당한다면 variableDeclaration
                                        ASTNode innerParent = new ASTNode("VariableDeclaration",null, firstNode.line);

                                        if (firstNode.type == "STRING" || firstNode.type == "IDENT") {
                                            innerParent.type = "variableName";
                                        }
                                        // ; 을 만난다면 tempStack의 내용을 파싱 후 blockNode의 child로 추가하기
                                        // = 이전 -> left stack, = 이후 -> right stack. = 이후의 요소는 InitExpr의 child node
                                        Stack<ASTNode> reversed = new Stack<>();
                                        //tempStack의 내용을 reversed Stack으로 push -> 뒤집어서 다시 left,right stack에 넣어야 tempStack에 넣어진 순서 유지 가능
                                        while (!tempStack.isEmpty()) {
                                            reversed.push(tempStack.pop());
                                        }
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

                                        ASTNode rightExpr = buildExpressionTree(rightStack);
                                        ASTNode assign = new ASTNode("InitExpr", "=", rightExpr.line);

                                        assign.addChild(rightExpr);
                                        //leftStack의 내용을 꺼내 child로 추가
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

                                        innerParent.addChild(assign);
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
