package de.uni_potsdam.hpi.asg.delaymatch.setup;

/*
 * Copyright (C) 2016 - 2017 Norman Kluge
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
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModuleInst;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureEntry;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureEntry.EntryType;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord.MeasureEdge;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord.MeasureType;
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

    public boolean generate(boolean future, boolean past, boolean check) {
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
                            generateMeasures(future, past, check, mod, path, eachid);
                        }
                    } else {
                        generateMeasures(future, past, check, mod, path, null);
                    }

                }
            }
        }
        return true;
    }

    private boolean generateMeasures(boolean future, boolean past, boolean check, DelayMatchModule mod, MatchPath path, Integer eachid) {
        addMeasureAddition(mod, path, eachid, mod);
        for(DelayMatchModuleInst inst : mod.getInstances()) {
            if(future) {
                if(!generateFutureRecords(inst, path, eachid)) {
                    logger.error("Generate future subtraction for " + mod.getModuleName() + "(" + inst.getInstName() + ") failed");
                    return false;
                }
            }
            if(past) {
                if(!generatePastRecords(inst, path, eachid)) {
                    logger.error("Generate past subtraction for " + mod.getModuleName() + "(" + inst.getInstName() + ") failed");
                    return false;
                }
            }
            if(check) {
                if(!generateCheckRecords(inst, path, eachid)) {
                    logger.error("Generate check subtraction for " + mod.getModuleName() + "(" + inst.getInstName() + ") failed");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean generateCheckRecords(DelayMatchModuleInst dminst, MatchPath path, Integer eachid) {
        // Start signals
        List<String> startSigNames = new ArrayList<>();
        Set<VerilogSignal> startsigs = new HashSet<>();
        for(Port p : path.getMatch().getFrom()) {
            startsigs.addAll(p.getCorrespondingSignals(dminst.getDMmodule().getVerilogModule()));
        }
        for(VerilogSignal sig : startsigs) {
            startSigNames.add(sig.getName());
        }

        // End signals
        List<String> endSigNames = new ArrayList<>();
        Set<VerilogSignal> endSigs = new HashSet<>();
        for(Port p : path.getMatch().getTo()) {
            endSigs.addAll(p.getCorrespondingSignals(dminst.getDMmodule().getVerilogModule()));
        }
        for(VerilogSignal sig : endSigs) {
            endSigNames.add(sig.getName());
        }

        for(String start : startSigNames) {
            for(String end : endSigNames) {
                MeasureRecord rec = dminst.getDMmodule().getMeasureRecord(MeasureEdge.both, start, MeasureEdge.both, end, MeasureType.min, dminst.getDMmodule());
                dminst.addCheckSubtraction(path, eachid, rec);
            }
        }

//        System.out.println(inst.getModule().getModulename() + ", " + inst.getInstName());
//        System.out.println(startSigNames.toString());
//        System.out.println(endSigNames.toString());
        return true;
    }

    private boolean generatePastRecords(DelayMatchModuleInst dminst, MatchPath path, Integer eachid) {
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

//        // FetchHack
//        boolean fetchhackpossible = true;
//        List<VerilogModuleInstance> fetchHackDpModules = new ArrayList<>();
//        for(VerilogModuleInstance vmi : dpModules) {
//            if(vmi.getModule().getModulename().contains("Fetch")) {
//                for(VerilogModuleConnection con : vmi.getConnections().values()) {
//                    if(con.getWriter() == vmi) {
//                        continue;
//                    }
//                    if(con.getWriter() == null) { //external
//                        fetchhackpossible = false;
//                        break;
//                    }
//                    fetchHackDpModules.add(con.getWriter());
//                }
//            }
//        }
//        if(fetchhackpossible) {
//            dpModules.addAll(fetchHackDpModules);
//        }

        // Start sigs
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
                dminst.addPastSubtractionTraces(path, eachid, tf.find(start, Edge.falling, end, Edge.rising));
//                dminst.addPastSubstractionTraces(path, tf.find("r1", Edge.rising, end, Edge.rising));
            }
        }

        if(dminst.getPastSubtrationTraces(path, eachid) == null) {
            return true;
        }

        for(Trace tr : dminst.getPastSubtrationTraces(path, eachid)) {
            if(!generateMeasures(tr.getTrace(), dminst.getDMmodule())) {
                return false;
            }
        }

        return true;
    }

    private static final Pattern dpSigPattern = Pattern.compile("([ra])[A-Z]([0-9]*)_([0-9]+)");
    private static final Pattern hsSigPattern = Pattern.compile("([ra])([0-9]+)");

    private boolean generateMeasures(SequenceBox box, DelayMatchModule requester) {
        Matcher m = null, m2 = null;
        for(PTBox inner : box.getContent()) {
            if(inner instanceof TransitionBox) {
                TransitionBox tinner = (TransitionBox)inner;
                for(TransitionBox prev : tinner.getPrevs()) {
                    DelayMatchModule mod = findModule(prev.getTransition().getSignal(), tinner.getTransition().getSignal());
                    if(mod != null) {
                        if(!createMeasureRecord(tinner, prev, mod, requester)) {
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
                            if(transtable.contains(prev.getTransition(), tinner.getTransition())) {
                                if(transtable.get(prev.getTransition(), tinner.getTransition()).getType() != EntryType.datapathDelay) {
                                    logger.error("Entry type contradiction");
                                    return false;
                                }
                            } else {
                                transtable.put(prev.getTransition(), tinner.getTransition(), new MeasureEntry(EntryType.datapathDelay));
                            }
//                            System.out.println("## DP: " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName());
                            continue;
                        }
                    }
                    // external check
                    m = hsSigPattern.matcher(tinner.getTransition().getSignal().getName());
                    m2 = hsSigPattern.matcher(prev.getTransition().getSignal().getName());
                    if(m.matches() && m2.matches()) {
                        if(!m.group(1).equals(m2.group(1)) && m.group(2).equals(m2.group(2))) { // one must be r, one a && same HS channel
                            if(transtable.contains(prev.getTransition(), tinner.getTransition())) {
                                if(transtable.get(prev.getTransition(), tinner.getTransition()).getType() != EntryType.externalDelay) {
                                    logger.error("Entry type contradiction");
                                    return false;
                                }
                            } else {
                                transtable.put(prev.getTransition(), tinner.getTransition(), new MeasureEntry(EntryType.externalDelay));
                            }
//                            System.out.println("## External: " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName());
                            continue;
                        }
                    }

                    //TODO: internal signals (-> jump over) 
                    logger.warn("Module for " + prev.getTransition().getSignal().getName() + " > " + tinner.getTransition().getSignal().getName() + " not found");
                    if(transtable.contains(prev.getTransition(), tinner.getTransition())) {
                        if(transtable.get(prev.getTransition(), tinner.getTransition()).getType() != EntryType.unknown) {
                            logger.error("Entry type contradiction");
                            return false;
                        }
                    } else {
                        transtable.put(prev.getTransition(), tinner.getTransition(), new MeasureEntry(EntryType.unknown));
                    }
                }
            } else if(inner instanceof ParallelBox) {
                ParallelBox pinner = (ParallelBox)inner;
                for(SequenceBox sb : pinner.getParallelLines()) {
                    if(!generateMeasures(sb, requester)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean createMeasureRecord(TransitionBox curr, TransitionBox prev, DelayMatchModule mod, DelayMatchModule requester) {
        if(transtable.contains(prev.getTransition(), curr.getTransition())) {
            if(transtable.get(prev.getTransition(), curr.getTransition()).getType() != EntryType.recordDelay) {
                logger.error("Entry type contradiction");
                return false;
            }
        } else {
            MeasureEdge fromEdge = convertEdge(prev.getTransition().getEdge());
            String fromSignals = prev.getTransition().getSignal().getName();
            MeasureEdge toEdge = convertEdge(curr.getTransition().getEdge());
            String toSignals = curr.getTransition().getSignal().getName();
            MeasureRecord rec = mod.getMeasureRecord(fromEdge, fromSignals, toEdge, toSignals, MeasureType.min, requester);
            transtable.put(prev.getTransition(), curr.getTransition(), new MeasureEntry(rec));
        }
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

    private boolean generateFutureRecords(DelayMatchModuleInst dminst, MatchPath path, Integer eachid) {
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
                MeasureRecord rec = othermodule.getMeasureRecord(MeasureEdge.both, otherinst.getValue().getName(), MeasureEdge.both, to.toString(), MeasureType.min, dminst.getDMmodule());
                dminst.addFutureSubtraction(path, eachid, rec);
            }
        }
        return true;
    }

    private void addMeasureAddition(DelayMatchModule mod, MatchPath path, Integer eachid, DelayMatchModule requester) {
        String from = PortHelper.getPortListAsDCString(path.getMeasure().getFrom(), eachid, mod.getSignals());
        String to = PortHelper.getPortListAsDCString(path.getMeasure().getTo(), eachid, mod.getSignals());
        MeasureRecord rec = mod.getMeasureRecord(MeasureEdge.both, from, MeasureEdge.both, to, MeasureType.max, requester);
        for(DelayMatchModuleInst inst : mod.getInstances()) {
            inst.addMeasureAddition(path, eachid, rec);
        }
    }

    public Table<Transition, Transition, MeasureEntry> getTransTable() {
        return transtable;
    }
}
