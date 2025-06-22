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
            ASTNode stmtNode = tempStack.pop();
            if (isOperator(stmtNode.value)) {
                while (!opStack.isEmpty() && precedence(opStack.peek()) >= precedence(stmtNode.value)) {
                    String op = opStack.pop();
                    ASTNode right= nodeStack.pop();
                    ASTNode left = nodeStack.pop();
                    ASTNode opNode;

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

                if (stmtNode.type.equals("IDENT")) {
                    stmtNode.type = "VariableName";
                } else if (stmtNode.type.equals("NUMBER")) {
                    stmtNode.type = "Literal";
                } else {
                    stmtNode.type = "Type";
                }

                nodeStack.push(stmtNode);
            }
        }
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


        return nodeStack.pop();
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
                        }else {
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

                                    if (innerToken.value.equals("}")) break;
                                    //세미콜론 만났을때 작업
                                    //연산자 우선순위에 따라 파싱
                                    if (innerAction.equals("R_Stmt")) {
                                        ASTNode exprTree = buildExpressionTree(tempStack);
                                        blockNode.addChild(exprTree);

//                                        ASTNode stmtNode = new ASTNode("Statement", null, innerToken.line);
//                                        statementContent.forEach(stmtNode::addChild);
//                                        blockNode.addChild(stmtNode);
//                                        ASTNode stmtNode = new ASTNode("VariableDeclaration", null, innerToken.line);
//                                        int j = statementContent.size()-1;
//                                        while (j >= 0) {
//                                            ASTNode innerNode = statementContent.get(j);
//
//
//                                            j--;
////                                        }
//
//                                        statementContent.clear();
                                    } else if (innerAction.equals("S")) {
                                        tempStack.add(new ASTNode(innerToken.type, innerToken.value, innerToken.line));
//                                        statementContent.add(new ASTNode(innerToken.type, innerToken.value, innerToken.line));
                                    }
                                    i++;
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
