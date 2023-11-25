package win.lioil.bluetooth.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DBManager {

    private Context context;
    private final static String dbName = "leiguan2.db";
    private static DBManager mInstance;//单例
    private DaoMaster.DevOpenHelper openHelper;
    private DaoSession mDaoSession;
    private SQLiteDatabase db;
    public DBManager(Context context){
        this.context = context;
        openHelper = new DBOpenHelper(context,dbName);

    }

    /**
     * 双重检索获取DBManager对象的单例
     * @param context
     * @return
     */
    public static DBManager getInstance(Context context){
        if (mInstance == null){
            synchronized (DBManager.class){
                if (mInstance == null){
                    mInstance = new DBManager(context);
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取可读的数据库
     * @return
     */
    public SQLiteDatabase getReadableDatabase(){
        if (openHelper == null){
            openHelper = new DBOpenHelper(context,dbName);
        }
        SQLiteDatabase db = openHelper.getReadableDatabase();
        return db;
    }

    /**
     * 获取可写的数据库
     * @return
     */
    public SQLiteDatabase getWritableDatabase(){
        if (openHelper == null){
            openHelper = new DBOpenHelper(context,dbName);
        }
        SQLiteDatabase db = openHelper.getWritableDatabase();
        return db;
    }

    /**
     * 获取可写的会话层
     * @return
     */
    public DaoSession getWriteDaoSession(){
        DaoMaster daoMaster = new DaoMaster(getWritableDatabase());
        mDaoSession = daoMaster.newSession();
        return mDaoSession;
    }
    /**
     * 获取可读的会话层
     * @return
     */
    public DaoSession getReadDaoSession(){
        DaoMaster daoMaster = new DaoMaster(getReadableDatabase());
        mDaoSession = daoMaster.newSession();
        return mDaoSession;
    }


}
