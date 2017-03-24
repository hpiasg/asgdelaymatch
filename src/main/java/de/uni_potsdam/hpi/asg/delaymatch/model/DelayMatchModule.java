package de.uni_potsdam.hpi.asg.delaymatch.model;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord.MeasureEdge;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord.MeasureType;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class DelayMatchModule {
    private static final Logger                       logger   = LogManager.getLogger();
    private static final int                          NOEACHID = -1;

    private VerilogModule                             module;
    private ProfileComponent                          profilecomp;
    private List<DelayMatchModuleInst>                instances;
    private String                                    measureOutputfile;
    private String                                    sdfFileName;

    private BiMap<String, MeasureRecord>              measureRecords;
    private Map<MeasureRecord, Set<DelayMatchModule>> requesters;

    private Table<MatchPath, Integer, Float>          targetValue;
    private Table<MatchPath, Integer, Float>          minValueFactor;
    private Table<MatchPath, Integer, Float>          maxValueFactor;

    public DelayMatchModule(VerilogModule module, ProfileComponent profilecomp) {
        this.module = module;
        this.profilecomp = profilecomp;
        this.instances = new ArrayList<>();

        this.measureRecords = HashBiMap.create();
        this.requesters = new HashMap<>();

        this.targetValue = HashBasedTable.create();
        this.minValueFactor = HashBasedTable.create();
        this.maxValueFactor = HashBasedTable.create();
    }

    public void setMatchVal(MatchPath path, Float val) {
        setMatchVal(path, NOEACHID, val);
    }

    public void setMatchVal(MatchPath path, Integer eachid, Float val) {
        if(!targetValue.contains(path, eachid)) {
            targetValue.put(path, eachid, val);
            minValueFactor.put(path, eachid, DelayMatchMain.matchMinStartFactor);
            maxValueFactor.put(path, eachid, DelayMatchMain.matchMaxStartFactor);
        } else {
            if(!(targetValue.get(path, eachid).equals(val))) {
                System.out.println("New val != old val: " + val + " != " + targetValue.get(path, eachid));
                //TODO: ??
                targetValue.put(path, eachid, val);
            }
            minValueFactor.put(path, eachid, minValueFactor.get(path, eachid) + DelayMatchMain.matchMinIncreaseFactor);
            maxValueFactor.put(path, eachid, maxValueFactor.get(path, eachid) + DelayMatchMain.matchMaxIncreaseFactor);
        }
    }

    public Float getControlMinVal(MatchPath path) {
        return getControlMinVal(path, NOEACHID);
    }

    public Float getControlMinVal(MatchPath path, Integer eachid) {
        float val = targetValue.get(path, eachid);
        float factor = minValueFactor.get(path, eachid);
        return val * factor;
    }

    public Float getControlMaxVal(MatchPath path) {
        return getControlMaxVal(path, NOEACHID);
    }

    public Float getControlMaxVal(MatchPath path, Integer eachid) {
        float val = targetValue.get(path, eachid);
        float factor = maxValueFactor.get(path, eachid);
        return val * factor;
    }

    public boolean addValue(String id, Float value) {
        if(measureRecords.containsKey(id)) {
            measureRecords.get(id).setValue(value);
            return true;
        }
        logger.error("Id not defined: " + id);
        return false;
    }

    public MeasureRecord getMeasureRecord(MeasureEdge fromEdge, String fromSignals, MeasureEdge toEdge, String toSignals, MeasureType type, DelayMatchModule requester) {
        String id = MeasureRecord.getID(fromEdge, fromSignals, toEdge, toSignals, type);
        if(!measureRecords.containsKey(id)) {
            MeasureRecord rec = new MeasureRecord(fromEdge, fromSignals, toEdge, toSignals, type);
            measureRecords.put(id, rec);
            requesters.put(rec, new HashSet<DelayMatchModule>());
        }
        MeasureRecord rec = measureRecords.get(id);
        requesters.get(rec).add(requester);
        return rec;
    }

    public boolean removeMeasureRecordsFromRequester(DelayMatchModule requester) {
        Set<MeasureRecord> recsToRemove = new HashSet<>();
        for(Entry<MeasureRecord, Set<DelayMatchModule>> entry : requesters.entrySet()) {
            Set<DelayMatchModule> list = entry.getValue();
            if(list.contains(requester)) {
                list.remove(requester);
                if(list.isEmpty()) {
                    recsToRemove.add(entry.getKey());
                }
            }
        }
        for(MeasureRecord rec : recsToRemove) {
            requesters.remove(rec);
            measureRecords.inverse().remove(rec);
        }
        return true;
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

    public void setSdfFileName(String sdfFileName) {
        this.sdfFileName = sdfFileName;
    }

    public String getSdfFileName() {
        return sdfFileName;
    }
}
