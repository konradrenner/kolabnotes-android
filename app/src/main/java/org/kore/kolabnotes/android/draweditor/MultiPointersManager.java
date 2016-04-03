/*
 * Copyright (C) 2016 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of Kolab Notes.
 *
 * Kolab Notes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kore.kolabnotes.android.draweditor;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by yaroslav on 24.03.16.
 */

/**
 * Management drawing multiple fingers, binding of drawing lines with a certain finger.
 */

public class MultiPointersManager {
    private int mMaxPointers;
    private HashMap<Integer, Line> mMultiLine;

    MultiPointersManager(int maxPointers) {
        mMultiLine = new HashMap<Integer, Line>(maxPointers);
        mMaxPointers = maxPointers;
    }

    public void addLine(int idPointer, Line line) {
        if (mMultiLine.size() == mMaxPointers) {
            return;
        }

        mMultiLine.put(idPointer, line);
    }

    public Line getLine(int idPointer) {
        return mMultiLine.get(idPointer);
    }

    public void removeLine(int idPointer) {
        mMultiLine.remove(idPointer);
    }

    public Set<Integer> getKeys() {
        return mMultiLine.keySet();
    }
}
