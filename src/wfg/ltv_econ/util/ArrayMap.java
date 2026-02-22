/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wfg.ltv_econ.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * ArrayMap is a generic key -> value mapping data structure that is
 * designed to be more memory efficient than a traditional {@link java.util.HashMap}.
 * It keeps its mappings in an array data structure -- an integer array of hash
 * codes for each item, and an Object array of the key/value pairs.  This allows it to
 * avoid having to create an extra object for every entry put in to the map, and it
 * also tries to control the growth of the size of these arrays more aggressively
 * (since growing them only requires copying the entries in the array, not rebuilding
 * a hash map).
 *
 * <p>Note that this implementation is not intended to be appropriate for data structures
 * that may contain large numbers of items. It is generally slower than a traditional
 * HashMap, since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array. For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.</p>
 *
 * <p>Because this container is intended to better balance memory use, unlike most other
 * standard Java containers it will shrink its array as items are removed from it. Currently
 * you have no control over this shrinking -- if you set a capacity and then remove an
 * item, it may reduce the capacity to better match the current size.  In the future an
 * explicit call to set the capacity should turn off this aggressive shrinking behavior.</p>
 *
 * <p>This structure is <b>NOT</b> thread-safe.</p>
 */
public final class ArrayMap<K, V> implements Map<K, V> {
    /**
     * Attempt to spot concurrent modifications to this data structure.
     *
     * It's best-effort, but any time we can throw something more diagnostic than an
     * ArrayIndexOutOfBoundsException deep in the ArrayMap internals it's going to
     * save a lot of development time.
     *
     * Good times to look for CME include after any allocArrays() call and at the end of
     * functions that change mSize (put/remove/clear).
     */
    private static final boolean CONCURRENT_MODIFICATION_EXCEPTIONS = true;

    /**
     * The minimum amount by which the capacity of a ArrayMap will increase.
     * This is tuned to be relatively space-efficient.
     */
    private static final int BASE_SIZE = 4;

    /**
     * Maximum number of entries to have in array caches.
     */
    private static final int CACHE_SIZE = 10;

    /**
     * Special hash array value that indicates the container is immutable.
     */
    static final int[] EMPTY_IMMUTABLE_INTS = new int[0];

    /**
     * @hide Special immutable empty ArrayMap.
     */
    @SuppressWarnings("rawtypes")
    public static final ArrayMap EMPTY = new ArrayMap<>(-1);

    /**
     * Caches of small array objects to avoid spamming garbage.  The cache
     * Object[] variable is a pointer to a linked list of array objects.
     * The first entry in the array is a pointer to the next array in the
     * list; the second entry is a pointer to the int[] hash code array for it.
     */
    static Object[] mBaseCache;
    static int mBaseCacheSize;
    static Object[] mTwiceBaseCache;
    static int mTwiceBaseCacheSize;

    /**
     * Separate locks for each cache since each can be accessed independently of the other without
     * risk of a deadlock.
     */
    private static final Object sBaseCacheLock = new Object();
    private static final Object sTwiceBaseCacheLock = new Object();
    private final boolean mIdentityHashCode;
    int[] mHashes;
    Object[] mArray;
    int mSize;
    private static final int binarySearchHashes(int[] hashes, int size, int hash) {
        int lo = 0;
        int hi = size - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = hashes[mid];

            if (midVal < hash) {
                lo = mid + 1;
            } else if (midVal > hash) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo; // not found
    }

    int indexOf(Object key, int hash) {
        final int N = mSize;
        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return ~0;
        }
        int index = binarySearchHashes(mHashes, N, hash);
        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) return index;
        
        // If the key at the returned index matches, that's what we want.
        if (key.equals(mArray[index<<1])) return index;
        
        // Search for a matching key after the index.
        int end;
        for (end = index + 1; end < N && mHashes[end] == hash; end++) {
            if (key.equals(mArray[end << 1])) return end;
        }
        // Search for a matching key before the index.
        for (int i = index - 1; i >= 0 && mHashes[i] == hash; i--) {
            if (key.equals(mArray[i << 1])) return i;
        }
        // Key not found -- return negative value indicating where a
        // new entry for this key should go. We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return ~end;
    }

    int indexOfNull() {
        final int N = mSize;
        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return ~0;
        }
        int index = binarySearchHashes(mHashes, N, 0);
        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index;
        }
        // If the key at the returned index matches, that's what we want.
        if (null == mArray[index<<1]) {
            return index;
        }
        // Search for a matching key after the index.
        int end;
        for (end = index + 1; end < N && mHashes[end] == 0; end++) {
            if (null == mArray[end << 1]) return end;
        }
        // Search for a matching key before the index.
        for (int i = index - 1; i >= 0 && mHashes[i] == 0; i--) {
            if (null == mArray[i << 1]) return i;
        }
        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return ~end;
    }

    private void allocArrays(final int size) {
        if (mHashes == EMPTY_IMMUTABLE_INTS) {
            throw new UnsupportedOperationException("ArrayMap is immutable");
        }
        if (size == (BASE_SIZE*2)) {
            synchronized (sTwiceBaseCacheLock) {
                if (mTwiceBaseCache != null) {
                    final Object[] array = mTwiceBaseCache;
                    mArray = array;

                    mTwiceBaseCache = (Object[]) array[0];
                    mHashes = (int[]) array[1];
                    if (mHashes != null) {
                        array[0] = array[1] = null;
                        mTwiceBaseCacheSize--;
                        return;
                    }

                    mTwiceBaseCache = null;
                    mTwiceBaseCacheSize = 0;
                }
            }
        } else if (size == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCache != null) {
                    final Object[] array = mBaseCache;
                    mArray = array;

                    mBaseCache = (Object[]) array[0];
                    mHashes = (int[]) array[1];
                    if (mHashes != null) {
                        array[0] = array[1] = null;
                        mBaseCacheSize--;
                        return;
                    }

                    mBaseCache = null;
                    mBaseCacheSize = 0;
                }
            }
        }
        mHashes = new int[size];
        mArray = new Object[size<<1];
    }

    /**
     * Make sure <b>NOT</b> to call this method with arrays that can still be modified. In other
     * words, don't pass mHashes or mArray in directly.
     */
    private static void freeArrays(final int[] hashes, final Object[] array, final int size) {
        if (hashes.length == (BASE_SIZE*2)) {
            synchronized (sTwiceBaseCacheLock) {
                if (mTwiceBaseCacheSize < CACHE_SIZE) {
                    array[0] = mTwiceBaseCache;
                    array[1] = hashes;
                    for (int i=(size<<1)-1; i>=2; i--) {
                        array[i] = null;
                    }
                    mTwiceBaseCache = array;
                    mTwiceBaseCacheSize++;
                }
            }
        } else if (hashes.length == BASE_SIZE) {
            synchronized (sBaseCacheLock) {
                if (mBaseCacheSize < CACHE_SIZE) {
                    array[0] = mBaseCache;
                    array[1] = hashes;
                    for (int i=(size<<1)-1; i>=2; i--) {
                        array[i] = null;
                    }
                    mBaseCache = array;
                    mBaseCacheSize++;
                }
            }
        }
    }

    /**
     * Create a new empty ArrayMap.  The default capacity of an array map is 0, and
     * will grow once items are added to it.
     */
    public ArrayMap() {
        this(0, false);
    }

    /**
     * Create a new ArrayMap with a given initial capacity.
     */
    public ArrayMap(int capacity) {
        this(capacity, false);
    }

    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /** {@hide} */
    public ArrayMap(int capacity, boolean identityHashCode) {
        mIdentityHashCode = identityHashCode;
        // If this is immutable, use the sentinal EMPTY_IMMUTABLE_INTS
        // instance instead of the usual EmptyArray.INT. The reference
        // is checked later to see if the array is allowed to grow.
        if (capacity < 0) {
            mHashes = EMPTY_IMMUTABLE_INTS;
            mArray = EMPTY_OBJECT_ARRAY;
        } else if (capacity == 0) {
            mHashes = EMPTY_INT_ARRAY;
            mArray = EMPTY_OBJECT_ARRAY;
        } else {
            allocArrays(capacity);
        }
        mSize = 0;
    }

    /**
     * Create a new ArrayMap with the mappings from the given ArrayMap.
     */
    public ArrayMap(ArrayMap<K, V> map) {
        this();
        if (map != null) {
            putAll(map);
        }
    }

    /**
     * Make the array map empty. All storage is released.
     */
    @Override
    public void clear() {
        if (mSize > 0) {
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            final int osize = mSize;
            mHashes = EMPTY_INT_ARRAY;
            mArray = EMPTY_OBJECT_ARRAY;
            mSize = 0;
            freeArrays(ohashes, oarray, osize);
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && mSize > 0) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * @hide
     * Like {@link #clear}, but doesn't reduce the capacity of the ArrayMap.
     */
    public void erase() {
        if (mSize > 0) {
            final int N = mSize<<1;
            final Object[] array = mArray;
            for (int i=0; i<N; i++) {
                array[i] = null;
            }
            mSize = 0;
        }
    }

    /**
     * Ensure the array map can hold at least <var>minimumCapacity</var>
     * items.
     */
    public void ensureCapacity(int minimumCapacity) {
        final int osize = mSize;
        if (mHashes.length < minimumCapacity) {
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            allocArrays(minimumCapacity);
            if (mSize > 0) {
                System.arraycopy(ohashes, 0, mHashes, 0, osize);
                System.arraycopy(oarray, 0, mArray, 0, osize<<1);
            }
            freeArrays(ohashes, oarray, osize);
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && mSize != osize) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Check whether a key exists in the array.
     *
     * @param key The key to search for.
     * @return Returns true if the key exists, else false.
     */
    @Override
    public boolean containsKey(Object key) {
        return indexOfKey(key) >= 0;
    }

    /**
     * Returns the index of a key in the set.
     *
     * @param key The key to search for.
     * @return Returns the index of the key if it exists, else a negative integer.
     */
    public int indexOfKey(Object key) {
        return key == null ? indexOfNull()
                : indexOf(key, mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified value, or a negative number if no keys map to the
     * specified value.
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     */
    public int indexOfValue(Object value) {
        final int N = mSize*2;
        final Object[] array = mArray;
        if (value == null) {
            for (int i=1; i<N; i+=2) {
                if (array[i] == null) {
                    return i>>1;
                }
            }
        } else {
            for (int i=1; i<N; i+=2) {
                if (value.equals(array[i])) {
                    return i>>1;
                }
            }
        }
        return -1;
    }

    /**
     * Check whether a value exists in the array.  This requires a linear search
     * through the entire array.
     *
     * @param value The value to search for.
     * @return Returns true if the value exists, else false.
     */
    @Override
    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    /**
     * Retrieve a value from the array.
     * @param key The key of the value to retrieve.
     * @return Returns the value associated with the given key,
     * or null if there is no such key.
     */
    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        final int index = indexOfKey(key);
        return index >= 0 ? (V)mArray[(index<<1)+1] : null;
    }

    /**
     * Returns the key at the specified index in the map.
     *
     * <p>The index must be between {@code 0} and {@link #size()}-1. Accessing an index
     * outside this range will throw an {@link ArrayIndexOutOfBoundsException}.</p>
     *
     * @param index the index of the key to return, must be >= 0 and < {@link #size()}
     * @return the key at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public K keyAt(int index) {
        if (index < 0 || index >= mSize) {
            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }
        return (K) mArray[index << 1];
    }

    /**
     * Returns the value at the specified index in the map.
     *
     * <p>The index must be between {@code 0} and {@link #size()}-1. Accessing an index
     * outside this range will throw an {@link ArrayIndexOutOfBoundsException}.</p>
     *
     * @param index the index of the value to return, must be >= 0 and < {@link #size()}
     * @return the value at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public V valueAt(int index) {
        if (index < 0 || index >= mSize) {
            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }
        return (V) mArray[(index << 1) + 1];
    }

    /**
     * Sets the value at the specified index in the map.
     *
     * <p>The index must be between 0 and {@link #size()}-1. Accessing an index
     * outside this range will throw an {@link ArrayIndexOutOfBoundsException}.</p>
     *
     * @param index the index of the value to replace
     * @param value the new value to store
     * @return the previous value at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public V setValueAt(int index, V value) {
        if (index < 0 || index >= mSize) {
            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }
        int valueIndex = (index << 1) + 1;
        V old = (V) mArray[valueIndex];
        mArray[valueIndex] = value;
        return old;
    }

    /**
     * Return true if the array map contains no items.
     */
    @Override
    public boolean isEmpty() {
        return mSize <= 0;
    }

    /**
     * Add a new value to the array map.
     * @param key The key under which to store the value.  If
     * this key already exists in the array, its value will be replaced.
     * @param value The value to store for the given key.
     * @return Returns the old value that was stored for the given key, or null if there
     * was no such key.
     */
    @Override
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        final int osize = mSize;
        final int hash;
        int index;
        if (key == null) {
            hash = 0;
            index = indexOfNull();
        } else {
            hash = mIdentityHashCode ? System.identityHashCode(key) : key.hashCode();
            index = indexOf(key, hash);
        }
        if (index >= 0) {
            index = (index<<1) + 1;
            final V old = (V)mArray[index];
            mArray[index] = value;
            return old;
        }
        index = ~index;
        if (osize >= mHashes.length) {
            final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                    : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            allocArrays(n);
            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
                throw new ConcurrentModificationException();
            }
            if (mHashes.length > 0) {
                System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
                System.arraycopy(oarray, 0, mArray, 0, oarray.length);
            }
            freeArrays(ohashes, oarray, osize);
        }
        if (index < osize) {
            System.arraycopy(mHashes, index, mHashes, index + 1, osize - index);
            System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
            if (osize != mSize || index >= mHashes.length) {
                throw new ConcurrentModificationException();
            }
        }
        mHashes[index] = hash;
        mArray[index<<1] = key;
        mArray[(index<<1)+1] = value;
        mSize++;
        return null;
    }

    /**
     * Special fast path for appending items to the end of the array without validation.
     * The array must already be large enough to contain the item.
     * @hide
     */
    public void append(K key, V value) {
        int index = mSize;
        final int hash = key == null ? 0
                : (mIdentityHashCode ? System.identityHashCode(key) : key.hashCode());
        if (index >= mHashes.length) {
            throw new IllegalStateException("Array is full");
        }
        if (index > 0 && mHashes[index-1] > hash) {
            put(key, value);
            return;
        }
        mSize = index+1;
        mHashes[index] = hash;
        index <<= 1;
        mArray[index] = key;
        mArray[index+1] = value;
    }

    /**
     * The use of the {@link #append} function can result in invalid array maps, in particular
     * an array map where the same key appears multiple times.  This function verifies that
     * the array map is valid, throwing IllegalArgumentException if a problem is found.  The
     * main use for this method is validating an array map after unpacking from an IPC, to
     * protect against malicious callers.
     * @hide
     */
    public void validate() {
        final int N = mSize;
        if (N <= 1) {
            // There can't be dups.
            return;
        }
        int basehash = mHashes[0];
        int basei = 0;
        for (int i=1; i<N; i++) {
            int hash = mHashes[i];
            if (hash != basehash) {
                basehash = hash;
                basei = i;
                continue;
            }
            // We are in a run of entries with the same hash code.  Go backwards through
            // the array to see if any keys are the same.
            final Object cur = mArray[i<<1];
            for (int j=i-1; j>=basei; j--) {
                final Object prev = mArray[j<<1];
                if (cur == prev) {
                    throw new IllegalArgumentException("Duplicate key in ArrayMap: " + cur);
                }
                if (cur != null && prev != null && cur.equals(prev)) {
                    throw new IllegalArgumentException("Duplicate key in ArrayMap: " + cur);
                }
            }
        }
    }

    /**
     * Perform a {@link #put(Object, Object)} of all key/value pairs in <var>array</var>
     * @param array The array whose contents are to be retrieved.
     */
    public void putAll(ArrayMap<? extends K, ? extends V> array) {
        final int N = array.mSize;
        ensureCapacity(mSize + N);
        if (mSize == 0) {
            if (N > 0) {
                System.arraycopy(array.mHashes, 0, mHashes, 0, N);
                System.arraycopy(array.mArray, 0, mArray, 0, N<<1);
                mSize = N;
            }
        } else {
            for (int i=0; i<N; i++) {
                put(array.keyAt(i), array.valueAt(i));
            }
        }
    }

    /**
     * Remove an existing key from the array map.
     * @param key The key of the mapping to remove.
     * @return Returns the value that was stored under the key, or null if there
     * was no such key.
     */
    @Override
    public V remove(Object key) {
        final int index = indexOfKey(key);
        if (index >= 0) {
            return removeAt(index);
        }
        return null;
    }

    /**
     * Remove the key/value mapping at the given index.
     *
     * <p>For indices outside of the range <code>0...size()-1</code>, the behavior is undefined for
     * apps targeting {@link android.os.Build.VERSION_CODES#P} and earlier, and an
     * {@link ArrayIndexOutOfBoundsException} is thrown for apps targeting
     * {@link android.os.Build.VERSION_CODES#Q} and later.</p>
     *
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @return Returns the value that was stored at this index.
     */
    @SuppressWarnings("unchecked")
    public V removeAt(int index) {
        if (index < 0 || index >= mSize) {
            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }
        final Object old = mArray[(index << 1) + 1];
        final int osize = mSize;
        final int nsize;
        if (osize <= 1) {
            // Now empty.
            final int[] ohashes = mHashes;
            final Object[] oarray = mArray;
            mHashes = EMPTY_INT_ARRAY;
            mArray = EMPTY_OBJECT_ARRAY;
            freeArrays(ohashes, oarray, osize);
            nsize = 0;
        } else {
            nsize = osize - 1;
            if (mHashes.length > (BASE_SIZE*2) && mSize < mHashes.length/3) {
                // Shrunk enough to reduce size of arrays.  We don't allow it to
                // shrink smaller than (BASE_SIZE*2) to avoid flapping between
                // that and BASE_SIZE.
                final int n = osize > (BASE_SIZE*2) ? (osize + (osize>>1)) : (BASE_SIZE*2);
                final int[] ohashes = mHashes;
                final Object[] oarray = mArray;
                allocArrays(n);
                if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
                    throw new ConcurrentModificationException();
                }
                if (index > 0) {
                    System.arraycopy(ohashes, 0, mHashes, 0, index);
                    System.arraycopy(oarray, 0, mArray, 0, index << 1);
                }
                if (index < nsize) {
                    System.arraycopy(ohashes, index + 1, mHashes, index, nsize - index);
                    System.arraycopy(oarray, (index + 1) << 1, mArray, index << 1,
                            (nsize - index) << 1);
                }
            } else {
                if (index < nsize) {
                    System.arraycopy(mHashes, index + 1, mHashes, index, nsize - index);
                    System.arraycopy(mArray, (index + 1) << 1, mArray, index << 1,
                            (nsize - index) << 1);
                }
                mArray[nsize << 1] = null;
                mArray[(nsize << 1) + 1] = null;
            }
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
            throw new ConcurrentModificationException();
        }
        mSize = nsize;
        return (V)old;
    }

    /**
     * Return the number of items in this array map.
     */
    @Override
    public int size() {
        return mSize;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns false if the object is not a map, or
     * if the maps have different sizes. Otherwise, for each key in this map,
     * values of both maps are compared. If the values for any key are not
     * equal, the method returns false, otherwise it returns true.
     * 
     * @param object Nullable
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            if (size() != map.size()) {
                return false;
            }
            try {
                for (int i=0; i<mSize; i++) {
                    K key = keyAt(i);
                    V mine = valueAt(i);
                    Object theirs = map.get(key);
                    if (mine == null) {
                        if (theirs != null || !map.containsKey(key)) {
                            return false;
                        }
                    } else if (!mine.equals(theirs)) {
                        return false;
                    }
                }
            } catch (NullPointerException ignored) {
                return false;
            } catch (ClassCastException ignored) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int[] hashes = mHashes;
        final Object[] array = mArray;
        int result = 0;
        for (int i = 0, v = 1, s = mSize; i < s; i++, v+=2) {
            Object value = array[v];
            result += hashes[i] ^ (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a key or a value, the string "(this Map)"
     * will appear in its place.
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i=0; i<mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            Object key = keyAt(i);
            if (key != this) {
                buffer.append(key);
            } else {
                buffer.append("(this Map)");
            }
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(deepToString(value));
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    public static final String deepToString(final Object obj) {
        if (obj == null) return "null";

        final Class<?> cls = obj.getClass();
        if (!cls.isArray()) return obj.toString();

        if (obj instanceof Object[]) {
            final Object[] arr = (Object[]) obj;
            final StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(deepToString(arr[i]));
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof int[]) {
            return Arrays.toString((int[]) obj);
        } else if (obj instanceof long[]) {
            return Arrays.toString((long[]) obj);
        } else if (obj instanceof float[]) {
            return Arrays.toString((float[]) obj);
        } else if (obj instanceof double[]) {
            return Arrays.toString((double[]) obj);
        } else if (obj instanceof boolean[]) {
            return Arrays.toString((boolean[]) obj);
        } else if (obj instanceof char[]) {
            return Arrays.toString((char[]) obj);
        } else if (obj instanceof byte[]) {
            return Arrays.toString((byte[]) obj);
        } else if (obj instanceof short[]) {
            return Arrays.toString((short[]) obj);
        }

        return obj.toString();
    }

    /**
     * Determine if the array map contains all of the keys in the given collection.
     * @param collection The collection whose contents are to be checked against.
     * @return Returns true if this array map contains a key for every entry
     * in <var>collection</var>, else returns false.
     */
    public boolean containsAll(Collection<?> collection) {
        for (Object key : collection) {
            if (!containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs the given action for all elements in the stored order. This implementation overrides
     * the default implementation to avoid iterating using the {@link #entrySet()} and iterates in
     * the key-value order consistent with {@link #keyAt(int)} and {@link #valueAt(int)}.
     *
     * @param action The action to be performed for each element
     */
    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException("action must not be null");
        }
        final int size = mSize;
        for (int i = 0; i < size; ++i) {
            if (size != mSize) {
                throw new ConcurrentModificationException();
            }
            action.accept(keyAt(i), valueAt(i));
        }
    }

    /**
     * Perform a {@link #put(Object, Object)} of all key/value pairs in <var>map</var>
     * @param map The map whose contents are to be retrieved.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(mSize + map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove all keys in the array map that exist in the given collection.
     * @param collection The collection whose contents are to be used to remove keys.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public boolean removeAll(Collection<?> collection) {
        boolean removedAny = false;
        for (Object key : collection) {
            final int index = indexOfKey(key);
            if (index >= 0) {
                removeAt(index);
                removedAny = true;
            }
        }
        return removedAny;
    }

    /**
     * Replaces each entry's value with the result of invoking the given function on that entry
     * until all entries have been processed or the function throws an exception. Exceptions thrown
     * by the function are relayed to the caller. This implementation overrides
     * the default implementation to avoid iterating using the {@link #entrySet()} and iterates in
     * the key-value order consistent with {@link #keyAt(int)} and {@link #valueAt(int)}.
     *
     * @param function The function to apply to each entry
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException("function must not be null");
        }
        final int size = mSize;
        try {
            for (int i = 0; i < size; ++i) {
                final int valIndex = (i << 1) + 1;
                //noinspection unchecked
                mArray[valIndex] = function.apply((K) mArray[i << 1], (V) mArray[valIndex]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ConcurrentModificationException();
        }
        if (size != mSize) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Remove all keys in the array map that do <b>not</b> exist in the given collection.
     * @param collection The collection whose contents are to be used to determine which
     * keys to keep.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public final boolean retainAll(Collection<?> collection) {
        boolean removedAny = false;
        for (int i = mSize - 1; i >= 0; i--) {
            Object key = mArray[i << 1];
            if (!collection.contains(key)) {
                removeAt(i);
                removedAny = true;
            }
        }
        return removedAny;
    }

    /**
     * Return a {@link java.util.Set} for iterating over and interacting with all mappings
     * in the array map.
     *
     * <p><b>Note:</b> this is a very inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     *
     * <p><b>Note:</b></p> the semantics of this
     * Set are subtly different than that of a {@link java.util.HashMap}: most important,
     * the {@link java.util.Map.Entry Map.Entry} object returned by its iterator is a single
     * object that exists for the entire iterator, so you can <b>not</b> hold on to it
     * after calling {@link java.util.Iterator#next() Iterator.next}.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = new HashSet<>(mSize);
        for (int i = 0; i < mSize; i++) {
            final int index = i;
            set.add(new Map.Entry<>() {
                @Override
                public final K getKey() { return (K) mArray[index << 1]; }

                @Override
                public final V getValue() { return (V) mArray[(index << 1) + 1]; }

                @Override
                public final V setValue(V value) {
                    V old = (V) mArray[(index << 1) + 1];
                    mArray[(index << 1) + 1] = value;
                    return old;
                }

                @Override
                public final boolean equals(Object o) {
                    if (!(o instanceof Map.Entry)) return false;
                    final var e = (Map.Entry<?, ?>) o;
                    return Objects.equals(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
                }

                @Override
                public final int hashCode() {
                    return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
                }
            });
        }
        return set;
    }

    /**
     * Return a {@link java.util.Set} for iterating over and interacting with all keys
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        final Set<K> set = new HashSet<>(mSize);
        for (int i = 0; i < mSize; i++) {
            set.add((K) mArray[i << 1]);
        }
        return set;
    }

    /**
     * Return a {@link java.util.Collection} for iterating over and interacting with all values
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects and allocates additional state
     * information associated with the container that will remain for the life of the container.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        final List<V> list = new ArrayList<>(mSize);
        for (int i = 0; i < mSize; i++) {
            list.add((V) mArray[(i << 1) + 1]);
        }
        return list;
    }
}