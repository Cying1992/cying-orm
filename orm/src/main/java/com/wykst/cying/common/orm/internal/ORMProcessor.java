package com.wykst.cying.common.orm.internal;

import com.wykst.cying.common.orm.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;

import static javax.lang.model.element.Modifier.*;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午10:20
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class ORMProcessor extends AbstractProcessor {
	public static final String SUFFIX = "$$Dao";
	public static final String ANDROID_PREFIX = "android.";
	public static final String JAVA_PREFIX = "java.";
	private static Elements elementUtils;
	private static Types typeUtils;
	private static Filer filer;
	private static Messager messager;

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		elementUtils = env.getElementUtils();
		typeUtils = env.getTypeUtils();
		filer = env.getFiler();
		messager = processingEnv.getMessager();
	}


	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<>();
		types.add(Column.class.getCanonicalName());
		types.add(NotNull.class.getCanonicalName());
		types.add(Table.class.getCanonicalName());
		types.add(Unique.class.getCanonicalName());
		types.add(Key.class.getCanonicalName());
		return types;
	}


	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		try {
			Map<TypeElement, TableClass> tableInfoMap = findTableClass(roundEnv);
			for (Map.Entry<TypeElement, TableClass> entry : tableInfoMap.entrySet()) {
				TypeElement typeElement = entry.getKey();
				TableClass tableClass = entry.getValue();
				try {
					JavaFileObject jfo = filer.createSourceFile(tableClass.getFqcn(), typeElement);
					Writer writer = jfo.openWriter();
					writer.write(tableClass.brewJava());
					writer.flush();
					writer.close();
				} catch (IOException e) {
					error(typeElement, "Unable to write base dao for type %s: %s", typeElement,
							e.getMessage());
				}
			}
			return true;
		} catch (Exception e1) {
			error(null, "failed generate code:%s--->%s", e1, Arrays.toString(e1.getStackTrace()));
		}

		return false;
	}

	private Map<TypeElement, TableClass> findTableClass(RoundEnvironment roundEnv) {
		Map<TypeElement, TableClass> tableClassMap = new HashMap<>();
		TableClass tableClass;
		for (Element normalElement : roundEnv.getElementsAnnotatedWith(Table.class)) {
			TypeElement element = (TypeElement) normalElement;
			tableClass = new TableClass(element);
			if (!tableClass.hasPrimaryKey()) {
				error(element, "Table '%s' don't have the primary key", tableClass.getTableName());
				return tableClassMap;
			}
			tableClassMap.put(element, tableClass);
		}
		return tableClassMap;
	}

	static void checkKeyWord(Element fieldElement, String columnName, String entityClassName, String fieldName) {
		if (columnName == null || columnName.isEmpty()) return;
		for (String keyword : SqliteKeyword.keywords) {
			if (keyword.equalsIgnoreCase(columnName)) {
				error(fieldElement, "%s.%s :This column name  must not be a sqlite3 keyword  '%s'", entityClassName, fieldName, columnName);
				return;
			}
		}
	}

	static boolean isEnum(Element fieldElement) {
		Element element = typeUtils.asElement(fieldElement.asType());
		return element != null && ElementKind.ENUM.equals(element.getKind());
	}

	static String getFieldClassNameOf(Element fieldElement) {

		TypeMirror typeMirror = fieldElement.asType();
		String fieldClassName = null;

		if (typeMirror instanceof PrimitiveType) {
			fieldClassName = typeMirror.getKind().name().toLowerCase();

		} else if (typeMirror instanceof DeclaredType) {

			TypeElement fieldType = (TypeElement) typeUtils.asElement(typeMirror);
			fieldClassName = fieldType.getQualifiedName().toString();
		} else if (typeMirror instanceof ArrayType && ((ArrayType) typeMirror).getComponentType().getKind() == TypeKind.BYTE) {
			fieldClassName = byte[].class.getCanonicalName();

		} else {
			error(fieldElement, "not support this type which field name is %s", fieldElement.getSimpleName());

		}
		return fieldClassName;
	}

	static String getPackageNameOf(TypeElement type) {

		return elementUtils.getPackageOf(type).getQualifiedName().toString();
	}

	static boolean isAnnotationPresent(Class<? extends Annotation> annotationClass, Element element) {
		return element.getAnnotation(annotationClass) != null;
	}


	static void error(Element element, String message, Object... args) {
		if (args.length > 0) {
			message = String.format(message, args);
		}
		messager.printMessage(ERROR, message, element);
	}

	static boolean isFieldInaccessibleViaGeneratedCode(TypeElement typeElement, Element fieldElement) {
		boolean hasError = false;
		Set<Modifier> modifiers = fieldElement.getModifiers();
		if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC) || modifiers.contains(FINAL)) {
			error(fieldElement, "The table entity's fields must not be private , static or final. (%s.%s)", typeElement.getQualifiedName(),
					fieldElement.getSimpleName());
			hasError = true;
		}

		return hasError;
	}

	static boolean isClassInaccessibleViaGeneratedCode(Element element) {
		Element enclosingElement = element.getEnclosingElement();
		ElementKind enclosingElementKind = enclosingElement.getKind();

		if (ElementKind.PACKAGE.equals(enclosingElementKind)) {
			return false;
		} else if (enclosingElementKind.isClass() || enclosingElementKind.isInterface()) {
			Set<Modifier> selfModifiers = element.getModifiers();
			if (selfModifiers.contains(PRIVATE) || !selfModifiers.contains(STATIC)) {
				error(element, "Clas %s must be static and not be private which will cause the nested table entity class to be  inaccessible via generated code.", element.getSimpleName());
				return true;
			}
			return isClassInaccessibleViaGeneratedCode(enclosingElement);
		} else {
			error(element, "unexpected element kind of %s", enclosingElementKind);
			return true;
		}
	}

	static void isNotClassType(Class<? extends Annotation> annotationClass, Element element) {
		if (!ElementKind.CLASS.equals(element.getKind())) {
			error(element, "%s-annotated is not class type", annotationClass.getSimpleName());
		}
	}

}
