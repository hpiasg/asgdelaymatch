package de.uni_potsdam.hpi.asg.delaymatch;

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

import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.Variable;

public class DelayMatchPlan {

    private String                name;
    private ProfileComponent      profilecomp;
    private Map<String, Variable> variables;
    private String                measureOutputfile;
    private Set<String>           instances;

    public DelayMatchPlan(String name, ProfileComponent profilecomp, Map<String, Variable> variables) {
        this.name = name;
        this.profilecomp = profilecomp;
        this.variables = variables;
        this.instances = new HashSet<>();
    }

    public boolean addInstance(String instance) {
        return this.instances.add(instance);
    }

    public Set<String> getInstances() {
        return instances;
    }

    public String getName() {
        return name;
    }

    public ProfileComponent getProfilecomp() {
        return profilecomp;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public void setMeasureOutputfile(String measureOutputfile) {
        this.measureOutputfile = measureOutputfile;
    }

    public String getMeasureOutputfile() {
        return measureOutputfile;
    }
}
