package orm;

import com.cying.common.orm.Key;
import com.cying.common.orm.Table;

import javax.swing.plaf.basic.BasicEditorPaneUI;

/**
 * User: Cying
 * Date: 2015/7/6
 * Time: 23:02
 */
@Table
public class Test {
    @Key
    long id;
}
