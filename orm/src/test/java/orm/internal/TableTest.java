package orm.internal;

import com.cying.common.orm.internal.ORMProcessor;
import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.ASSERT;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

/**
 * User: Cying
 * Date: 2015/7/6
 * Time: 22:55
 */
public class TableTest {
    @Test
    public void testTable() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", Joiner.on('\n').join(
                "package test;",
                "import com.cying.common.orm.Table;",
                "import com.cying.common.orm.Key;",
                "@Table",
                "public class Test {",
                "  @Key long id;",
		        "  String name;",
                "}"));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/Test$$ViewBinder",
                Joiner.on('\n').join(
		                "// Generated code from Cying-ORM. Do not modify!",
		                "package test;",
                        "import android.content.ContentValues;",
                        "import android.database.Cursor;",
                        "import com.cying.common.orm.BaseDao;",
                        "public class Test$$Dao extends BaseDao<Test> {",
                        "    private static String SQL=\"CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT);\"",
                        "    static {",
                        "       saveSQL(SQL);",
                        "    }",
                        "    @Override public Test cursorToEntity(Cursor cursor) {",
                        "        Test entity=new Test();",
                        "        entity.id=cursor.getLong(cursor.getColumnIndex(\"id\"));",
		                "        entity.name=cursor.getString(cursor.getColumnIndex(\"name\"));",
                        "        return entity;",
                        "    }",
                        "    @Override public ContentValues entityToValues(Test entity) {",
                        "        ContentValues values=new ContentValues();",
		                "        values.put(\"name\",entity.name)",
                        "        return values;",
                        "    }",
                        "    @Override public String getTableName() {",
                        "        return \"test\";",
                        "    }",
                        "    @Override public String getIndentityName() {",
                        "        return \"id\";",
                        "    }",
                        "    @Override public long getIndentity(Test entity) {",
                        "        return entity.id;",
                        "    }",
                        "}"
                ));

        ASSERT.about(javaSource()).that(source)
                .processedWith(new ORMProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }
}