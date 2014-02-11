package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.IntegerConstant;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class ArrayFactory {

    private static final Assignment.Size SIZE = TypeSize.ZERO.toDecreasingSize();

    private static interface ArrayCreator extends Assignment {

        static enum Primitive implements ArrayCreator {

                BOOLEAN(Opcodes.T_BOOLEAN, Opcodes.BASTORE),
                BYTE(Opcodes.T_BYTE, Opcodes.BASTORE),
                SHORT(Opcodes.T_SHORT, Opcodes.SASTORE),
                CHARACTER(Opcodes.T_CHAR, Opcodes.CASTORE),
                INTEGER(Opcodes.T_INT, Opcodes.IASTORE),
                LONG(Opcodes.T_LONG, Opcodes.LASTORE),
                FLOAT(Opcodes.T_FLOAT, Opcodes.FASTORE),
                DOUBLE(Opcodes.T_DOUBLE, Opcodes.DASTORE);

                private final int creationOpcode;
                private final int storageOpcode;

                private Primitive(int creationOpcode, int storageOpcode) {
                    this.creationOpcode = creationOpcode;
                    this.storageOpcode = storageOpcode;
                }

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor) {
                    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
                    return SIZE;
                }

                @Override
                public int getStorageOpcode() {
                    return storageOpcode;
                }
            }

            static class Reference implements ArrayCreator {

                private final String internalTypeName;

                private Reference(TypeDescription referenceType) {
                    this.internalTypeName = referenceType.getInternalName();
                }

                @Override
                public boolean isValid() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor) {
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
                    return SIZE;
                }

                @Override
                public int getStorageOpcode() {
                    return Opcodes.AASTORE;
                }
            }

        int getStorageOpcode();
    }



    public static ArrayFactory of(TypeDescription typeDescription) {
        if (!typeDescription.isArray()) {
            throw new IllegalArgumentException("Expected array type instead of " + typeDescription);
        }
        return new ArrayFactory(typeDescription.getComponentType(), makeCreatorFor(typeDescription.getComponentType()));
    }

    private static ArrayCreator makeCreatorFor(TypeDescription componentType) {
        if (componentType.isPrimitive()) {
            if (componentType.represents(boolean.class)) {
                return ArrayCreator.Primitive.BOOLEAN;
            } else if (componentType.represents(byte.class)) {
                return ArrayCreator.Primitive.BYTE;
            } else if (componentType.represents(short.class)) {
                return ArrayCreator.Primitive.SHORT;
            } else if (componentType.represents(char.class)) {
                return ArrayCreator.Primitive.CHARACTER;
            } else if (componentType.represents(int.class)) {
                return ArrayCreator.Primitive.INTEGER;
            } else if (componentType.represents(long.class)) {
                return ArrayCreator.Primitive.LONG;
            } else if (componentType.represents(float.class)) {
                return ArrayCreator.Primitive.FLOAT;
            } else if (componentType.represents(double.class)) {
                return ArrayCreator.Primitive.DOUBLE;
            } else {
                throw new IllegalArgumentException("Cannot create array of type " + componentType);
            }
        } else {
            return new ArrayCreator.Reference(componentType);
        }
    }

    private class ArrayAssignment implements Assignment {

        private final List<Assignment> assignments;

        public ArrayAssignment(List<Assignment> assignments) {
            this.assignments = assignments;
        }

        @Override
        public boolean isValid() {
            for (Assignment assignment : assignments) {
                if (!assignment.isValid()) {
                    return false;
                }
            }
            return arrayCreator.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = IntegerConstant.forValue(assignments.size()).apply(methodVisitor);
            // The array's construction does not alter the stack's size.
            size = size.aggregate(arrayCreator.apply(methodVisitor));
            int index = 0;
            for (Assignment assignment : assignments) {
                methodVisitor.visitInsn(Opcodes.DUP);
                size = size.aggregate(TypeSize.SINGLE.toIncreasingSize());
                size = size.aggregate(IntegerConstant.forValue(index++).apply(methodVisitor));
                size = size.aggregate(assignment.apply(methodVisitor));
                methodVisitor.visitInsn(arrayCreator.getStorageOpcode());
                size = size.aggregate(sizeDecrease);
            }
            return size;
        }
    }

    private final TypeDescription componentType;
    private final ArrayCreator arrayCreator;
    private final Assignment.Size sizeDecrease;

    protected ArrayFactory(TypeDescription componentType, ArrayCreator arrayCreator) {
        this.componentType = componentType;
        this.arrayCreator = arrayCreator;
        // Size decreases by index and array reference (2) and array element (1, 2) after each element storage.
        sizeDecrease = TypeSize.DOUBLE.toDecreasingSize().aggregate(componentType.getStackSize().toDecreasingSize());
    }

    public Assignment withValues(List<Assignment> assignments) {
        return new ArrayAssignment(assignments);
    }

    public TypeDescription getComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayFactory that = (ArrayFactory) o;
        return arrayCreator.equals(that.arrayCreator)
                && componentType.equals(that.componentType)
                && sizeDecrease.equals(that.sizeDecrease);
    }

    @Override
    public int hashCode() {
        int result = componentType.hashCode();
        result = 31 * result + arrayCreator.hashCode();
        result = 31 * result + sizeDecrease.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ArrayFactory{" +
                "componentType=" + componentType +
                ", arrayCreator=" + arrayCreator +
                ", sizeDecrease=" + sizeDecrease +
                '}';
    }
}

