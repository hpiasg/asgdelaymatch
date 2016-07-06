package de.uni_potsdam.hpi.asg.delaymatch.verilogparser;

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
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchPlan;

public class VerilogInstanceParser {

    private Set<DelayMatchPlan> modules;

    public VerilogInstanceParser(Set<DelayMatchPlan> modules) {
        this.modules = modules;
    }

    public boolean parse(File vfile) {
        List<String> lines = FileHelper.getInstance().readFile(vfile);
        if(lines == null) {
            return false;
        }

        Pattern p = null;
        Matcher m = null;
        for(DelayMatchPlan plan : modules) {
            p = Pattern.compile("\\s*" + plan.getName() + "\\s+([A-Za-z0-9]+)\\s+\\(.*");
            for(String line : lines) {
                m = p.matcher(line);
                if(m.matches()) {
                    plan.addInstance(m.group(1));
                }
            }
        }

        return true;
    }
}
