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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_potsdam.hpi.asg.delaymatch.profile.Path;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class DelayMatchModule {

    private VerilogModule                            module;
    private ProfileComponent                         profilecomp;
    private String                                   measureOutputfile;
    private Map<String, Float>                       values;
    private Map<Path, String>                        measureids;
    private Map<Path, Map<String, DelayMatchModule>> negids;
    private List<String>                             measuretcl;

    public DelayMatchModule(VerilogModule module, ProfileComponent profilecomp) {
        this.module = module;
        this.profilecomp = profilecomp;
        this.values = new HashMap<>();
        this.measureids = new HashMap<>();
        this.negids = new HashMap<>();
    }

    public void addValue(String id, Float value) {
        this.values.put(id, value);
    }

    public Float getValue(String id) {
        return values.get(id);
    }

    public void addMeasurePath(String id, Path p) {
        this.measureids.put(p, id);
    }

    public Float getMeasureValue(Path p) {
        if(!measureids.containsKey(p)) {
            return null;
        }
        String id = measureids.get(p);
        if(!values.containsKey(id)) {
            return null;
        }
        return values.get(id);
    }

    public void addNegativeMatchPath(Path p, String id, DelayMatchModule module) {
        if(!negids.containsKey(p)) {
            negids.put(p, new HashMap<String, DelayMatchModule>());
        }
        negids.get(p).put(id, module);
    }

    public List<Float> getNegativeMatchValues(Path p) {
        if(!negids.containsKey(p)) {
            return null;
        }
        List<Float> retVal = new ArrayList<>();
        Map<String, DelayMatchModule> map = negids.get(p);
        for(Entry<String, DelayMatchModule> entry : map.entrySet()) {
            Float val = entry.getValue().getValue(entry.getKey());
            if(val == null) {
                return null;
            }
            retVal.add(val);
        }
        return retVal;
    }

    public void addMeasureTclLines(List<String> lines) {
        if(measuretcl == null) {
            measuretcl = new ArrayList<>();
        }
        measuretcl.addAll(lines);
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

    public List<String> getMeasuretcl() {
        return measuretcl;
    }
}
