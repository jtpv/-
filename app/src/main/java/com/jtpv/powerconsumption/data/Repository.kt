package com.jtpv.powerconsumption.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

/** 数据访问层：封装车辆、补能记录、费用与偏好的增删改查，上层界面不直接接触 Cursor */
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

    fun getVehicle(id: Long): Vehicle? {
        helper.readableDatabase.query(
            DbHelper.T_VEHICLE, null, DbHelper.COL_ID + "=?", arrayOf(id.toString()),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) return readVehicle(c)
        }
        return null
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
        put(DbHelper.COL_PLATE, v.plateNo)
        put(DbHelper.COL_MODEL, v.modelName)
        put(DbHelper.COL_PURCHASE_DATE, v.purchaseDate)
        put(DbHelper.COL_ODO_CURRENT, v.odoCurrent)
        put(DbHelper.COL_INSURANCE_EXPIRY, v.insuranceExpiry)
        put(DbHelper.COL_INSPECTION_EXPIRY, v.inspectionExpiry)
        put(DbHelper.COL_MAINT_KM, v.maintIntervalKm)
        put(DbHelper.COL_MAINT_MONTH, v.maintIntervalMonth)
    }

    private fun readVehicle(c: Cursor) = Vehicle(
        id = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ID)),
        name = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_NAME)),
        batteryKwh = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_BATTERY)),
        tankLiter = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_TANK)),
        presetIndex = c.getInt(c.getColumnIndexOrThrow(DbHelper.COL_PRESET)),
        plateNo = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_PLATE)),
        modelName = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_MODEL)),
        purchaseDate = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_PURCHASE_DATE)),
        odoCurrent = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ODO_CURRENT)),
        insuranceExpiry = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_INSURANCE_EXPIRY)),
        inspectionExpiry = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_INSPECTION_EXPIRY)),
        maintIntervalKm = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_MAINT_KM)),
        maintIntervalMonth = c.getInt(c.getColumnIndexOrThrow(DbHelper.COL_MAINT_MONTH))
    )

    // ==================== 补能记录 ====================

    fun listRecords(vehicleId: Long): List<Record> {
        val out = ArrayList<Record>()
        helper.readableDatabase.query(
            DbHelper.T_RECORD,
            null,
            DbHelper.COL_VEHICLE_ID + "=?",
            arrayOf(vehicleId.toString()),
            null,
            null,
            DbHelper.COL_DATE + " DESC, " + DbHelper.COL_ID + " DESC"
        ).use { c ->
            while (c.moveToNext()) out.add(readRecord(c))
        }
        return out
    }

    fun getRecord(id: Long): Record? {
        helper.readableDatabase.query(
            DbHelper.T_RECORD, null, DbHelper.COL_ID + "=?",
            arrayOf(id.toString()), null, null, null
        ).use { c ->
            if (c.moveToFirst()) return readRecord(c)
        }
        return null
    }

    fun insertRecord(r: Record): Long =
        helper.writableDatabase.insert(DbHelper.T_RECORD, null, recordValues(r))

    fun updateRecord(r: Record) {
        helper.writableDatabase.update(
            DbHelper.T_RECORD, recordValues(r),
            DbHelper.COL_ID + "=?", arrayOf(r.id.toString())
        )
    }

    fun deleteRecord(id: Long) {
        helper.writableDatabase.delete(
            DbHelper.T_RECORD, DbHelper.COL_ID + "=?", arrayOf(id.toString())
        )
    }

    private fun recordValues(r: Record): ContentValues = ContentValues().apply {
        put(DbHelper.COL_VEHICLE_ID, r.vehicleId)
        put(DbHelper.COL_TYPE, r.type)
        put(DbHelper.COL_DATE, r.date)
        put(DbHelper.COL_AMOUNT, r.amount)
        put(DbHelper.COL_PRICE, r.price)
        put(DbHelper.COL_COST, r.cost)
        put(DbHelper.COL_ODO_TOTAL, r.odoTotal)
        put(DbHelper.COL_ODO_EV, r.odoEv)
        put(DbHelper.COL_ODO_HEV, r.odoHev)
        put(DbHelper.COL_IS_FULL, if (r.isFull) 1 else 0)
        put(DbHelper.COL_NOTE, r.note)
    }

    private fun readRecord(c: Cursor) = Record(
        id = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ID)),
        vehicleId = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_VEHICLE_ID)),
        type = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_TYPE)),
        date = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_DATE)),
        amount = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_AMOUNT)),
        price = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_PRICE)),
        cost = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_COST)),
        odoTotal = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ODO_TOTAL)),
        odoEv = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ODO_EV)),
        odoHev = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ODO_HEV)),
        isFull = c.getInt(c.getColumnIndexOrThrow(DbHelper.COL_IS_FULL)) == 1,
        note = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_NOTE))
    )

    // ==================== 费用 ====================

    /**
     * 列出费用记录。
     * type 传入 null 表示不按类型过滤 —— 用 null 而非空串，是为了让「全部」与
     * 「某个恰好为空的类型」在语义上不混淆。
     */
    fun listExpenses(vehicleId: Long, type: String? = null): List<Expense> {
        val out = ArrayList<Expense>()
        val where: String
        val args: Array<String>
        if (type == null) {
            where = DbHelper.COL_VEHICLE_ID + "=?"
            args = arrayOf(vehicleId.toString())
        } else {
            where = DbHelper.COL_VEHICLE_ID + "=? AND " + DbHelper.COL_TYPE + "=?"
            args = arrayOf(vehicleId.toString(), type)
        }
        helper.readableDatabase.query(
            DbHelper.T_EXPENSE, null, where, args, null, null,
            DbHelper.COL_DATE + " DESC, " + DbHelper.COL_ID + " DESC"
        ).use { c ->
            while (c.moveToNext()) out.add(readExpense(c))
        }
        return out
    }

    fun getExpense(id: Long): Expense? {
        helper.readableDatabase.query(
            DbHelper.T_EXPENSE, null, DbHelper.COL_ID + "=?",
            arrayOf(id.toString()), null, null, null
        ).use { c ->
            if (c.moveToFirst()) return readExpense(c)
        }
        return null
    }

    fun insertExpense(e: Expense): Long =
        helper.writableDatabase.insert(DbHelper.T_EXPENSE, null, expenseValues(e))

    fun updateExpense(e: Expense) {
        helper.writableDatabase.update(
            DbHelper.T_EXPENSE, expenseValues(e),
            DbHelper.COL_ID + "=?", arrayOf(e.id.toString())
        )
    }

    fun deleteExpense(id: Long) {
        helper.writableDatabase.delete(
            DbHelper.T_EXPENSE, DbHelper.COL_ID + "=?", arrayOf(id.toString())
        )
    }

    private fun expenseValues(e: Expense): ContentValues = ContentValues().apply {
        put(DbHelper.COL_VEHICLE_ID, e.vehicleId)
        put(DbHelper.COL_TYPE, e.type)
        put(DbHelper.COL_DATE, e.date)
        put(DbHelper.COL_AMOUNT, e.amount)
        put(DbHelper.COL_ODO, e.odo)
        put(DbHelper.COL_SHOP, e.shop)
        put(DbHelper.COL_NOTE, e.note)
    }

    private fun readExpense(c: Cursor) = Expense(
        id = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ID)),
        vehicleId = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_VEHICLE_ID)),
        type = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_TYPE)),
        date = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_DATE)),
        amount = c.getDouble(c.getColumnIndexOrThrow(DbHelper.COL_AMOUNT)),
        odo = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ODO)),
        shop = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_SHOP)),
        note = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_NOTE))
    )

    // ==================== 偏好（键值） ====================

    fun getPref(key: String, def: String = ""): String {
        helper.readableDatabase.query(
            DbHelper.T_PREF, null, DbHelper.COL_PREF_KEY + "=?",
            arrayOf(key), null, null, null
        ).use { c ->
            if (c.moveToFirst()) {
                val v = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_PREF_VALUE))
                if (v.isNotEmpty()) return v
            }
        }
        return def
    }

    fun setPref(key: String, value: String) {
        val cv = ContentValues().apply {
            put(DbHelper.COL_PREF_KEY, key)
            put(DbHelper.COL_PREF_VALUE, value)
        }
        // 以主键冲突替换实现 upsert，避免先查后写产生竞态
        helper.writableDatabase.insertWithOnConflict(
            DbHelper.T_PREF, null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ==================== 保养基准 ====================

    /**
     * 上次保养里程：取保养记录中最大的里程读数，而非「最近一条的里程」。
     * 用户补录旧记录是常态，按日期取会得出倒退的基准值。
     */
    fun lastMaintenanceOdo(vehicleId: Long): Long {
        val values = listExpenses(vehicleId, ExpenseType.MAINTENANCE)
            .map { it.odo }
            .filter { it > 0L }
        return values.maxOrNull() ?: 0L
    }

    /** 上次保养日期（按记录日期取最新一条） */
    fun lastMaintenanceDate(vehicleId: Long): String =
        listExpenses(vehicleId, ExpenseType.MAINTENANCE).firstOrNull()?.date ?: ""

    // ==================== 统计 ====================

    fun stats(vehicleId: Long): Stats = Calc.stats(listRecords(vehicleId))

    fun costSummary(vehicleId: Long): CostSummary =
        Calc.costSummary(listRecords(vehicleId), listExpenses(vehicleId), LocalDate.now())
}
