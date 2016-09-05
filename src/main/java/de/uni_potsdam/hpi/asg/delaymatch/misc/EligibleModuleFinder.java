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

import java.util.HashMap;
import java.util.Map;

import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;

public class EligibleModuleFinder {

    private ProfileComponents comps;

    public EligibleModuleFinder(ProfileComponents comps) {
        this.comps = comps;
    }

    public Map<String, DelayMatchModule> find(Map<String, VerilogModule> modules) {
        Map<String, DelayMatchModule> retVal = new HashMap<>();
        for(VerilogModule module : modules.values()) {
            ProfileComponent pc = comps.getComponentByRegex(module.getModulename());
//            if(pc != null) {
            retVal.put(module.getModulename(), new DelayMatchModule(module, pc));
//            }
        }
        return retVal;
    }
}
