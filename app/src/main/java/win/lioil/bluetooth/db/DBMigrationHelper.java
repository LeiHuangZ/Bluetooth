package win.lioil.bluetooth.db;

/**
 * @author LeiHuang 1252065297@qq.com
 * <code>
 * Create At 21:22 2023/4/20
 * Update By <whom>
 * Update At 21:22 2023/4/20
 * describe <Function description>
 * </code>
 */

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.StandardDatabase;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 实现GreenDao数据库升级保留原始交易数据
 */
public class DBMigrationHelper {
    private static final String TAG = "DBMigrationHelper";

    //DB中约束为NOT NULL的数据类型
    List<Class> notNullClasses = Arrays.<Class>asList(boolean.class, byte.class, char.class,
            short.class, int.class, long.class, float.class, double.class,
            Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class);

    private Database db;
    private Collection<AbstractDao<?, ?>> allDaos;

    public DBMigrationHelper(SQLiteDatabase database) {
        db = new StandardDatabase(database);
        DaoMaster master = new DaoMaster(database);
        DaoSession session = master.newSession(IdentityScopeType.None);
        allDaos = session.getAllDaos();
    }

    /** 更改表结构，将旧表数据导入新表 */
    public void upgradeTablesScheme() {
        for (AbstractDao<?, ?> dao : allDaos) {
            @SuppressWarnings("unchecked")
            Class<? extends AbstractDao<?, ?>> daoCls = (Class<? extends AbstractDao<?, ?>>) dao.getClass();
            handleUpgradeTableScheme(db, daoCls);
        }
    }

    /** 删除临时表 */
    public void dropTempTables() {
        for (AbstractDao<?, ?> dao : allDaos) {
            @SuppressWarnings("unchecked")
            Class<? extends AbstractDao<?, ?>> daoCls = (Class<? extends AbstractDao<?, ?>>) dao.getClass();
            dropTempTable(db, daoCls);
        }
    }

    /** 执行表结构变更 */
    private void handleUpgradeTableScheme(Database db, Class<? extends AbstractDao<?, ?>> daoCls) {
        DaoConfig daoConfig = new DaoConfig(db, daoCls);
        String tableName = daoConfig.tablename;
        String tempTableName = tableName + "_TEMP";
        //重命名表
        db.execSQL("ALTER TABLE " + tableName + " RENAME TO " + tempTableName);
        //创建新表
        createTable(daoCls, db, true);

        List<String> oldColumns = getColumns(db, tempTableName);
        List<String> shareColumns = new ArrayList<>();
        List<String> newNotNullColumns = new ArrayList<>();
        for (Property property : daoConfig.properties) {
            if (oldColumns.contains(property.columnName)) {
                shareColumns.add(property.columnName);
            } else if (checkNotNull(property)) {
                newNotNullColumns.add(property.columnName);
            }
        }

        //将旧表数据导入新表
        String insertSQL = "INSERT INTO " + tableName + "("
                + TextUtils.join(",", shareColumns);
        if (!newNotNullColumns.isEmpty()) {
            insertSQL += ",";
            insertSQL += TextUtils.join(",", newNotNullColumns);
        }
        insertSQL += ") SELECT ";
        insertSQL += TextUtils.join(",", shareColumns);

        String notNullStr = "";
        for (String column : newNotNullColumns) {
            notNullStr += ",0 AS " + column;
        }
        insertSQL += notNullStr;
        insertSQL += " FROM " + tempTableName;
        Log.e("DBOpenHelper", "DB onUpgrade insertSQL:" + insertSQL);
        db.execSQL(insertSQL);
    }

    /** 是否是非空列 */
    private boolean checkNotNull(Property property) {
        return notNullClasses.contains(property.type);
    }

    /** 删除旧表 */
    private void dropTempTable(Database db, Class<? extends AbstractDao<?, ?>> daoCls) {
        DaoConfig daoConfig = new DaoConfig(db, daoCls);
        String tableName = daoConfig.tablename;
        String tempTableName = tableName + "_TEMP";
        //删除旧表
        db.execSQL("DROP TABLE " + tempTableName);
    }

    /** 获取数据库所有列名称 */
    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 1", null);
            if (cursor != null) {
                Collections.addAll(columns, cursor.getColumnNames());
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columns;
    }

    /** 创建表 */
    private static void createTable(Class<? extends AbstractDao<?, ?>> daoCls, Database db, boolean b) {
        reflectMethod(daoCls, "createTable", db, b);
    }

    /**
     * 反射createTable或dropTable方法
     */
    private static void reflectMethod(@NonNull Class<? extends AbstractDao<?, ?>> daoCls,
                                      String methodName, Database db, boolean isExists) {
        try {
            Method method = daoCls.getDeclaredMethod(methodName, Database.class, boolean.class);
            method.invoke(null, db, isExists);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    /** 数据库升级接口，自定义升级时可实现本接口 */
    public interface IDBMigration {

        void onMigrate(SQLiteDatabase db);
    }

    /**
     * <pre>
     * V35版本数据库升级类,升级内容如下：
     * </pre>
     */
    public static class DBMigration_35 implements IDBMigration {
        @Override
        public void onMigrate(SQLiteDatabase db) {
            Database database = new StandardDatabase(db);
            //TODO   具体实现
        }
    }

    /**
     * <pre>
     * V40版本数据库升级类,升级内容如下：
     * </pre>
     */
    public static class DBMigration_40 implements IDBMigration {
        @Override
        public void onMigrate(SQLiteDatabase db) {
            //TODO  具体实现
        }
    }
}

