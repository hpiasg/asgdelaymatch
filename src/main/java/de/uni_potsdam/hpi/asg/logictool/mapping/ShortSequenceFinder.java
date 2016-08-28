package de.uni_potsdam.hpi.asg.logictool.mapping;

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

import de.uni_potsdam.hpi.asg.logictool.mapping.seqhelper.IOBehaviour;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqhelper.IOBehaviourSimulationStep;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqhelper.IOBehaviourSimulationStepFactory;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqhelper.IOBehaviourSimulationStepPool;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqhelper.SequenceFrontCmp;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition.Edge;

public class ShortSequenceFinder {
    private static final Logger           logger = LogManager.getLogger();

    private StateGraph                    origsg;

    private IOBehaviourSimulationStepPool pool;
    private long                          rmSub  = 0;
    private long                          rmFall = 0;

    public ShortSequenceFinder(StateGraph stategraph) {
        this.origsg = stategraph;
    }

    public List<List<Transition>> decomposeAND(Signal startSig, Signal endSig) {
        Set<State> startStates = new HashSet<>();
        for(State s : origsg.getStates()) {
            for(Entry<Transition, State> entry2 : s.getNextStates().entrySet()) {
                if(entry2.getKey().getSignal() == startSig && entry2.getKey().getEdge() == Edge.falling) {
                    startStates.add(entry2.getValue());
                }
            }
        }

        SortedSet<IOBehaviour> sequences = new TreeSet<>(new SequenceFrontCmp());
        Deque<IOBehaviourSimulationStep> steps = new ArrayDeque<>();

        pool = new IOBehaviourSimulationStepPool(new IOBehaviourSimulationStepFactory());
        pool.setMaxTotal(-1);

        IOBehaviourSimulationStep newStep;
        for(State s : startStates) {
            try {
                newStep = pool.borrowObject();
            } catch(Exception e) {
                e.printStackTrace();
                logger.error("Could not borrow object");
                return null;
            }
            newStep.setStart(s);
            newStep.setNextState(s);
            steps.add(newStep);
        }

        if(steps.isEmpty()) {
            return null;
        }

//        long stepsEvaledTotal = 0;
        IOBehaviourSimulationStep step = null;
        while(!steps.isEmpty()) {
            step = steps.removeLast();
            getNewSteps(step, endSig, sequences, steps, startStates);
//            stepsEvaledTotal++;
//            if(stepsEvaledTotal % 100000 == 0) {
//                logger.debug("Pool: " + "Created: " + pool.getCreatedCount() + ", Borrowed: " + pool.getBorrowedCount() + ", Returned: " + pool.getReturnedCount() + ", Active: " + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
//            }
        }
        logger.debug("Pool: " + "Created: " + pool.getCreatedCount() + ", Borrowed: " + pool.getBorrowedCount() + ", Returned: " + pool.getReturnedCount() + ", Active: " + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
        logger.debug("RmSub: " + rmSub + " // RmFall: " + rmFall);

        Queue<IOBehaviour> checkQ = new LinkedList<>();
        Set<IOBehaviour> tmpSeq = new HashSet<>();
        checkQ.addAll(sequences);
        IOBehaviour check = null;
        while((check = checkQ.poll()) != null) {
            tmpSeq.clear();
            tmpSeq.addAll(sequences);
            for(IOBehaviour beh : tmpSeq) {
                if(beh == check) {
                    continue;
                }
                tail.clear();
                tail.addAll(check.getSequence());
                allcovered = true;
                tmpindex = 0;
                for(Transition t : beh.getSequence()) {
                    tmpindex = indexOfStart(tail, t, tmpindex);
                    if(tmpindex == -1) {
                        allcovered = false;
                        break;
                    }
                    tmpindex++;
                }
                if(allcovered) {
                    sequences.remove(check);
                }
            }
        }

        List<List<Transition>> sequences2 = new ArrayList<>();
        for(IOBehaviour beh : sequences) {
            sequences2.add(beh.getSequence());
        }

        return sequences2;
    }

    private List<Transition> tail = new ArrayList<>();
    private int              tmpindex;
    private boolean          allcovered;

    public int indexOfStart(List<Transition> list, Transition o, int start) {
        for(int i = start; i < list.size(); i++) {
            if(o == list.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private void getNewSteps(IOBehaviourSimulationStep step, Signal sig, Set<IOBehaviour> sequences, Deque<IOBehaviourSimulationStep> newSteps, Set<State> startStates) {

        int sum = 0;
        for(State s : startStates) {
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

        for(Entry<Transition, State> entry : step.getNextState().getNextStates().entrySet()) {
            if(entry.getKey().getSignal() == sig) {
                List<Transition> seq = new ArrayList<>(step.getSequence());
                IOBehaviour beh = new IOBehaviour(seq, step.getStart(), step.getNextState());
                sequences.add(beh);
                pool.returnObject(step);
                return;
            }
        }

        for(IOBehaviour beh : sequences) {
            tail.clear();
            tail.addAll(step.getSequence());
            allcovered = true;
            tmpindex = 0;
            for(Transition t : beh.getSequence()) {
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

        for(Entry<Transition, State> entry : step.getNextState().getNextStates().entrySet()) {
            IOBehaviourSimulationStep newStep;
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
