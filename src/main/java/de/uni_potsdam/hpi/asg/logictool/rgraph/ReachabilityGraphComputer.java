package de.uni_potsdam.hpi.asg.logictool.rgraph;

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
import java.util.Set;

import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractSTGGraphComputer;

public class ReachabilityGraphComputer extends AbstractSTGGraphComputer<MarkingState> {
    public ReachabilityGraphComputer(STG stg) {
        super(MarkingState.class, stg);
    }

    public ReachabilityGraph compute() {
        if(!internalCompute(false)) {
            return null;
        }

        Set<MarkingState> states2 = new HashSet<>(states.values());
        clear();
        return new ReachabilityGraph(stg, states2);
    }
}
