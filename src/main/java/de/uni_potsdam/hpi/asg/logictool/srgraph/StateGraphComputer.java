package de.uni_potsdam.hpi.asg.logictool.srgraph;

/*
 * Copyright (C) 2014 - 2016 Norman Kluge
 * 
 * This file is part of ASGlogic.
 * 
 * ASGlogic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGlogic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGlogic.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Place;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State.Value;

import gnu.trove.map.hash.THashMap;

public class StateGraphComputer {
    private static final Logger logger        = LogManager.getLogger();

    private static final long   showThreshold = 100;

    private STG                 stg;
    private SortedSet<Signal>   sortedSignals;
    private State               init;

    //tmp
    private Set<Transition>     activatedTrans;
    private Map<BitSet, State>  states;
    private boolean             enabledForFiring;
    private boolean             allact;
    private BitSet              mid;
    private State               state;
    private boolean             newState;
    private boolean             currentlyNotActivated;
    private int                 x;
    private SimulationStep      newStep;
    private int                 numsteps;
    private Queue<State>        checkstates;

    private SimulationStepPool  pool;

    public StateGraphComputer(STG stg, SortedSet<Signal> sortedSignals) {
        this.stg = stg;
        this.sortedSignals = sortedSignals;
    }

    public StateGraph compute() {
        Queue<SimulationStep> steps = new LinkedList<SimulationStep>();
        List<SimulationStep> newSteps = new ArrayList<SimulationStep>();
        states = new THashMap<BitSet, State>();
        activatedTrans = new HashSet<Transition>();

        checkstates = new LinkedList<>();

        pool = new SimulationStepPool(new SimulationStepFactory());
        pool.setMaxTotal(-1);
        numsteps = 0;

        List<Place> marking = new ArrayList<Place>();
        marking.addAll(stg.getInitMarking());
        init = getNewSteps(marking, newSteps, null, null);
        steps.addAll(newSteps);

        long prevSize = 0;
        SimulationStep step = null;
        while(!steps.isEmpty()) {
            newSteps.clear();
            step = steps.poll();

            marking.clear();
            marking.addAll(step.getMarking());
            fire(marking, step.getFireTrans());
            getNewSteps(marking, newSteps, step.getFireTrans(), step.getState());
            pool.returnObject(step);
            steps.addAll(newSteps);

            if(states.size() >= (prevSize + showThreshold)) {
                logger.info("States: " + states.size() + " - Transitions to evaluate: " + newSteps.size());
                prevSize = states.size();
            }
        }
        logger.debug("Pool: " + pool.getCreatedCount() + " // Steps: " + numsteps);

        Set<State> states2 = new HashSet<State>();
        states2.addAll(states.values());

        logger.info("Number of states: " + states2.size());

//		new Graph(new StateGraph(stg, init, states2, sortedSignals), true, null);

        clear();
        return new StateGraph(stg, init, states2, sortedSignals);
    }

    private void fire(List<Place> marking, Transition fireTrans) {
        enabledForFiring = true;
        for(Place p : fireTrans.getPreset()) {
            if(!marking.contains(p)) {
                enabledForFiring = false;
                break;
            }
        }
        if(enabledForFiring) {
            marking.removeAll(fireTrans.getPreset());
            marking.addAll(fireTrans.getPostset());
            return;
        } else {
            logger.error("Transition not enabled");
            return;
        }
    }

    private State getNewSteps(List<Place> marking, List<SimulationStep> newSteps, Transition firedTrans, State prevState) {
        // check wich transitions are activated in this marking
        activatedTrans.clear();
        allact = true;
        for(Place p : marking) {
            for(Transition t : p.getPostset()) {
                allact = true;
                for(Place p2 : t.getPreset()) {
                    if(!marking.contains(p2)) {
                        allact = false;
                        break;
                    }
                }
                if(allact) {
                    activatedTrans.add(t);
                }
            }
        }

        // known state?
        mid = getMarkingId(marking);
        state = null;
        newState = false;
        if(states.containsKey(mid)) {
            state = states.get(mid);
        } else {
            state = new State();
            states.put(mid, state);
            newState = true;
        }
//		System.out.println("State " + state.toStringSimple() + ": " + newState);

        if(newState) {
            // compute new simulation steps
            for(Transition t : activatedTrans) {
                try {
                    newStep = pool.borrowObject();
                } catch(Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                newStep.addPlaces(marking);
                newStep.setFireTrans(t);
                newStep.setState(state);
                newSteps.add(newStep);
                numsteps++;

                // set activated trans
                if(!t.getSignal().isDummy()) {
                    state.setSignalState(t.getSignal(), (t.getEdge() == Edge.rising) ? Value.rising : Value.falling);
                }
            }
        }

        if(firedTrans != null) {
            // self trigger
            if(!firedTrans.getSignal().isDummy()) {
                currentlyNotActivated = true;
                for(Transition t : activatedTrans) {
                    if(t.getSignal() == firedTrans.getSignal()) {
                        currentlyNotActivated = false;
                        break;
                    }
                }
                if(currentlyNotActivated) {
                    state.setSignalState(firedTrans.getSignal(), (firedTrans.getEdge() == Edge.rising) ? Value.high : Value.low);
                }
            }
        }

        if(prevState != null) {
            // get state signals from prev state
            for(Entry<Signal, Value> entry : prevState.getStateValues().entrySet()) {
                currentlyNotActivated = true;
                for(Transition t : activatedTrans) {
                    if(t.getSignal() == entry.getKey()) {
                        currentlyNotActivated = false;
                        break;
                    }
                }
                if(currentlyNotActivated) {
                    switch(entry.getValue()) {
                        case high:
                        case low:
                            state.setSignalState(entry.getKey(), entry.getValue());
                            break;
                        case falling:
                            if(entry.getKey() != firedTrans.getSignal()) {
                                state.setSignalState(entry.getKey(), Value.high);
                            }
                            break;
                        case rising:
                            if(entry.getKey() != firedTrans.getSignal()) {
                                state.setSignalState(entry.getKey(), Value.low);
                            }
                            break;
                    }
                }
            }

            // set state signals for prev state
            for(Transition t : activatedTrans) {
                if(!t.getSignal().isDummy()) {
                    checkstates.add(prevState);
                    State s = null;
                    while(!checkstates.isEmpty()) {
                        s = checkstates.poll();
                        if(!s.isSignalSet(t.getSignal())) {
                            //System.out.println(s.toString() + " Set: " + t.getSignal());
                            s.setSignalState(t.getSignal(), (t.getEdge() == Edge.rising) ? Value.low : Value.high);
                            checkstates.addAll(s.getPrevStates());
                        }
                    }
                }
            }

            // arcs
            if(firedTrans != null) {
                prevState.addEdgeNextState(state, firedTrans);
            }
        }

        return state;
    }

    private void clear() {
        activatedTrans = null;
        pool.clear();
        pool = null;
        states = null;
        //System.gc();
    }

    private BitSet getMarkingId(List<Place> marking) {
        x = 0;
        BitSet retVal = new BitSet(stg.getPlaces().size());
        for(Entry<String, Place> entry : stg.getPlaces().entrySet()) {
            if(marking.contains(entry.getValue())) {
                retVal.set(x);
            }
            x++;
        }
        //System.out.println("#" + marking.toString() + ": " + retVal.toString());
        return retVal;
    }
}
