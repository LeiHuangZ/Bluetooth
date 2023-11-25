package win.lioil.bluetooth.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.greenrobot.greendao.database.StandardDatabase;

import java.lang.reflect.Method;

public class DBOpenHelper extends DaoMaster.DevOpenHelper {

    public DBOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 数据库创建
        StandardDatabase database = new StandardDatabase(db);
        DaoMaster.createAllTables(database, false);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e("DBOpenHelper", "DB onUpgrade");
        try {
            if (newVersion > oldVersion) {
                //更改表结构
                DBMigrationHelper helper = new DBMigrationHelper(db);
                helper.upgradeTablesScheme();

//                for (int i = oldVersion + 1; i <= newVersion; i++) {
//                    //获取不同数据库版本对应的迁移方法
//                    //当用户跨版本升级时，依次执行每个版本对应的升级方法
//                    Method method = getMigrationMethod(i);
//                    if (method != null) {
//                        method.invoke(this, db);
//                    }
//                }

                //删除临时表
                helper.dropTempTables();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DBOpenHelper", "DB onUpgrade failed:" + e.getMessage());
        }
    }

    /**
     * 获取数据库版本对应的迁移方法
     * 数据库迁移方法命名格式为 migration_Scheme,其中
     * Scheme为每个数据库版本的版本号
     */
    private Method getMigrationMethod(int version) {
        try {
            return getClass().getDeclaredMethod("migration_" + version, SQLiteDatabase.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** V35版本数据库升级 */
    @SuppressWarnings("unused")
    private void migration_35(SQLiteDatabase db) {
        DBMigrationHelper.IDBMigration migration = new DBMigrationHelper.DBMigration_35();
        migration.onMigrate(db);
    }

    /** V40版本数据库升级 */
    @SuppressWarnings("unused")
    private void migration_40(SQLiteDatabase db) {
        DBMigrationHelper.IDBMigration migration = new DBMigrationHelper.DBMigration_40();
        migration.onMigrate(db);
    }
}
