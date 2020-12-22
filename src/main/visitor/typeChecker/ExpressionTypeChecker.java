package main.visitor.typeChecker;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.ast.types.NullType;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;


public class ExpressionTypeChecker extends Visitor<Type> {
    private final Graph<String> classHierarchy;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public boolean isSubtype(Type subType, Type superType) {
        if (subType instanceof IntType && superType instanceof IntType) {
            return true;
        }
        if (subType instanceof NoType) {
            return true;
        }

        if (subType instanceof BoolType && superType instanceof BoolType) {
            return true;
        }
        return false;
        //todo: add others
    }

    public boolean isEqualitySupported(Type firstOperand, Type secondOperand) {
        if (firstOperand instanceof ListType || secondOperand instanceof ListType) {
            return false;
        }
        if (firstOperand.toString().equals(secondOperand.toString())) {
            return true;
        }

        if (firstOperand instanceof NoType || secondOperand instanceof NoType) {
            return true;
        }

        if (
                (
                        firstOperand instanceof NullType &&
                        (secondOperand instanceof ClassType || secondOperand instanceof FptrType)
                ) ||
                (
                        secondOperand instanceof NullType &&
                        (firstOperand instanceof ClassType || firstOperand instanceof FptrType)
                )
        ) {
            return true;
        }

        return false;
    }

//    public boolean isAssignmentSupported(Type firstOperand, Type secondOperand) {
//        if (firstOperand.toString().equals(secondOperand.toString())) {
//            return true;
//        }
//
//        if (firstOperand instanceof NoType || secondOperand instanceof NoType) {
//            return true;
//        }
//
//        if (
//                (
//                        firstOperand instanceof NullType &&
//                                (secondOperand instanceof ClassType || secondOperand instanceof FptrType)
//                ) ||
//                        (
//                                secondOperand instanceof NullType &&
//                                        (firstOperand instanceof ClassType || firstOperand instanceof FptrType)
//                        )
//        ) {
//            return true;
//        }
//
//        return false;
//    }

    public boolean isValidLHS(Expression lhs) {
        return lhs instanceof Identifier ||
               lhs instanceof ObjectOrListMemberAccess ||
               lhs instanceof ListAccessByIndex;
    }

    public boolean isAllElementsHaveSameType(ListType list) {
        ArrayList<ListNameType> elementNameTypes = list.getElementsTypes();
        Type firstElementType = elementNameTypes.get(0).getType();
        for (ListNameType nameType: elementNameTypes) {
            if (!firstElementType.toString().equals(nameType.getType().toString())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Expression firstOperand = binaryExpression.getFirstOperand();
        Expression secondOperand = binaryExpression.getSecondOperand();
        Type firstOperandType = firstOperand.accept(this);
        Type secondOperandType = secondOperand.accept(this);

        BinaryOperator binaryOperator = binaryExpression.getBinaryOperator();
        if (
                binaryOperator == BinaryOperator.add ||
                binaryOperator == BinaryOperator.mult ||
                binaryOperator == BinaryOperator.sub ||
                binaryOperator == BinaryOperator.div ||
                binaryOperator == BinaryOperator.mod ||
                binaryOperator == BinaryOperator.lt ||
                binaryOperator == BinaryOperator.gt
        ) {
            if (isSubtype(firstOperandType, new IntType()) && isSubtype(secondOperandType, new IntType())) {
                return new IntType();
            } else {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
        }

        if (
                binaryOperator == BinaryOperator.or ||
                binaryOperator == BinaryOperator.and
        ) {
            if (isSubtype(firstOperandType, new BoolType()) && isSubtype(secondOperandType, new BoolType())) {
                return new BoolType();
            } else {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
        }

        if (
                binaryOperator == BinaryOperator.eq ||
                binaryOperator == BinaryOperator.neq
        ) {
            if (isEqualitySupported(firstOperandType, secondOperandType)) {
                return new BoolType();
            } else {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
        }

        if (
                binaryOperator == BinaryOperator.assign
        ) {
            if (!isValidLHS(firstOperand)) {
                binaryExpression.addError(new LeftSideNotLvalue(binaryExpression.getLine()));
            } //todo: complete
        }

        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        Expression operand = unaryExpression.getOperand();
        Type operandType = operand.accept(this);
        UnaryOperator unaryOperator = unaryExpression.getOperator();

        if (
                unaryOperator == UnaryOperator.postinc ||
                unaryOperator == UnaryOperator.preinc  ||
                unaryOperator == UnaryOperator.postdec ||
                unaryOperator == UnaryOperator.predec
        ) {
            if (isSubtype(operandType, new IntType()) && isValidLHS(operand)) {
                return new IntType();
            } else if (!isSubtype(operandType, new IntType())) {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), unaryOperator.name()));
                return new NoType();
            } else {
                unaryExpression.addError(new IncDecOperandNotLvalue(unaryExpression.getLine(), unaryOperator.name()));
                return new NoType();
            }
        }

        if (unaryOperator == UnaryOperator.minus) {
            if (isSubtype(operandType, new IntType())) {
                return new IntType();
            } else {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), unaryOperator.name()));
                return new NoType();
            }
        }

        if (
                unaryOperator == UnaryOperator.not
        ) {
            if (isSubtype(operandType, new BoolType())) {
                return new BoolType();
            } else {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), unaryOperator.name()));
                return new NoType();
            }
        }
        return null;
    }

    @Override
    public Type visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        //TODO
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        String searchVal = LocalVariableSymbolTableItem.START_KEY + identifier.getName();
        try {
            LocalVariableSymbolTableItem getItem = (LocalVariableSymbolTableItem) SymbolTable.top.getItem(searchVal, true);
            return getItem.getType();
        } catch (ItemNotFoundException exp) {
            identifier.addError(new VarNotDeclared(identifier.getLine(), identifier.getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        Expression instance = listAccessByIndex.getInstance();
        Type instanceType = instance.accept(this);
        boolean isInstanceCorrect = true;

        if (instanceType instanceof NoType) {
            isInstanceCorrect = false;
        }

        if (isInstanceCorrect && !(instanceType instanceof ListType)) {
            listAccessByIndex.addError(new ListAccessByIndexOnNoneList(listAccessByIndex.getLine()));
            isInstanceCorrect = false;
        }

        Expression index = listAccessByIndex.getIndex();
        Type indexType = index.accept(this);
// a: {string, int, bool}
// a["hello"]
        assert instanceType instanceof ListType;
        if (!isAllElementsHaveSameType((ListType) instanceType)) {
            if (!(index instanceof IntValue)) {
                listAccessByIndex.addError(new CantUseExprAsIndexOfMultiTypeList(listAccessByIndex.getLine()));
            } else {
//                return getListAccessByIndexType((ListType) instanceType, index);
            }
        }

        if (!isSubtype(indexType, new IntType())) {
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }

        //khoshhalim:
//        return getListAccessByIndexType((ListType) instanceType, index);


        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        //TODO
        return null;
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        return newClassInstance.getClassType();
        // is it right?
    }

    @Override
    public Type visit(ThisClass thisClass) {
        //TODO
        return null;
    }

    @Override
    public Type visit(ListValue listValue) {
        //TODO
        return null;
    }

    @Override
    public Type visit(NullValue nullValue) {
        return new NullType();
    }

    @Override
    public Type visit(IntValue intValue) {
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        return new BoolType();
    }

    @Override
    public Type visit(StringValue stringValue) {
        return new StringType();
    }
}
