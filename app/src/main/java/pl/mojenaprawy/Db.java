package pl.mojenaprawy;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;

public class Db extends SQLiteOpenHelper {
    public Db(Context c) { super(c, "naprawy.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE repairs(id INTEGER PRIMARY KEY AUTOINCREMENT,client_id INTEGER NOT NULL,device TEXT NOT NULL,description TEXT DEFAULT '',day TEXT NOT NULL,start_time TEXT DEFAULT '',minutes INTEGER DEFAULT 0,amount REAL DEFAULT 0,paid INTEGER DEFAULT 0,FOREIGN KEY(client_id) REFERENCES clients(id))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {}
    public long addClient(String name,String phone) { ContentValues v=new ContentValues(); v.put("name",name); v.put("phone",phone); return getWritableDatabase().insert("clients",null,v); }
    public Cursor clients(){ return getReadableDatabase().rawQuery("SELECT id,name,phone FROM clients ORDER BY name COLLATE NOCASE",null); }
    public Cursor repairs(String day){ return getReadableDatabase().rawQuery("SELECT r.id,c.name,r.device,r.description,r.day,r.start_time,r.minutes,r.amount,r.paid FROM repairs r JOIN clients c ON c.id=r.client_id WHERE r.day=? ORDER BY r.start_time",new String[]{day}); }
    public Cursor repair(long id){return getReadableDatabase().rawQuery("SELECT client_id,device,description,day,start_time,minutes,amount,paid FROM repairs WHERE id=?",new String[]{String.valueOf(id)});}
    public Cursor days(String month){return getReadableDatabase().rawQuery("SELECT DISTINCT day FROM repairs WHERE day LIKE ?",new String[]{month+"%"});}
    public Cursor totals(String month){return getReadableDatabase().rawQuery("SELECT COUNT(*),COALESCE(SUM(amount),0),COALESCE(SUM(CASE WHEN paid=1 THEN amount ELSE 0 END),0) FROM repairs WHERE day LIKE ?",new String[]{month+"%"});}
    public void saveRepair(long id,long client,String device,String description,String day,String time,int minutes,double amount,boolean paid){ContentValues v=new ContentValues();v.put("client_id",client);v.put("device",device);v.put("description",description);v.put("day",day);v.put("start_time",time);v.put("minutes",minutes);v.put("amount",amount);v.put("paid",paid?1:0);if(id<0)getWritableDatabase().insert("repairs",null,v);else getWritableDatabase().update("repairs",v,"id=?",new String[]{String.valueOf(id)});}
    public void deleteRepair(long id){getWritableDatabase().delete("repairs","id=?",new String[]{String.valueOf(id)});}
    public void setPaid(long id,boolean paid){ContentValues v=new ContentValues();v.put("paid",paid?1:0);getWritableDatabase().update("repairs",v,"id=?",new String[]{String.valueOf(id)});}
}
