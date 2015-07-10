package orm;

import com.cying.common.orm.Key;
import com.cying.common.orm.NotNull;
import com.cying.common.orm.Table;

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

	@NotNull
	String name;

	int a;
	long b;
	double c;
	float d;

	@NotNull
	Integer e;

	@NotNull
	Double f;

	@NotNull
	Float g;
	@NotNull
	Date h;
	@NotNull
	Calendar i;
	@NotNull
	Timestamp j;
	@NotNull
	BigDecimal k;
	@NotNull
	MyEnum m;
	@NotNull
	byte[] n;
	boolean o;
	@NotNull
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
