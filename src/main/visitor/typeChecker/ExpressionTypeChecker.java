package main.visitor.typeChecker;

import main.ast.nodes.declaration.variableDec.VarDeclaration;
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
import main.symbolTable.items.*;
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

    public boolean isClassSubtype(ClassType subType, ClassType superType) {
        return classHierarchy.isSecondNodeAncestorOf(
                subType.getClassName().getName(),
                superType.getClassName().getName()
        );
    }

    public boolean isFptrSubtype(FptrType subType, FptrType superType) {
        ArrayList<Type> subTypeArgs = subType.getArgumentsTypes();
        ArrayList<Type> superTypeArgs = superType.getArgumentsTypes();

        if (subTypeArgs.size() != superTypeArgs.size()) {
            return false;
        }

        for (int i = 0; i < subTypeArgs.size(); i++) {
            if (!isSubtype(superTypeArgs.get(i), subTypeArgs.get(i))) {
                return false;
            }
        }

        return isSubtype(subType.getReturnType(), superType.getReturnType());
    }

    public boolean isListSubtype(ListType subType, ListType superType) {
        ArrayList<ListNameType> subTypeElements = subType.getElementsTypes();
        ArrayList<ListNameType> superTypeElements = superType.getElementsTypes();
        if (subTypeElements.size() != superTypeElements.size()) {
            return false;
        }

        for (int i = 0; i < subTypeElements.size(); i++) {
            if (!isSubtype(subTypeElements.get(i).getType(), superTypeElements.get(i).getType())) {
                return false;
            }
        }

        return true;
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

        if (subType instanceof StringType && superType instanceof StringType) {
            return true;
        }

        if (subType instanceof NullType && superType instanceof ClassType) {
            return true;
        }

        if (subType instanceof ClassType && superType instanceof ClassType) {
            return isClassSubtype((ClassType) subType, (ClassType) superType);
        }

        if (subType instanceof FptrType && superType instanceof FptrType) {
            return isFptrSubtype((FptrType) subType, (FptrType) superType);
        }

        if (subType instanceof ListType && superType instanceof ListType) {
            return isListSubtype((ListType) subType, (ListType) superType);
        }

        return false;
    }

    public boolean isEqualitySupported(Type firstOperand, Type secondOperand) {
        // todo: equality for function pointers
        if (firstOperand instanceof ListType || secondOperand instanceof ListType) {
            return false;
        }


        if (firstOperand instanceof NoType || secondOperand instanceof NoType) {
            return false;
        }

        if (firstOperand.toString().equals(secondOperand.toString())) {
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

    public boolean isValidLHS(Expression lhs, Type lhsType) {
        boolean validMember = false;
        if (lhs instanceof ObjectOrListMemberAccess) {
            validMember = !(lhsType instanceof FptrType);
        } //todo: fptr left hand side?
        return lhs instanceof Identifier || lhs instanceof ListAccessByIndex || validMember;
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

    public Type findListElementTypeByIndex(ListType list, Expression index, boolean isSingleType) {
        ArrayList<ListNameType> elementNameTypes = list.getElementsTypes();
        if (isSingleType) {
            return elementNameTypes.get(0).getType();
        } else {
            int indexNumber = ((IntValue) index).getConstant();
            if (indexNumber >= elementNameTypes.size()) {
                return elementNameTypes.get(0).getType();
            }
            return elementNameTypes.get(indexNumber).getType();
        }
    }

    public Type findMember(String memberName) {
        try {
            MethodSymbolTableItem methodSymbolTableItem = (MethodSymbolTableItem) currentSymbolTable.getItem(MethodSymbolTableItem.START_KEY + memberName, true);
            return new FptrType(methodSymbolTableItem.getArgTypes(), methodSymbolTableItem.getReturnType());
        } catch (ItemNotFoundException e) {
            try {
                FieldSymbolTableItem fieldSymbolTableItem = (FieldSymbolTableItem) currentSymbolTable.getItem(FieldSymbolTableItem.START_KEY + memberName, true);
                return fieldSymbolTableItem.getType();
            } catch (ItemNotFoundException e2) {
                return null;
            }
        }

    }

    public Type findElement(ArrayList<ListNameType> listElementsTypes, String elementKey) {
        for (ListNameType listElementsType: listElementsTypes) {
            if (listElementsType.getName().getName().equals(elementKey)) {
                return listElementsType.getType();
            }
        }
        return null;
    }

    public Type classMemberAccess(ObjectOrListMemberAccess objectOrListMemberAccess, ClassType instanceType, Expression memberName) {
        Identifier classId = instanceType.getClassName();
        try {
            ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + classId.getName(), true);
            SymbolTable classSymbolTable;
            classSymbolTable = classSymbolTableItem.getClassSymbolTable();
            currentSymbolTable = classSymbolTable;
            String memberNameStr = ((Identifier) memberName).getName();
            Type memberNameType = findMember(memberNameStr);
            if (memberNameType == null) {
                objectOrListMemberAccess.addError(new MemberNotAvailableInClass(objectOrListMemberAccess.getLine(), memberNameStr, classId.getName()));
                return new NoType();
            }
            return memberNameType;
        } catch (ItemNotFoundException e) {
//            objectOrListMemberAccess.addError(new ClassNotDeclared(objectOrListMemberAccess.getLine(), classId.getName()));
//            return new NoType();
            System.out.println("wtfffffffffff");
        }
        return null;
    }

    public Type listMemberAccess(ObjectOrListMemberAccess objectOrListMemberAccess, ListType instanceType, Expression memberName) {
        ArrayList<ListNameType> listElementsTypes = instanceType.getElementsTypes();
        String memberNameStr = ((Identifier) memberName).getName();
        Type memberNameType = findElement(listElementsTypes, memberNameStr);
        if (memberNameType == null) {
            objectOrListMemberAccess.addError(new ListMemberNotFound(objectOrListMemberAccess.getLine(), memberNameStr));
            return new NoType();
        }
        return memberNameType;
    }

    public boolean isOperandVoidMethodCall(Expression operand, Type operandType) {
        if (operand instanceof MethodCall) {
            return operandType instanceof NullType;
        }
        return false;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Expression firstOperand = binaryExpression.getFirstOperand();
        Expression secondOperand = binaryExpression.getSecondOperand();
        Type firstOperandType = firstOperand.accept(this);
        Type secondOperandType = secondOperand.accept(this);
        boolean isFirstOperandNoType= false;
        boolean isSecondOperandNoType = false;

        if (isOperandVoidMethodCall(firstOperand, firstOperandType)) {
            binaryExpression.addError(new CantUseValueOfVoidMethod(binaryExpression.getLine()));
            firstOperandType = new NoType();
        }

        if (isOperandVoidMethodCall(secondOperand, secondOperandType)) {
            binaryExpression.addError(new CantUseValueOfVoidMethod(binaryExpression.getLine()));
            secondOperandType = new NoType();
        }

        if (firstOperandType instanceof NoType) {
            isFirstOperandNoType = true;
        }

        if (secondOperandType instanceof NoType) {
            isSecondOperandNoType = true;
        }

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
            if (firstOperandType instanceof IntType && secondOperandType instanceof IntType) {
                return new IntType();
            } else if (!isSubtype(firstOperandType, new IntType()) || !isSubtype(secondOperandType, new IntType())) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
            return new NoType();
        }

        if (
                binaryOperator == BinaryOperator.or ||
                binaryOperator == BinaryOperator.and
        ) {
            if (firstOperandType instanceof BoolType && secondOperandType instanceof BoolType) {
                return new BoolType();
            } else if (!isSubtype(firstOperandType, new BoolType()) || !isSubtype(secondOperandType, new BoolType())) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
            return new NoType();
        }

        if (
                binaryOperator == BinaryOperator.eq ||
                binaryOperator == BinaryOperator.neq
        ) {
            if (isEqualitySupported(firstOperandType, secondOperandType)) {
                return new BoolType();
            } else if (!isSecondOperandNoType && !isFirstOperandNoType) {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
            return new NoType();
        }

        if (
                binaryOperator == BinaryOperator.assign
        ) {
            if (!isValidLHS(firstOperand, firstOperandType) && !isFirstOperandNoType) {
                binaryExpression.addError(new LeftSideNotLvalue(binaryExpression.getLine()));
            } else if (isFirstOperandNoType || isSecondOperandNoType) {
                return new NoType();
            }
            else if (isSubtype(secondOperandType, firstOperandType)) {
                return firstOperandType;
            } else {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), binaryOperator.name()));
                return new NoType();
            }
        }

        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        Expression operand = unaryExpression.getOperand();
        Type operandType = operand.accept(this);
        UnaryOperator unaryOperator = unaryExpression.getOperator();

        if (isOperandVoidMethodCall(operand, operandType)) {
            unaryExpression.addError(new CantUseValueOfVoidMethod(unaryExpression.getLine()));
            operandType = new NoType();
        }

        if (
                unaryOperator == UnaryOperator.postinc ||
                unaryOperator == UnaryOperator.preinc  ||
                unaryOperator == UnaryOperator.postdec ||
                unaryOperator == UnaryOperator.predec
        ) {
            if (isSubtype(operandType, new IntType()) && isValidLHS(operand, operandType)) {
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
        boolean isInstanceCorrect = true;
        boolean isInstanceNoType = false;

        Expression instance = objectOrListMemberAccess.getInstance();
        Type instanceType = instance.accept(this);
        if (instanceType instanceof NoType) {
            isInstanceNoType = true;
        }

        if (!(instanceType instanceof ClassType || instanceType instanceof ListType || isInstanceNoType)) {
            isInstanceCorrect = false;
            objectOrListMemberAccess.addError(new MemberAccessOnNoneObjOrListType(objectOrListMemberAccess.getLine()));
        }

        if (!isInstanceCorrect) {
            return new NoType();
        }

        Identifier memberName = objectOrListMemberAccess.getMemberName();
        if (instanceType instanceof ClassType) {
            return classMemberAccess(objectOrListMemberAccess, (ClassType) instanceType, memberName);
        }
        else if (instanceType instanceof ListType) {
            return listMemberAccess(objectOrListMemberAccess, (ListType) instanceType, memberName);
        }

        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        try {
            LocalVariableSymbolTableItem localVariableSymbolTableItem =
                    (LocalVariableSymbolTableItem) currentSymbolTable.getItem(
                            LocalVariableSymbolTableItem.START_KEY + identifier.getName(),
                            true
                    );
            //todo: is it ok?
            Type idType = localVariableSymbolTableItem.getType();
            if (idType instanceof ClassType) {
                try {
                    SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + ((ClassType) idType).getClassName().getName(), true);
                } catch (ItemNotFoundException e) {
                    return new NoType();
                } //todo: add to other places
            }
            return localVariableSymbolTableItem.getType();
        } catch (ItemNotFoundException e2) {
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

        boolean isIndexCorrect = true;

        if (!isSubtype(indexType, new IntType())) {
            isIndexCorrect = false;
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }

        boolean isListSingleType = true;

        if (isInstanceCorrect) {
            isListSingleType = isAllElementsHaveSameType((ListType) instanceType);
            if (!isListSingleType) {
                if (!(index instanceof IntValue)) {
                    listAccessByIndex.addError(new CantUseExprAsIndexOfMultiTypeList(listAccessByIndex.getLine()));
                    isIndexCorrect = false;
                }
            }
        }

        if (isInstanceCorrect && isIndexCorrect) {
            return findListElementTypeByIndex((ListType) instanceType, index, isListSingleType);
        } else {
            return new NoType();
        }

    }

    @Override
    public Type visit(MethodCall methodCall) {
        Expression instance = methodCall.getInstance();
        Type instanceType = instance.accept(this);

        ArrayList<Expression> passedArgs = methodCall.getArgs();
        ArrayList<Type> passedArgTypes = new ArrayList<>();

        for (Expression exp: passedArgs) {
            Type expType = exp.accept(this);
            passedArgTypes.add(expType);
        }

        if (instanceType instanceof FptrType) {
            ArrayList<Type> methodArgsTypes = ((FptrType) instanceType).getArgumentsTypes();
            boolean sizeMatch = false;
            boolean argTypesMatch = true;
            if (methodArgsTypes.size() == passedArgTypes.size()) {
                sizeMatch = true;
                for (int i = 0; i < passedArgTypes.size(); i++) {
                    if (!isSubtype(passedArgTypes.get(i), methodArgsTypes.get(i))) {
                        argTypesMatch = false;
                    }
                    if (isOperandVoidMethodCall(passedArgs.get(i), passedArgTypes.get(i))) {
                        passedArgs.get(i).addError(new CantUseValueOfVoidMethod(passedArgs.get(i).getLine()));
                        argTypesMatch = false;
                    }// todo: should i set to noType? or get 2 errors
                }
            }
            if (!sizeMatch || !argTypesMatch) {
                methodCall.addError(new MethodCallNotMatchDefinition(methodCall.getLine()));
                return new NoType();
            }
            return ((FptrType) instanceType).getReturnType();
        }


        if (instanceType instanceof NoType) {
            return new NoType();
        }

        methodCall.addError(new CallOnNoneFptrType(methodCall.getLine()));
        return new NoType();
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        ClassType classType = newClassInstance.getClassType();
        ArrayList<Expression> newClassInstanceArgs = newClassInstance.getArgs();
        ArrayList<Type> passedArgs = new ArrayList<>();
        ArrayList<VarDeclaration> constructorArgs;

        for (Expression exp: newClassInstanceArgs) {
            Type expType = exp.accept(this);
            passedArgs.add(expType);
        }

        try {
            constructorArgs = ((ClassSymbolTableItem) SymbolTable.root.getItem
                    (
                            ClassSymbolTableItem.START_KEY + classType.getClassName().getName(),
                            true
                    )
            ).getClassDeclaration().getConstructor().getArgs();

            boolean sizeMatch = false;
            boolean argTypesMatch = true;
            if (constructorArgs.size() == passedArgs.size()) {
                sizeMatch = true;
                for (int i = 0; i < passedArgs.size(); i++) {
                    if (!isSubtype(passedArgs.get(i), constructorArgs.get(i).getType())) {
                        argTypesMatch = false;
                        break;
                    }
                }
            }
            if (!sizeMatch || !argTypesMatch) {
                newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
                return new NoType();
            }
            return classType;
        } catch (ItemNotFoundException e) {
            newClassInstance.addError(new ClassNotDeclared(newClassInstance.getLine(), newClassInstance.getClassType().getClassName().getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ThisClass thisClass) {
        return new ClassType(currentClassDeclaration.getClassName());
    }

    @Override
    public Type visit(ListValue listValue) {
        ArrayList<Expression> listValueElements = listValue.getElements();
        ArrayList<ListNameType> elementsTypes = new ArrayList<>();
        for (Expression exp: listValueElements) {
            Type expType = exp.accept(this);
            if (isOperandVoidMethodCall(exp, expType)) {
                exp.addError(new CantUseValueOfVoidMethod(exp.getLine()));
                expType = new NoType();
            }
            elementsTypes.add(new ListNameType(expType));
        }
        return new ListType(elementsTypes);
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
