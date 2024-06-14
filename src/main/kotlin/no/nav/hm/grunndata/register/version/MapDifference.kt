package no.nav.hm.grunndata.register.version

val excludedKeys: Set<String> = setOf("updated", "version")

data class MapDifference<K, V>(
    val entriesInCommon: Map<K, V> = emptyMap(),
    val entriesDiffering: Map<K, Pair<V?, V?>> = emptyMap(),
    val entriesOnlyOnLeft: Map<K, V> = emptyMap(),
    val entriesOnlyOnRight: Map<K, V> = emptyMap()
)

fun <K, V> Map<K, V>.mapDifference(other: Map<K, V>): MapDifference<K, V> {
    val entriesInCommon = mutableMapOf<K, V>()
    val entriesDiffering = mutableMapOf<K, Pair<V?, V?>>()
    val entriesOnlyOnLeft = mutableMapOf<K, V>()
    val entriesOnlyOnRight = mutableMapOf<K, V>()

    for (key in this.keys) {
        if (excludedKeys.contains(key.toString())) {
            continue
        }
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


data class Difference<K,V>(val status: DiffStatus, val diff: MapDifference<K,V>)

enum class DiffStatus {
    NO_DIFF,
    DIFF,
    NEW
}

fun <K, V> Map<K,V>.difference(
    other: Map<K, V>
): Difference<K,V> {
    val difference = this.mapDifference(other)
    return Difference(
        status = when {
            difference.entriesDiffering.isNotEmpty()
                    || difference.entriesOnlyOnLeft.isNotEmpty()
                    || difference.entriesOnlyOnRight.isNotEmpty() -> DiffStatus.DIFF

            else -> DiffStatus.NO_DIFF
        },
        diff = difference
    )
}
