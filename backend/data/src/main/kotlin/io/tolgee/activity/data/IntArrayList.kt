package io.tolgee.activity.data

/** Primitive growable `int` array — used where `ArrayList<Int>` would box every element. */
class IntArrayList(
  initialCapacity: Int = 4,
) {
  private var data: IntArray = IntArray(initialCapacity)
  var size: Int = 0
    private set

  fun isEmpty(): Boolean = size == 0

  operator fun get(index: Int): Int {
    if (index < 0 || index >= size) throw IndexOutOfBoundsException("index=$index size=$size")
    return data[index]
  }

  fun add(value: Int) {
    if (size == data.size) {
      data = data.copyOf(if (data.size == 0) 4 else data.size * 2)
    }
    data[size++] = value
  }

  fun indexOf(value: Int): Int {
    for (i in 0 until size) {
      if (data[i] == value) return i
    }
    return -1
  }

  fun contains(value: Int): Boolean = indexOf(value) >= 0

  fun removeAt(index: Int) {
    if (index < 0 || index >= size) throw IndexOutOfBoundsException("index=$index size=$size")
    val tail = size - index - 1
    if (tail > 0) System.arraycopy(data, index + 1, data, index, tail)
    size--
  }
}
