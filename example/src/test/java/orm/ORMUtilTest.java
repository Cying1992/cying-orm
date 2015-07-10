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

import java.util.Iterator;

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
	public void testInner() {
		BaseDao<TestEntity.InnerEntity> innerDao = ORMUtil.getDao(TestEntity.InnerEntity.class);
		TestEntity.InnerEntity innerEntity = new TestEntity.InnerEntity();
		innerEntity.innerId = 8;
		innerEntity.innerName = "mm";
		ContentValues innerValues = new ContentValues();
		innerValues.put("innername", innerEntity.innerName);

		assertThat(innerDao.getTableName()).isEqualToIgnoringCase("innerentity");
		assertThat(innerDao.getIndentityName()).isEqualToIgnoringCase("innerid");
		assertThat(innerDao.getIndentity(innerEntity)).isEqualTo(innerEntity.innerId);
		assertThat(innerDao.getTableSQL())
				.isEqualToIgnoringCase("create table innerentity (innerid integer primary key autoincrement,innername TEXT);");

		innerDao.deleteAll();
	}

	//@Test
	public void testOuter() {
		BaseDao<TestEntity> dao = ORMUtil.getDao(TestEntity.class);
		TestEntity entity = new TestEntity();
		entity.name = "shit";


		ContentValues values = new ContentValues();
		values.put("name", entity.name);

		assertThat(dao.getTableName()).isEqualToIgnoringCase("testentity");
		assertThat(dao.getIndentityName()).isEqualToIgnoringCase("id");
		assertThat(dao.getIndentity(entity)).isEqualTo(entity.id);
		assertThat(dao.getTableSQL())
				.isEqualToIgnoringCase("create table testentity (id integer primary key autoincrement,name TEXT);");

		entity.id = dao.save(entity);
		assertThat(entity.id).isGreaterThan(0);
		assertThat(dao.findById(entity.id)).isEqualsToByComparingFields(entity);
		Iterator<TestEntity> iterator = dao.findAsIterator(null);
		while (iterator.hasNext()) {
			assertThat(iterator.next()).isEqualsToByComparingFields(entity);
		}

		assertThat(dao.count()).isEqualTo(1);
		assertThat(dao.first()).isEqualsToByComparingFields(entity);
		assertThat(dao.last()).isEqualsToByComparingFields(entity);
		assertThat(dao.delete(entity)).isTrue();
		assertThat(dao.count()).isEqualTo(0);
		assertThat(dao.delete(entity)).isFalse();
		assertThat(dao.first()).isNull();
		assertThat(dao.last()).isNull();

		dao.deleteAll();
	}

	//@Test
	public void testCurd() {

		BaseDao<TestEntity> dao = ORMUtil.getDao(TestEntity.class);
		TestEntity entity1 = new TestEntity();
		entity1.name = "name";


		//insert
		long id = dao.save(entity1);
		assertThat(id).isGreaterThan(0);
//		System.out.println(dao.first().bmm+"");
		assertThat(dao.findById(id)).isLenientEqualsToByIgnoringFields(entity1, "id");
		assertThat(dao.first()).isLenientEqualsToByIgnoringFields(entity1, "id");
		assertThat(dao.last()).isLenientEqualsToByIgnoringFields(entity1, "id");

		assertThat(dao.count()).isEqualTo(1);
		//assertThat(dao.listPage(0, 0)).isNull();
		assertThat(dao.listPage(1, 0)).isNotNull().hasSize(1);
		assertThat(dao.listPage(1, 1)).isNotNull().isEmpty();

		//delete
		assertThat(dao.delete(entity1)).isFalse();

		//update
		entity1.id = id;
		entity1.name = "changedName";
		assertThat(dao.save(entity1)).isEqualTo(id);
		assertThat(dao.count()).isEqualTo(1);
		assertThat(dao.findById(id)).isEqualsToByComparingFields(entity1);
		assertThat(dao.first()).isEqualsToByComparingFields(entity1);
		assertThat(dao.last()).isEqualsToByComparingFields(entity1);
		assertThat(dao.listPage(1, 0)).isNotNull().hasSize(1);
		assertThat(dao.listPage(1, 1)).isNotNull().isEmpty();
		assertThat(dao.first("name=?", "kk")).isNull();
		assertThat(dao.last("name=?", "kk")).isNull();
		assertThat(dao.first("name=?", "changedName")).isEqualsToByComparingFields(entity1);
		assertThat(dao.last("name=?", "changedName")).isEqualsToByComparingFields(entity1);
		assertThat(dao.find("name=?", "kk")).hasSize(0);
		assertThat(dao.find("name=?", "changedName")).hasSize(1);
		assertThat(dao.find("name=?", "changedName").get(0)).isEqualsToByComparingFields(entity1);

		assertThat(dao.listAll()).hasSize(1);
		assertThat(dao.listAll().get(0)).isEqualsToByComparingFields(entity1);

		entity1.id = -1L;
		entity1.name = "mmmmmm";
		assertThat(dao.save(entity1)).isEqualTo(-1);
		assertThat(dao.count()).isEqualTo(1);
		assertThat(dao.first().name).isEqualTo("changedName");

		entity1.id = null;
		entity1.name = "haha";
		assertThat(dao.save(entity1)).isGreaterThan(0);
		assertThat(dao.count()).isEqualTo(2);

		//delete
		//assertThat(dao.delete(entity1)).isTrue();
		//assertThat(dao.deleteAll()).isZero();

		dao.deleteAll();
	}


	@Test
	public void saveWithPrimaryKeyNull(){
		BaseDao<TestEntity> dao=ORMUtil.getDao(TestEntity.class);
		TestEntity entity=new TestEntity();
		entity.id=null;
		entity.name = "";
		entity.id=dao.save(entity);

		TestEntity savedEntity=dao.first();

		assertThat(dao.count()).isEqualTo(1);
		assertThat(savedEntity.id).isGreaterThan(0);
		assertThat(savedEntity).isEqualsToByComparingFields(entity);

		dao.deleteAll();
	}

	@Test
	public void saveWithPrimaryKeyLessThanOne(){
		BaseDao<TestEntity> dao=ORMUtil.getDao(TestEntity.class);
		TestEntity entity=new TestEntity();
		entity.id=0L;
		entity.name="name";
		assertThat(entity.id < 1).isTrue();

		//dao.insertMany(entity,entity);
		//assertThat(dao.count()).isEqualTo(2);

		dao.save(entity);
		TestEntity first=dao.first();
		assertThat(dao.count()).isEqualTo(1);
		assertThat(first).isLenientEqualsToByIgnoringFields(entity, "id");

		//entity.id=null;
		entity.name="changed";
		dao.save(entity);
		TestEntity last=dao.last();
		assertThat(dao.count()).isEqualTo(2);
		assertThat(last.name).isEqualToIgnoringCase(entity.name);
		assertThat(last).isLenientEqualsToByIgnoringFields(entity, "id");

		assertThat(first.id).isLessThan(last.id);

		dao.deleteAll();
	}

	@Test
	public void saveWithPrimaryKeyGreaterThanOne(){
		BaseDao<TestEntity> dao=ORMUtil.getDao(TestEntity.class);
		TestEntity entity=new TestEntity();
		entity.id=3L;
		entity.name="name";
		entity.id=dao.save(entity);

		TestEntity savedEntity=dao.first();
		assertThat(dao.count()).isEqualTo(1);
		assertThat(savedEntity.id).isGreaterThan(0);

		entity.name="changed";
		assertThat(dao.save(entity)).isEqualTo(entity.id);
		savedEntity=dao.first();
		assertThat(dao.count()).isEqualTo(1);
		assertThat(savedEntity).isEqualsToByComparingFields(entity);

		dao.deleteAll();

	}


}
