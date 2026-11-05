package pl.mojenaprawy;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

public final class Backup {
    private static final String[] CLIENT_COLUMNS={"id","name","phone"};
    private static final String[] REPAIR_COLUMNS={"id","client_id","device","description","day","start_time","minutes","amount","paid"};
    private static final String[] PART_COLUMNS={"id","repair_id","name","amount","paid"};
    private Backup(){}
    private static JSONArray rows(SQLiteDatabase db,String table,String[] columns)throws Exception{
        JSONArray arr=new JSONArray();
        try(Cursor c=db.query(table,columns,null,null,null,null,"id")){
            while(c.moveToNext()){
                JSONObject obj=new JSONObject();
                for(int i=0;i<columns.length;i++){
                    int type=c.getType(i);
                    if(type==Cursor.FIELD_TYPE_NULL)obj.put(columns[i],JSONObject.NULL);
                    else if(type==Cursor.FIELD_TYPE_INTEGER)obj.put(columns[i],c.getLong(i));
                    else if(type==Cursor.FIELD_TYPE_FLOAT)obj.put(columns[i],c.getDouble(i));
                    else obj.put(columns[i],c.getString(i));
                }
                arr.put(obj);
            }
        }
        return arr;
    }
    public static String exportData(Db helper)throws Exception{
        SQLiteDatabase db=helper.getReadableDatabase();
        JSONObject root=new JSONObject();
        root.put("format","pl.mojenaprawy.backup");root.put("version",1);
        root.put("clients",rows(db,"clients",CLIENT_COLUMNS));
        root.put("repairs",rows(db,"repairs",REPAIR_COLUMNS));
        root.put("parts",rows(db,"parts",PART_COLUMNS));
        return root.toString(2);
    }
    private static void requireFields(JSONObject item,String[] columns)throws Exception{
        for(String column:columns)if(!item.has(column)||item.isNull(column))throw new Exception("Brak pola: "+column);
    }
    private static void validateIds(JSONArray arr,String[] columns,HashSet<Long> ids)throws Exception{
        for(int i=0;i<arr.length();i++){
            JSONObject item=arr.getJSONObject(i);requireFields(item,columns);
            long id=item.getLong("id");if(id<=0||!ids.add(id))throw new Exception("Nieprawidłowy lub powtórzony identyfikator");
        }
    }
    private static void validateMoneyAndPaid(JSONObject item)throws Exception{
        double amount=item.getDouble("amount");
        if(!Double.isFinite(amount)||amount<0)throw new Exception("Nieprawidłowa kwota");
        int paid=item.getInt("paid");if(paid!=0&&paid!=1)throw new Exception("Nieprawidłowy status płatności");
    }
    private static void insert(SQLiteDatabase db,String table,JSONArray items,String[] columns)throws Exception{
        for(int i=0;i<items.length();i++){
            JSONObject obj=items.getJSONObject(i);
            android.content.ContentValues cv=new android.content.ContentValues();
            for(String col:columns){Object value=obj.get(col);if(value instanceof Integer||value instanceof Long)cv.put(col,((Number)value).longValue());else if(value instanceof Number)cv.put(col,((Number)value).doubleValue());else cv.put(col,String.valueOf(value));}
            if(db.insertOrThrow(table,null,cv)<0)throw new Exception("Błąd zapisu: "+table);
        }
    }
    public static int[] importData(Db helper,String json)throws Exception{
        JSONObject root=new JSONObject(json);
        if(!"pl.mojenaprawy.backup".equals(root.optString("format"))||root.getInt("version")!=1)throw new Exception("Nieobsługiwany format kopii zapasowej");
        JSONArray clients=root.getJSONArray("clients"),repairs=root.getJSONArray("repairs"),parts=root.getJSONArray("parts");
        HashSet<Long> clientIds=new HashSet<>(),repairIds=new HashSet<>(),partIds=new HashSet<>();
        validateIds(clients,CLIENT_COLUMNS,clientIds);validateIds(repairs,REPAIR_COLUMNS,repairIds);validateIds(parts,PART_COLUMNS,partIds);
        for(int i=0;i<clients.length();i++){JSONObject c=clients.getJSONObject(i);if(c.getString("name").trim().isEmpty())throw new Exception("Klient bez nazwy");}
        for(int i=0;i<repairs.length();i++){
            JSONObject r=repairs.getJSONObject(i);
            if(!clientIds.contains(r.getLong("client_id")))throw new Exception("Naprawa bez klienta");
            if(r.getString("device").trim().isEmpty())throw new Exception("Naprawa bez urządzenia");
            java.time.LocalDate.parse(r.getString("day"));
            if(r.getInt("minutes")<0)throw new Exception("Nieprawidłowy czas naprawy");
            validateMoneyAndPaid(r);
        }
        for(int i=0;i<parts.length();i++){
            JSONObject p=parts.getJSONObject(i);
            if(!repairIds.contains(p.getLong("repair_id")))throw new Exception("Część bez naprawy");
            if(p.getString("name").trim().isEmpty())throw new Exception("Część bez nazwy");
            validateMoneyAndPaid(p);
        }
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try{
            db.delete("parts",null,null);db.delete("repairs",null,null);db.delete("clients",null,null);
            insert(db,"clients",clients,CLIENT_COLUMNS);insert(db,"repairs",repairs,REPAIR_COLUMNS);insert(db,"parts",parts,PART_COLUMNS);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new int[]{clients.length(),repairs.length(),parts.length()};
    }
}
