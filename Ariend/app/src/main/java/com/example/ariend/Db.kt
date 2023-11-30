package com.example.ariend

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MsgModel (
    val id: Int,
    val message: String,
)

class DBHandler
    (context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "Ariend"

        private const val DB_VERSION = 1

        private const val TABLE_NAME = "Messages"

        private const val ID_COL = "Id"

        private const val MESSAGE_COL = "Message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " TEXT, "
                + MESSAGE_COL + " TEXT)")

        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addMessage(id: Int, message: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(ID_COL, id)
        values.put(MESSAGE_COL, message)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllMessages(): ArrayList<MsgModel> {
        val db = this.readableDatabase
        val list: ArrayList<MsgModel> = ArrayList()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val message = cursor.getString(1)
                val msg = MsgModel(id, message)
                list.add(msg)

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return list
    }

    fun deleteDb() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}