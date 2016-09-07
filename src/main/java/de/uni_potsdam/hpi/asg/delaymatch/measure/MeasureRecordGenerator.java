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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModuleInst;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureEdge;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureType;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.profile.Port;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleConnection;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal.Direction;

public class MeasureRecordGenerator {
    private static final Logger           logger = LogManager.getLogger();

    private Map<String, DelayMatchModule> modules;

    public MeasureRecordGenerator(Map<String, DelayMatchModule> modules) {
        this.modules = modules;
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
                                    return false;
                                }
                            }
                            if(past) {
                                if(!generatePastRecords(mod, path)) {
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

    private boolean generatePastRecords(DelayMatchModule mod, MatchPath path) {

        return true;
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
}
