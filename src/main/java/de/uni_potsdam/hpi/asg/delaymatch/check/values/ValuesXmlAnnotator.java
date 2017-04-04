package de.uni_potsdam.hpi.asg.delaymatch.check.values;

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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXml;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlEach;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlModule;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlPath;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class ValuesXmlAnnotator {
    private static final Logger           logger = LogManager.getLogger();

    private Map<String, DelayMatchModule> modules;

    public ValuesXmlAnnotator(Map<String, DelayMatchModule> modules) {
        this.modules = modules;
    }

    public boolean annotate(File valfile) {
        ValuesXml valxml = ValuesXmlFile.readIn(valfile);
        if(valxml == null) {
            return false;
        }

        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                ValuesXmlModule valmod = valxml.getModule(mod);
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    ValuesXmlPath valpath = valmod.getPath(path);
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        for(int eachid = 0; eachid < num; eachid++) {
                            ValuesXmlEach valeach = valpath.getEach(Integer.toString(eachid));
//                            for(DelayMatchModuleInst inst : mod.getInstances()) {
//                                ValuesXmlInstance valinst = valeach.getInstance(inst);
//                            }
                            mod.setFactors(path, eachid, valeach.getMinValueFactor(), valeach.getMaxValueFactor());
                        }
                    } else {
                        ValuesXmlEach valeach = valpath.getEach(ValuesXmlEach.NOEACHID);
                        mod.setFactors(path, valeach.getMinValueFactor(), valeach.getMaxValueFactor());
                    }
                }
            }
        }

        return true;
    }
}
