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

import java.util.HashSet;

import java.util.List;
import java.util.Set;

import de.uni_potsdam.hpi.asg.common.stg.model.Transition;

public class TempTrace {

    private List<Transition> trace;
    private Set<Transition>  transitions;

    public TempTrace(List<Transition> trace) {
        this.trace = trace;
        this.transitions = new HashSet<>(trace);
    }

    public List<Transition> getTrace() {
        return trace;
    }

    public Set<Transition> getTransitions() {
        return transitions;
    }

    @Override
    public String toString() {
        return trace.toString();
    }
}
