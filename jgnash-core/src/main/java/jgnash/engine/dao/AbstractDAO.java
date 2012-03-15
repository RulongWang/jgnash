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
package jgnash.engine.dao;

import java.util.Iterator;
import java.util.List;

import jgnash.engine.StoredObject;

/**
 * Basic DAO
 *
 * @author Craig Cavanaugh
 * @version $Id: AbstractDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class AbstractDAO {

    protected static <T extends StoredObject> List<T> stripMarkedForRemoval(List<T> list) {
        for (Iterator<T> i = list.iterator(); i.hasNext();) {
            T node = i.next();
            if (node.isMarkedForRemoval()) {
                i.remove();
            }
        }
        return list;
    }

}