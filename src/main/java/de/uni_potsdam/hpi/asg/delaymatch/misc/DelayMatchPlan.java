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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class DelayMatchPlan {

    private VerilogModule    module;
    private ProfileComponent profilecomp;
    private String           measureOutputfile;
    private Set<String>      instances;
    private List<Float>      values;

    public DelayMatchPlan(VerilogModule module, ProfileComponent profilecomp) {
        this.module = module;
        this.profilecomp = profilecomp;
        this.instances = new HashSet<>();
        this.values = new ArrayList<>();
    }

    public boolean addValue(Float value) {
        return this.values.add(value);
    }

    public boolean addInstance(String instance) {
        return this.instances.add(instance);
    }

    public Set<String> getInstances() {
        return instances;
    }

    public List<Float> getValues() {
        return values;
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
}
