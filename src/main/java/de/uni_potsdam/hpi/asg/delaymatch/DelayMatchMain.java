package de.uni_potsdam.hpi.asg.delaymatch;

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

import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.iohelper.Zipper;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.delaymatch.io.Config;
import de.uni_potsdam.hpi.asg.delaymatch.io.ConfigFile;
import de.uni_potsdam.hpi.asg.delaymatch.io.RemoteInvocation;
import de.uni_potsdam.hpi.asg.delaymatch.match.MatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.measure.MeasureMain;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.misc.EligibleModuleFinder;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.VerilogParser;

public class DelayMatchMain {
    private static Logger                       logger;
    private static DelayMatchCommandlineOptions options;
    public static Config                        config;

    public static float                         matchMaxFactor = 1.1f;

    public static void main(String[] args) {
        int status = main2(args);
        System.exit(status);
    }

    public static int main2(String[] args) {
        try {
            long start = System.currentTimeMillis();
            int status = -1;
            options = new DelayMatchCommandlineOptions();
            if(options.parseCmdLine(args)) {
                logger = LoggerHelper.initLogger(options.getOutputlevel(), options.getLogfile(), options.isDebug());
                logger.debug("Args: " + Arrays.asList(args).toString());
                config = ConfigFile.readIn(options.getConfigfile());
                if(config == null) {
                    logger.error("Could not read config");
                    return 1;
                }
                WorkingdirGenerator.getInstance().create(options.getWorkingdir(), config.workdir, "delaywork", null);
                status = execute();
                zipWorkfile();
//                WorkingdirGenerator.getInstance().delete();
            }
            long end = System.currentTimeMillis();
            if(logger != null) {
                logger.info("Runtime: " + LoggerHelper.formatRuntime(end - start, false));
            }
            return status;
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("An error occurred: " + e.getLocalizedMessage());
            return 1;
        }
    }

    private static int execute() {
        ProfileComponents comps = ProfileComponents.readIn(options.getProfilefile());
        if(comps == null) {
            return 1;
        }

        RemoteInvocation rinv = config.toolconfig.designCompilerCmd;
        if(rinv == null) {
            return 1;
        }
        RemoteInformation rinfo = new RemoteInformation(rinv.hostname, rinv.username, rinv.password, rinv.workingdir);

        VerilogParser vparser = new VerilogParser();
        if(!vparser.parseVerilogStructure(options.getVfile())) {
            return 1;
        }

//        VerilogModule rootModule = vparser.getRootModule();
//        new VerilogGraph(rootModule, true, null);

        EligibleModuleFinder find = new EligibleModuleFinder(comps);
        Map<String, DelayMatchModule> modules = find.find(vparser.getModules());
        if(modules == null) {
            return 1;
        }

        logger.info("Measure phase");
        MeasureMain memain = new MeasureMain(rinfo, modules, vparser.getRootModule(), options.isFuture(), options.getSTGfile());
        if(!memain.measure(options.getVfile())) {
            return 1;
        }

        logger.info("Match phase");
        MatchMain mamain = new MatchMain(rinfo, modules, memain.getTransTable());
        if(!mamain.match(options.getVfile())) {
            return 1;
        }

        if(!FileHelper.getInstance().copyfile(mamain.getMatchedfilename(), options.getOutfile())) {
            return 1;
        }

        return 0;
    }

    private static boolean zipWorkfile() {
        if(options.getWorkfile() != null) {
            if(!Zipper.getInstance().zip(options.getWorkfile())) {
                logger.warn("Could not zip temp files");
                return false;
            }
        } else {
            logger.warn("No zip outfile");
            return false;
        }
        return true;
    }
}