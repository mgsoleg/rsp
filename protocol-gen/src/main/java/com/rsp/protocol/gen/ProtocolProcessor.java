package com.rsp.protocol.gen;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"com.rsp.protocol.gen.Protocol"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ProtocolProcessor extends AbstractProcessor {

    private static final int INVALID_ORDER_IN_PROTOCOL_CHUNK = -1;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(Protocol.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation <Protocol> can be applied only to class. ");
                return true;
            }
            proccessProtocolFor((TypeElement) element);
        }
        return true;
    }

    private void proccessProtocolFor(TypeElement element) {
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        int chunks = element.getAnnotation(Protocol.class).chunks();
        VariableElement[] protocolFields = new VariableElement[chunks];

        int fieldsCount = 0;

        for (Element e : enclosedElements) {
            if (elementIsField(e)) {
                int order = checkAndReturnValidOrderFor(chunks, protocolFields, (VariableElement) e);
                if (order != INVALID_ORDER_IN_PROTOCOL_CHUNK) {
                    protocolFields[order] = (VariableElement) e;
                    fieldsCount++;
                }
            }
        }

        if (doesChunksCoorespondsToFieldsCount(chunks, fieldsCount)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "number of protocol fields should be equals to chunks field ", element);
        }

        applyAndReturnProtocolVisitorFor(element, protocolFields).extendWithMarshallers();
    }

    private ProtocolVisitor applyAndReturnProtocolVisitorFor(TypeElement annotatedClass, VariableElement[] protocolFields) {
        ProtocolVisitor protocolVisitor = new ProtocolVisitor(processingEnv, annotatedClass);
        for (int i = 0; i < protocolFields.length; i++) {
            VariableElement e = protocolFields[i];
            e.accept(protocolVisitor, null);
        }
        return protocolVisitor;
    }

    private boolean doesChunksCoorespondsToFieldsCount(int chunks, int countOfFields) {
        return countOfFields != (chunks);
    }

    private int checkAndReturnValidOrderFor(int chunks, VariableElement[] protocolFields, VariableElement field) {
        ProtocolChunk annotation = field.getAnnotation(ProtocolChunk.class);
        if (validAnnotationOrder(annotation, chunks, protocolFields)) {
            return annotation.order();
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Protocol fields should have distinct order with step 1", field);
            return INVALID_ORDER_IN_PROTOCOL_CHUNK;
        }
    }

    private boolean validAnnotationOrder(ProtocolChunk annotation, int chunks, VariableElement[] protocolFields) {
        return (annotation != null) && (annotation.order() >= 0 && annotation.order() < chunks && protocolFields[annotation.order()] == null);
    }

    private boolean elementIsField(Element e) {
        return e.getKind() == ElementKind.FIELD;
    }
}
