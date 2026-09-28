package com.jtpv.powerconsumption.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 本地存储。
 *
 * 刻意使用框架自带的 SQLiteOpenHelper，而非 Room：
 * Room 需要 KSP/kapt 注解处理器，其版本必须与 Kotlin 严格对应，错位即构建失败，
 * 而本机无法编译验证，容错空间极小。
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_VEHICLE)
        db.execSQL(CREATE_RECORD)
        seedDefaultVehicle(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 当前为版本 1，尚无升级逻辑
    }

    /** 首次创建时写入一辆默认车辆，避免界面出现空状态 */
    private fun seedDefaultVehicle(db: SQLiteDatabase) {
        val cv = ContentValues()
        cv.put(COL_NAME, "我的车")
        cv.put(COL_BATTERY, 18.32)
        cv.put(COL_TANK, 48.0)
        cv.put(COL_PRESET, 1)
        db.insert(T_VEHICLE, null, cv)
    }

    companion object {
        const val DB_NAME = "power_consumption.db"
        const val DB_VERSION = 1

        const val T_VEHICLE = "vehicle"
        const val T_RECORD = "record"

        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_BATTERY = "battery_kwh"
        const val COL_TANK = "tank_liter"
        const val COL_PRESET = "preset_index"

        private const val CREATE_VEHICLE = """
            CREATE TABLE vehicle (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                battery_kwh REAL NOT NULL DEFAULT 0,
                tank_liter REAL NOT NULL DEFAULT 0,
                preset_index INTEGER NOT NULL DEFAULT 6
            )
        """

        private const val CREATE_RECORD = """
            CREATE TABLE record (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                amount REAL NOT NULL DEFAULT 0,
                price REAL NOT NULL DEFAULT 0,
                cost REAL NOT NULL DEFAULT 0,
                odo_total INTEGER NOT NULL DEFAULT 0,
                odo_ev INTEGER NOT NULL DEFAULT 0,
                odo_hev INTEGER NOT NULL DEFAULT 0,
                is_full INTEGER NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT ''
            )
        """
    }
}
