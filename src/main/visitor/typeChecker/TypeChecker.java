package main.visitor.typeChecker;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.Map;

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

    @Override
    public Void visit(Program program) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        //TODO
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
