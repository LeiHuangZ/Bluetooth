package win.lioil.bluetooth.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @author LeiHuang 1252065297@qq.com
 * <code>
 * Create At 18:19 2023/5/23
 * Update By <whom>
 * Update At 18:19 2023/5/23
 * describe <Function description>
 * </code>
 */
@Entity
public class BtMacDao {
    @Id(autoincrement = true)
    private Long id;

    @Unique
    private String macAdr;

    @Generated(hash = 1277427630)
    public BtMacDao(Long id, String macAdr) {
        this.id = id;
        this.macAdr = macAdr;
    }

    @Generated(hash = 1281886123)
    public BtMacDao() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMacAdr() {
        return this.macAdr;
    }

    public void setMacAdr(String macAdr) {
        this.macAdr = macAdr;
    }
}
