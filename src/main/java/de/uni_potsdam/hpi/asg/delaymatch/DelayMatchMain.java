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

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper.Mode;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.iohelper.Zipper;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.ReadTechnologyHelper;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.check.CheckMain;
import de.uni_potsdam.hpi.asg.delaymatch.io.Config;
import de.uni_potsdam.hpi.asg.delaymatch.io.ConfigFile;
import de.uni_potsdam.hpi.asg.delaymatch.io.RemoteInvocation;
import de.uni_potsdam.hpi.asg.delaymatch.match.MatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.measure.MeasureMain;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.setup.EligibleModuleFinder;
import de.uni_potsdam.hpi.asg.delaymatch.setup.MeasureRecordGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.setup.sdf.SplitSdfMain;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.VerilogParser;

public class DelayMatchMain {

    public static final String                  DEF_CONFIG_FILE_NAME   = "delaymatchconfig.xml";
    public static final File                    DEF_CONFIG_FILE        = new File(CommonConstants.DEF_CONFIG_DIR_FILE, DEF_CONFIG_FILE_NAME);

    private static Logger                       logger;
    private static DelayMatchCommandlineOptions options;
    public static Config                        config;

    public static float                         matchMinStartFactor    = 1.0f;
    public static float                         matchMinIncreaseFactor = 0.1f;
    public static float                         matchMaxStartFactor    = 1.1f;
    public static float                         matchMaxIncreaseFactor = 0.2f;

    private static int                          maxIterations          = 20;

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
                logger = LoggerHelper.initLogger(options.getOutputlevel(), options.getLogfile(), options.isDebug(), Mode.cmdline);
                logger.debug("Args: " + Arrays.asList(args).toString());
                config = ConfigFile.readIn(options.getConfigfile());
                if(config == null) {
                    logger.error("Could not read config");
                    return -1;
                }
                WorkingdirGenerator.getInstance().create(options.getWorkingdir(), config.workdir, "delaywork", null);
                status = execute();
                zipWorkfile();
                WorkingdirGenerator.getInstance().delete();
            }
            long end = System.currentTimeMillis();
            if(logger != null) {
                logger.info("Runtime: " + LoggerHelper.formatRuntime(end - start, false));
            }
            return status;
        } catch(Exception e) {
//            e.printStackTrace();
            System.out.println("An error occurred: " + e.getLocalizedMessage());
            return -1;
        }
    }

    /**
     * 
     * @return -1: runtime error, 0: ok, 1: timing violation detected
     */
    private static int execute() {
        Technology tech = ReadTechnologyHelper.read(options.getTechnology(), config.defaultTech);
        if(tech == null) {
            logger.error("No technology found");
            return -1;
        }

        ProfileComponents comps = ProfileComponents.readIn(options.getProfilefile());
        if(comps == null) {
            return -1;
        }

        if(config.toolconfig == null) {
            logger.error("External tools (remote login) not configured");
            return -1;
        }

        RemoteInvocation rinv = config.toolconfig.designCompilerCmd;
        if(rinv == null) {
            logger.error("Remote login not configured");
            return -1;
        }
        RemoteInformation rinfo = new RemoteInformation(rinv.hostname, rinv.username, rinv.password, rinv.workingdir);

        VerilogParser vparser = new VerilogParser();
        if(!vparser.parseVerilogStructure(options.getVfile())) {
            return -1;
        }

        logger.info("Setup phase");
        EligibleModuleFinder find = new EligibleModuleFinder(comps);
        Map<String, DelayMatchModule> modules = find.find(vparser.getModules());
        if(modules == null) {
            return -1;
        }

        MeasureRecordGenerator rec = new MeasureRecordGenerator(modules, options.getSTGfile(), vparser.getRootModule());
        if(!rec.generate(options.isFuture(), options.getSTGfile() != null, true)) {
            return -1;
        }

        File verilogFile = options.getVfile();
        String name = verilogFile.getName().split("\\.")[0];

        if(options.isVerifyOnly() && options.getSdfFile() != null) {
            logger.info("Split SDF");
            SplitSdfMain ssdfmain = new SplitSdfMain(name, rinfo, modules, tech);
            if(!ssdfmain.split(options.getSdfFile(), options.getVfile(), vparser.getRootModule().getModulename())) {
                return -1;
            }
        }

        MeasureMain memain = new MeasureMain(name, rinfo, modules, tech, rec);
        CheckMain cmain = new CheckMain(modules, rec);
        MatchMain mamain = new MatchMain(name, rinfo, modules, tech);

        int turnid = 1;
        while(turnid <= maxIterations) {
            logger.info("------------------------------");
            logger.info("Measure phase #" + turnid);
            if(!memain.measure(turnid, verilogFile)) {
                return -1;
            }

            logger.info("Check phase #" + turnid);
            if(!cmain.check()) {
                return -1;
            }

            if(options.isVerifyOnly() || cmain.isAllOk()) {
                break;
            }

            logger.info("Match phase #" + turnid);
            if(!mamain.match(turnid, verilogFile)) {
                return -1;
            }

            verilogFile = new File(WorkingdirGenerator.getInstance().getWorkingdir(), mamain.getMatchedfilename());
            turnid++;
        }

        logger.info("------------------------------");
        boolean timingOk = false;
        if(options.isVerifyOnly()) {
            if(cmain.isAllOk()) {
                logger.info("No timing violations in the design");
                timingOk = true;
            } else {
                logger.warn("There are timing violations in the design");
                timingOk = false;
            }
        } else {
            if(cmain.isAllOk()) {
                logger.info("There are no timing violations left in the design");
                timingOk = true;
            } else {
                logger.warn("Max iterations reached, but there are still timing violations in the design");
                timingOk = false;
            }
        }

        if(options.getOutfile() != null) {
            if(!FileHelper.getInstance().copyfile(verilogFile, options.getOutfile())) {
                return -1;
            }
        }

        if(options.getSdcFile() != null) {
            File sdc = cmain.getSdcFile();
            if(sdc.exists()) {
                if(!FileHelper.getInstance().copyfile(sdc, options.getSdcFile())) {
                    return -1;
                }
            } else {
                logger.warn("Generating SDC file failed");
            }
        }

        if(timingOk) {
            return 0;
        } else {
            return 1;
        }
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