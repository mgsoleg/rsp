package com.rsp.protocol.gen;

import com.squareup.javapoet.TypeName;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class SyntaxHelper {

    static final String VARIABLE_ACCESS_BASE = " (($T) this).$L  ";
    static String BUFFER_NAME = "byteBuffer";

    private SyntaxHelper(){}

    static String getLoopClockName(VariableElement field) {
        return new StringBuilder()
                .append("count")
                .append(field.getSimpleName()).toString();
    }


    static String getVarInitialization(String loopClockName) {
        return new StringBuilder()
                .append("int ")
                .append(loopClockName)
                .append(" = 0").toString();
    }

    static String getBeginControlFlow(String loopClockName) {
        return new StringBuilder()
                .append("while (")
                .append(loopClockName)
                .append(" < ")
                .append(VARIABLE_ACCESS_BASE)
                .append(".length )")
                .toString();
    }

    static String writeToBufferFrom(TypeMirror type) {
        if (type.getKind() != TypeKind.BYTE) {
            return new StringBuffer()
                    .append(BUFFER_NAME)
                    .append(".put")
                    .append(capitilizePrimitive(type))
                    .append("(")
                    .append(VARIABLE_ACCESS_BASE)
                    .append(")")
                    .toString();
        }
        return new StringBuffer()
                .append(BUFFER_NAME)
                .append(".put(")
                .append(VARIABLE_ACCESS_BASE)
                .append(')')
                .toString();
    }


    static String getWriteToVariableStatement(String loopClockName, TypeMirror primitiveType) {
        return new StringBuilder()
                .append(VARIABLE_ACCESS_BASE)
                .append('[')
                .append(loopClockName)
                .append(']')
                .append('=')
                .append(readFromBufferFor(primitiveType))
                .toString();
    }

    static String getWriteToBufferStatement(String loopClockName, TypeMirror primitiveType) {
        return new StringBuilder()
                .append(BUFFER_NAME)
                .append(".put")
                .append(primitiveType.getKind() == TypeKind.BYTE ? "" : capitilizePrimitive(primitiveType))
                .append('(')
                .append(VARIABLE_ACCESS_BASE)
                .append('[')
                .append(loopClockName)
                .append(']')
                .append(')')
                .toString();
    }

    static String readFromBufferIn(TypeMirror type) {
        return new StringBuilder()
                .append(VARIABLE_ACCESS_BASE)
                .append('=')
                .append(readFromBufferFor(type))
                .toString();
    }

    static String readFromBufferFor(TypeMirror type) {

        if (type.getKind() != TypeKind.BYTE) {
            return new StringBuffer(BUFFER_NAME)
                    .append(".get")
                    .append(capitilizePrimitive(type))
                    .append("()")
                    .toString();
        }
        return new StringBuffer(BUFFER_NAME)
                .append(".get()")
                .toString();
    }

    private static String capitilizePrimitive(TypeMirror type) {
        return StringUtils.capitalize(TypeName.get(type).toString());
    }


    static int getByteSizeForPrimitive(TypeKind kind) {

        switch (kind) {
            case BYTE:
                return 1;
            case INT:
                return Integer.SIZE / Byte.SIZE;
            case LONG:
                return Long.SIZE / Byte.SIZE;
            case SHORT:
                return Short.SIZE / Byte.SIZE;
            case CHAR:
                return Character.SIZE / Byte.SIZE;
            case DOUBLE:
                return Double.SIZE / Byte.SIZE;
            default:
                throw new IllegalStateException("No support at the moment for " + kind);
        }
    }

    static String getIncrementLoopClock(String loopClockName) {
        return new StringBuilder(loopClockName).append("++").toString();
    }

}
