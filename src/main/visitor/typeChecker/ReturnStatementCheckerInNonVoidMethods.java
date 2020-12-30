package main.visitor.typeChecker;


import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.ast.types.NullType;
import main.compileErrorException.typeErrors.MissingReturnStatement;

import java.util.ArrayList;

public class ReturnStatementCheckerInNonVoidMethods {

    private Boolean doesStatementReturn(Statement statement) {
        if (statement instanceof ConditionalStmt) {
            return doesConditionalStmtReturn((ConditionalStmt) statement);

        } else if (statement instanceof ForeachStmt) {
            return false;

        } else if (statement instanceof ForStmt) {
            return false;

        } else if (statement instanceof BlockStmt) {
            ArrayList<Statement> statements = ((BlockStmt) statement).getStatements();
            for (Statement stmt: statements) {
                if (doesStatementReturn(stmt)) {
                    return true;
                }
            }
            return false;

        } else if (statement instanceof AssignmentStmt) {
            return false;

        } else if (statement instanceof PrintStmt) {
            return false;

        } else if (statement instanceof ReturnStmt) {
            return true;

        } else if (statement instanceof MethodCallStmt) {
            return false;

        } else if (statement instanceof ContinueStmt) {
            return false;

        } else if (statement instanceof BreakStmt) {
            return false;

        }
        return false;
    }

    private Boolean doesConditionalStmtReturn(ConditionalStmt statement) {
        boolean doesThenReturn = doesStatementReturn(statement.getThenBody());
        boolean doesElseReturn = false;
        Statement elseBody = statement.getElseBody();
        if (elseBody != null) {
            doesElseReturn = doesStatementReturn(elseBody);
        }
        return doesThenReturn && doesElseReturn;
    }

    public Void isReturnStatementAvailableInMethodDeclaration(MethodDeclaration methodDeclaration) {
        if (methodDeclaration.getReturnType() instanceof NullType) {
            return null;
        }
        ArrayList<Statement> statements = methodDeclaration.getBody();
        for (Statement statement: statements) {
            if (doesStatementReturn(statement)) {
                return null;
            }
        }

        methodDeclaration.addError(new MissingReturnStatement(methodDeclaration));
        return null;
    }
}
