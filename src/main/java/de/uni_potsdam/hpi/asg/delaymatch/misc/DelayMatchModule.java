package de.uni_potsdam.hpi.asg.delaymatch.misc;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureEdge;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureRecord.MeasureType;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class DelayMatchModule {
    private static final Logger        logger = LogManager.getLogger();

    private VerilogModule              module;
    private ProfileComponent           profilecomp;
    private List<DelayMatchModuleInst> instances;
    private String                     measureOutputfile;
    private Map<String, MeasureRecord> measureRecords;

    public DelayMatchModule(VerilogModule module, ProfileComponent profilecomp) {
        this.module = module;
        this.profilecomp = profilecomp;
        this.measureRecords = new HashMap<>();
        this.instances = new ArrayList<>();
    }

    public boolean addValue(String id, Float value) {
        if(measureRecords.containsKey(id)) {
            measureRecords.get(id).setValue(value);
            return true;
        }
        logger.error("Id not defined: " + id);
        return false;
    }

    public MeasureRecord getMeasureRecord(MeasureEdge fromEdge, String fromSignals, MeasureEdge toEdge, String toSignals, MeasureType type) {
        String id = MeasureRecord.getID(fromEdge, fromSignals, toEdge, toSignals, type);
        if(!measureRecords.containsKey(id)) {
            measureRecords.put(id, new MeasureRecord(fromEdge, fromSignals, toEdge, toSignals, type));
        }
        return measureRecords.get(id);
    }

    public Map<String, MeasureRecord> getMeasureRecords() {
        return Collections.unmodifiableMap(measureRecords);
    }

    public String getModuleName() {
        return module.getModulename();
    }

    public ProfileComponent getProfilecomp() {
        return profilecomp;
    }

    public Map<String, VerilogSignal> getSignals() {
        return module.getSignals();
    }

    public Map<String, VerilogSignalGroup> getSignalGroups() {
        return module.getSignalGroups();
    }

    public void setMeasureOutputfile(String measureOutputfile) {
        this.measureOutputfile = measureOutputfile;
    }

    public String getMeasureOutputfile() {
        return measureOutputfile;
    }

    public List<DelayMatchModuleInst> getInstances() {
        return instances;
    }

    public boolean addInstance(DelayMatchModuleInst inst) {
        return this.instances.add(inst);
    }

    public VerilogModule getVerilogModule() {
        return module;
    }
}
