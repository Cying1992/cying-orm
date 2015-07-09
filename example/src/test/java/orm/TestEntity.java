package orm;

import android.app.Activity;
import com.cying.common.orm.*;
import orm.internal.EnumWrapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * User: Cying
 * Date: 2015/7/6
 * Time: 23:02
 */
@Table
public class TestEntity{

	@Key
	Long id;

	//@Column("id")
	String name;

	int a;
	long b;
	double c;
	float d;

	@NotNull(NullValueStrategy.DEFAULT)
	Integer e;

	@NotNull()
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
