package no.nav.hm.grunndata.register.version

val excludedKeys: Set<String> =
    setOf(
        "updated",
        "version",
        "draftStatus",
        "adminStatus",
        "count",
        "countDrafts",
        "countPending",
        "countPublished",
        "countDeclined",
        "published",
        "titleLowercase",
        "updatedBy",
        "updatedByUser",
        "mainProduct"
    )

data class MapDifference<K, V>(
    val entriesDiffering: Map<K, Pair<V?, V?>> = emptyMap(),
    val entriesOnlyOnLeft: Map<K, V> = emptyMap(),
    val entriesOnlyOnRight: Map<K, V> = emptyMap(),
)

fun <K, V> Map<K, V>.mapDifference(
    other: Map<K, V>,
    parentKey: K? = null,
): MapDifference<K, V> {
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
            if (valueThis is Map<*, *> && valueOther is Map<*, *>) {
                val nestedDiff = (valueThis as Map<K, V>).mapDifference(valueOther as Map<K, V>, key)
                entriesDiffering.putAll(nestedDiff.entriesDiffering)
                entriesOnlyOnLeft.putAll(nestedDiff.entriesOnlyOnLeft)
                entriesOnlyOnRight.putAll(nestedDiff.entriesOnlyOnRight)
            } else if (key == "techData" || key == "productData.techData") {
                val thisList = valueThis as List<Map<String, String>>
                val otherList = valueOther as List<Map<String, String>>
                val caseInsensitiveDiff = thisList.zip(otherList).any { (thisMap, otherMap) ->
                    thisMap.any { (k, v) ->
                        otherMap[k]?.equals(v, ignoreCase = true) == false
                    }
                }
                if (caseInsensitiveDiff) {
                    entriesDiffering[if (parentKey != null) "$parentKey.$key" as K else key] =
                        Pair(valueThis as V, valueOther as V)
                }
            } else if (valueThis != valueOther) {
                entriesDiffering[if (parentKey != null) "$parentKey.$key" as K else key] =
                    Pair(valueThis as V, valueOther as V)
            }
        } else {
            entriesOnlyOnLeft[if (parentKey != null) "$parentKey.$key" as K else key] = this[key]!!
        }
    }

    for (key in other.keys) {
        if (!this.containsKey(key)) {
            entriesOnlyOnRight[if (parentKey != null) "$parentKey.$key" as K else key] = other[key]!!
        }
    }

    return MapDifference(
        entriesDiffering = entriesDiffering,
        entriesOnlyOnLeft = entriesOnlyOnLeft,
        entriesOnlyOnRight = entriesOnlyOnRight,
    )
}

data class Difference<K, V>(val status: DiffStatus, val diff: MapDifference<K, V>)

enum class DiffStatus {
    NO_DIFF,
    DIFF,
    NEW,
}

fun <K, V> Map<K, V>.difference(other: Map<K, V>): Difference<K, V> {
    val difference = this.mapDifference(other)
    return Difference(
        status =
            when {
                difference.entriesDiffering.isNotEmpty() ||
                        difference.entriesOnlyOnLeft.isNotEmpty() ||
                        difference.entriesOnlyOnRight.isNotEmpty() -> DiffStatus.DIFF

                else -> DiffStatus.NO_DIFF
            },
        diff = difference,
    )
}
