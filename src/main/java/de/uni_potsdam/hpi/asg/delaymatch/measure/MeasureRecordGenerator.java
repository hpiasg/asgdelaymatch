package de.uni_potsdam.hpi.asg.delaymatch.measure;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModuleInst;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureEntry;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureEntry.EntryType;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureEdge;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureType;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.profile.Port;
import de.uni_potsdam.hpi.asg.delaymatch.trace.TraceFinder;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.PTBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.ParallelBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.TransitionBox;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleConnection;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal.Direction;

public class MeasureRecordGenerator {
    private static final Logger                         logger = LogManager.getLogger();

    private Map<String, DelayMatchModule>               modules;
    private File                                        file;
    private VerilogModule                               rootModule;
    private Table<Transition, Transition, MeasureEntry> transtable;

    public MeasureRecordGenerator(Map<String, DelayMatchModule> modules, File file, VerilogModule rootModule) {
        this.modules = modules;
        this.file = file;
        this.rootModule = rootModule;
        this.transtable = HashBasedTable.create();
    }

    public boolean generate(boolean future, boolean past) {
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        for(int eachid = 0; eachid < num; eachid++) {
                            addMeasureAddition(mod, path, eachid);
                        }
                    } else {
                        addMeasureAddition(mod, path, null);
                    }

                    if(future || past) {
                        for(DelayMatchModuleInst inst : mod.getInstances()) {
                            if(future) {
                                if(!generateFutureRecords(inst, path)) {
                                    logger.error("Generate future substraction for " + mod.getModuleName() + "(" + inst.getInstName() + ") failed");
                                    return false;
                                }
                            }
                            if(past) {
                                if(!generatePastRecords(inst, path)) {
                                    logger.error("Generate past substraction for " + mod.getModuleName() + "(" + inst.getInstName() + ") failed");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean generatePastRecords(DelayMatchModuleInst dminst, MatchPath path) {
        VerilogModuleInstance inst = dminst.getVerilogModuleInst();

        // End signals
        List<String> endSigNames = new ArrayList<>();
        Set<VerilogSignal> startsigs = new HashSet<>();
        for(Port p : path.getMatch().getFrom()) {
            startsigs.addAll(p.getCorrespondingSignals(dminst.getDMmodule().getVerilogModule()));
        }
        for(VerilogSignal sig : startsigs) {
            VerilogModuleConnection con = inst.getConnections().get(sig);
            if(con == null) {
                return false;
            }
            endSigNames.add(con.getWriterSig().getName());
        }

        // Connected modules (dp)
        List<VerilogModuleInstance> dpModules = new ArrayList<>();
        Set<VerilogSignal> dpsigs = new HashSet<>();
        for(Port p : path.getMeasure().getFrom()) {
            dpsigs.addAll(p.getCorrespondingSignals(dminst.getDMmodule().getVerilogModule()));
        }
        for(VerilogSignal sig : dpsigs) {
            VerilogModuleConnection con = inst.getConnections().get(sig);
            if(con == null) {
                return false;
            }
            dpModules.add(con.getWriter());
        }

        // End sigs
        List<String> startSigNames = new ArrayList<>();
        final Pattern sigNamePattern = Pattern.compile(".+_\\d+");
        for(VerilogModuleInstance dpinst : dpModules) {
            for(Entry<VerilogSignal, VerilogModuleConnection> entry : dpinst.getConnections().entrySet()) {
                VerilogSignal sig = entry.getKey();
                if(sig.getDirection() == Direction.output) {
                    VerilogModuleConnection con = entry.getValue();
                    for(VerilogSignal readerSig : con.getReader().values()) {
                        Matcher m = sigNamePattern.matcher(readerSig.getName());
                        if(m.matches()) {
                            startSigNames.add(readerSig.getName());
                            break;
                        }
                    }
                }
            }
        }

//        System.out.println("----------");
//        System.out.println(dminst.getDMmodule().getModuleName());
//        System.out.println(dminst.getInstName());
//        System.out.println(endSigNames);
//        System.out.println(dpModules);
//        System.out.println(startSigNames);

        // Traces
        TraceFinder tf = new TraceFinder(file);
        for(String start : startSigNames) {
            for(String end : endSigNames) {
                dminst.addPastSubstractionTraces(path, tf.find(start, Edge.falling, end, Edge.rising));
//                dminst.addPastSubstractionTraces(path, tf.find("r1", Edge.rising, end, Edge.rising));
            }
        }

        for(Trace tr : dminst.getPastSubstrationTraces(path)) {
            if(!generateMeasures(tr.getTrace())) {
                return false;
            }
            System.out.println("------");
        }

        return true;
    }

    private static final Pattern dpSigPattern = Pattern.compile("([ra])[A-Z]([0-9]*)_([0-9]+)");
    private static final Pattern hsSigPattern = Pattern.compile("([ra])([0-9]+)");

    private boolean generateMeasures(SequenceBox box) {
        Matcher m = null, m2 = null;
        for(PTBox inner : box.getContent()) {
            if(inner instanceof TransitionBox) {
                TransitionBox tinner = (TransitionBox)inner;
                for(TransitionBox prev : tinner.getPrevs()) {
                    DelayMatchModule mod = findModule(prev.getTransition().getSignal(), tinner.getTransition().getSignal());
                    if(mod != null) {
                        if(!createMeasureRecord(tinner, prev, mod)) {
                            return false;
                        }
                        continue;
                    }
                    // no module found
                    // check dp
                    m = dpSigPattern.matcher(tinner.getTransition().getSignal().getName());
                    m2 = dpSigPattern.matcher(prev.getTransition().getSignal().getName());
                    if(m.matches() && m2.matches()) {
                        if(!m.group(1).equals(m2.group(1)) && m.group(3).equals(m2.group(3))) { // one must be r, one a && same HS component id
                            transtable.put(prev.getTransition(), tinner.getTransition(), new MeasureEntry(EntryType.datapathDelay));
//                            System.out.println("## DP: " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName());
                            continue;
                        }
                    }
                    // external check
                    m = hsSigPattern.matcher(tinner.getTransition().getSignal().getName());
                    m2 = hsSigPattern.matcher(prev.getTransition().getSignal().getName());
                    if(m.matches() && m2.matches()) {
                        if(!m.group(1).equals(m2.group(1)) && m.group(2).equals(m2.group(2))) { // one must be r, one a && same HS channel
                            transtable.put(prev.getTransition(), tinner.getTransition(), new MeasureEntry(EntryType.externalDelay));
//                            System.out.println("## External: " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName());
                            continue;
                        }
                    }
                    //TODO: internal signals (-> jump over) 

                    logger.warn("Module for " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName() + " not found");
                }
            } else if(inner instanceof ParallelBox) {
                ParallelBox pinner = (ParallelBox)inner;
                for(SequenceBox sb : pinner.getParallelLines()) {
                    if(!generateMeasures(sb)) {
                        return false;
                    }
                }
            }
        }
        return true;

    }

    private boolean createMeasureRecord(TransitionBox curr, TransitionBox prev, DelayMatchModule mod) {
        MeasureEdge fromEdge = convertEdge(prev.getTransition().getEdge());
        String fromSignals = prev.getTransition().getSignal().getName();
        MeasureEdge toEdge = convertEdge(curr.getTransition().getEdge());
        String toSignals = curr.getTransition().getSignal().getName();
        MeasureRecord rec = new MeasureRecord(fromEdge, fromSignals, toEdge, toSignals, MeasureType.min);
        mod.addMeasureRecord(rec);
        transtable.put(prev.getTransition(), curr.getTransition(), new MeasureEntry(rec));
        return true;
    }

    private DelayMatchModule findModule(Signal input, Signal output) {
        for(VerilogModuleInstance inst : rootModule.getSubmodules()) {
            VerilogSignal inpSig = inst.getModule().getSignal(input.getName());
            VerilogSignal outSig = inst.getModule().getSignal(output.getName());
            if(inpSig != null && outSig != null) {
                if(inpSig.getDirection() == Direction.input && outSig.getDirection() == Direction.output) {
//                    System.out.println(inst.getModule().getModulename() + ": " + input.getName() + ";" + output.getName());
                    return modules.get(inst.getModule().getModulename());
                }
            }
        }
        return null;
    }

    private MeasureEdge convertEdge(Edge edge) {
        switch(edge) {
            case falling:
                return MeasureEdge.falling;
            case rising:
                return MeasureEdge.rising;
        }
        logger.warn("Unregcognised edge");
        return MeasureEdge.both;
    }

    private boolean generateFutureRecords(DelayMatchModuleInst dminst, MatchPath path) {
        Set<VerilogSignal> sigs = new HashSet<>();
        for(Port p : path.getMatch().getTo()) {
            sigs.addAll(p.getCorrespondingSignals(dminst.getDMmodule().getVerilogModule()));
        }
        VerilogModuleInstance inst = dminst.getVerilogModuleInst();
        for(VerilogSignal sig : sigs) {
            VerilogModuleConnection con = inst.getConnections().get(sig);
            if(con == null) {
                return false;
            }
            Map<VerilogModuleInstance, VerilogSignal> others = con.getReader();
            if(others == null) {
                return false;
            }
            for(Entry<VerilogModuleInstance, VerilogSignal> otherinst : others.entrySet()) {
                String othermodulename = otherinst.getKey().getModule().getModulename();
                StringBuilder to = new StringBuilder();
                for(VerilogSignal sig2 : otherinst.getKey().getModule().getSignals().values()) {
                    if(sig2.getDirection() == Direction.output) {
                        to.append(sig2.getName() + " ");
                    }
                }
                to.setLength(to.length() - 1);
                DelayMatchModule othermodule = modules.get(othermodulename);
                MeasureRecord rec = new MeasureRecord(MeasureEdge.both, otherinst.getValue().getName(), MeasureEdge.both, to.toString(), MeasureType.min);
                othermodule.addMeasureRecord(rec);
                dminst.addFutureSubstraction(path, rec);
            }
        }
        return true;
    }

    private void addMeasureAddition(DelayMatchModule mod, MatchPath path, Integer eachid) {
        String from = PortHelper.getPortListAsDCString(path.getMeasure().getFrom(), eachid, mod.getSignals());
        String to = PortHelper.getPortListAsDCString(path.getMeasure().getTo(), eachid, mod.getSignals());
        MeasureRecord rec = new MeasureRecord(MeasureEdge.both, from, MeasureEdge.both, to, MeasureType.max);
        mod.addMeasureRecord(rec);
        for(DelayMatchModuleInst inst : mod.getInstances()) {
            inst.addMeasureAddition(path, rec);
        }
    }

    public Table<Transition, Transition, MeasureEntry> getTransTable() {
        return transtable;
    }
}
