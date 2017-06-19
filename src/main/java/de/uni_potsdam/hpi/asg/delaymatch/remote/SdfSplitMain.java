package de.uni_potsdam.hpi.asg.delaymatch.remote;

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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModuleInst;

public class SdfSplitMain {
    private static final Logger           logger = LogManager.getLogger();

    private RemoteInformation             rinfo;
    private Map<String, DelayMatchModule> modules;
    private Technology                    tech;
    private String                        name;
    private File                          workingDir;
    private String                        rootModule;

    private File                          lastSdfFile;
    private long                          lastTime;

    public SdfSplitMain(String name, RemoteInformation rinfo, Map<String, DelayMatchModule> modules, Technology tech, String rootModule) {
        this.name = name;
        this.rinfo = rinfo;
        this.modules = modules;
        this.tech = tech;
        this.workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
        this.rootModule = rootModule;
    }

    public boolean split(int turnid, File sdfFile, File vFile, File sdcFile) {
        File dcShFile = new File(workingDir, turnid + "_" + name + "_split.sh");
        File dcTclFile = new File(workingDir, turnid + "_" + name + "_split.tcl");
        File logFile = new File(workingDir, turnid + "_" + name + "_split.log");

        File sdfInFile = new File(workingDir, turnid + "_" + name + "_split_in.sdf");
        boolean generateSdf = false;
        if(sdfFile == null) {
            generateSdf = true;
        } else {
            if(!FileHelper.getInstance().copyfile(sdfFile, sdfInFile)) {
                return false;
            }
        }

        Map<DelayMatchModuleInst, File> subSdfFiles = new HashMap<>();
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getInstances().isEmpty()) {
                continue;
            }
            DelayMatchModuleInst inst = mod.getInstances().get(0);
            File file = new File(workingDir, turnid + "_" + name + "_split_" + inst.getInstName() + ".sdf");
            subSdfFiles.put(inst, file);
            mod.setSdfFile(file);
        }

        SdfSplitScript script = new SdfSplitScript(rinfo, "split", workingDir, tech);
        if(!script.generate(dcShFile, dcTclFile, vFile, sdcFile, generateSdf, sdfInFile, subSdfFiles, logFile, rootModule)) {
            logger.error("Generating Sdf split script failed");
            return false;
        }
        if(!script.execute()) {
            logger.error("Execute Sdf split script failed");
            return false;
        }
        if(!script.getResult()) {
            logger.error("Execute Sdf split script failed. Error: " + script.getResultErrorMsg());
            return false;
        }

        lastSdfFile = sdfInFile;
        lastTime = script.getTime();

        return true;
    }

    public File getLastSdfFile() {
        return lastSdfFile;
    }

    public long getLastTime() {
        return lastTime;
    }
}
