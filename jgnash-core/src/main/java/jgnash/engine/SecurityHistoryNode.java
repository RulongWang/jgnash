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
package jgnash.engine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jgnash.util.DateUtils;

/**
 * Historical data for a <code>SecurityNode</code>.
 *
 * @author Craig Cavanaugh
 *
 */
public class SecurityHistoryNode implements Comparable<SecurityHistoryNode>, Serializable {
    
    private static final long serialVersionUID = 1L;

    private Date date = DateUtils.trimDate(new Date());

    private BigDecimal price = BigDecimal.ZERO;

    private BigDecimal high = BigDecimal.ZERO;

    private BigDecimal low = BigDecimal.ZERO;

    private long volume = 0;

    /**
     * public no-argument constructor for reflection
     */
    public SecurityHistoryNode() {
    }

    public void setHigh(final BigDecimal high) {
        if (high != null) {
            this.high = high;
        }
    }

    public void setLow(final BigDecimal low) {
        if (low != null) {
            this.low = low;
        }
    }

    public void setVolume(final long volume) {
        this.volume = volume;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public long getVolume() {
        return volume;
    }

    public void setDate(final Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Null assignment is not allowed");
        }
        this.date = DateUtils.trimDate(date);
    }

    public Date getDate() {
        return date;
    }

    public void setPrice(final BigDecimal price) {
        if (price != null) {
            this.price = price;
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Compare using on the month, day and year of the node ignoring hours and
     * seconds
     *
     * @param node node to compare
     */
    @Override
    public int compareTo(final SecurityHistoryNode node) {
        return getDate().compareTo(node.getDate());
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof SecurityHistoryNode && date.equals(((SecurityHistoryNode) other).date);
    }
}