package orm;

import android.content.ContentValues;
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
	static {
		ORMUtil.init(Robolectric.application, "cyingdb", 1, new ORMUtil.SQLiteCallback() {
			public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
			}

			public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			}
		}, "orm");
	}

	//@Test
	public void testInner(){
		BaseDao<TestEntity.InnerEntity> innerDao=ORMUtil.getDao(TestEntity.InnerEntity.class);
		TestEntity.InnerEntity innerEntity=new TestEntity.InnerEntity();
		innerEntity.innerId=8;
		innerEntity.innerName="mm";
		ContentValues innerValues= new ContentValues();
		innerValues.put("innername", innerEntity.innerName);

		assertThat(innerDao.getTableName()).isEqualToIgnoringCase("innerentity");
		assertThat(innerDao.getIndentityName()).isEqualToIgnoringCase("innerid");
		assertThat(innerDao.getIndentity(innerEntity)).isEqualTo(innerEntity.innerId);
		assertThat(innerDao.getTableSQL())
				.isEqualToIgnoringCase("create table innerentity (innerid integer primary key autoincrement,innername TEXT);");
	}

	@Test
	public void testOuter(){

		BaseDao<TestEntity> dao = ORMUtil.getDao(TestEntity.class);
		TestEntity entity = new TestEntity();
		entity.name="shit";

		ContentValues values = new ContentValues();
		values.put("name", entity.name);

		assertThat(dao.getTableName()).isEqualToIgnoringCase("testentity");
		assertThat(dao.getIndentityName()).isEqualToIgnoringCase("id");
		assertThat(dao.getIndentity(entity)).isEqualTo(entity.id);
		assertThat(dao.getTableSQL())
				.isEqualToIgnoringCase("create table testentity (id integer primary key autoincrement,name TEXT);");

		entity.id=dao.save(entity);
		assertThat(entity.id).isGreaterThan(0);
		assertThat(dao.count()).isEqualTo(1);
		assertThat(dao.first()).isEqualsToByComparingFields(entity);
		assertThat(dao.last()).isEqualsToByComparingFields(entity);
		assertThat(dao.delete(entity)).isTrue();
		assertThat(dao.count()).isEqualTo(0);
		assertThat(dao.delete(entity)).isFalse();
		assertThat(dao.first()).isNull();
		assertThat(dao.last()).isNull();



	}

}
