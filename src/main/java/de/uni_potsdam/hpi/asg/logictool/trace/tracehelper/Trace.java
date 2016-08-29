package de.uni_potsdam.hpi.asg.logictool.trace.tracehelper;

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

import java.util.List;

import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;

public class Trace {

    private List<Transition> sequence;

    public Trace(List<Transition> sequence) {
        this.sequence = sequence;
    }

    public List<Transition> getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return sequence.toString();
    }
}
