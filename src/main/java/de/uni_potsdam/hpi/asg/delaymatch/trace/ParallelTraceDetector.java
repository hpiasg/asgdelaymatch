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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.delaymatch.trace.helper.TempTrace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Box;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.ParallelBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.TransitionBox;

public class ParallelTraceDetector {
    private static final Logger logger = LogManager.getLogger();

    private List<Trace>         traces;

    public ParallelTraceDetector() {
        this.traces = new ArrayList<>();
    }

    public boolean detect(SortedSet<TempTrace> tmptraces) {
        Map<Integer, List<TempTrace>> equalTransitionTraces = new HashMap<>();
        for(TempTrace trace : tmptraces) {
            int hash = trace.getTransitions().hashCode();
            if(!equalTransitionTraces.containsKey(hash)) {
                equalTransitionTraces.put(hash, new ArrayList<TempTrace>());
            }
            equalTransitionTraces.get(hash).add(trace);
        }
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
                        logger.error("Not all transitions placed!");
                        return false;
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
                            logger.error("Common sublist -1");
                            return false;
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
                                logger.error("Box is not a SequenceBox. But it should");
                                return false;
                            }
                        }
                    }
                } else {
                    Map<Transition, ParallelBox> parallelBoxes = new HashMap<>();
                    for(Transition trans : currEmpty) {
                        if(predecessors.get(trans).isEmpty()) {
                            logger.error("Parallel trans with empty predecessor");
                            return false;
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
                            logger.error("Parallel Trans with multiple predecessors");
                            return false;
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
        return true;
    }

    private List<Box> getTransitiveBox(SequenceBox start) {
        List<Box> retVal = new ArrayList<>();
        Box next = start;
        while(next != null) {
            retVal.add(next);
            next = next.getSuperBox();
        }
        return Lists.reverse(retVal);
    }

    public List<Trace> getTraces() {
        return traces;
    }
}
