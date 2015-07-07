package com.cying.common.orm.internal;

import com.cying.common.orm.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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

import static com.cying.common.orm.internal.SqliteKeyword.isKeyWord;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
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
	public SourceVersion getSupportedSourceVersion() {
		return super.getSupportedSourceVersion();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<>();
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

	private Map<TypeElement, TableInfo> findTableInfo(RoundEnvironment roundEnv) throws Exception{
		Map<TypeElement, TableInfo> tableInfoMap = new LinkedHashMap<>();
		TableInfo tableInfo;
		for (Element normalElement : roundEnv.getElementsAnnotatedWith(Table.class)) {
			TypeElement element = (TypeElement) normalElement;
			if (isNotClassType(Table.class, element) || isClassInaccessibleViaGeneratedCode(Table.class, "class", element) || isBindingInWrongPackage(Table.class, element)) {
				return tableInfoMap;
			}
			tableInfo = createTableInfo(tableInfoMap, element);
			addColumnInfo(tableInfo, ElementFilter.fieldsIn(element.getEnclosedElements()));

			if (!tableInfo.hasPrimaryKey()) {
				error(element, "%s don't have primary key", tableInfo.getTableName());
			}
		}
		return tableInfoMap;
	}

	private TableInfo createTableInfo(Map<TypeElement, TableInfo> tableInfoMap, TypeElement element) {
		TableInfo tableInfo = null;
		String tableName = element.getAnnotation(Table.class).value().toLowerCase();
		if (tableName.isEmpty()) {
			tableName = element.getSimpleName().toString().toLowerCase();
		}
		if (tableInfoMap.containsKey(element)) {
			error(element, "already exists table with name of %s", tableName);
		} else {

			String classPackage = getPackageName(element);
			String entityClassName = getClassName(element, classPackage);
			String daoClassName = entityClassName.replace(".", "$") + SUFFIX;
			tableInfo = new TableInfo(tableName, classPackage, entityClassName, daoClassName);
			tableInfoMap.put(element, tableInfo);
		}
		return tableInfo;
	}

	private static String getClassName(TypeElement type, String packageName) {
		int packageLen = packageName.length() + 1;
		return type.getQualifiedName().toString().substring(packageLen);
	}

	private void addColumnInfo(TableInfo tableInfo, List<VariableElement> fieldElementList) {
		for (VariableElement fieldElement : fieldElementList) {
			addColumnInfo(tableInfo, fieldElement);
		}
	}

	private void addColumnInfo(TableInfo tableInfo, VariableElement fieldElement) {
		if (!isAnnotationPresent(Ignore.class, fieldElement)) {
			try {
				if (isFieldInaccessibleViaGeneratedCode(Column.class, "fields", fieldElement)) {
					return;
				}

				String fieldClassName = getFieldClassName(fieldElement);
				ColumnInfo columnInfo = new ColumnInfo(fieldElement, fieldClassName);

				//check whether the column name is sqlite keyword
				if (isKeyWord(columnInfo.getColumnName())) {
					error(fieldElement, "%s.%s column  must not be sqlite keyword '%s'", tableInfo.getEntityClassName(), columnInfo.getFieldName(), columnInfo.getColumnName());
				}

				//set  primary key column
				if (isAnnotationPresent(Key.class, fieldElement)) {
					setPrimaryKey(tableInfo, fieldElement, fieldClassName);
				}

				tableInfo.addColumn(columnInfo);
			} catch (Exception e) {
				error(fieldElement, " failed:%s ", e);
			}
		}

	}

	private void setPrimaryKey(TableInfo tableInfo, Element fieldElement, String fieldClassName) {
		TypeMirror typeMirror = fieldElement.asType();
		String fieldName = fieldElement.getSimpleName().toString();
		if (tableInfo.hasPrimaryKey()) {
			error(fieldElement, "@Class (%s) :@Field (%s) :table '%s' already has the primary key '%s'",
					tableInfo.getEntityClassName(), fieldName,
					tableInfo.getTableName(), tableInfo.getPrimaryKeyColumnName());
		} else {
			tableInfo.setPrimaryKeyColumnName(fieldName.toLowerCase());
			tableInfo.setPrimaryKeyFieldName(fieldName);
			if (typeMirror.getKind() != TypeKind.LONG && !fieldClassName.equals(Long.class.getCanonicalName())) {
				error(fieldElement, "@Class (%s) :@Field (%s) :the primary key must be long or Long",
						tableInfo.getEntityClassName(), tableInfo.getPrimaryKeyFieldName());
			}
		}

	}

	private String getFieldClassName(Element fieldElement) {
		TypeMirror typeMirror = fieldElement.asType();
		String fieldClassName;
		if (typeMirror instanceof PrimitiveType) {
			fieldClassName = typeMirror.getKind().name().toLowerCase();

		} else if (typeMirror instanceof DeclaredType) {
			TypeElement fieldType = (TypeElement) typeUtils.asElement(fieldElement.asType());
			fieldClassName = fieldType.getQualifiedName().toString();
		} else if (typeMirror instanceof ArrayType && ((ArrayType) typeMirror).getComponentType().getKind() == TypeKind.BYTE) {
			fieldClassName = byte[].class.getCanonicalName();

		} else {
			throw new IllegalArgumentException("not support this type which field name is " + fieldElement.getSimpleName());
		}
		return fieldClassName;
	}

	private String getPackageName(TypeElement type) {

		return elementUtils.getPackageOf(type).getQualifiedName().toString();
	}

	private static boolean isAnnotationPresent(Class<? extends Annotation> annotationClass, Element element) {
		return element.getAnnotation(annotationClass) != null;
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

	private boolean isFieldInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
	                                                    String targetThing, Element element) {

		boolean hasError = false;
		String className = element.getSimpleName().toString();
		if (element instanceof TypeElement) {
			className = ((TypeElement) element).getQualifiedName().toString();
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
	                                        TypeElement element) {
		TypeMirror superMirrow = element.getSuperclass();
		if (superMirrow instanceof NoType) return false;
		Element superElement = typeUtils.asElement(superMirrow);


		if (superElement instanceof TypeElement) {
			String qualifiedName = ((TypeElement) superElement).getQualifiedName().toString();
			if (Object.class.getCanonicalName().equals(qualifiedName)) return false;

			if (qualifiedName.startsWith(ANDROID_PREFIX)) {
				error(element, "@%s-annotated class incorrectly extends Android framework class. (%s)",
						annotationClass.getSimpleName(), qualifiedName);
				return true;
			}
			if (qualifiedName.startsWith(JAVA_PREFIX)) {
				error(element, "@%s-annotated class incorrectly extends Java framework class. (%s)",
						annotationClass.getSimpleName(), qualifiedName);
				return true;
			}

			return isBindingInWrongPackage(annotationClass, (TypeElement) superElement);
		} else {
			return false;
		}
	}

	private boolean isClassInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
	                                                    String targetThing, Element element) {
		Element enclosingElement = element.getEnclosingElement();
		ElementKind enclosingElementKind = enclosingElement.getKind();

		if (ElementKind.PACKAGE.equals(enclosingElementKind)) {
			return false;
		} else if (enclosingElementKind.isClass() || enclosingElementKind.isInterface()) {
			Set<Modifier> selfModifiers = element.getModifiers();
			if (selfModifiers.contains(PRIVATE) || !selfModifiers.contains(STATIC)) {
				error(element, "%s %s must be static and not be private because the nested table entity class is  inaccessible via generated code.", targetThing, element.getSimpleName());
				return true;
			}
			return isClassInaccessibleViaGeneratedCode(annotationClass, targetThing, enclosingElement);
		} else {
			error(element, "unexpected element kind of %s", enclosingElementKind);
			return true;
		}
	}

	private boolean isNotClassType(Class<? extends Annotation> annotationClass, Element element) {
		if (!ElementKind.CLASS.equals(element.getKind())) {
			error(element, "%s-annotated is not class type", annotationClass.getSimpleName());
			return true;
		}
		return false;
	}

}
