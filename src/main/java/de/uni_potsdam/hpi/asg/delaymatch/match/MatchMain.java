package de.uni_potsdam.hpi.asg.delaymatch.match;

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchPlan;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;

public class MatchMain {
    private static final Logger  logger      = LogManager.getLogger();

    private static final Pattern arrivalTime = Pattern.compile("\\s+data arrival time\\s+([0-9.]+)");

    private Set<DelayMatchPlan>  modules;

    public MatchMain(Set<DelayMatchPlan> modules) {
        this.modules = modules;
    }

    public boolean match() {
        return generateConstraints(new File(WorkingdirGenerator.getInstance().getWorkingdir() + "match.tcl"));
    }

    private boolean generateConstraints(File outfile) {
        StringBuilder outdata = new StringBuilder();
        List<Float> values = new ArrayList<>();

        for(DelayMatchPlan plan : modules) {
            values.clear();
            List<String> lines = FileHelper.getInstance().readFile(plan.getMeasureOutputfile());
            Matcher m = null;
            outdata.append("# " + plan.getName() + FileHelper.getNewline());

            for(String line : lines) {
                m = arrivalTime.matcher(line);
                if(m.matches()) {
                    values.add(Float.parseFloat(m.group(1)));
                }
            }

            int index = 0;
            List<String> constraints = new ArrayList<>();
            Set<String> donttouch = new HashSet<>();

            outdata.append("elaborate " + plan.getName() + " -architecture verilog -library DEFAULT" + FileHelper.getNewline());

            for(MatchPath path : plan.getProfilecomp().getMatchpaths()) {
                if(path.getForeach() != null) {
                    int num = plan.getVariables().get(path.getForeach()).getCount();
                    for(int eachid = 0; eachid < num; eachid++) {
                        if(index == values.size()) {
                            logger.error("index");
                            return false;
                        }
                        constraints.add(generateMatch(plan, path, eachid, values.get(index)));
                        donttouch.add(generateDontTouch(plan, path, eachid));
                        index++;
                    }
                } else {
                    if(index == values.size()) {
                        logger.error("index");
                        return false;
                    }
                    constraints.add(generateMatch(plan, path, null, values.get(index)));
                    donttouch.add(generateDontTouch(plan, path, null));
                    index++;
                }
            }

            for(String str : constraints) {
                outdata.append(str + FileHelper.getNewline());
            }
            for(String str : donttouch) {
                outdata.append(str + FileHelper.getNewline());
            }
            outdata.append("compile" + FileHelper.getNewline());
        }

        FileHelper.getInstance().writeFile(outfile, outdata.toString());
        return true;
    }

    private String generateMatch(DelayMatchPlan plan, MatchPath path, Integer eachid, Float value) {
        String from = PortHelper.getPortListAsString(path.getMatch().getFrom(), eachid, plan.getVariables());
        String to = PortHelper.getPortListAsString(path.getMatch().getTo(), eachid, plan.getVariables());
        return "set_min_delay -from [get_ports {" + from + "}] -to [get_ports {" + to + "}] " + value;
    }

    private String generateDontTouch(DelayMatchPlan plan, MatchPath path, Integer eachid) {
        return "set_dont_touch_network [get_ports {" + PortHelper.getPortListAsString(path.getMeasure().getFrom(), eachid, plan.getVariables()) + "}]";
    }
}
