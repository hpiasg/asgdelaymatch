package de.uni_potsdam.hpi.asg.delaymatch.trace;

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

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.delaymatch.trace.helper.TempTrace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.rgraph.ReachabilityGraph;
import de.uni_potsdam.hpi.asg.delaymatch.trace.rgraph.ReachabilityGraphComputer;

public class TraceFinder {

    private File file;

    public TraceFinder(File file) {
        this.file = file;
    }

    public List<Trace> find(String startSigName, Edge startEdge, String endSigName, Edge endEdge) {
        // STG import
        STG stg = GFile.importFromFile(file);
        if(stg == null) {
            return null;
        }

        //find signals
        Signal startSig = null;
        Signal endSig = null;
        for(Signal sig : stg.getSignals()) {
            if(sig.getName().equals(startSigName)) {
                startSig = sig;
            } else if(sig.getName().equals(endSigName)) {
                endSig = sig;
            }
        }

        // Sequencing
        SequenceShrinker sshrink = SequenceShrinker.create(stg, startEdge, endEdge, startSig, endSig);
        if(sshrink == null) {
            return null;
        }
        if(!sshrink.shrinkSequences()) {
            return null;
        }

        // State graph generation
        ReachabilityGraphComputer graphcomp = new ReachabilityGraphComputer(stg);
        ReachabilityGraph stateGraph = graphcomp.compute();

        // TmpTraces
        ShortesTracesFinder stfinder = new ShortesTracesFinder(stateGraph);
        SortedSet<TempTrace> tmptraces = stfinder.findTraces(startSig, startEdge, endSig, endEdge);

        // Parallel detection
        ParallelTraceDetector ptd = new ParallelTraceDetector();
        if(!ptd.detect(tmptraces)) {
            return null;
        }
        List<Trace> traces = ptd.getTraces();

        // Sequencing
        if(!sshrink.extendSequences(traces)) {
            return null;
        }

        return traces;
    }
}
