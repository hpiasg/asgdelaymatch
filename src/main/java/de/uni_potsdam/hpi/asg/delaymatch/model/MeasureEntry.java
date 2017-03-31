package de.uni_potsdam.hpi.asg.delaymatch.model;

/*
 * Copyright (C) 2016 Norman Kluge
 * 
 * This file is part of ASGdelaymatch.
 * 
 * ASGdelaymatch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGdelaymatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGdelaymatch.  If not, see <http://www.gnu.org/licenses/>.
 */

public class MeasureEntry {

    public enum EntryType {
        recordDelay, externalDelay, datapathDelay, unknown
    }

    private MeasureRecord rec;
    private EntryType     type;

    public MeasureEntry(MeasureRecord rec) {
        this.rec = rec;
        this.type = EntryType.recordDelay;
    }

    public MeasureEntry(EntryType type) {
        this.type = type;
    }

    public MeasureRecord getRecord() {
        return rec;
    }

    public EntryType getType() {
        return type;
    }
}
