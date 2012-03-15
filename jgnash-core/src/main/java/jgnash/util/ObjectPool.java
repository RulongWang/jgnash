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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Object pool for expensive objects that can be reused.  The pool
 * is thread-safe
 *
 * @author Craig Cavanaugh
 * @version $Id: ObjectPool.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public abstract class ObjectPool<T> {

    private final Queue<T> objects;

    public ObjectPool() {
        objects = new ConcurrentLinkedQueue<>();
    }

    public abstract T createInstance();

    public T take() {
        T t;

        if ((t = objects.poll()) == null) {
            t = createInstance();
        }
        return t;
    }

    public void put(final T object) {
        objects.offer(object);
    }
}