package orm;

import com.wykst.cying.common.orm.Column;
import com.wykst.cying.common.orm.Key;
import com.wykst.cying.common.orm.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * User: Cying
 * Date: 2015/7/6
 * Time: 23:02
 */
@Table(database = "kk")
public class TestEntity {

	@Key
	Long id;

	@Column
	String name;

	@Column
	TestEntity entity;

	int a;
	long b;
	double c;
	float d;


	Integer e;


	Double f;

	Float g;

	Date h;

	Calendar i;

	Timestamp j;

	BigDecimal k;

	MyEnum m;

	byte[] n;
	boolean o;

	Boolean p;


	@Table
	static class InnerEntity {
		@Key
		long innerId;
		String innerName;
	}


	 enum MyEnum{

	}
}
