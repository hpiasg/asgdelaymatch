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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.Place;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraphComputer;
import de.uni_potsdam.hpi.asg.logictool.trace.ParallelTraceDetector;
import de.uni_potsdam.hpi.asg.logictool.trace.ShortesTracesFinder;
import de.uni_potsdam.hpi.asg.logictool.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.logictool.trace.model.Trace;
import de.uni_potsdam.hpi.asg.logictool.trace.model.TransitionBox;
import de.uni_potsdam.hpi.asg.logictool.trace.tracehelper.TempTrace;

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
        List<SortedSet<Transition>> sequences = findSequences(stg, startEdge, endEdge, startSig, endSig);
        Map<Transition, SortedSet<Transition>> sequences2 = shrinkSequences(stg, sequences);
        System.out.println("Seq: " + sequences2);
//        GFile.writeGFile(stg, new File("/home/norman/workspace/delaymatch/target/test-runs/out.g"));

        // State graph generation
        StateGraph stateGraph = generateMarkingGraph(stg);
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

        if(!extendSequences(sequences2, traces)) {
            return;
        }

        System.out.println(traces);

        long end = System.currentTimeMillis();
        System.out.println(LoggerHelper.formatRuntime(end - start, true));
    }

    private static boolean extendSequences(Map<Transition, SortedSet<Transition>> sequences2, List<Trace> traces) {
        for(Trace trace : traces) {
            for(Entry<Transition, SortedSet<Transition>> entry : sequences2.entrySet()) {
                if(trace.getTransitionMap().containsKey(entry.getKey())) {
                    TransitionBox tb = trace.getTransitionMap().get(entry.getKey());
                    if(!(tb.getSuperBox() instanceof SequenceBox)) {
                        logger.error("SuperBox of TransitionBox is not a SequenceBox");
                        return false;
                    }
                    SequenceBox sb = (SequenceBox)tb.getSuperBox();
                    int pos = sb.getContent().indexOf(tb);
                    if(pos != -1) {
                        sb.getContent().remove(pos);
                        for(Transition t8 : entry.getValue()) {
                            TransitionBox tb1 = new TransitionBox(sb, t8);
                            sb.getContent().add(pos++, tb1);
                            trace.getTransitionMap().put(t8, tb1);
                        }
                    } else {
                        logger.error("Index -1 should not happen");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static StateGraph generateMarkingGraph(STG stg) {
        SortedSet<Signal> sortedSignals = new TreeSet<Signal>();
        for(Signal sig : stg.getSignals()) {
            if(!sig.isDummy()) {
                sortedSignals.add(sig);
            }
        }
        StateGraphComputer graphcomp = new StateGraphComputer(stg, sortedSignals);
        StateGraph stateGraph = graphcomp.compute();
        return stateGraph;
    }

    private static Map<Transition, SortedSet<Transition>> shrinkSequences(STG stg, List<SortedSet<Transition>> sequences) {
        Map<Transition, SortedSet<Transition>> sequences2 = new HashMap<>();
        for(SortedSet<Transition> seq : sequences) {
            Transition first = seq.first();
            Transition last = seq.last();
            if(first == last) {
                continue;
            }
            first.getPostset().clear();
            first.getPostset().addAll(last.getPostset());
            for(Transition t3 : seq) {
                if(t3 == first) {
                    continue;
                }
                Place p = t3.getPreset().get(0);
                t3.getPreset().clear();
                t3.getSignal().getTransitions().remove(t3);
                stg.getTransitions().remove(t3);
                stg.getPlaces().remove(p.getId());
            }
            sequences2.put(first, seq);
        }
        return sequences2;
    }

    private static List<SortedSet<Transition>> findSequences(STG stg, Edge startEdge, Edge endEdge, Signal startSig, Signal endSig) {
        List<SortedSet<Transition>> sequences = new ArrayList<>();
        Set<Transition> alreadyInSeq = new HashSet<>();
        Queue<Transition> queue = new LinkedList<>(stg.getTransitions());
        Transition t, t2 = null;
        while((t = queue.poll()) != null) {
            Queue<Transition> queue2 = new LinkedList<>();
            queue2.add(t);
            SortedSet<Transition> newseq = new TreeSet<>(new TransitionTraceSort());
            while((t2 = queue2.poll()) != null) {
                if((t2.getSignal() == startSig && t2.getEdge() == startEdge) || (t2.getSignal() == endSig && t2.getEdge() == endEdge)) {
                    continue;
                }
                if(alreadyInSeq.contains(t2)) {
                    continue;
                }
                if(t2.getPostset().size() == 1 && t2.getPreset().size() == 1) {
                    newseq.add(t2);
                    alreadyInSeq.add(t2);
                    queue.remove(t2);
                    Place post = t2.getPostset().get(0);
                    if(!stg.getInitMarking().contains(post) && post.getPreset().size() == 1 && post.getPostset().size() == 1) {
                        queue2.addAll(post.getPostset());
                    }
                    Place pre = t2.getPreset().get(0);
                    if(!stg.getInitMarking().contains(pre) && pre.getPreset().size() == 1 && pre.getPostset().size() == 1) {
                        queue2.addAll(pre.getPreset());
                    }
                }
            }
            if(!newseq.isEmpty()) {
                sequences.add(newseq);
            }
        }
        return sequences;
    }
}
