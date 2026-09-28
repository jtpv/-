package com.jtpv.powerconsumption.data

import android.content.ContentValues
import android.database.Cursor

/** 数据访问层：封装车辆与记录的增删改查，上层界面不直接接触 Cursor */
class Repository(private val helper: DbHelper) {

    // ==================== 车辆 ====================

    fun listVehicles(): List<Vehicle> {
        val out = ArrayList<Vehicle>()
        helper.readableDatabase
            .query(DbHelper.T_VEHICLE, null, null, null, null, null, DbHelper.COL_ID + " ASC")
            .use { c ->
                while (c.moveToNext()) out.add(readVehicle(c))
            }
        return out
    }

    fun insertVehicle(v: Vehicle): Long =
        helper.writableDatabase.insert(DbHelper.T_VEHICLE, null, vehicleValues(v))

    fun updateVehicle(v: Vehicle) {
        helper.writableDatabase.update(
            DbHelper.T_VEHICLE,
            vehicleValues(v),
            DbHelper.COL_ID + "=?",
            arrayOf(v.id.toString())
        )
    }

    private fun vehicleValues(v: Vehicle): ContentValues = ContentValues().apply {
        put(DbHelper.COL_NAME, v.name)
        put(DbHelper.COL_BATTERY, v.batteryKwh)
        put(DbHelper.COL_TANK, v.tankLiter)
        put(DbHelper.COL_PRESET, v.presetIndex)
    }

    private fun readVehicle(c: Cursor) = Vehicle(
        id = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ID)),
        name = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_NAME)),
        batteryKwh = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_BATTERY)),
        tankLiter = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_TANK)),
        presetIndex = c.getInt(c.getColumnIndexOrThrow(DbHelper.COL_PRESET))
    )

    // ==================== 记录 ====================

    fun listRecords(vehicleId: Long): List<Record> {
        val out = ArrayList<Record>()
        helper.readableDatabase.query(
            DbHelper.T_RECORD,
            null,
            "vehicle_id=?",
            arrayOf(vehicleId.toString()),
            null,
            null,
            "date DESC, _id DESC"
        ).use { c ->
            while (c.moveToNext()) out.add(readRecord(c))
        }
        return out
    }

    fun getRecord(id: Long): Record? {
        helper.readableDatabase.query(
            DbHelper.T_RECORD, null, "_id=?", arrayOf(id.toString()), null, null, null
        ).use { c ->
            if (c.moveToFirst()) return readRecord(c)
        }
        return null
    }

    fun insertRecord(r: Record): Long =
        helper.writableDatabase.insert(DbHelper.T_RECORD, null, recordValues(r))

    fun updateRecord(r: Record) {
        helper.writableDatabase.update(
            DbHelper.T_RECORD, recordValues(r), "_id=?", arrayOf(r.id.toString())
        )
    }

    fun deleteRecord(id: Long) {
        helper.writableDatabase.delete(DbHelper.T_RECORD, "_id=?", arrayOf(id.toString()))
    }

    private fun recordValues(r: Record): ContentValues = ContentValues().apply {
        put("vehicle_id", r.vehicleId)
        put("type", r.type)
        put("date", r.date)
        put("amount", r.amount)
        put("price", r.price)
        put("cost", r.cost)
        put("odo_total", r.odoTotal)
        put("odo_ev", r.odoEv)
        put("odo_hev", r.odoHev)
        put("is_full", if (r.isFull) 1 else 0)
        put("note", r.note)
    }

    private fun readRecord(c: Cursor) = Record(
        id = c.getLong(c.getColumnIndexOrThrow("_id")),
        vehicleId = c.getLong(c.getColumnIndexOrThrow("vehicle_id")),
        type = c.getString(c.getColumnIndexOrThrow("type")),
        date = c.getString(c.getColumnIndexOrThrow("date")),
        amount = c.getDouble(c.getColumnIndexOrThrow("amount")),
        price = c.getDouble(c.getColumnIndexOrThrow("price")),
        cost = c.getDouble(c.getColumnIndexOrThrow("cost")),
        odoTotal = c.getLong(c.getColumnIndexOrThrow("odo_total")),
        odoEv = c.getLong(c.getColumnIndexOrThrow("odo_ev")),
        odoHev = c.getLong(c.getColumnIndexOrThrow("odo_hev")),
        isFull = c.getInt(c.getColumnIndexOrThrow("is_full")) == 1,
        note = c.getString(c.getColumnIndexOrThrow("note"))
    )

    // ==================== 统计 ====================

    fun stats(vehicleId: Long): Stats = Calc.stats(listRecords(vehicleId))
}
