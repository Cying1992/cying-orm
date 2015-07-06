package orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.cying.common.orm.BaseDao;
import com.cying.common.orm.ORMUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
/**
 * User: Cying
 * Date: 2015/7/7
 * Time: 0:10
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ORMUtilTest {

	@Test public void testCreateTable(){
		ORMUtil.init(Robolectric.application, "cyingdb", 1, new ORMUtil.SQLiteCallback() {
			@Override
			public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
			}

			@Override
			public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			}
		}, "orm");
		BaseDao<orm.Test> baseDao=new BaseDao<orm.Test>() {
			@Override
			public orm.Test cursorToEntity(Cursor cursor) {
				return null;
			}

			@Override
			public ContentValues entityToValues(orm.Test entity) {
				return null;
			}

			@Override
			public String getTableName() {
				return null;
			}

			@Override
			public String getIndentityName() {
				return null;
			}

			@Override
			public long getIndentity(orm.Test entity) {
				return 0;
			}
		};
		assertThat(ORMUtil.getDao(orm.Test.class).getTableName()).isEqualToIgnoringCase("test");

	}

}
