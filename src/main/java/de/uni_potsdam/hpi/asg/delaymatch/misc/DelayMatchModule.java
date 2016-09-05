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

import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.profile.Path;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class DelayMatchModule {
    private static final Logger                 logger = LogManager.getLogger();

    private VerilogModule                       module;
    private ProfileComponent                    profilecomp;
    private String                              measureOutputfile;
    private Map<String, MeasureRecord>          measureRecords;

    private Map<MatchPath, MeasureRecord>       measureAdditions;
    private Map<MatchPath, List<MeasureRecord>> futureSubtractions;

    public DelayMatchModule(VerilogModule module, ProfileComponent profilecomp) {
        this.module = module;
        this.profilecomp = profilecomp;
        this.measureRecords = new HashMap<>();
        this.measureAdditions = new HashMap<>();
        this.futureSubtractions = new HashMap<>();
    }

    public boolean addValue(String id, Float value) {
        if(measureRecords.containsKey(id)) {
            measureRecords.get(id).setValue(value);
            return true;
        }
        logger.error("Id not defined: " + id);
        return false;
    }

    public void addMeasureRecord(MeasureRecord rec) {
        measureRecords.put(rec.getId(), rec);
    }

    public void addMeasureAddition(MatchPath p, MeasureRecord rec) {
        measureAdditions.put(p, rec);
    }

    public void addFutureSubstraction(MatchPath p, MeasureRecord rec) {
        if(!futureSubtractions.containsKey(p)) {
            futureSubtractions.put(p, new ArrayList<MeasureRecord>());
        }
        futureSubtractions.get(p).add(rec);
    }

    public Map<String, MeasureRecord> getMeasureRecords() {
        return Collections.unmodifiableMap(measureRecords);
    }

    public Map<MatchPath, List<MeasureRecord>> getFutureSubtractions() {
        return Collections.unmodifiableMap(futureSubtractions);
    }

    public Map<MatchPath, MeasureRecord> getMeasureAdditions() {
        return Collections.unmodifiableMap(measureAdditions);
    }

    public String getName() {
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

    public VerilogModule getVerilogModule() {
        return module;
    }
}
