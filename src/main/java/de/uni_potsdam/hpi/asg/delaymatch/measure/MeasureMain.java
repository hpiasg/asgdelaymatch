package de.uni_potsdam.hpi.asg.delaymatch.measure;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.misc.MeasureEntry;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;

public class MeasureMain {
    private static final Logger                         logger      = LogManager.getLogger();

    private static final Pattern                        arrivalTime = Pattern.compile("\\s+data arrival time\\s+([0-9.]+)");
    private static final Pattern                        pathSpec    = Pattern.compile("ASGdm;(.*);");

    private RemoteInformation                           rinfo;
    private Map<String, DelayMatchModule>               modules;
    private boolean                                     future;
    private File                                        stgFile;
    private VerilogModule                               rootModule;
    private Technology                                  tech;

    private Table<Transition, Transition, MeasureEntry> transtable;

    public MeasureMain(RemoteInformation rinfo, Map<String, DelayMatchModule> modules, VerilogModule rootModule, boolean future, File stgFile, Technology tech) {
        this.rinfo = rinfo;
        this.modules = modules;
        this.future = future;
        this.rootModule = rootModule;
        this.stgFile = stgFile;
        this.tech = tech;
    }

    public boolean measure(File vfile) {
        MeasureRecordGenerator rec = new MeasureRecordGenerator(modules, stgFile, rootModule);
        if(!rec.generate(future, stgFile != null)) {
            return false;
        }
        this.transtable = rec.getTransTable();

        MeasureScriptGenerator gen = MeasureScriptGenerator.create(vfile, modules, tech);
        if(!gen.generate()) {
            return false;
        }

        Set<String> uploadfiles = new HashSet<>();
        uploadfiles.addAll(gen.getScriptFiles());
        uploadfiles.add(vfile.getAbsolutePath());

        if(!run(uploadfiles, gen.getExec())) {
            return false;
        }

        if(!parseValues()) {
            return false;
        }

        return true;
    }

    private boolean parseValues() {
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getMeasureOutputfile() != null) {
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

    public Table<Transition, Transition, MeasureEntry> getTransTable() {
        return transtable;
    }
}
