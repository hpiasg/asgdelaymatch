package de.uni_potsdam.hpi.asg.logictool.trace.tracehelper;

/*
 * Copyright (C) 2015 Norman Kluge
 * 
 * This file is part of ASGlogic.
 * 
 * ASGlogic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGlogic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGlogic.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;

public class TraceSimulationStep {

    private State            nextState;
    private List<Transition> sequence;
    private State            start;
    private List<State>      states;

    public TraceSimulationStep() {
        sequence = new ArrayList<>();
        states = new ArrayList<>();
    }

    public void setNextState(State state) {
        this.nextState = state;
    }

    public State getNextState() {
        return nextState;
    }

    public void setStart(State start) {
        this.start = start;
    }

    public State getStart() {
        return start;
    }

    public List<Transition> getSequence() {
        return sequence;
    }

    public List<State> getStates() {
        return states;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for(State s : states) {
            if(s == null) {
                System.out.println("meop");
            }
            str.append(s.getId() + "-");
        }
        return str.toString() + "[" + nextState.getId() + "]; " + sequence.toString() + "; Start: S" + start.getId() + "";
    }
}
