package de.uni_potsdam.hpi.asg.delaymatch.check;

/*
 * Copyright (C) 2017 Norman Kluge
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModuleInst;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class Constraints {
    private static final Logger                 logger = LogManager.getLogger();

    private Map<String, DelayMatchModule>       modules;
    private Map<DelayMatchModule, List<String>> constraints;

    public Constraints(Map<String, DelayMatchModule> modules) {
        this.modules = modules;
        this.constraints = new HashMap<>();
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                constraints.put(mod, new ArrayList<String>());
            }
        }
    }

    public boolean generate(File file) {
        if(!updateConstraints()) {
            return false;
        }
        return writeOut(file);
    }

    private boolean updateConstraints() {
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                constraints.get(mod).clear();
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        for(int eachid = 0; eachid < num; eachid++) {
                            Float min = mod.getControlMinVal(path, eachid);
                            Float max = mod.getControlMaxVal(path, eachid);
                            generateSdc(mod, path, eachid, min, max);
                        }
                    } else {
                        Float min = mod.getControlMinVal(path);
                        Float max = mod.getControlMaxVal(path);
                        generateSdc(mod, path, null, min, max);
                    }
                }
            }
        }
        return true;
    }

    private void generateSdc(DelayMatchModule mod, MatchPath path, Integer eachid, Float min, Float max) {
        for(DelayMatchModuleInst inst : mod.getInstances()) {
            String from = PortHelper.getPortListAsDCString(path.getMatch().getFrom(), eachid, mod.getSignals(), inst.getInstName());
            String to = PortHelper.getPortListAsDCString(path.getMatch().getTo(), eachid, mod.getSignals(), inst.getInstName());
            constraints.get(mod).add("set_min_delay -from [get_pins { " + from + " }] -to [get_pins { " + to + " }] " + min.toString());
            constraints.get(mod).add("set_max_delay -from [get_pins { " + from + " }] -to [get_pins { " + to + " }] " + max.toString());
        }
    }

    private boolean writeOut(File file) {
        List<String> content = new ArrayList<>();
        content.add("set sdc_version 1.8");
        content.add("set_units -time ns -resistance kOhm -capacitance pF -voltage V -current mA");
        for(List<String> lines : constraints.values()) {
            content.addAll(lines);
        }
        return FileHelper.getInstance().writeFile(file, content);
    }
}
