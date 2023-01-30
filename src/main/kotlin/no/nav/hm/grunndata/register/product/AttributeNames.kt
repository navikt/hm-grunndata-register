package no.nav.hm.grunndata.register.product

import java.util.*

enum class AttributeNames(private val type: AttributeType) {

    manufacturer(AttributeType.STRING),
    articlename(AttributeType.STRING),
    compatible(AttributeType.LIST),
    series(AttributeType.LIST),
    keywords(AttributeType.LIST),
    shortdescription(AttributeType.HTML),
    text(AttributeType.HTML),
    url(AttributeType.URL)
}

enum class AttributeType {
    STRING, HTML, URL, LIST, JSON
}

inline fun <reified K: Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> =
    pairs.toMap(EnumMap<K, V>(K::class.java))

inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return T::class.java.enumConstants.any { it.name == name}
}