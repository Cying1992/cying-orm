package orm;

import com.wykst.cying.common.orm.Column;
import com.wykst.cying.common.orm.Key;
import com.wykst.cying.common.orm.Table;

/**
 * User: Cying
 * Date: 2015/8/7
 * Time: 1:25
 */
@Table
public class C {
	@Key
	Long id;
	@Column
	A a;
}
