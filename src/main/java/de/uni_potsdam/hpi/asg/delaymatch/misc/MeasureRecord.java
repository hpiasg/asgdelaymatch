package de.uni_potsdam.hpi.asg.delaymatch.misc;

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

public class MeasureRecord {

    public enum MeasureEdge {
        rising, falling, both
    }

    public enum MeasureType {
        min, max
    }

    private MeasureEdge fromEdge;
    private String      fromSignals;
    private MeasureEdge toEdge;
    private String      toSignals;
    private MeasureType type;

    private String      id;
    private Float       value;

    public static String getID(MeasureEdge fromEdge, String fromSignals, MeasureEdge toEdge, String toSignals, MeasureType type) {
        return type.toString() + "_from_" + fromEdge + "_" + fromSignals.replace(" ", "_") + "_to_" + toEdge + "_" + toSignals.replace(" ", "_");
    }

    public MeasureRecord(MeasureEdge fromEdge, String fromSignals, MeasureEdge toEdge, String toSignals, MeasureType type) {
        this.type = type;
        this.fromEdge = fromEdge;
        this.fromSignals = fromSignals;
        this.toEdge = toEdge;
        this.toSignals = toSignals;
        this.id = MeasureRecord.getID(fromEdge, fromSignals, toEdge, toSignals, type);
    }

    public MeasureEdge getFromEdge() {
        return fromEdge;
    }

    public String getFromSignals() {
        return fromSignals;
    }

    public MeasureEdge getToEdge() {
        return toEdge;
    }

    public String getToSignals() {
        return toSignals;
    }

    public MeasureType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }
}
