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
import de.uni_potsdam.hpi.asg.common.io.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;

public class MatchMain {
    private static final Logger   logger      = LogManager.getLogger();

    private static final Pattern  arrivalTime = Pattern.compile("\\s+data arrival time\\s+([0-9.]+)");
    private static final Pattern  pathSpec    = Pattern.compile("ASGdm;(.*);");

    private RemoteInformation     rinfo;
    private Set<DelayMatchModule> modules;
    private String                matchedfilename;

    public MatchMain(RemoteInformation rinfo, Set<DelayMatchModule> modules) {
        this.rinfo = rinfo;
        this.modules = modules;
    }

    public boolean match(File vfile) {
        if(!parseValues()) {
            return false;
        }

        MatchScriptGenerator gen = MatchScriptGenerator.create(vfile, modules);
        if(!gen.generate()) {
            return false;
        }
        matchedfilename = gen.getOutfile();

        Set<String> uploadfiles = new HashSet<>();
        uploadfiles.addAll(gen.getScriptFiles());
        uploadfiles.add(vfile.getAbsolutePath());

        List<String> execScripts = new ArrayList<>();
        execScripts.add(gen.getExec());

        MatchRemoteOperationWorkflow wf = new MatchRemoteOperationWorkflow(rinfo, "match");
        if(!wf.run(uploadfiles, execScripts)) {
            return false;
        }

        return true;
    }

    private boolean parseValues() {
        for(DelayMatchModule mod : modules) {
            List<String> lines = FileHelper.getInstance().readFile(mod.getMeasureOutputfile());
            if(lines == null) {
                return false;
            }
            Matcher m = null;
            String currSpec = null;
            for(String line : lines) {
                m = pathSpec.matcher(line);
                if(m.matches()) {
                    currSpec = m.group(1);
                    continue;
                }
                m = arrivalTime.matcher(line);
                if(m.matches()) {
                    if(currSpec == null) {
                        logger.error("No spec?");
                        return false;
                    }
                    mod.addValue(currSpec, Float.parseFloat(m.group(1)));
                    currSpec = null;
                }
            }
        }
        return true;
    }

    public String getMatchedfilename() {
        return matchedfilename;
    }
}
