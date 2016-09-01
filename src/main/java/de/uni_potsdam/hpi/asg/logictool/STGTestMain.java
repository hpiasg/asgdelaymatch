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

import com.google.common.collect.Lists;

import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.logictool.srgraph.GraphicalStateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraphComputer;
import de.uni_potsdam.hpi.asg.logictool.stg.GFile;
import de.uni_potsdam.hpi.asg.logictool.stg.csc.CSCSolver;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Place;
import de.uni_potsdam.hpi.asg.logictool.stg.model.STG;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.trace.ShortesTracesFinder;
import de.uni_potsdam.hpi.asg.logictool.trace.model.Box;
import de.uni_potsdam.hpi.asg.logictool.trace.model.PTBox;
import de.uni_potsdam.hpi.asg.logictool.trace.model.ParallelBox;
import de.uni_potsdam.hpi.asg.logictool.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.logictool.trace.model.Trace;
import de.uni_potsdam.hpi.asg.logictool.trace.model.TransitionBox;
import de.uni_potsdam.hpi.asg.logictool.trace.tracehelper.TempTrace;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition.Edge;

public class STGTestMain {

    public static void main(String[] args) {
        LoggerHelper.initLogger(0, null, true);
        long start = System.currentTimeMillis();

//        String filename = "/home/norman/share/testdir/gcd_fordeco.g";
//        String startSigName = "aD_2";
//        Edge startEdge = Edge.falling;
//        String endSigName = "rD_25";
//        Edge endEdge = Edge.rising;

//        String filename = "/home/norman/share/testdir/gcd_fordeco.g";
//        String startSigName = "r1";
//        Edge startEdge = Edge.rising;
//        String endSigName = "rD_25";
//        Edge endEdge = Edge.rising;

        String filename = "/home/norman/share/testdir/parallel.g";
        String startSigName = "a";
        Edge startEdge = Edge.rising;
        String endSigName = "i";
        Edge endEdge = Edge.rising;

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

        //check parallel
        Map<Integer, List<TempTrace>> equalTransitionTraces = new HashMap<>();
        for(TempTrace trace : tmptraces) {
            int hash = trace.getTransitions().hashCode();
            if(!equalTransitionTraces.containsKey(hash)) {
                equalTransitionTraces.put(hash, new ArrayList<TempTrace>());
            }
            equalTransitionTraces.get(hash).add(trace);
        }
        List<Trace> traces = new ArrayList<>();
        for(List<TempTrace> list : equalTransitionTraces.values()) {
            Map<Transition, TransitionBox> transmap = new HashMap<>();

            if(list.size() == 1) {
                SequenceBox box = new SequenceBox(null);
                for(Transition tx : list.get(0).getTrace()) {
                    TransitionBox tb = new TransitionBox(box, tx);
                    box.getContent().add(tb);
                    transmap.put(tx, tb);
                }
                traces.add(new Trace(box, transmap));
                continue;
            }

            Map<Transition, Set<Transition>> commonPredecessors = new HashMap<>();
            Map<Transition, Set<Transition>> directPredecessors = new HashMap<>();
            Map<Transition, Set<Transition>> predecessors = new HashMap<>();
            for(Transition tx : list.get(0).getTransitions()) {
                commonPredecessors.put(tx, new HashSet<Transition>());
                for(Transition tx2 : list.get(0).getTransitions()) {
                    commonPredecessors.get(tx).add(tx2);
                }
                directPredecessors.put(tx, new HashSet<Transition>());
            }
            for(TempTrace ttrace : list) {
                for(int i = 0; i < ttrace.getTrace().size(); i++) {
                    Transition curr = ttrace.getTrace().get(i);
                    commonPredecessors.get(curr).retainAll(ttrace.getTrace().subList(0, i));
                    if(i > 0) {
                        directPredecessors.get(curr).add(ttrace.getTrace().get(i - 1));
                    }
                }
            }
            for(Transition tx : list.get(0).getTransitions()) {
                predecessors.put(tx, new HashSet<>(commonPredecessors.get(tx)));
                predecessors.get(tx).retainAll(directPredecessors.get(tx));
            }

            SequenceBox rootbox = new SequenceBox(null);
            Set<Transition> transitionsleft = new HashSet<>(list.get(0).getTransitions());
            Map<Transition, SequenceBox> boxmap = new HashMap<>();
            while(true) {
                Set<Transition> currEmpty = new HashSet<>();
                for(Transition tx : transitionsleft) {
                    if(commonPredecessors.get(tx).isEmpty()) {
                        currEmpty.add(tx);
                    }
                }

                if(currEmpty.isEmpty()) {
                    if(!transitionsleft.isEmpty()) {
                        System.out.println("Error: not all transitions placed!");
                    }
                    break;
                } else if(currEmpty.size() == 1) {
                    //sequential
                    Transition trans = currEmpty.iterator().next();
                    if(predecessors.get(trans).isEmpty()) {
                        // first
                        TransitionBox tb = new TransitionBox(rootbox, trans);
                        rootbox.getContent().add(tb);
                        boxmap.put(trans, rootbox);
                        transmap.put(trans, tb);
                    } else if(predecessors.get(trans).size() == 1) {
                        // sequential
                        SequenceBox box = boxmap.get(predecessors.get(trans).iterator().next());
                        TransitionBox tb = new TransitionBox(box, trans);
                        box.getContent().add(tb);
                        boxmap.put(trans, box);
                        transmap.put(trans, tb);
                    } else {
                        // join
                        List<List<Box>> boxhier = new ArrayList<>();
                        int min = -1;
                        for(Transition predecessor : predecessors.get(trans)) {
                            List<Box> boxlist = getTransitiveBox(boxmap.get(predecessor));
                            boxhier.add(boxlist);
                            if(min == -1) {
                                min = boxlist.size();
                            }
                            if(boxlist.size() < min) {
                                min = boxlist.size();
                            }
                        }
                        int firstuncommon = -1;
                        for(int i = 0; i < min; i++) {
                            Box value = null;
                            for(List<Box> boxes : boxhier) {
                                if(value == null) {
                                    value = boxes.get(i);
                                    continue;
                                }
                                if(value != boxes.get(i)) {
                                    firstuncommon = i;
                                }
                            }
                        }
                        if(firstuncommon == -1) {
                            System.out.println("ERROR: common sublist");
                        } else if(firstuncommon == 0) {
                            TransitionBox tb = new TransitionBox(rootbox, trans);
                            rootbox.getContent().add(tb);
                            boxmap.put(trans, rootbox);
                            transmap.put(trans, tb);
                        } else {
                            Box box = boxhier.get(0).get(firstuncommon - 2); //PBox is last common (-1) -but we need SBox in front of it (-1) = (-2)
                            if(box instanceof SequenceBox) {
                                SequenceBox sbox = (SequenceBox)box;
                                TransitionBox tb = new TransitionBox(sbox, trans);
                                sbox.getContent().add(tb);
                                boxmap.put(trans, sbox);
                                transmap.put(trans, tb);
                            } else {
                                System.out.println("ERROR: no sequencebox");
                            }
                        }
                    }
                } else {
                    Map<Transition, ParallelBox> parallelBoxes = new HashMap<>();
                    for(Transition trans : currEmpty) {
                        if(predecessors.get(trans).isEmpty()) {
                            System.out.println("ERROR: Parallel Trans with empty predecessor!");
                        } else if(predecessors.get(trans).size() == 1) {
                            Transition predecessor = predecessors.get(trans).iterator().next();
                            SequenceBox prebox = boxmap.get(predecessor);
                            if(!parallelBoxes.containsKey(predecessor)) {
                                ParallelBox pbox = new ParallelBox(prebox);
                                parallelBoxes.put(predecessor, pbox);
                                prebox.getContent().add(pbox);
                            }
                            ParallelBox pbox = parallelBoxes.get(predecessor);
                            SequenceBox sbox = new SequenceBox(pbox);
                            pbox.getParallelLines().add(sbox);
                            TransitionBox tb = new TransitionBox(sbox, trans);
                            sbox.getContent().add(tb);
                            boxmap.put(trans, sbox);
                            transmap.put(trans, tb);
                        } else {
                            System.out.println("ERROR: Parallel Trans with multiple predecessors!");
                        }
                    }
                }

                for(Transition tx : currEmpty) {
                    for(Set<Transition> set : commonPredecessors.values()) {
                        set.remove(tx);
                    }
                    transitionsleft.remove(tx);
                }
            }
            traces.add(new Trace(rootbox, transmap));
        }

        System.out.println(traces);

        //extend shrunk sequences
        for(Trace trace : traces) {
            for(Entry<Transition, SortedSet<Transition>> entry : sequences2.entrySet()) {
                if(trace.getTransitionMap().containsKey(entry.getKey())) {
                    TransitionBox tb = trace.getTransitionMap().get(entry.getKey());
                    if(!(tb.getSuperBox() instanceof SequenceBox)) {
                        System.out.println("ERROR: SuperBox of Transition is not Sequence");
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
                        System.out.println("ERROR: Index -1 should not happen");
                    }
                }

            }
        }

        System.out.println(traces);

//        for(List<Transition> seq : sequences3) {
//            System.out.println(seq);
//            for(Transition t9 : seq) {
//                System.out.println(t9);
//            }
//        }

        long end = System.currentTimeMillis();
        System.out.println(LoggerHelper.formatRuntime(end - start, true));
    }

    private static StateGraph generateMarkingGraph(STG stg) {
        SortedSet<Signal> sortedSignals = new TreeSet<Signal>();
        for(Signal sig : stg.getSignals()) {
            if(!sig.isDummy()) {
                sortedSignals.add(sig);
            }
        }
        StateGraphComputer graphcomp = new StateGraphComputer(stg, sortedSignals, null);
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

    private static List<Box> getTransitiveBox(SequenceBox start) {
        List<Box> retVal = new ArrayList<>();
        Box next = start;
        while(next != null) {
            retVal.add(next);
            next = next.getSuperBox();
        }
        return Lists.reverse(retVal);
    }
}
