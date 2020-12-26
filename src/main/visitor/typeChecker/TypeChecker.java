package main.visitor.typeChecker;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);

//        SymbolTable.top = new SymbolTable();
//        Map<String, SymbolTableItem> items = SymbolTable.root.items;
//        for (Map.Entry<String, SymbolTableItem> entry : items.entrySet()) {
//            System.out.println(entry.getKey() + ":" + entry.getValue().getName());
//        }
////         SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + "A").getClass();
//
//        ClassSymbolTableItem classSymbolTableItem = null;
//        try {
//            classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + "A", true);
//        } catch (ItemNotFoundException e) {
//            System.out.println("class niiiiist");
//        }
//        SymbolTable classSymbolTable;
//        classSymbolTable = classSymbolTableItem.getClassSymbolTable();
//        try {
//            System.out.println((classSymbolTable.getItem(MethodSymbolTableItem.START_KEY + "A2", true)).getName());
//
//        } catch (ItemNotFoundException e) {
//            System.out.println("method niiiiist");
//        }
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
            System.out.println("something is very wrong :))"); //Todo
        }
        return null;
    }

    public Void validMain(ClassDeclaration mainDeclaration) {
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
                mainDeclaration.addError(new MainConstructorCantHaveArgs(mainDeclaration.getLine()));
            }

        }
        return null;
    }

    public Void validClass(ClassDeclaration classDeclaration) {
        Identifier parent = classDeclaration.getParentClassName();
        if (parent != null) {
            boolean doesParentExist = classHierarchy.doesGraphContainNode(parent.getName());
            if (!doesParentExist) {
                classDeclaration.addError(new ClassNotDeclared(classDeclaration.getLine(), parent.getName()));
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
        if (isClassMain(classDeclaration)) {
            validMain(classDeclaration);
        } else {
            validClass(classDeclaration);
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
        //todo: return type in constructor

        SymbolTable preSymbolTable = currentSymbolTable;
        setCurrentSymbolTable(constructorDeclaration.getMethodName().getName());
        String currentClassDeclarationName = currentClassDeclaration.getClassName().getName();
        if (!constructorDeclaration.getMethodName().getName().equals(currentClassDeclarationName)) {
            constructorDeclaration.addError(new ConstructorNotSameNameAsClass(constructorDeclaration.getLine()));
        }

        ArrayList<VarDeclaration> args = constructorDeclaration.getArgs();
        for (VarDeclaration varDeclaration : args) {
            varDeclaration.accept(this);
        }
        ArrayList<VarDeclaration> localVars = constructorDeclaration.getLocalVars();
        for (VarDeclaration varDeclaration : localVars) {
            varDeclaration.accept(this);
        }

        ArrayList<Statement> statements = constructorDeclaration.getBody();
        for (Statement statement : statements) {
            statement.accept(this);
        }
        currentSymbolTable = preSymbolTable;
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        //TODO
        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        //TODO
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration) {
        //TODO
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        //TODO
        return null;
    }

}
