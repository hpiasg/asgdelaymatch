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

import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal.Direction;

public class VerilogSignalGroup {

    private String    groupname;
    private Direction dir;
    private int       count;
    private int       datawidth;

    public VerilogSignalGroup(String groupname, Direction dir) {
        this.groupname = groupname;
        this.dir = dir;
        this.count = 1;
        this.datawidth = 0;
    }

    public void setCountWithId(int id) {
        if(this.count < (id + 1)) {
            this.count = id + 1;
        }
    }

    public void setDatawidth(int datawidth) {
        this.datawidth = datawidth;
    }

    public String getGroupName() {
        return groupname;
    }

    public int getCount() {
        return count;
    }

    public int getDatawidth() {
        return datawidth;
    }

    @Override
    public String toString() {
        return "Group:" + groupname + "(" + dir + "), count:" + count + ", width:" + datawidth;
    }

    public Direction getDirection() {
        return dir;
    }
}
