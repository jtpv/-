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
 *
 * 版本演进：
 *  v1 —— vehicle + record（补能记录）
 *  v2 —— vehicle 补充档案字段；新增 expense（其他费用）与 pref（键值偏好）
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_VEHICLE)
        db.execSQL(CREATE_RECORD)
        db.execSQL(CREATE_EXPENSE)
        db.execSQL(CREATE_PREF)
        seedDefaultVehicle(db)
    }

    /**
     * 迁移而非重建 —— 重建会清空用户已录入的数据。
     * SQLite 的 ALTER TABLE ADD COLUMN 对 NOT NULL 列必须给出 DEFAULT，否则报错。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE vehicle ADD COLUMN plate_no TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE vehicle ADD COLUMN model_name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE vehicle ADD COLUMN purchase_date TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE vehicle ADD COLUMN odo_current INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vehicle ADD COLUMN insurance_expiry TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE vehicle ADD COLUMN inspection_expiry TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                "ALTER TABLE vehicle ADD COLUMN maint_interval_km INTEGER NOT NULL DEFAULT " +
                    Vehicle.DEFAULT_MAINT_KM
            )
            db.execSQL(
                "ALTER TABLE vehicle ADD COLUMN maint_interval_month INTEGER NOT NULL DEFAULT " +
                    Vehicle.DEFAULT_MAINT_MONTH
            )
            db.execSQL(CREATE_EXPENSE)
            db.execSQL(CREATE_PREF)
        }
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
        const val DB_VERSION = 2

        const val T_VEHICLE = "vehicle"
        const val T_RECORD = "record"
        const val T_EXPENSE = "expense"
        const val T_PREF = "pref"

        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_BATTERY = "battery_kwh"
        const val COL_TANK = "tank_liter"
        const val COL_PRESET = "preset_index"

        const val COL_PLATE = "plate_no"
        const val COL_MODEL = "model_name"
        const val COL_PURCHASE_DATE = "purchase_date"
        const val COL_ODO_CURRENT = "odo_current"
        const val COL_INSURANCE_EXPIRY = "insurance_expiry"
        const val COL_INSPECTION_EXPIRY = "inspection_expiry"
        const val COL_MAINT_KM = "maint_interval_km"
        const val COL_MAINT_MONTH = "maint_interval_month"

        const val COL_VEHICLE_ID = "vehicle_id"
        const val COL_TYPE = "type"
        const val COL_DATE = "date"
        const val COL_AMOUNT = "amount"
        const val COL_PRICE = "price"
        const val COL_COST = "cost"
        const val COL_ODO_TOTAL = "odo_total"
        const val COL_ODO_EV = "odo_ev"
        const val COL_ODO_HEV = "odo_hev"
        const val COL_IS_FULL = "is_full"
        const val COL_NOTE = "note"

        const val COL_ODO = "odo"
        const val COL_SHOP = "shop"

        const val COL_PREF_KEY = "k"
        const val COL_PREF_VALUE = "v"

        private const val CREATE_VEHICLE = """
            CREATE TABLE vehicle (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                battery_kwh REAL NOT NULL DEFAULT 0,
                tank_liter REAL NOT NULL DEFAULT 0,
                preset_index INTEGER NOT NULL DEFAULT 6,
                plate_no TEXT NOT NULL DEFAULT '',
                model_name TEXT NOT NULL DEFAULT '',
                purchase_date TEXT NOT NULL DEFAULT '',
                odo_current INTEGER NOT NULL DEFAULT 0,
                insurance_expiry TEXT NOT NULL DEFAULT '',
                inspection_expiry TEXT NOT NULL DEFAULT '',
                maint_interval_km INTEGER NOT NULL DEFAULT 10000,
                maint_interval_month INTEGER NOT NULL DEFAULT 12
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

        private const val CREATE_EXPENSE = """
            CREATE TABLE expense (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                amount REAL NOT NULL DEFAULT 0,
                odo INTEGER NOT NULL DEFAULT 0,
                shop TEXT NOT NULL DEFAULT '',
                note TEXT NOT NULL DEFAULT ''
            )
        """

        private const val CREATE_PREF = """
            CREATE TABLE pref (
                k TEXT PRIMARY KEY,
                v TEXT NOT NULL DEFAULT ''
            )
        """
    }
}
