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

import de.uni_potsdam.hpi.asg.common.io.LoggerHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.ShortSequenceFinder;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraphComputer;
import de.uni_potsdam.hpi.asg.logictool.stg.GFile;
import de.uni_potsdam.hpi.asg.logictool.stg.csc.CSCSolver;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Place;
import de.uni_potsdam.hpi.asg.logictool.stg.model.STG;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;

public class STGTestMain {

    public static void main(String[] args) {
        LoggerHelper.initLogger(0, null, true);
        long start = System.currentTimeMillis();

        // STG import
        STG stg = GFile.importFromFile(new File("/home/norman/share/testdir/gcd_fordeco.g"));
        if(stg == null) {
            return;
        }

        //dummify
        Signal startSig = null;
        Signal endSig = null;
        List<Signal> sigs = new ArrayList<>(stg.getSignals());
        for(Signal sig : sigs) {
            if(sig.getName().equals("aD_2")) {
                // Start
                startSig = sig;
                List<Transition> trans = new ArrayList<>(sig.getTransitions());
                for(Transition t : trans) {
                    switch(t.getEdge()) {
                        case falling:
                            break;
                        case rising:
                            stg.addSignal("aD_2_tmp", SignalType.internal);
                            Signal s2 = stg.getSignal("aD_2_tmp");
                            t.setSignal(s2);
                            sig.getTransitions().remove(t);
                            break;
                    }
                }
            } else if(sig.getName().equals("rD_25")) {
                // Ende
                endSig = sig;
                List<Transition> trans = new ArrayList<>(sig.getTransitions());
                for(Transition t : trans) {
                    switch(t.getEdge()) {
                        case falling:
                            stg.addSignal("rD_25_tmp", SignalType.internal);
                            Signal s2 = stg.getSignal("rD_25_tmp");
                            t.setSignal(s2);
                            sig.getTransitions().remove(t);
                            break;
                        case rising:
                            break;
                    }
                }
            } else {
//                sig.dummify();
            }
        }

        //sequencing
        List<SortedSet<Transition>> sequences = new ArrayList<>();
        Set<Transition> alreadyInSeq = new HashSet<>();
        Queue<Transition> queue = new LinkedList<>(stg.getTransitions());
        Transition t, t2 = null;
        while((t = queue.poll()) != null) {
            Queue<Transition> queue2 = new LinkedList<>();
            queue2.add(t);
            SortedSet<Transition> newseq = new TreeSet<>(new TransitionSequenceSort());
            while((t2 = queue2.poll()) != null) {
                if(t2.getSignal() == startSig || t2.getSignal() == endSig) {
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
        System.out.println(sequences2);

        GFile.writeGFile(stg, new File("/home/norman/workspace/delaymatch/target/test-runs/out.g"));

        SortedSet<Signal> sortedSignals = new TreeSet<Signal>();
        for(Signal sig : stg.getSignals()) {
            if(!sig.isDummy()) {
                sortedSignals.add(sig);
            }
        }

        CSCSolver cscsolver = null;

        // State graph generation
        StateGraphComputer graphcomp = new StateGraphComputer(stg, sortedSignals, cscsolver);
        StateGraph stateGraph = graphcomp.compute();
        if(stateGraph == null) {
            return;
        }
//        new GraphicalStateGraph(stateGraph, true, null);

        ShortSequenceFinder comp = new ShortSequenceFinder(stateGraph);
        List<List<Transition>> sequences3 = comp.decomposeAND(startSig, endSig);

        for(List<Transition> seq : sequences3) {
            for(Entry<Transition, SortedSet<Transition>> entry : sequences2.entrySet()) {
                int pos = seq.indexOf(entry.getKey());
                if(pos != -1) {
                    seq.remove(pos);
                    for(Transition t8 : entry.getValue()) {
                        seq.add(pos++, t8);
                    }
                }
            }
        }

        for(List<Transition> seq : sequences3) {
            System.out.println(seq);
            for(Transition t9 : seq) {
                System.out.println(t9);
            }
        }

        long end = System.currentTimeMillis();
        System.out.println(LoggerHelper.formatRuntime(end - start, true));
    }
}
