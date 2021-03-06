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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Place;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.delaymatch.trace.helper.TransitionSequenceSort;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.TransitionBox;

public class SequenceShrinker {
    private static final Logger                    logger = LogManager.getLogger();

    private STG                                    stg;
    private List<SortedSet<Transition>>            sequences;
    private Map<Transition, SortedSet<Transition>> sequences2;

    private SequenceShrinker(STG stg) {
        this.stg = stg;
    }

    public static SequenceShrinker create(STG stg, Edge startEdge, Edge endEdge, Signal startSig, Signal endSig) {
        SequenceShrinker retVal = new SequenceShrinker(stg);
        if(!retVal.findSequences(startEdge, endEdge, startSig, endSig)) {
            return null;
        }
        return retVal;
    }

    private boolean findSequences(Edge startEdge, Edge endEdge, Signal startSig, Signal endSig) {
        sequences = new ArrayList<>();
        Set<Transition> alreadyInSeq = new HashSet<>();
        Queue<Transition> queue = new LinkedList<>(stg.getTransitions());
        Transition t, t2 = null;
        while((t = queue.poll()) != null) {
            Queue<Transition> queue2 = new LinkedList<>();
            queue2.add(t);
            SortedSet<Transition> newseq = new TreeSet<>(new TransitionSequenceSort(stg.getInitMarking()));
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
        return true;
    }

    public boolean shrinkSequences() {
        sequences2 = new HashMap<>();
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
        return true;
    }

    public boolean extendSequences(List<Trace> traces) {
        for(Trace trace : traces) {
            for(Entry<Transition, SortedSet<Transition>> entry : sequences2.entrySet()) {
                if(trace.getTransitionMap().containsKey(entry.getKey())) {
                    TransitionBox tb = trace.getTransitionMap().get(entry.getKey());
                    if(!(tb.getSuperBox() instanceof SequenceBox)) {
                        logger.error("SuperBox of TransitionBox is not a SequenceBox");
                        return false;
                    }
                    Set<TransitionBox> prevs = tb.getPrevs();
                    TransitionBox prev = null;
                    SequenceBox sb = (SequenceBox)tb.getSuperBox();
                    int pos = sb.getContent().indexOf(tb);
                    if(pos != -1) {
                        sb.getContent().remove(pos);
                        for(Transition t8 : entry.getValue()) {
                            TransitionBox tb1 = null;
                            if(prevs != null) {
                                tb1 = new TransitionBox(sb, t8, prevs);
                                prevs = null;
                            } else {
                                tb1 = new TransitionBox(sb, t8, prev);
                            }
                            sb.getContent().add(pos++, tb1);
                            trace.getTransitionMap().put(t8, tb1);
                            prev = tb1;
                        }
                        // fix prev
                        for(TransitionBox tbx : trace.getTransitionMap().values()) {
                            if(tbx.getPrevs().contains(tb)) {
                                tbx.getPrevs().remove(tb);
                                tbx.getPrevs().add(prev);
                            }
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
}
