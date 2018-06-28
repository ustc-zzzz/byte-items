package de.randombyte.byteitems

import de.randombyte.kosp.extensions.orNull
import de.randombyte.kosp.getServiceOrFail
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.persistence.DataFormats
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.service.sql.SqlService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sql.DataSource

data class Config(val url: String = "", val tableName: String = ByteItems.ID.replace('-', '_')) {
    fun save(key: String, item: ItemStackSnapshot?): Boolean = dataSource?.connection?.use {
        if (item == null) it.prepareStatement("DELETE FROM $tableName WHERE id=?").use {
            it.setString(1, key)
            it.executeUpdate() > 0
        } else {
            val type = item.type.id
            val data = item.toContainer()
            val bytes = ByteArrayOutputStream().use { DataFormats.NBT.writeTo(it, data); it.toByteArray() }
            it.prepareStatement("INSERT INTO $tableName VALUES(?, ?, ?)").use {
                it.setString(1, key)
                it.setString(2, type)
                it.setBytes(3, bytes)
                it.executeUpdate() > 0
            }
        }
    } ?: false

    fun load(key: String): ItemStackSnapshot? = dataSource?.connection?.use {
        val bytes = it.prepareStatement("SELECT item FROM $tableName WHERE id=?").use {
            it.setString(1, key)
            it.executeQuery().let { if (it.next()) it.getBytes("item") else null }
        }
        val data = bytes?.let { ByteArrayInputStream(it).use { DataFormats.NBT.readFrom(it) } }
        data?.let { ItemStack.builder().build(it).orNull()?.createSnapshot() }
    }

    fun list(): Map<String, ItemType> = HashMap<String, ItemType>().apply {
        dataSource?.connection?.use {
            val result = it.prepareStatement("SELECT id, type FROM $tableName").executeQuery()
            while (result.next()) {
                val key = result.getString("id")
                val type = result.getString("type")
                type?.let { Sponge.getRegistry().getType(ItemType::class.java, type).orNull() }?.let { put(key, it) }
            }
        }
    }

    private val dataSource: DataSource? = if (url.isEmpty()) null else createDataSource().apply(this::createTable)

    private fun createDataSource(): DataSource {
        val sql = getServiceOrFail(SqlService::class)
        val plugin = Sponge.getPluginManager().getPlugin(ByteItems.ID).get().instance.get()
        return sql.getConnectionUrlFromAlias(url).orNull()?.let(sql::getDataSource) ?: sql.getDataSource(plugin, url)
    }

    private fun createTable(dataSource: DataSource) {
        val pars = "(id VARCHAR(64) NOT NULL, type VARCHAR(64) NOT NULL, item BLOB NOT NULL)"
        dataSource.connection.use { it.prepareStatement("CREATE TABLE IF NOT EXISTS $tableName $pars").execute() }
    }
}