package de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model;

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

public class VerilogSignalNormal extends VerilogSignal {

    private String    name;
    private Direction dir;
    private int       datawidth;

    public VerilogSignalNormal(String name, Direction dir) {
        this.name = name;
        this.dir = dir;
        this.datawidth = 0;
    }

    @Override
    public void setDatawidth(int datawidth) {
        this.datawidth = datawidth;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getDatawidth() {
        return datawidth;
    }

    @Override
    public String toString() {
        return name + ":" + dir + ",width:" + datawidth;
    }

    @Override
    public Direction getDirection() {
        return dir;
    }
}
