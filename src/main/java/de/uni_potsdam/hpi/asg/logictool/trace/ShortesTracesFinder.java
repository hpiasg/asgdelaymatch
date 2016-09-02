package de.uni_potsdam.hpi.asg.logictool.trace;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.logictool.rgraph.MarkingState;
import de.uni_potsdam.hpi.asg.logictool.rgraph.ReachabilityGraph;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TempTrace;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TraceCmp;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TraceSimulationStep;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TraceSimulationStepFactory;
import de.uni_potsdam.hpi.asg.logictool.trace.helper.TraceSimulationStepPool;

public class ShortesTracesFinder {
    private static final Logger     logger = LogManager.getLogger();

    private ReachabilityGraph       origsg;

    private TraceSimulationStepPool pool;
    private List<Transition>        tail;
    private int                     tmpindex;
    private boolean                 allcovered;

    public ShortesTracesFinder(ReachabilityGraph stategraph) {
        this.origsg = stategraph;
        this.tail = new ArrayList<>();
    }

    public SortedSet<TempTrace> findTraces(Signal startSig, Edge startEdge, Signal endSig, Edge endEdge) {
        SortedSet<TempTrace> traces = new TreeSet<>(new TraceCmp());
        Deque<TraceSimulationStep> steps = new ArrayDeque<>();

        pool = new TraceSimulationStepPool(new TraceSimulationStepFactory());
        pool.setMaxTotal(-1);

        Set<MarkingState> startStates = findStartStatesAndCreateSteps(startSig, startEdge, steps);
        if(steps.isEmpty()) {
            return null;
        }

        long stepsEvaledTotal = 0;
        TraceSimulationStep step = null;
        while(!steps.isEmpty()) {
            step = steps.removeLast();
            getNewSteps(step, endSig, endEdge, traces, steps, startStates);
            stepsEvaledTotal++;
            if(stepsEvaledTotal % 100000 == 0) {
                logger.debug("Pool: " + "Created: " + pool.getCreatedCount() + ", Borrowed: " + pool.getBorrowedCount() + ", Returned: " + pool.getReturnedCount() + ", Active: " + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
            }
        }
        logger.debug("Pool: " + "Created: " + pool.getCreatedCount() + ", Borrowed: " + pool.getBorrowedCount() + ", Returned: " + pool.getReturnedCount() + ", Active: " + pool.getNumActive() + ", Idle: " + pool.getNumIdle());

        filterTraces(traces);
        return traces;
    }

    private void filterTraces(SortedSet<TempTrace> traces) {
        /*
         * Filter traces:
         * a-b-c-d-e
         * a-x-b-c-d-e is a subtrace of first and will be removed
         * (the result is that there are no transitions in the trace which are parallel to the start or end transition) 
         */
        Queue<TempTrace> checkQ = new LinkedList<>();
        Set<TempTrace> tmpSeq = new HashSet<>();
        checkQ.addAll(traces);
        TempTrace check = null;
        while((check = checkQ.poll()) != null) {
            tmpSeq.clear();
            tmpSeq.addAll(traces);
            for(TempTrace beh : tmpSeq) {
                if(beh == check) {
                    continue;
                }
                tail.clear();
                tail.addAll(check.getTrace());
                allcovered = true;
                tmpindex = 0;
                for(Transition t : beh.getTrace()) {
                    tmpindex = indexOfStart(tail, t, tmpindex);
                    if(tmpindex == -1) {
                        allcovered = false;
                        break;
                    }
                    tmpindex++;
                }
                if(allcovered) {
                    traces.remove(check);
                }
            }
        }
    }

    private Set<MarkingState> findStartStatesAndCreateSteps(Signal startSig, Edge startEdge, Deque<TraceSimulationStep> steps) {
        Set<MarkingState> startStates = new HashSet<>();
        TraceSimulationStep newStep = null;
        for(MarkingState s : origsg.getStates()) {
            for(Entry<Transition, MarkingState> entry2 : s.getNextStates().entrySet()) {
                if(entry2.getKey().getSignal() == startSig && entry2.getKey().getEdge() == startEdge) {
                    startStates.add(entry2.getValue());
                    try {
                        newStep = pool.borrowObject();
                    } catch(Exception e) {
                        e.printStackTrace();
                        logger.error("Could not borrow object");
                        return null;
                    }
                    newStep.setStart(entry2.getValue());
                    newStep.setNextState(entry2.getValue());
                    newStep.getSequence().add(entry2.getKey());
                    steps.add(newStep);
                }
            }
        }
        return startStates;
    }

    public int indexOfStart(List<Transition> list, Transition o, int start) {
        for(int i = start; i < list.size(); i++) {
            if(o == list.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private void getNewSteps(TraceSimulationStep step, Signal endSig, Edge endEdge, Set<TempTrace> sequences, Deque<TraceSimulationStep> newSteps, Set<MarkingState> startStates) {
        int sum = 0;
        for(MarkingState s : startStates) {
            sum += Collections.frequency(step.getStates(), s);
        }
        if(sum > 1) {
            pool.returnObject(step);
            return;
        }

        int occurrences = Collections.frequency(step.getStates(), step.getNextState());
        if(occurrences >= 2) {
            pool.returnObject(step);
            return;
        }

        for(Entry<Transition, MarkingState> entry : step.getNextState().getNextStates().entrySet()) {
            if(entry.getKey().getSignal() == endSig && entry.getKey().getEdge() == endEdge) {
                List<Transition> seq = new ArrayList<>(step.getSequence());
                seq.add(entry.getKey());
                TempTrace beh = new TempTrace(seq);
                sequences.add(beh);
                pool.returnObject(step);
                return;
            }
        }

        for(TempTrace beh : sequences) {
            tail.clear();
            tail.addAll(step.getSequence());
            allcovered = true;
            tmpindex = 0;
            for(Transition t : beh.getTrace()) {
                tmpindex = indexOfStart(tail, t, tmpindex);
                if(tmpindex == -1) {
                    allcovered = false;
                    break;
                }
                tmpindex++;
            }
            if(allcovered) {
                pool.returnObject(step);
                return;
            }
        }

        for(Entry<Transition, MarkingState> entry : step.getNextState().getNextStates().entrySet()) {
            TraceSimulationStep newStep;
            try {
                newStep = pool.borrowObject();
            } catch(Exception e) {
                e.printStackTrace();
                logger.error("Could not borrow object");
                return;
            }
            newStep.getSequence().addAll(step.getSequence());
            newStep.getSequence().add(entry.getKey());
            newStep.setStart(step.getStart());
            newStep.setNextState(entry.getValue());
            newStep.getStates().addAll(step.getStates());
            newStep.getStates().add(step.getNextState());
            newSteps.add(newStep);
        }
        pool.returnObject(step);
    }
}
