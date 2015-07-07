package orm;

import com.cying.common.orm.Key;
import com.cying.common.orm.Table;

/**
 * User: Cying
 * Date: 2015/7/6
 * Time: 23:02
 */
@Table
public class TestEntity {
	@Key
	Long id;
	String name;
	int inde;
	double num;
	float phone;
	boolean bmm;


	@Table
	static class InnerEntity{
		@Key
		long innerId;
		String innerName;
	}

}
