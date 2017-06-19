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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;

public class MatchMain {
    private static final Logger           logger = LogManager.getLogger();

    private String                        name;
    private RemoteInformation             rinfo;
    private Map<String, DelayMatchModule> modules;
    private Technology                    tech;
    private File                          workingDir;
    private String                        rootModule;

    private File                          lastVFile;
    private File                          lastSdfFile;
    private long                          lastTime;

    public MatchMain(String name, RemoteInformation rinfo, Map<String, DelayMatchModule> modules, Technology tech, String rootModule) {
        this.name = name;
        this.rinfo = rinfo;
        this.modules = modules;
        this.tech = tech;
        this.workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
        this.rootModule = rootModule;
    }

    public boolean match(int turnid, File vInFile) {
        File dcShFile = new File(workingDir, turnid + "_" + name + "_match.sh");
        File dcTclFile = new File(workingDir, turnid + "_" + name + "_match.tcl");
        File logFile = new File(workingDir, turnid + "_" + name + "_match.log");
        File vOutFile = new File(workingDir, turnid + "_" + name + "_match.v");
        File sdfOutFile = new File(workingDir, turnid + "_" + name + "_match.sdf");

        Map<DelayMatchModule, File> subLogFiles = new HashMap<>();
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                subLogFiles.put(mod, new File(workingDir, turnid + "_" + name + "_" + mod.getModuleName() + "_match.log"));
            }
        }

        MatchScript script = new MatchScript(rinfo, "match", workingDir, tech, modules);
        if(!script.generate(dcShFile, dcTclFile, vInFile, vOutFile, sdfOutFile, logFile, subLogFiles, rootModule)) {
            logger.error("Generating Match script failed");
            return false;
        }
        if(!script.execute()) {
            logger.error("Execute Match script failed");
            return false;
        }
        if(!script.getResult()) {
            logger.error("Execute Match script failed. Error: " + script.getResultErrorMsg());
            return false;
        }

        lastTime = script.getTime();
        lastVFile = vOutFile;
        lastSdfFile = sdfOutFile;

        return true;
    }

    public long getLastTime() {
        return lastTime;
    }

    public File getLastSdfFile() {
        return lastSdfFile;
    }

    public File getLastVFile() {
        return lastVFile;
    }
}
