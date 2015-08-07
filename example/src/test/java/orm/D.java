package orm;

import com.wykst.cying.common.orm.Column;
import com.wykst.cying.common.orm.Key;
import com.wykst.cying.common.orm.Table;

/**
 * User: Cying
 * Date: 2015/8/7
 * Time: 11:15
 */
@Table
public class D {
	@Key
	Long id;
	@Column
	A a;
	@Column
	C c;
	@Column
	B b;
	@Column
	D d;
}
