package de.uni_potsdam.hpi.asg.delaymatch.trace.model;

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

import java.util.ArrayList;
import java.util.List;

public class SequenceBox extends Box {

    private List<PTBox> content;

    public SequenceBox(Box superBox) {
        super(superBox);
        this.content = new ArrayList<>();
    }

    public List<PTBox> getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[S ");
        for(PTBox box : content) {
            str.append(box.toString());
        }
        str.append("]");
        return str.toString();
    }
}
