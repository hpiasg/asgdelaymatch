package de.uni_potsdam.hpi.asg.delaymatch.match;

/*
 * Copyright (C) 2016 - 2017 Norman Kluge
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
import java.util.Map;
import java.util.Set;

import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;

public class MatchMain {

    private String                        name;
    private RemoteInformation             rinfo;
    private Map<String, DelayMatchModule> modules;
    private String                        matchedfilename;
    private Technology                    tech;

    public MatchMain(String name, RemoteInformation rinfo, Map<String, DelayMatchModule> modules, Technology tech) {
        this.name = name;
        this.rinfo = rinfo;
        this.modules = modules;
        this.tech = tech;
    }

    public boolean match(int turnid, File vfile) {
        MatchScriptGenerator gen = MatchScriptGenerator.create(turnid, name, vfile, modules, tech);
        if(!gen.generate()) {
            return false;
        }

        matchedfilename = gen.getOutfile();

        Set<String> uploadfiles = new HashSet<>();
        uploadfiles.addAll(gen.getScriptFiles());
        uploadfiles.add(gen.getVInFile());

        List<String> execScripts = new ArrayList<>();
        execScripts.add(gen.getExec());

        MatchRemoteOperationWorkflow wf = new MatchRemoteOperationWorkflow(rinfo, "match");
        if(!wf.run(uploadfiles, execScripts)) {
            return false;
        }

        return true;
    }

    public String getMatchedfilename() {
        return matchedfilename;
    }
}
