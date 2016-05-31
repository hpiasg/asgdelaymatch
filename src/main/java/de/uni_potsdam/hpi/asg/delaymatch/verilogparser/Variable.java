package de.uni_potsdam.hpi.asg.delaymatch.verilogparser;

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

public class Variable {

    private String name;
    private int    count;
    private int    datawidth;

    public Variable(String name) {
        this.name = name;
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

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public int getDatawidth() {
        return datawidth;
    }

    @Override
    public String toString() {
        return name + ", count:" + count + ", width:" + datawidth;
    }
}
