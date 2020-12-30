package main.visitor.typeChecker;

import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.compileErrorException.typeErrors.UnreachableStatements;

import java.util.ArrayList;

enum State {
    RETURN_STMT,
    BREAK_CONTINUE_STMT,
    NOTHING
}

public class UnreachableStmtChecker {
    private int nestedLoopsCount = 0;

    private boolean inLoop() {
        return nestedLoopsCount > 0;
    }

    private State checkStatementUnreachable(Statement statement) {
        if (statement instanceof ConditionalStmt) {
            return checkConditionalStmtReachable((ConditionalStmt) statement);

        } else if (statement instanceof ForeachStmt) {
            return checkForeachStmtReachable((ForeachStmt) statement);

        } else if (statement instanceof ForStmt) {
            return checkForStmtReachable((ForStmt) statement);

        } else if (statement instanceof BlockStmt) {
            return checkBlockStmtReachable((BlockStmt) statement);

        } else if (statement instanceof AssignmentStmt) {
            return State.NOTHING;

        } else if (statement instanceof PrintStmt) {
            return State.NOTHING;

        } else if (statement instanceof ReturnStmt) {
            return State.RETURN_STMT;

        } else if (statement instanceof MethodCallStmt) {
            return State.NOTHING;

        } else if (statement instanceof ContinueStmt) {
            if (inLoop()) {
                return State.BREAK_CONTINUE_STMT;
            }
            return State.NOTHING;

        } else if (statement instanceof BreakStmt) {
            if (inLoop()) {
                return State.BREAK_CONTINUE_STMT;
            }
            return State.NOTHING;

        }
        return State.NOTHING;
    }

    private State checkConditionalStmtReachable(ConditionalStmt statement) {
        State thenBodyState = checkStatementUnreachable(statement.getThenBody());
        Statement elseBody = statement.getElseBody();
        if (
                elseBody != null && (
                thenBodyState == State.BREAK_CONTINUE_STMT ||
                thenBodyState == State.RETURN_STMT
                )
        ) {
            State elseBodyState = checkStatementUnreachable(elseBody);
            if (elseBodyState == State.RETURN_STMT && thenBodyState == State.RETURN_STMT) {
                return State.RETURN_STMT;
            } else if (elseBodyState == State.BREAK_CONTINUE_STMT) {
                return State.BREAK_CONTINUE_STMT;
            }
        }
        return State.NOTHING;
    }

    private State checkForStmtReachable(ForStmt statement) {
        nestedLoopsCount ++;
        Statement body = statement.getBody();
        checkStatementUnreachable(body);
        nestedLoopsCount --;
        return State.NOTHING;
    }

    private State checkForeachStmtReachable(ForeachStmt statement) {
        nestedLoopsCount ++;
        Statement body = statement.getBody();
        checkStatementUnreachable(body);
        nestedLoopsCount --;
        return State.NOTHING;
    }

    private State checkBlockStmtReachable(BlockStmt statement) {
        ArrayList<Statement> statements = statement.getStatements();
        State blockResult = State.NOTHING;
        for (int i = 0; i < statements.size(); i++) {
            State stmtResult = checkStatementUnreachable(statements.get(i));
            if (
                    stmtResult == State.BREAK_CONTINUE_STMT ||
                    stmtResult == State.RETURN_STMT
            ) {
               blockResult = stmtResult;
               if (isFinalStmt(i, statements.size())) {
                   statements.get(i + 1).addError(new UnreachableStatements(statements.get(i + 1)));
               }
            }
        }
        return blockResult;
    }

    private boolean isFinalStmt(int i, int size) {
        return i < size - 1;
    }

    public Void checkMethodReachable(MethodDeclaration methodDeclaration) {
        ArrayList<Statement> statements = methodDeclaration.getBody();
        for (int i = 0; i < statements.size(); i++) {
            State stmtResult = checkStatementUnreachable(statements.get(i));
            if (stmtResult == State.RETURN_STMT) {
                if (isFinalStmt(i, statements.size())) {
                    statements.get(i + 1).addError(new UnreachableStatements(statements.get(i + 1)));
                }
            }
        }
        return null;
    }
}
