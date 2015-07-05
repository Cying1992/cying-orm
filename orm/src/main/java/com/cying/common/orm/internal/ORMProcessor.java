package com.cying.common.orm.internal;

import com.cying.common.orm.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午10:20
 */

public final class ORMProcessor extends AbstractProcessor {
    public static final String SUFFIX = "$$Dao";
    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.add(Column.class.getCanonicalName());
        types.add(Ignore.class.getCanonicalName());
        types.add(NotNull.class.getCanonicalName());
        types.add(Table.class.getCanonicalName());
        types.add(Unique.class.getCanonicalName());
        types.add(Key.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        note("Begin generate cying-orm code: ");
        try {
            Map<TypeElement, TableInfo> tableInfoMap = findTableInfo(roundEnv);
            for (Map.Entry<TypeElement, TableInfo> entry : tableInfoMap.entrySet()) {
                TypeElement typeElement = entry.getKey();
                TableInfo tableInfo = entry.getValue();

                try {
                    JavaFileObject jfo = filer.createSourceFile(tableInfo.getFqcn(), typeElement);
                    Writer writer = jfo.openWriter();
                    writer.write(tableInfo.brewJava());
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    error(typeElement, "Unable to write base dao for type %s: %s", typeElement,
                            e.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            //  e.printStackTrace();
            error(null, "failed generate code:%s", e);
        }

        return false;
    }

    private Map<TypeElement, TableInfo> findTableInfo(RoundEnvironment roundEnv) {
        Map<TypeElement, TableInfo> tableInfoMap = new LinkedHashMap<>();
        TableInfo tableInfo;
        for (Element normalElement : roundEnv.getElementsAnnotatedWith(Table.class)) {

            TypeElement element = (TypeElement) normalElement;
            if (isInaccessibleViaGeneratedCode(Table.class, "class", element) || isBindingInWrongPackage(Table.class, element)) {
                return tableInfoMap;
            }
            String tableName = element.getAnnotation(Table.class).value().toLowerCase();
            if (tableName == null || tableName.isEmpty()) {
                tableName = element.getSimpleName().toString().toLowerCase();
            }
            if (tableInfoMap.containsKey(tableName)) {
                error(element, "already exists table with name of %s", tableName);
                continue;
            } else {
                String targetType = element.getQualifiedName().toString();
                String classPackage = getPackageName(element);
                String className = getClassName(element, classPackage) + SUFFIX;
                tableInfo = new TableInfo(tableName, classPackage, targetType, className);
                tableInfoMap.put(element, tableInfo);
            }

            for (VariableElement fieldElement : ElementFilter.fieldsIn(element.getEnclosedElements())) {

                addColumnInfo(tableInfo, fieldElement);
            }
        }
        return tableInfoMap;
    }


    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private void addColumnInfo(TableInfo tableInfo, VariableElement fieldElement) {
        if(isInaccessibleViaGeneratedCode(Column.class,"fields",fieldElement))
        {
            return;
        }
        String fieldName = fieldElement.getSimpleName().toString();
        note("fieldName=%s", fieldName);

        Ignore ignore = fieldElement.getAnnotation(Ignore.class);
        Key key = fieldElement.getAnnotation(Key.class);
        if (ignore == null) {
            try {

                TypeMirror typeMirror = fieldElement.asType();


                //the  primary key column
                if (key != null) {
                    if (tableInfo.hasPrimaryKey()) {
                        error(fieldElement, "@Class (%s) :@Field (%s) :table '%s' already has the primary key '%s'",
                                tableInfo.getEntityClassName(), fieldName,
                                tableInfo.getTableName(), tableInfo.getPrimaryKeyColumnName());
                    } else {
                        tableInfo.setPrimaryKeyColumnName(fieldName.toLowerCase());
                        tableInfo.setPrimaryKeyFieldName(fieldName);
                        if (typeMirror.getKind() != TypeKind.LONG) {
                            error(fieldElement, "@Class (%s) :@Field (%s) :the primary key can only be long or Long",
                                    tableInfo.getEntityClassName(), tableInfo.getPrimaryKeyFieldName());
                        }
                    }
                    return;
                }

                //other columns except the primary key
                String fieldClassName;
                note(byte[].class.getCanonicalName());


                if (typeMirror instanceof PrimitiveType) {
                    fieldClassName = typeMirror.getKind().name().toLowerCase();

                } else if (typeMirror instanceof DeclaredType) {
                    TypeElement fieldType = (TypeElement) typeUtils.asElement(fieldElement.asType());
                    fieldClassName = fieldType.getQualifiedName().toString();
                } else if (typeMirror instanceof ArrayType && ((ArrayType) typeMirror).getComponentType().getKind() == TypeKind.BYTE) {
                    fieldClassName = byte[].class.getCanonicalName();

                } else {
                    throw new IllegalArgumentException("not support this type which field name is " + fieldName);
                }

                tableInfo.addColumn(new ColumnInfo(fieldElement, fieldClassName));
            } catch (Exception e) {
                //e.printStackTrace();
                error(fieldElement, " failed:%s ", e);
            }
        }

    }

    private String getPackageName(TypeElement type) {

        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private void note(String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing,Element element) {

        boolean hasError = false;
         String className=element.getSimpleName().toString();
        if(element instanceof TypeElement){
             className=((TypeElement)element).getQualifiedName().toString();
        }
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, className,
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        //TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = element.getSimpleName().toString();

        if (qualifiedName.startsWith(ANDROID_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith(JAVA_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    static boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

}
