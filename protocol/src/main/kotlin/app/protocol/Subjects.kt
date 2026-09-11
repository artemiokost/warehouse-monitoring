package app.protocol

object Subjects {
    const val EVERY_MEASUREMENT = "warehouse.*.measurement"

    /** One subject token: no dots, wildcards or whitespace. The same alphabet is a valid Pekko actor name. */
    private val WAREHOUSE_ID = Regex("[A-Za-z0-9_-]+")

    fun measurement(warehouseId: String): String = "warehouse.$warehouseId.measurement"

    fun checkWarehouseId(id: String): String {
        require(WAREHOUSE_ID.matches(id)) { "warehouse id must match ${WAREHOUSE_ID.pattern}, got '$id'" }
        return id
    }
}
