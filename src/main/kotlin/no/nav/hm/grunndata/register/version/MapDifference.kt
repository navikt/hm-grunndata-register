package no.nav.hm.grunndata.register.version

data class MapDifference<K, V>(
    val entriesInCommon: Map<K, V>,
    val entriesDiffering: Map<K, Pair<V?, V?>>,
    val entriesOnlyOnLeft: Map<K, V>,
    val entriesOnlyOnRight: Map<K, V>
)

fun <K, V> Map<K, V>.difference(other: Map<K, V>): MapDifference<K, V> {
    val entriesInCommon = mutableMapOf<K, V>()
    val entriesDiffering = mutableMapOf<K, Pair<V?, V?>>()
    val entriesOnlyOnLeft = mutableMapOf<K, V>()
    val entriesOnlyOnRight = mutableMapOf<K, V>()

    for (key in this.keys) {
        if (other.containsKey(key)) {
            val valueThis = this[key]
            val valueOther = other[key]
            if (valueThis == valueOther) {
                entriesInCommon[key] = valueThis!!
            } else {
                entriesDiffering[key] = Pair(valueThis, valueOther)
            }
        } else {
            entriesOnlyOnLeft[key] = this[key]!!
        }
    }

    for (key in other.keys) {
        if (!this.containsKey(key)) {
            entriesOnlyOnRight[key] = other[key]!!
        }
    }

    return MapDifference(
        entriesInCommon = entriesInCommon,
        entriesDiffering = entriesDiffering,
        entriesOnlyOnLeft = entriesOnlyOnLeft,
        entriesOnlyOnRight = entriesOnlyOnRight
    )
}
