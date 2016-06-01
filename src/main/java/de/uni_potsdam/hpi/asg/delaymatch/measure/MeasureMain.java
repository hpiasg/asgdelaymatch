package de.uni_potsdam.hpi.asg.delaymatch.measure;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchPlan;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.VerilogInterfaceParser;

public class MeasureMain {

    private ProfileComponents comps;

    public MeasureMain(ProfileComponents comps) {
        this.comps = comps;
    }

    public boolean measure(File vfile) {
        Set<DelayMatchPlan> modules = new HashSet<>();
        Pattern p = Pattern.compile("module (.*) \\(.*");
        Matcher m;
        List<String> lines = FileHelper.getInstance().readFile(vfile);
        VerilogInterfaceParser parser = null;
        for(String str : lines) {
            m = p.matcher(str);
            if(m.matches()) {
                String modulename = m.group(1);
                ProfileComponent pc = comps.getComponentByRegex(modulename);
                parser = null;
                if(pc != null) {
                    parser = new VerilogInterfaceParser();
                    modules.add(new DelayMatchPlan(modulename, pc, parser.getVariables()));
                }
            }
            if(parser != null) {
                parser.addLine(str);
            }
        }

        MeasureScriptGenerator gen = MeasureScriptGenerator.create(vfile, modules);
        if(!gen.generate()) {
            return false;
        }

        return true;
    }

}
