package dev.glucoselog.phone
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EntryStore(context:Context,databaseName:String="glucose.db"):SQLiteOpenHelper(context,databaseName,null,1),SyncStore {
 override fun onCreate(db:SQLiteDatabase) { db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY,value REAL NOT NULL,unit TEXT NOT NULL,time INTEGER NOT NULL,offset INTEGER NOT NULL,context INTEGER NOT NULL,note TEXT NOT NULL,revision INTEGER NOT NULL,deleted INTEGER NOT NULL DEFAULT 0,synced INTEGER NOT NULL DEFAULT 0)") }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) { error("Unsupported database upgrade") }
 private fun read(c:Cursor)=Entry(c.getString(0),c.getDouble(1),GlucoseUnit.valueOf(c.getString(2)),c.getLong(3),c.getInt(4),c.getInt(5),c.getString(6),c.getLong(7),c.getInt(8)!=0,c.getInt(9)!=0)
 private fun rows(where:String)=readableDatabase.rawQuery("SELECT * FROM entries WHERE $where ORDER BY time DESC",null).use { c -> buildList { while(c.moveToNext()) add(read(c)) } }
 fun all()=rows("deleted=0")
 override fun pending()=rows("synced=0")
 fun save(e:Entry) {
  e.validate(System.currentTimeMillis()/1000)
  val db=writableDatabase; db.beginTransaction()
  try {
   val previous=db.rawQuery("SELECT revision,deleted FROM entries WHERE id=?",arrayOf(e.id)).use { c -> if(c.moveToFirst()) { require(c.getInt(1)==0) { "This reading was deleted." }; c.getLong(0) } else 0L }
   e.requireRevision(previous)
   val v=ContentValues().apply {
    put("id",e.id); put("value",e.value); put("unit",e.unit.name); put("time",e.time); put("offset",e.offset); put("context",e.context); put("note",e.note); put("revision",previous+1); put("deleted",0); put("synced",0)
   }
   db.insertWithOnConflict("entries",null,v,SQLiteDatabase.CONFLICT_REPLACE).also { check(it!=-1L) { "Could not save reading." } }
   db.setTransactionSuccessful()
  } finally { db.endTransaction() }
 }
 fun delete(id:String) { writableDatabase.execSQL("UPDATE entries SET deleted=1,synced=0,revision=revision+1 WHERE id=? AND deleted=0",arrayOf(id)) }
 override fun acknowledge(id:String,revision:Long) {
  val db=writableDatabase; db.beginTransaction()
  try {
   db.execSQL("DELETE FROM entries WHERE id=? AND revision=? AND deleted=1",arrayOf<Any>(id,revision))
   db.execSQL("UPDATE entries SET synced=1 WHERE id=? AND revision=? AND deleted=0",arrayOf<Any>(id,revision))
   db.setTransactionSuccessful()
  } finally { db.endTransaction() }
 }
 companion object {
  @Volatile private var instance:EntryStore?=null
  fun get(context:Context):EntryStore=instance ?: synchronized(this) { instance ?: EntryStore(context.applicationContext).also { instance=it } }
 }
}
