package app.protocol

object Subjects {
    const val EVERY_MEASUREMENT = "warehouse.*.measurement"

    fun measurement(warehouseId: String): String = "warehouse.$warehouseId.measurement"
}
