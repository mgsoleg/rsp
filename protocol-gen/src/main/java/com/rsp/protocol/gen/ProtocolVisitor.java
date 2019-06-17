package com.rsp.protocol.gen;

import com.squareup.javapoet.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class ProtocolVisitor extends ElementScanner8<Void, Void> {

    private List<CodeBlock> unmarshallerOrder = new ArrayList<>();
    private List<CodeBlock> marshallerOrder = new ArrayList<>();

    private int messageSizeInBytes = 0;

    private final Trees trees;
    private final Messager logger;
    private final Filer filer;
    private final TypeElement baseClass;
    private final TreeMaker treeMaker;
    private final Names names;

    ProtocolVisitor(ProcessingEnvironment env, TypeElement element) {
        super();
        baseClass = element;
        trees = Trees.instance(env);
        filer = env.getFiler();
        logger = env.getMessager();

        final JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
        treeMaker = TreeMaker.instance(javacEnv.getContext());
        names = Names.instance(javacEnv.getContext());
    }


    @Override
    public Void visitVariable(VariableElement field, Void aVoid) {
        ((JCTree) trees.getTree(field)).accept(new TreeTranslator() {
            @Override
            public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                super.visitVarDef(jcVariableDecl);
                jcVariableDecl.mods.flags &= ~Flags.PRIVATE;
            }
        });
        addFieldToProcessAndCountBufferLength(field);
        return super.visitVariable(field, aVoid);
    }

    private void addFieldToProcessAndCountBufferLength(VariableElement field) {

        final TypeKind kind = field.asType().getKind();

        if (kind.isPrimitive()) {
            processPrimitive(field);

        } else if (kind == TypeKind.ARRAY) {
            processArray(field);

        } else {
            logger.printMessage(Diagnostic.Kind.ERROR, "Only primitives and arrays of primitives can be used.", field);
        }
    }

    private void processPrimitive(VariableElement field) {
        addReadBufferStatement(field);
        addWriteBufferOperation(field);
        messageSizeInBytes += SyntaxHelper.getByteSizeForPrimitive(field.asType().getKind());
    }

    private void addWriteBufferOperation(VariableElement field) {
        TypeMirror type = field.asType();
        final String statement = SyntaxHelper.writeToBufferFrom(type);

        CodeBlock writeToBuffer = CodeBlock
                .builder()
                .addStatement(statement, ClassName.get(baseClass), field.getSimpleName())
                .build();

        marshallerOrder.add(writeToBuffer);
    }


    private void addReadBufferStatement(VariableElement field) {
        TypeMirror type = field.asType();
        final String statement = SyntaxHelper.readFromBufferIn(type);

        CodeBlock readFromBuffer = CodeBlock
                .builder()
                .addStatement(statement, ClassName.get(baseClass), field.getSimpleName())
                .build();

        unmarshallerOrder.add(readFromBuffer);
    }


    private void processArray(VariableElement field) {

        ArrayType asArrayType = (ArrayType) field.asType();
        final ProtocolChunk arrayAnnotation = field.getAnnotation(ProtocolChunk.class);

        if (arrayAnnotation.elementsNumber() == 0) {
            logger.printMessage(Diagnostic.Kind.ERROR, "For array based chunks you should specify size of it. Please use elementsNumber field", field);
            return;
        }

        final String loopClockName = SyntaxHelper.getLoopClockName(field);

        final String variableInitialization = SyntaxHelper.getVarInitialization(loopClockName);

        final String beginControlFlow = SyntaxHelper.getBeginControlFlow(loopClockName);

        final TypeMirror primitiveType = asArrayType.getComponentType();
        final String writeToVariableStatement = SyntaxHelper.getWriteToVariableStatement(loopClockName, primitiveType);

        final String incrementLoopClock = SyntaxHelper.getIncrementLoopClock(loopClockName);

        CodeBlock writeToVariableLoop = CodeBlock
                .builder()
                .addStatement(variableInitialization)
                .beginControlFlow(beginControlFlow, ClassName.get(baseClass), field.getSimpleName())
                .addStatement(writeToVariableStatement, ClassName.get(baseClass), field.getSimpleName())
                .addStatement(incrementLoopClock)
                .endControlFlow()
                .build();
        unmarshallerOrder.add(writeToVariableLoop);

        final String writeToBufferStatement = SyntaxHelper.getWriteToBufferStatement(loopClockName, primitiveType);

        CodeBlock writeToBufferLoop = CodeBlock
                .builder()
                .addStatement(variableInitialization)
                .beginControlFlow(beginControlFlow, ClassName.get(baseClass), field.getSimpleName())
                .addStatement(writeToBufferStatement, ClassName.get(baseClass), field.getSimpleName())
                .addStatement(incrementLoopClock)
                .endControlFlow()
                .build();
        marshallerOrder.add(writeToBufferLoop);

        messageSizeInBytes += (SyntaxHelper.getByteSizeForPrimitive(primitiveType.getKind()) * arrayAnnotation.elementsNumber());
    }


    void extendWithMarshallers() {
        final List<? extends TypeMirror> interfaces = baseClass.getInterfaces();


        final TypeSpec.Builder typeSpecbuilder = TypeSpec.classBuilder(baseClass.getSimpleName() + "$$Proxy")
                .addModifiers(Modifier.ABSTRACT)
                .superclass(ClassName.get(baseClass.getSuperclass()))
                .addOriginatingElement(baseClass)
                .addMethod(getUnMarshallerMethod())
                .addMethod(getMarshallerMethod())
                .addMethod(getMessageByteSizeMethod());
        interfaces.forEach(i -> typeSpecbuilder.addSuperinterface(ClassName.get(i)));
        final TypeSpec typeSpec = typeSpecbuilder.build();

        final JavaFile javaFile = JavaFile.builder(baseClass.getEnclosingElement().toString(), typeSpec)
                .addFileComment("Generated by Protocol processor, do not modify")
                .build();
        try {
            final JavaFileObject sourceFile = filer.createSourceFile(
                    javaFile.packageName + "." + typeSpec.name, baseClass);
            try (final Writer writer = new BufferedWriter(sourceFile.openWriter())) {
                javaFile.writeTo(writer);
            }
            JCTree.JCExpression selector = treeMaker.Ident(names.fromString(javaFile.packageName));
            selector = treeMaker.Select(selector, names.fromString(typeSpec.name));
            ((JCTree.JCClassDecl) trees.getTree(baseClass)).extending = selector;
        } catch (IOException e) {
            logger.printMessage(Diagnostic.Kind.ERROR, e.getMessage(), baseClass);
        }
    }

    private MethodSpec getMarshallerMethod() {
        final MethodSpec.Builder marshallerMethodBuilder = MethodSpec.methodBuilder("marshallTo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(ByteBuffer.class), SyntaxHelper.BUFFER_NAME);
        marshallerOrder.forEach(marshallerMethodBuilder::addCode);
        return marshallerMethodBuilder.build();
    }

    private MethodSpec getMessageByteSizeMethod() {
        return MethodSpec.methodBuilder("messageSizeInBytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return " + messageSizeInBytes).build();
    }

    private MethodSpec getUnMarshallerMethod() {
        final MethodSpec.Builder unmarshallerMethodBuilder = MethodSpec.methodBuilder("unMarshallFrom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(ByteBuffer.class), SyntaxHelper.BUFFER_NAME);
        unmarshallerOrder.forEach(unmarshallerMethodBuilder::addCode);
        return unmarshallerMethodBuilder.build();
    }

}
