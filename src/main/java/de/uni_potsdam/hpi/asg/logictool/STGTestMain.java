package de.uni_potsdam.hpi.asg.logictool;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.logictool.rgraph.ReachabilityGraph;
import de.uni_potsdam.hpi.asg.logictool.rgraph.ReachabilityGraphComputer;
import de.uni_potsdam.hpi.asg.logictool.trace.ParallelTraceDetector;
import de.uni_potsdam.hpi.asg.logictool.trace.SequenceShrinker;
import de.uni_potsdam.hpi.asg.logictool.trace.ShortesTracesFinder;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TempTrace;
import de.uni_potsdam.hpi.asg.logictool.trace.model.Trace;

public class STGTestMain {
    private static Logger logger;

    public static void main(String[] args) {
        LoggerHelper.initLogger(0, null, true);
        logger = LogManager.getLogger();
        long start = System.currentTimeMillis();

//        String filename = "/home/norman/share/testdir/gcd_fordeco.g";
//        String startSigName = "aD_2";
//        Edge startEdge = Edge.falling;
//        String endSigName = "rD_25";
//        Edge endEdge = Edge.rising;

        String filename = "/home/norman/share/testdir/gcd_fordeco.g";
        String startSigName = "r1";
        Edge startEdge = Edge.rising;
        String endSigName = "rD_25";
        Edge endEdge = Edge.rising;

//        String filename = "/home/norman/share/testdir/parallel.g";
//        String startSigName = "a";
//        Edge startEdge = Edge.rising;
//        String endSigName = "i";
//        Edge endEdge = Edge.rising;

        // STG import
        STG stg = GFile.importFromFile(new File(filename));
        if(stg == null) {
            return;
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

        //Sequencing
        SequenceShrinker sshrink = SequenceShrinker.create(stg, startEdge, endEdge, startSig, endSig);
        if(sshrink == null) {
            return;
        }
        if(!sshrink.shrinkSequences()) {
            return;
        }
//        GFile.writeGFile(stg, new File("/home/norman/workspace/delaymatch/target/test-runs/out.g"));

        // State graph generation
        ReachabilityGraphComputer graphcomp = new ReachabilityGraphComputer(stg);
        ReachabilityGraph stateGraph = graphcomp.compute();
//        new GraphicalStateGraph(stateGraph, true, null);

        ShortesTracesFinder stfinder = new ShortesTracesFinder(stateGraph);
        SortedSet<TempTrace> tmptraces = stfinder.findTraces(startSig, startEdge, endSig, endEdge);
//        for(TempTrace tr : tmptraces) {
//            System.out.println(tr);
//        }

        ParallelTraceDetector ptd = new ParallelTraceDetector();
        if(!ptd.detect(tmptraces)) {
            return;
        }
        List<Trace> traces = ptd.getTraces();

        System.out.println(traces);

        if(!sshrink.extendSequences(traces)) {
            return;
        }

        System.out.println(traces);

        long end = System.currentTimeMillis();
        System.out.println(LoggerHelper.formatRuntime(end - start, true));
    }
}
