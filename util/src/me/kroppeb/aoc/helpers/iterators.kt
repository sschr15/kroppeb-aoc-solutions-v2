package me.kroppeb.aoc.helpers

import me.kroppeb.aoc.helpers.collections.LazySet
import me.kroppeb.aoc.helpers.collections.extensions.repeat
import me.kroppeb.aoc.helpers.sint.*
import java.math.BigInteger


fun <T> Iterator<T>.getNext(): T {
	hasNext()
	return next()
}

fun <T> Iterator<T>.getNextOrNull(): T? {
	if (hasNext()) return next()
	return null
}

fun String.e(): List<Char> = map { it }

@JvmName("e2")
fun Iterable<String>.e(): List<List<Char>> = map { it.e() }

@JvmName("e3")
fun Iterable<Iterable<String>>.e(): List<List<List<Char>>> = map { it.e() }

@JvmName("e4")
fun Iterable<Iterable<Iterable<String>>>.e(): List<List<List<List<Char>>>> = map { it.e() }

inline fun <T, R> Iterable<Iterable<T>>.map2(convert: (T) -> R): List<List<R>> = map { it.map(convert) }

@JvmName("rleDecodeInt")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <T, R> Iterable<T>.rleDecode(value: (T) -> R, length: (T) -> Int) =
	flatMap { listOf(value(it)).repeat(length(it)) }


@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <T, R> Iterable<T>.rleDecodes(value: (T) -> R, length: (T) -> Sint) =
	flatMap { listOf(value(it)).repeat(length(it)) }


inline fun <T, R> Iterable<T>.rleEncode(convert: (T, Int) -> R) = blockCountsI().map { (a, b) -> convert(a, b) }


operator fun <T> Set<T>.times(other: Set<T>): Set<T> = intersect(other)
operator fun <T> Set<T>.plus(other: Set<T>): Set<T> = union(other)

private class SetRef<T>(val iterable: Iterable<T>) {
	override fun equals(other: Any?): Boolean {
		if (other !is SetRef<*>) return false
		if (other.iterable !== iterable) return false
		return true
	}

	override fun hashCode(): Int {
		return System.identityHashCode(iterable)
	}
}

private val seenCollectionsInAsSet = LinkedHashMap<SetRef<*>, Set<*>>()
private var seenCollectionsInAsSetWarned = mutableSetOf<SetRef<*>>()
fun <T> Iterable<T>.asSet(): Set<T> = when (this) {
	is Set<T> -> this
	else -> {
		if (seenCollectionsInAsSetWarned.size < 5) {
			val ref = SetRef(this)
			val g = seenCollectionsInAsSet[ref]
			if (g != null && ref !in seenCollectionsInAsSetWarned) {
				if (this is Collection<T>) {
					if (this.size == g.size) {
						println("WARNING: A collection is being converted to a set multiple times, this might be slow")
						println("WARNING: Collection: ${this.toString().take(200)}")
						seenCollectionsInAsSetWarned += ref
					}
				}
			}
			if (seenCollectionsInAsSet.size >= 20) {
				// avoid memory leaking too much
				// pollFirst removes apparently
				seenCollectionsInAsSet.pollFirstEntry()
			}

			toSet().also { seenCollectionsInAsSet.putLast(ref, it) }
		} else {
			toSet()
		}
	}
}

fun <K, A> Map<K, A>.intersect(other: Iterable<K>): Map<K, A> =
	this.keys.intersect(other.asSet()).associateWith { this[it]!! }

fun <K, A, B> Map<K, A>.union(other: Map<K, B>): Map<K, Pair<A?, B?>> =
	this.keys.union(other.keys).associateWith { this[it] to other[it] }

inline fun <K, A> Map<K, A>.merge(other: Map<K, A>, m: (A, A) -> A): Map<K, A> =
	this.keys.union(other.keys).associateWith {
		val x = this[it]
		val y = other[it]
		when {
			x != null && y != null -> m(x, y)
			x != null -> x
			y != null -> y
			else -> error("?")
		}
	}

inline fun <K, A, B, R> Map<K, A>.mergeMap(other: Map<K, B>, m: (A?, B?) -> R) = this.union(other).mapValues { (_, v) ->
	m(v.first, v.second)
}

fun <K, A, B> Map<K, A>.intersect(other: Map<K, B>): Map<K, Pair<A, B>> = this.entries.mapNotNull { (key, value) ->
	if (key in other) key to (value to other[key]!!)
	else null
}.toMap()

inline fun <K, A, B, R> Map<K, A>.intersectMap(other: Map<K, B>, m: (A, B) -> R): Map<K, R> =
	this.entries.mapNotNull { (key, value) ->
		if (key in other) key to m(value, other[key]!!)
		else null
	}.toMap()

fun <K, V : Comparable<V>> Map<K, V>.minByValue(): K = minBy { it.value }.key
fun <K : Comparable<K>, V> Map<K, V>.minByKey(): V = minBy { it.key }.value

fun <K, V : Comparable<V>> Map<K, V>.maxByValue(): K = maxBy { it.value }.key
fun <K : Comparable<K>, V> Map<K, V>.maxByKey(): V = maxBy { it.key }.value

fun <K, V : Comparable<V>> Map<K, V>.allMinByValue(): List<K> = allMinBy { it.value }.map { it.key }
fun <K : Comparable<K>, V> Map<K, V>.allMinByKey(): List<V> = allMinBy { it.key }.map { it.value }
fun <K, V : Comparable<V>> Map<K, V>.allMaxByValue(): List<K> = allMaxBy { it.value }.map { it.key }
fun <K : Comparable<K>, V> Map<K, V>.allMaxByKey(): List<V> = allMaxBy { it.key }.map { it.value }


fun <T> generateTimes(times: Int, next: () -> T): List<T> {
	val ret = mutableListOf<T>()
	repeat(times) {
		ret.add(next())
	}
	return ret
}

fun <T> generateTimes(times: Sint, next: () -> T): List<T> {
	val ret = mutableListOf<T>()
	repeat(times) {
		ret.add(next())
	}
	return ret
}

/**
 * Seed isn't returned, the retured list has length times
 */
fun <T> generateTimes(times: Int, seed: T, next: (T) -> T): List<T> {
	var acc = seed
	val ret = mutableListOf<T>()
	repeat(times) {
		acc = next(acc)
		ret.add(acc)
	}
	return ret
}


/**
 * Seed isn't returned, the retured list has length times
 */
fun <T> generateTimes(times: Sint, seed: T, next: (T) -> T): List<T> {
	var acc = seed
	val ret = mutableListOf<T>()
	repeat(times) {
		acc = next(acc)
		ret.add(acc)
	}
	return ret
}

/**
 * The returned list has length times
 */
fun <S, T> generateStateTimes(times: Int, seed: S, next: (state: S) -> Pair<S, T>): List<T> {
	var acc = seed
	val ret = mutableListOf<T>()
	repeat(times) {
		val (s, t) = next(acc)
		acc = s
		ret.add(t)
	}
	return ret
}


/**
 * The retured list has length times
 */
fun <S, T> generateStateTimes(times: Sint, seed: S, next: (state: S) -> Pair<S, T>): List<T> {
	var acc = seed
	val ret = mutableListOf<T>()
	repeat(times) {
		val (s, t) = next(acc)
		acc = s
		ret.add(t)
	}
	return ret
}

inline fun <T, R> Iterable<T>.scan(start: R, transform: (R, T) -> R): List<R> {
	var acc = start
	val ret = mutableListOf(start)
	for (i in this) {
		acc = transform(acc, i)
		ret.add(acc)
	}
	return ret
}

inline fun <T, S, R> Iterable<T>.stateScan(start: S, transform: (S, T) -> Pair<S, R>): List<R> {
	var acc = start
	val ret = mutableListOf<R>()
	for (i in this) {
		val (s, r) = transform(acc, i)
		acc = s
		ret.add(r)
	}
	return ret
}

inline fun <T> Iterable<T>.scan(transform: (T, T) -> T): List<T> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()
	var acc = iter.next()
	val ret = mutableListOf<T>(acc)
	for (i in iter) {
		acc = transform(acc, i)
		ret.add(acc)
	}
	return ret
}

fun <T> Iterable<T>.countEachI(): Map<T, Int> {
	val counts = mutableMapOf<T, Int>()
	for (element in this) counts.merge(element, 1, Int::plus)
	return counts
}


fun <T> Iterable<T>.countEach(): Map<T, Sint> {
	val counts = mutableMapOf<T, Sint>()
	for (element in this) counts.merge(element, 1.s, Sint::plus)
	return counts
}

fun <T> Iterable<T>.blockCountsI(): List<Pair<T, Int>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()
	var acc = iter.next()
	var count = 1
	val ret = mutableListOf<Pair<T, Int>>()
	for (i in iter) {
		if (acc == i) count++
		else {
			ret.add(acc to count)
			acc = i
			count = 1

		}
	}
	ret.add(acc to count)
	return ret
}


fun <T> Iterable<T>.blockCounts(): List<Pair<T, Sint>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()
	var acc = iter.next()
	var count = 1.s
	val ret = mutableListOf<Pair<T, Sint>>()
	for (i in iter) {
		if (acc == i) count++
		else {
			ret.add(acc to count)
			acc = i
			count = 1.s

		}
	}
	ret.add(acc to count)
	return ret
}

fun <T> Iterable<T>.blocks(): List<List<T>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()
	var acc = iter.next()
	var count = mutableListOf<T>(acc)
	val ret = mutableListOf<List<T>>()
	for (i in iter) {
		if (acc == i) count.add(i)
		else {
			ret.add(count)
			acc = i
			count = mutableListOf<T>(acc)

		}
	}
	ret.add(count)
	return ret
}

/**
 * Verifies by trying to sort
 */
fun <T : Comparable<T>> Iterable<T>.isSorted(): Boolean = this.sorted() == this.toList()

fun <T : Comparable<T>> Iterable<T>.isAscending(): Boolean {
	val iter = iterator()
	if (!iter.hasNext()) return true
	var acc = iter.next()
	for (i in iter) {
		if (acc > i) return false
		acc = i
	}
	return true
}

fun <T : Comparable<T>> Iterable<T>.isDescending(): Boolean {
	val iter = iterator()
	if (!iter.hasNext()) return true
	var acc = iter.next()
	for (i in iter) {
		if (acc < i) return false
		acc = i
	}
	return true
}

fun <T : Comparable<T>> Iterable<T>.isStrictAscending(): Boolean {
	val iter = iterator()
	if (!iter.hasNext()) return true
	var acc = iter.next()
	for (i in iter) {
		if (acc >= i) return false
		acc = i
	}
	return true
}

fun <T : Comparable<T>> Iterable<T>.isStrictDescending(): Boolean {
	val iter = iterator()
	if (!iter.hasNext()) return true
	var acc = iter.next()
	for (i in iter) {
		if (acc <= i) return false
		acc = i
	}
	return true
}

/**
 *
 * @param transform called once for each item in the iterable
 */
inline fun <T, R> Iterable<T>.blockBy(transform: (T) -> R): List<List<T>> {
	// Like group by, but groups have to be continuous
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()
	val start = iter.next()
	var key = transform(start)
	var count = mutableListOf(start)
	val result = mutableListOf<List<T>>()
	for (i in iter) {
		val ikey = transform(i)
		if (key == ikey) count.add(i)
		else {
			result.add(count)
			key = ikey
			count = mutableListOf(start)

		}
	}
	return result
}

fun Iterable<*>.areDistinct(): Boolean {
	val seen = mutableSetOf<Any?>()
	for (i in this) if (!seen.add(i)) return false
	return true
}

inline fun <T, R> Iterable<T>.flatMapIndexed(transform: (Int, T) -> Iterable<R>): List<R> =
	mapIndexed(transform).flatten()


inline fun <T, R> Iterable<T>.flatMapIdx(transform: (Sint, T) -> Iterable<R>): List<R> = mapIdx(transform).flatten()


fun <T> Iterator<T>.subSetsWithLength(n: Int): List<List<T>> {
	if (n == 0) return listOf(emptyList())
	if (!this.hasNext()) return emptyList()

	val item = this.next()
	return this.subSetsWithLength(n - 1).map { listOf(item) + it } + this.subSetsWithLength(n)
}


fun <T> Iterator<T>.subSetsWithLength(n: Sint): List<List<T>> {
	if (n == 0.s) return listOf(emptyList())
	if (!this.hasNext()) return emptyList()

	val item = this.next()
	return this.subSetsWithLength(n - 1).map { listOf(item) + it } + this.subSetsWithLength(n)
}

fun <T> Iterable<T>.subSetsWithLength(n: Int) = iterator().subSetsWithLength(n)

fun <T> Iterable<T>.subSetsWithLength(n: Sint) = iterator().subSetsWithLength(n)

fun <T> Iterable<T>.pairWise(): List<Pair<T, T>> = flatMapIndexed { i, v -> drop(i + 1).map { v to it } }
fun <T> Iterable<T>.orderedPairWise(): List<Pair<T, T>> = flatMapIndexed { i, v ->
	filterIndexed { i2, _ -> i != i2 }.map { v to it }
}

fun <T> Iterable<T>.selfPairWise(): List<Pair<T, T>> = flatMapIndexed { i, v -> drop(i).map { v to it } }

fun <T> Iterable<T>.cartesianSquare(): List<Pair<T, T>> = flatMap { v -> map { v to it } }
fun <T, R> Iterable<T>.cartesianProduct(other: Iterable<R>): List<Pair<T, R>> = flatMap { v -> other.map { v to it } }
inline fun <T, R, S> Iterable<T>.cartesianProduct(other: Iterable<R>, transform: (T, R) -> S): List<S> =
	flatMap { v -> other.map { transform(v, it) } }

fun <T> Iterable<T>.cartesianPower(count: Int): List<List<T>> = if (count < 1) emptyList() else cartesianPower1(count)
fun <T> Iterable<T>.cartesianPower1(count: Int): List<List<T>> =
	if (count == 1) this.map { listOf(it) } else cartesianPower1(count - 1).cartesianProduct(this) { a, b -> a + b }

inline fun <T1, T2, R> cartesianProductOf(v1: Iterable<T1>, v2: Iterable<T2>, transform: (T1, T2) -> R): List<R> =
	v1.flatMap { i1 -> v2.map { i2 -> transform(i1, i2) } }

inline fun <T1, T2, T3, R> cartesianProductOf(
	v1: Iterable<T1>, v2: Iterable<T2>, v3: Iterable<T3>, transform: (T1, T2, T3) -> R
): List<R> = v1.flatMap { i1 -> cartesianProductOf(v2, v3) { i2, i3 -> transform(i1, i2, i3) } }

inline fun <T1, T2, T3, T4, R> cartesianProductOf(
	v1: Iterable<T1>, v2: Iterable<T2>, v3: Iterable<T3>, v4: Iterable<T4>, transform: (T1, T2, T3, T4) -> R
): List<R> = v1.flatMap { i1 -> cartesianProductOf(v2, v3, v4) { i2, i3, i4 -> transform(i1, i2, i3, i4) } }

inline fun <T1, T2, T3, T4, T5, R> cartesianProductOf(
	v1: Iterable<T1>,
	v2: Iterable<T2>,
	v3: Iterable<T3>,
	v4: Iterable<T4>,
	v5: Iterable<T5>,
	transform: (T1, T2, T3, T4, T5) -> R
): List<R> = v1.flatMap { i1 -> cartesianProductOf(v2, v3, v4, v5) { i2, i3, i4, i5 -> transform(i1, i2, i3, i4, i5) } }

inline fun <T1, T2, T3, T4, T5, T6, R> cartesianProductOf(
	v1: Iterable<T1>,
	v2: Iterable<T2>,
	v3: Iterable<T3>,
	v4: Iterable<T4>,
	v5: Iterable<T5>,
	v6: Iterable<T6>,
	transform: (T1, T2, T3, T4, T5, T6) -> R
): List<R> = v1.flatMap { i1 ->
	cartesianProductOf(v2, v3, v4, v5, v6) { i2, i3, i4, i5, i6 ->
		transform(
			i1, i2, i3, i4, i5, i6
		)
	}
}

inline fun <T1, T2> cartesianProductOf(
	v1: Iterable<T1>, v2: Iterable<T2>
): List<Pair<T1, T2>> = cartesianProductOf(v1, v2) { i1, i2 -> i1 to i2 }


fun <T> Iterator<T>.powerSet(): List<List<T>> {
	val iter = iterator()
	if (!iter.hasNext()) return listOf(listOf())
	val pre = listOf(iter.getNext())
	val next = iter.powerSet()
	return next + next.map { pre + it }
}

fun <T> Iterable<T>.powerSet(): List<List<T>> = iterator().powerSet()

//@Deprecated("use trait version")
fun Iterable<Int>.cumSum(): List<Int> = scan(Int::plus)

val <T>Iterable<Pair<T, *>>.firsts get() = map { it.first }
val <T>Iterable<Pair<*, T>>.seconds get() = map { it.second }

inline fun <T> Iterable<T>.splitOn(predicate: (T) -> Boolean): List<List<T>> {
	val d = mutableListOf<List<T>>()
	var u = mutableListOf<T>()
	for (i in this) {
		if (predicate(i)) {
			d += u
			u = mutableListOf()
		} else {
			u.add(i)
		}
	}
	d += u
	return d;
}


// transpose
fun <T> Iterable<Iterable<T>>.transpose(): List<List<T>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()

	val ret = mutableListOf<List<T>>()
	val iters = this.map { it.iterator() }

	while (iters.all { it.hasNext() }) {
		ret.add(iters.map { it.next() })
	}

	// check if any has items left
	if (iters.any { it.hasNext() }) throw IllegalArgumentException("Not all iterators have been exhausted")

	return ret
}

// transpose, but no throw
fun <T> Iterable<Iterable<T>>.transposeOrStop(): List<List<T>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()

	val ret = mutableListOf<List<T>>()
	val iters = this.map { it.iterator() }

	while (iters.all { it.hasNext() }) {
		ret.add(iters.map { it.next() })
	}

	return ret
}

//transpose or nulls
fun <T> Iterable<Iterable<T>>.transposeOrNulls(): List<List<T?>> {
	val iter = iterator()
	if (!iter.hasNext()) return emptyList()

	val ret = mutableListOf<List<T?>>()
	val iters = this.map { it.iterator() }

	while (iters.any { it.hasNext() }) {
		ret.add(iters.map { if (it.hasNext()) it.next() else null })
	}

	return ret
}


inline fun <T, R> Iterable<T>.repeatMap(count: Int, mapping: (Int, T) -> R): List<R> =
	(0 until count).flatMap { i -> map { mapping(i, it) } }

inline fun <T, R> Iterable<T>.repeatMap(count: Sint, mapping: (Sint, T) -> R): List<R> =
	(0 until count).flatMap { i -> map { mapping(i, it) } }

fun Iterable<String>.splitOnEmpty(): List<List<String>> = this.splitOn { it.isEmpty() }

@JvmName("productInts")
fun Iterable<Int>.product(): Long = this.fold(1L) { acc, i -> acc * i }
fun Iterable<Long>.product(): Long = this.fold(1L) { acc, i -> acc * i }
fun Iterable<Double>.product(): Double = this.fold(1.0) { acc, i -> acc * i }

@JvmName("productSints")
fun Iterable<Sint>.product(): Sint = this.fold(1.s) { acc, i -> acc * i }


@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("productOfInts")
inline fun <T> Iterable<T>.productOf(transform: (T) -> Int): Long = this.fold(1L) { acc, i -> acc * transform(i) }

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("productOfLongs")
inline fun <T> Iterable<T>.productOf(transform: (T) -> Long): Long = this.fold(1L) { acc, i -> acc * transform(i) }

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <T> Iterable<T>.productOf(transform: (T) -> Double): Double = this.fold(1.0) { acc, i -> acc * transform(i) }

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <T> Iterable<T>.productOf(transform: (T) -> Sint): Sint = this.fold(1.s) { acc, i -> acc * transform(i) }

fun Iterable<Char>.join(): String = this.joinToString("")

@JvmName("joinStrings")
fun Iterable<String>.join(): String = this.joinToString("")

fun <T : Comparable<T>> Iterable<T>.medianOdd(): T {
	val count = this.count()
	require(count % 2 == 1)
	return this.sorted()[count / 2]
}

fun <T : Comparable<T>> Iterable<T>.medianEven(): Pair<T, T> {
	val count = this.count()
	require(count % 2 == 0)
	val sorted = this.sorted()
	return sorted[count / 2 - 1] to sorted[count / 2]
}


fun <T> List<T>.permutations(): List<List<T>> = if (this.isEmpty()) listOf(emptyList()) else this.indices.flatMap {
	(this.subList(0, it) + this.subList(it + 1, this.size)).permutations().map { l -> listOf(this[it]) + l }
}

fun <T> Iterable<T>.permutations() = toList().permutations()

fun <T : Comparable<T>> Iterable<T>.max(n: Int) = this.sortedDescending().take(n)
fun <T : Comparable<T>> Iterable<T>.min(n: Int) = this.sorted().take(n)
fun <T : Comparable<T>> Iterable<T>.max(n: Sint) = this.sortedDescending().take(n)
fun <T : Comparable<T>> Iterable<T>.min(n: Sint) = this.sorted().take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.maxBy(n: Int, crossinline selector: (T) -> C) =
	this.sortedByDescending(selector).take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.minBy(n: Int, crossinline selector: (T) -> C) =
	this.sortedBy(selector).take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.maxBy(n: Sint, crossinline selector: (T) -> C) =
	this.sortedByDescending(selector).take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.minBy(n: Sint, crossinline selector: (T) -> C) =
	this.sortedBy(selector).take(n)


inline fun <T, C : Comparable<C>> Iterable<T>.maxOf(n: Int, selector: (T) -> C) =
	this.map(selector).sortedDescending().take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.minOf(n: Int, selector: (T) -> C) = this.map(selector).sorted().take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.maxOf(n: Sint, selector: (T) -> C) =
	this.map(selector).sortedDescending().take(n)

inline fun <T, C : Comparable<C>> Iterable<T>.minOf(n: Sint, selector: (T) -> C) = this.map(selector).sorted().take(n)

//region String destructors
operator fun String.component1(): Char = this[0]
operator fun String.component2(): Char = this[1]
operator fun String.component3(): Char = this[2]
operator fun String.component4(): Char = this[3]
operator fun String.component5(): Char = this[4]
operator fun String.component6(): Char = this[5]
operator fun String.component7(): Char = this[6]
operator fun String.component8(): Char = this[7]
operator fun String.component9(): Char = this[8]
operator fun String.component10(): Char = this[9]
operator fun String.component11(): Char = this[10]
operator fun String.component12(): Char = this[11]
operator fun String.component13(): Char = this[12]
operator fun String.component14(): Char = this[13]
operator fun String.component15(): Char = this[14]
operator fun String.component16(): Char = this[15]
operator fun String.component17(): Char = this[16]
operator fun String.component18(): Char = this[17]
operator fun String.component19(): Char = this[18]
operator fun String.component20(): Char = this[19]
operator fun String.component21(): Char = this[20]
operator fun String.component22(): Char = this[21]
operator fun String.component23(): Char = this[22]
operator fun String.component24(): Char = this[23]
operator fun String.component25(): Char = this[24]
operator fun String.component26(): Char = this[25]
operator fun String.component27(): Char = this[26]
operator fun String.component28(): Char = this[27]
operator fun String.component29(): Char = this[28]
operator fun String.component30(): Char = this[29]

operator fun String.get(indexes: IntRange): String = substring(indexes.first, indexes.last + 1)
operator fun String.get(indexes: SintRange): String = substring(indexes.first.i, (indexes.last + 1).i)

operator fun String.get(indexes: IntProgression): List<Char> = indexes.map { this[it] }
operator fun String.get(indexes: SintProgression): List<Char> = indexes.map { this[it.i] }

//endregion


infix fun <T> Collection<T>.splitIn(n: Int): List<List<T>> {
	val length = this.size
	require(size divBy n)
	return chunked(length / n)
}


infix fun String.splitIn(n: Int): List<String> {
	val length = this.length
	require(length divBy n)
	return chunked(length / n)
}

fun <T, R> Collection<T>.splitIn(n: Int, transform: (List<T>) -> R): List<R> {
	val length = this.size
	require(size divBy n)
	return chunked(length / n, transform)
}

infix fun <T> Collection<T>.splitIn(n: Sint): List<List<T>> {
	val length = this.size
	require(size divBy n)
	return chunked(length / n)
}


infix fun String.splitIn(n: Sint): List<String> {
	val length = this.length
	require(length divBy n)
	return e().chunked(length / n).map { it.join() }
}

fun <T, R> Collection<T>.splitIn(n: Sint, transform: (List<T>) -> R): List<R> {
	val length = this.size
	require(size divBy n)
	return chunked(length / n, transform)
}

fun <T> Collection<T>.splitIn2(): Pair<List<T>, List<T>> = splitIn(2).let { (a, b) -> a to b }
fun <T> String.splitIn2(): Pair<String, String> = splitIn(2).let { (a, b) -> a to b }
infix fun <T, R> Collection<T>.splitIn2(transform: (List<T>) -> R): Pair<R, R> =
	splitIn(2, transform).let { (a, b) -> a to b }

fun <T> Iterable<Iterable<T>>.union() = this.reduce(Iterable<T>::union).toSet()
fun <T> Iterable<Iterable<T>>.intersect() = this.reduce(Iterable<T>::intersect).toSet()


fun <T> Pair<Iterable<T>, Iterable<T>>.union() = first or second

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <T, R, V> Pair<Iterable<T>, Iterable<R>>.intersect(): Set<V> where V : T, V : R = first and second as Iterable<V>

infix fun <T> Iterable<T>.notIn(other: Iterable<*>): Set<T> = LazySet.difference(this, other)

// intersection, but better types
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <T, R, V> Iterable<T>.onlyIn(other: Iterable<R>): Set<V> where V : T, V : R = LazySet.intersection(this, other)


// onlyIn but it's infix
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
infix fun <T, R, V> Iterable<T>.inter(other: Iterable<R>): Set<V> where V : T, V : R = this.onlyIn(other)

// symdiff
infix fun <T> Iterable<T>.symDiff(other: Iterable<T>): Set<T> =
	LazySet.difference(this, other) or LazySet.difference(other, this)

// union, but it's infix
infix fun <T> Iterable<T>.or(other: Iterable<T>): Set<T> = LazySet.union(this, other)

// onlyIn but it's infix
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
infix fun <T, R, V> Iterable<T>.and(other: Iterable<R>): Set<V> where V : T, V : R = this.onlyIn(other)

// aka: does this intersect
infix fun <T> Iterable<T>.anyIn(other: Iterable<T>): Boolean {
	val o = other.asSet()
	return any { it in o }
}

// aka: is this a subset of
infix fun <T> Iterable<T>.allIn(other: Iterable<T>): Boolean {
	val o = other.asSet()
	return all { it in o }
}

// aka: are these fully distinct
infix fun <T> Iterable<T>.noneIn(other: Iterable<T>): Boolean {
	val o = other.asSet()
	return none { it in o }
}

// aka: is this a superset of
infix fun <T> Iterable<T>.containsAll(other: Iterable<T>) = other.allIn(this)

fun <T, R> Pair<Iterable<T>, Iterable<R>>.zipped() = first.zip(second)
inline fun <T, R, V> Pair<Iterable<T>, Iterable<R>>.zipped(transform: (T, R) -> V) = first.zip(second, transform)


fun <T> Iterable<T>.rotateLeft(i: Int): List<T> {
	val list = this.toList()
	val shift = i mod list.size

	return list.splitAt(shift).on { l, r -> r + l }
}

fun <T> Iterable<T>.rotateRight(i: Int): List<T> = rotateLeft(-i)


fun <T> Iterable<T>.rotateLeft(i: Sint): List<T> {
	val list = this.toList()
	val shift = i mod list.size

	return list.splitAt(shift).on { l, r -> r + l }
}

fun <T> Iterable<T>.rotateRight(i: Sint): List<T> = rotateLeft(-i)

fun <T> Iterable<T>.splitAt(i: Int): Pair<List<T>, List<T>> = take(i) to drop(i)
fun <T> Iterable<T>.splitAt(i: Sint): Pair<List<T>, List<T>> = take(i) to drop(i)
fun String.splitAt(i: Int): Pair<String, String> = take(i) to drop(i)
fun String.splitAt(i: Sint): Pair<String, String> = take(i) to drop(i)


fun <T> Iterable<T>.split2(item: T): Pair<List<T>, List<T>> {
	val list = this.toList()
	val idx = list.indexOf(item)
	return take(idx) to drop(idx + 1)
}

fun String.split2(item: String): Pair<String, String> {
	val idx = indexOf(item)
	return take(idx) to drop(idx + item.length)
}


inline fun <T> Iterable<T>.split2On(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
	val before = mutableListOf<T>()
	val iterator = this.iterator()
	for (i in iterator) {
		if (!predicate(i)) {
			before.add(i)
		} else {
			break
		}
	}
	val after = mutableListOf<T>()
	for (i in iterator) {
		after.add(i)
	}

	return before to after
}

fun <T> Iterable<T>.takeUntilInc(predicate: (T) -> Boolean): List<T> = buildList {
	for (i in this@takeUntilInc) {
		add(i)
		if (predicate(i)) {
			break
		}
	}
}

inline fun <T> Iterable<T>.partitionIndexed(predicate: (Int, T) -> Boolean) =
	withIndex().partition { (i, value) -> predicate(i, value) }.map { l -> l.map { it.value } }


inline fun <T> Iterable<T>.partitionIdx(predicate: (Sint, T) -> Boolean) =
	withIdx().partition { (i, value) -> predicate(i, value) }.map { l -> l.map { it.value } }

inline fun <T, R : T> T.applyNTimes(n: Int, action: (T) -> R): R {
	require(n > 0)
	return applyNTimesOr0(n, action) as R
}

inline fun <T> T.applyNTimesOr0(n: Int, action: (T) -> T): T {
	var cur = this;
	repeat(n) {
		cur = action(cur)
	}
	return cur
}


inline fun <T, R : T> T.applyNTimes(n: Sint, action: (T) -> R): R {
	require(n > 0)
	return applyNTimesOr0(n, action) as R
}

inline fun <T> T.applyNTimesOr0(n: Sint, action: (T) -> T): T {
	var cur = this;
	repeat(n) {
		cur = action(cur)
	}
	return cur
}

fun Iterable<Iterable<Boolean>>.printCrt() {
	for (i in this) {
		println(i.joinToString("") { if (it) "##" else "  " })
	}
}


@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K, V?>.filterNotNullValues() = filterValues { it != null } as Map<K, V>

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K?, V>.filterNotNullKeys() = filterKeys { it != null } as Map<K, V>


inline fun <T, K> Iterable<T>.associateByNotNull(keySelector: (T) -> K?): Map<K, T> =
	associateBy(keySelector).filterNotNullKeys()

inline fun <T, K, V> Iterable<T>.associateByNotNull(keySelector: (T) -> K?, valueTransform: (T) -> V?): Map<K, V> =
	associateBy(keySelector, valueTransform).filterNotNullKeys().filterNotNullValues()

inline fun <K, V> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> =
	associateWith(valueSelector).filterNotNullValues()

fun <T> List<T>.tas() = mutableListOf<T>()
fun <T> Set<T>.tas() = mutableSetOf<T>()
fun <K, V> Map<K, V>.tas() = mutableMapOf<K, V>()

fun <T> listTT(example: T) = mutableListOf<T>()
fun <T> setTT(example: T) = mutableSetOf<T>()
fun <K, V> mapTT(example: Pair<K, V>) = mutableMapOf<K, V>()
fun <K, V> mapTT(exampleKey: K, exampleValue: V) = mutableMapOf<K, V>()

val IntRange.size get() = this.last - this.first + 1
val LongRange.size get() = this.last - this.first + 1

val LongRange.sizeB get() = this.last.toBigInteger() - this.first.toBigInteger() + BigInteger.ONE



fun IntRange.sint() = this.first.s..this.last.s
fun LongRange.sint() = this.first.s..this.last.s
fun SintRange.sint() = this.first..this.last

fun SintRange.int() = this.first.i..this.last.i
fun SintRange.long() = this.first.l..this.last.l

fun IntProgression.sint() = this.first.s..this.last.s step this.step
fun LongProgression.sint() = this.first.s..this.last.s step this.step
fun SintProgression.sint() = this.first..this.last step this.step

fun SintProgression.int() = this.first.i..this.last.i step this.step.i
fun SintProgression.long() = this.first.l..this.last.l step this.step.l

fun IntRange.frac(n: Int): List<IntRange> = this.sint().frac(n).map { it.int() }
fun LongRange.frac(n: Int): List<LongRange> = this.sint().frac(n).map { it.long() }

fun SintRange.frac(n: Sint): List<SintRange> = frac(n.i)

fun SintRange.frac(n: Int): List<SintRange> {
	if (this.sizeS <= n) return this.map { it..it }
	val step = this.sizeS / n
	val rem = this.sizeS % n

	val ret = mutableListOf<SintRange>()
	for (i in 0 until n) {
		val start = this.first + i * step + min(i.s, rem)
		val end = start + step + if (i < rem) 1.s else 0.s
		ret.add(start until end)
	}
	return ret
}

val IntProgression.size get() = (this.last - this.first) / this.step + 1
val LongProgression.size get() = ((this.last - this.first) / this.step + 1)

fun IntProgression.frac(n: Int): List<IntProgression> {
	if (this.size <= n) return this.map { it..it step this.step }
	val step = this.size / n * this.step
	val rem = this.size % n

	val ret = mutableListOf<IntProgression>()
	for (i in 0 until n) {
		val start = this.first + i * step + min(i, rem) * this.step
		val end = start + step + if (i < rem) 1 else 0
		ret.add(start until end step this.step)
	}
	return ret
}

fun LongProgression.frac(n: Int): List<LongProgression> {
	if (this.size <= n) return this.map { it..it step this.step }
	val step = this.size / n * this.step
	val rem = this.size % n

	val ret = mutableListOf<LongProgression>()
	for (i in 0 until n) {
		val start = this.first + i * step + min(i.toLong(), rem) * this.step
		val end = start + step + if (i < rem) 1 else 0
		ret.add(start until end step this.step)
	}
	return ret
}

fun SintProgression.frac(n: Sint): List<SintProgression> = frac(n.i)

fun SintProgression.frac(n: Int): List<SintProgression> {
	if (this.sizeS <= n) return this.map { it..it step this.step }
	val step = this.sizeS / n * this.step
	val rem = this.sizeS % n

	val ret = mutableListOf<SintProgression>()
	for (i in 0 until n) {
		val start = this.first + i * step + min(i.s, rem) * this.step
		val end = start + step + if (i < rem) 1 else 0
		ret.add(start until end step this.step)
	}
	return ret
}


fun Iterable<*>.isEmpty() = !this.iterator().hasNext()