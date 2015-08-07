package orm;

import com.wykst.cying.common.orm.Column;
import com.wykst.cying.common.orm.Key;
import com.wykst.cying.common.orm.Table;

/**
 * User: Cying
 * Date: 2015/8/7
 * Time: 1:04
 */
@Table
public class A {
	@Key
	Long id;

	@Column
	String name;

	@Column
	A a;
	@Column
	C c;
	@Column
	B b;
	@Column
	D d;

}
