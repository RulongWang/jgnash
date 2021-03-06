/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.util;

import java.math.BigDecimal;

/**
 * A list class to cache BigDecimals at specified indexes. BigDecimalCache operates under the assumption that it may
 * need to expand the size of the list if an index larger than the current capacity is being set. A returned value of
 * null on a get(index) operation means that the value at that index has not been set.
 * 
 * @author Craig Cavanaugh
 *
 */
public final class BigDecimalCache {

    private BigDecimal cache[] = new BigDecimal[0];

    public BigDecimalCache(final int capacity) {
        ensureCapacity(capacity);
    }

    /**
     * Trims the capacity of this <tt>BigDecimalCache</tt> instance to the specified size.
     * 
     * @param size the new capacity of the cache
     */
    public void trimToSize(final int size) {
        int oldCapacity = cache.length;
        if (size < oldCapacity) {
            BigDecimal oldData[] = cache;
            cache = new BigDecimal[size];
            System.arraycopy(oldData, 0, cache, 0, size);
        }
    }

    /**
     * Increases the capacity of this <tt>BigDecimalCache</tt> instance, if necessary, to ensure that it can hold at
     * least the number of BigDecimals specified by the minimum capacity argument.
     * 
     * @param minCapacity the desired minimum capacity.
     */
    public void ensureCapacity(final int minCapacity) {
        if (minCapacity > cache.length) {
            int oldCapacity = cache.length;
            BigDecimal oldCache[] = cache;
            int newCapacity = oldCapacity + 5;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            cache = new BigDecimal[newCapacity];
            System.arraycopy(oldCache, 0, cache, 0, oldCapacity);
        }
    }

    /**
     * Returns the <tt>BigDecimal</tt> at the specified position in the cache
     * 
     * @param index index of the <tt>BigDecimal</tt> to return.
     * @return the <tt>BigDecimal</tt> at the specified position in this list.
     */
    public BigDecimal get(final int index) {
        return cache[index];
    }

    /**
     * Sets the <tt>BigDecimal</tt> at the specified position in this cache.
     * 
     * @param index index of the <tt>BigDecimal</tt> to replace.
     * @param element <tt>BigDecimal</tt> to be stored at the specified position.
     */
    public void set(final int index, final BigDecimal element) {
        ensureCapacity(index + 1); // capacity is one more than index
        cache[index] = element;
    }

    /**
     * Clear all of the BigDecimals in the cache.
     */
    public void clear() {
        cache = new BigDecimal[cache.length];
    }

    /**
     * Clears the cache all of <tt>BigDecimals</tt> starting at fromIndex.
     * 
     * @param fromIndex index of first BigDecimal to be cleared.
     */
    public void clear(final int fromIndex) {
        if (fromIndex >= 0 && fromIndex < cache.length - 1) {
            int toIndex = cache.length;
            for (int i = fromIndex; i < toIndex; i++) {
                cache[i] = null;
            }
        } else {
            ensureCapacity(fromIndex + 1);
        }
    }
}
