package de.uni_potsdam.hpi.asg.delaymatch.remote;

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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;

public class MeasureMain {
    private static final Logger           logger = LogManager.getLogger();

    private RemoteInformation             rinfo;
    private Map<String, DelayMatchModule> modules;
    private Technology                    tech;
    private String                        name;
    private File                          workingDir;

    private long                          lastTime;

    public MeasureMain(String name, RemoteInformation rinfo, Map<String, DelayMatchModule> modules, Technology tech) {
        this.name = name;
        this.rinfo = rinfo;
        this.modules = modules;
        this.tech = tech;
        this.workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
    }

    public boolean measure(int turnid, File vFile) {
        File dcShFile = new File(workingDir, turnid + "_" + name + "_measure.sh");
        File dcTclFile = new File(workingDir, turnid + "_" + name + "_measure.tcl");
        File logFile = new File(workingDir, turnid + "_" + name + "_measure.log");

        for(DelayMatchModule mod : modules.values()) {
            if(!mod.getMeasureRecords().isEmpty()) {
                File f = new File(workingDir, turnid + "_" + name + "_" + mod.getModuleName() + "_measure.log");
                mod.setMeasureOutputFile(f);
            }
        }

        MeasureScript script = new MeasureScript(rinfo, "measure", workingDir, tech, modules);
        if(!script.generate(dcShFile, dcTclFile, vFile, logFile)) {
            logger.error("Generating Measure script failed");
            return false;
        }
        if(!script.execute()) {
            logger.error("Execute Measure script failed");
            return false;
        }
        if(!script.getResult()) {
            logger.error("Execute Measure script failed. Error: " + script.getResultErrorMsg());
            return false;
        }

        lastTime = script.getTime();

        return true;
    }

    public long getLastTime() {
        return lastTime;
    }
}
