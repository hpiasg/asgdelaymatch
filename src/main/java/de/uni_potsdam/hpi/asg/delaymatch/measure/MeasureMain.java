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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_potsdam.hpi.asg.common.io.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchPlan;

public class MeasureMain {
    private RemoteInformation   rinfo;
    private Set<DelayMatchPlan> modules;

    public MeasureMain(RemoteInformation rinfo, Set<DelayMatchPlan> modules) {
        this.rinfo = rinfo;
        this.modules = modules;
    }

    public boolean measure(File vfile) {

        MeasureScriptGenerator gen = MeasureScriptGenerator.create(vfile, modules);
        if(!gen.generate()) {
            return false;
        }

        Set<String> uploadfiles = new HashSet<>();
        uploadfiles.addAll(gen.getScriptFiles());
        uploadfiles.add(vfile.getAbsolutePath());

        if(!run(uploadfiles, gen.getExec())) {
            return false;
        }

        return true;
    }

    private boolean run(Set<String> uploadfiles, String exec) {
        List<String> execScripts = new ArrayList<>();
        execScripts.add(exec);

        MeasureRemoteOperationWorkflow wf = new MeasureRemoteOperationWorkflow(rinfo, "measure");
        if(!wf.run(uploadfiles, execScripts)) {
            return false;
        }

        return true;
    }
}
