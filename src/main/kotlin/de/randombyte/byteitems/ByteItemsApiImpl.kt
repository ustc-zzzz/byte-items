package de.randombyte.byteitems

class ByteItemsApiImpl(val getConfig: () -> Config) : ByteItemsApi {
    override fun getItem(id: String) = getConfig().load(id.removePrefix(getByteItemsPrefix() + ":"))
}