package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.Expression;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;
    private final ReturnStatementChecker returnStatementChecker;
    private final UnreachableStmtChecker unreachableStmtChecker;

    public Void checkMethodDeclaration(MethodDeclaration methodDeclaration) {
        ArrayList<VarDeclaration> args = methodDeclaration.getArgs();
        for (VarDeclaration varDeclaration : args) {
            varDeclaration.accept(this);
        }

        ArrayList<VarDeclaration> localVars = methodDeclaration.getLocalVars();
        for (VarDeclaration varDeclaration : localVars) {
            varDeclaration.accept(this);
        }

        ArrayList<Statement> statements = methodDeclaration.getBody();
        for (Statement statement : statements) {
            statement.accept(this);
        }

        return null;
    }

    public void validateVarType(Type varDecType, Node node) {
        if (varDecType instanceof ClassType) {
            String className = ((ClassType) varDecType).getClassName().getName();
            boolean doesClassExist = classHierarchy.doesGraphContainNode(className);
            if (!doesClassExist) {
                node.addError(new ClassNotDeclared(node.getLine(), className));
            }
        } else if (varDecType instanceof ListType) {
            if (((ListType) varDecType).getElementsTypes().size() == 0) {
                node.addError(new CannotHaveEmptyList(node.getLine()));
            }
            boolean listHasDuplicateKey = checkListHasDuplicateKey((ListType) varDecType, node);
            if (listHasDuplicateKey) {
                node.addError(new DuplicateListId(node.getLine()));
            }
        } else if (varDecType instanceof FptrType) {
            validateVarType(((FptrType) varDecType).getReturnType(), node);
            ArrayList<Type> argumentsTypes = ((FptrType) varDecType).getArgumentsTypes();
            for (Type argumentType : argumentsTypes) {
                validateVarType(argumentType, node);
            }
        }
    }

    public boolean isPrintSupported(Type argType) {
        if (expressionTypeChecker.isSubtype(argType, new IntType())) {
            return true;
        } else if (expressionTypeChecker.isSubtype(argType, new StringType())) {
            return true;
        } else if (expressionTypeChecker.isSubtype(argType, new BoolType())) {
            return true;
        }
        return false;
    }

    public boolean checkListHasDuplicateKey(ListType listType, Node node) {
        ArrayList<ListNameType> listElementTypes = listType.getElementsTypes();
        boolean hasDuplicateKey = false;
        Set<String> hashSet = new HashSet<String>();
        for (ListNameType listNameType : listElementTypes) {
//            VarDeclaration varDeclaration = new VarDeclaration(listNameType.getName(), listNameType.getType());
//            varDeclaration.accept(this);
            validateVarType(listNameType.getType(), node);
            if (hashSet.contains(listNameType.getName().getName())) {
                hasDuplicateKey = true;
            }
            if (!listNameType.getName().getName().equals("")) {
                hashSet.add(listNameType.getName().getName());
            }
        }
        return hasDuplicateKey;
    }

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
        this.returnStatementChecker = new ReturnStatementChecker();
        this.unreachableStmtChecker = new UnreachableStmtChecker();
    }

    public boolean isClassMain(ClassDeclaration classDeclaration) {
        return classDeclaration.getClassName().getName().equals("Main");
    }

    public Void setCurrentSymbolTable(String declarationName) {
        SymbolTable preSymbolTable = currentSymbolTable;
        try {
            currentSymbolTable = (
                    (MethodSymbolTableItem) preSymbolTable.getItem(
                            MethodSymbolTableItem.START_KEY + declarationName,
                            true
                    )
            ).getMethodSymbolTable();
        } catch (ItemNotFoundException e) {

        }
        return null;
    }

    public Void validateMain(ClassDeclaration mainDeclaration) {
        Identifier parent = mainDeclaration.getParentClassName();
        if (parent != null) {
            mainDeclaration.addError(new MainClassCantExtend(mainDeclaration.getLine()));
        }
        ConstructorDeclaration constructorDeclaration = mainDeclaration.getConstructor();
        if (constructorDeclaration == null) {
            mainDeclaration.addError(new NoConstructorInMainClass(mainDeclaration));
        } else {
            constructorDeclaration.accept(this);
            if (!constructorDeclaration.getArgs().isEmpty()) {
                mainDeclaration.addError(new MainConstructorCantHaveArgs(constructorDeclaration.getLine()));
            }

        }
        return null;
    }

    public Void validateClass(ClassDeclaration classDeclaration) {
        Identifier parent = classDeclaration.getParentClassName();
        if (parent != null) {
            boolean doesParentExist = classHierarchy.doesGraphContainNode(parent.getName());
            if (!doesParentExist) {
                classDeclaration.addError(new ClassNotDeclared(classDeclaration.getLine(), parent.getName()));
            }
            if (parent.getName().equals("Main")) {
                classDeclaration.addError(new CannotExtendFromMainClass(classDeclaration.getLine()));
            }
        }

        ConstructorDeclaration constructorDeclaration = classDeclaration.getConstructor();
        if (constructorDeclaration != null) {
            constructorDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Program program) {
        currentSymbolTable = SymbolTable.root;
        boolean mainExists = false;
        for (ClassDeclaration classDeclaration : program.getClasses()) {
            classDeclaration.accept(this);
            if (isClassMain(classDeclaration)) {
                mainExists = true;
            }
        }

        if (!mainExists) {
            program.addError(new NoMainClass());
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        currentClassDeclaration = classDeclaration;

        try {
            currentSymbolTable = (
                    (ClassSymbolTableItem) SymbolTable.root.getItem(
                            ClassSymbolTableItem.START_KEY + classDeclaration.getClassName().getName(),
                            true
                    )
            ).getClassSymbolTable();
        } catch (ItemNotFoundException e) {

        }


        if (isClassMain(classDeclaration)) {
            validateMain(classDeclaration);
        } else {
            validateClass(classDeclaration);
        }

        ArrayList<FieldDeclaration> fieldDeclarations = classDeclaration.getFields();
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            fieldDeclaration.accept(this);
        }

        ArrayList<MethodDeclaration> methodDeclarations = classDeclaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            methodDeclaration.accept(this);
        }

        currentClassDeclaration = null;
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        SymbolTable preSymbolTable = currentSymbolTable;
        setCurrentSymbolTable(constructorDeclaration.getMethodName().getName());
        String currentClassDeclarationName = currentClassDeclaration.getClassName().getName();
        if (!constructorDeclaration.getMethodName().getName().equals(currentClassDeclarationName)) {
            constructorDeclaration.addError(new ConstructorNotSameNameAsClass(constructorDeclaration.getLine()));
        }
        currentReturnType = currentClassDeclaration.getConstructor().getReturnType();

        checkMethodDeclaration(constructorDeclaration);
        unreachableStmtChecker.checkMethodReachable(constructorDeclaration);

        currentSymbolTable = preSymbolTable;
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        SymbolTable preSymbolTable = currentSymbolTable;
        setCurrentSymbolTable(methodDeclaration.getMethodName().getName());
//        doesReturnStatementExist = false;
        Type returnType = methodDeclaration.getReturnType();
        validateVarType(returnType, methodDeclaration);
        currentReturnType = returnType;

        checkMethodDeclaration(methodDeclaration);
        if (returnType instanceof ClassType) {
            String className = ((ClassType) returnType).getClassName().getName();
            boolean doesClassExist = classHierarchy.doesGraphContainNode(className);
            if (!doesClassExist) {
                methodDeclaration.addError(new ClassNotDeclared(methodDeclaration.getLine(), className));
            }
            currentReturnType = new NoType();
        }

        returnStatementChecker.isReturnStatementAvailable(methodDeclaration);
        unreachableStmtChecker.checkMethodReachable(methodDeclaration);
        currentSymbolTable = preSymbolTable;
        currentReturnType = null;

        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        fieldDeclaration.getVarDeclaration().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration) {
        Type varDecType = varDeclaration.getType();
        validateVarType(varDecType, varDeclaration);

        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        Expression lValue = assignmentStmt.getlValue();
        Expression rValue = assignmentStmt.getrValue();
        Type rValueType = rValue.accept(expressionTypeChecker);
        hasSeenNoneLValue = false;
        Type lValueType = lValue.accept(expressionTypeChecker);

//        if (expressionTypeChecker.isOperandVoidMethodCall(lValue, lValueType)) {
//            assignmentStmt.addError(new CantUseValueOfVoidMethod(assignmentStmt.getLine()));
//            lValueType = new NoType();
//        }
//
//        if (expressionTypeChecker.isOperandVoidMethodCall(rValue, rValueType)) {
//            assignmentStmt.addError(new CantUseValueOfVoidMethod(assignmentStmt.getLine()));
//            rValueType = new NoType();
//        }

        boolean isLValueNoType = lValueType instanceof NoType;
        boolean isRValueNoType = rValueType instanceof NoType;

        if (hasSeenNoneLValue) {
            assignmentStmt.addError(new LeftSideNotLvalue(assignmentStmt.getLine()));
        }

        if (!(isLValueNoType || isRValueNoType || expressionTypeChecker.isSubtype(rValueType, lValueType))) {
            assignmentStmt.addError(new UnsupportedOperandType(assignmentStmt.getLine(), BinaryOperator.assign.name()));
        }

        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        ArrayList<Statement> statements = blockStmt.getStatements();
        for (Statement statement : statements) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        Expression condition = conditionalStmt.getCondition();
        Statement thenBody = conditionalStmt.getThenBody();
        Statement elseBody = conditionalStmt.getElseBody();
        Type conditionType = condition.accept(expressionTypeChecker);
        if (!(expressionTypeChecker.isSubtype(conditionType, new BoolType()))) {
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getLine()));
        }
        thenBody.accept(this);
        if (elseBody != null) {
            elseBody.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        inMethodCallStatement = true;
        methodCallStmt.getMethodCall().accept(expressionTypeChecker);
        inMethodCallStatement = false;
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Expression arg = print.getArg();
        Type argType = arg.accept(expressionTypeChecker);
        if (!isPrintSupported(argType)) {
            print.addError(new UnsupportedTypeForPrint(print.getLine()));
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        Type returnType = returnStmt.getReturnedExpr().accept(expressionTypeChecker);
        if (!expressionTypeChecker.isSubtype(returnType, currentReturnType)) {
            returnStmt.addError(new ReturnValueNotMatchMethodReturnType(returnStmt));
        }
//        doesReturnStatementExist = true;
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        if (nestedLoopsCount == 0) {
            breakStmt.addError(new ContinueBreakNotInLoop(breakStmt.getLine(), 0));
        }
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        if (nestedLoopsCount == 0) {
            continueStmt.addError(new ContinueBreakNotInLoop(continueStmt.getLine(), 1));
        }
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        Identifier variable = foreachStmt.getVariable();
        Expression list = foreachStmt.getList();
        Statement body = foreachStmt.getBody();
        nestedLoopsCount += 1;
        Type variableType = variable.accept(expressionTypeChecker);
        Type listType = list.accept(expressionTypeChecker);

        if (listType instanceof ListType) {
            boolean isListSingleType = expressionTypeChecker.isAllElementsHaveSameType((ListType) listType);
            if (!isListSingleType) {
                foreachStmt.addError(new ForeachListElementsNotSameType(foreachStmt.getLine()));
            }

            ArrayList<ListNameType> elementNameTypes = ((ListType) listType).getElementsTypes();
            Type firstElementType = elementNameTypes.get(0).getType();
            if (!variableType.toString().equals(firstElementType.toString())) {
                foreachStmt.addError(new ForeachVarNotMatchList(foreachStmt));
            }

        } else if (!(listType instanceof NoType)){
            foreachStmt.addError(new ForeachCantIterateNoneList(foreachStmt.getLine()));
        }

        body.accept(this);
        nestedLoopsCount -= 1;

        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        AssignmentStmt initialize = forStmt.getInitialize();
        Expression condition = forStmt.getCondition();
        AssignmentStmt update = forStmt.getUpdate();
        Statement body = forStmt.getBody();
        nestedLoopsCount += 1;
        initialize.accept(this);
        update.accept(this);
        Type conditionType = condition.accept(expressionTypeChecker);
        if (!(expressionTypeChecker.isSubtype(conditionType, new BoolType()))) {
            forStmt.addError(new ConditionNotBool(forStmt.getLine()));
        }
        body.accept(this);
        nestedLoopsCount -= 1;
        return null;
    }

}
