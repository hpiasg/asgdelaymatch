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
import de.uni_potsdam.hpi.asg.common.iohelper.Statistics;
import de.uni_potsdam.hpi.asg.common.iohelper.StatisticsFile;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.iohelper.Zipper;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.common.remote.AbstractScript;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.technology.ReadTechnologyHelper;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.check.CheckMain;
import de.uni_potsdam.hpi.asg.delaymatch.check.Constraints;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.ValuesXmlAnnotator;
import de.uni_potsdam.hpi.asg.delaymatch.io.Config;
import de.uni_potsdam.hpi.asg.delaymatch.io.ConfigFile;
import de.uni_potsdam.hpi.asg.delaymatch.io.RemoteInvocation;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.remote.MatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.remote.MeasureMain;
import de.uni_potsdam.hpi.asg.delaymatch.remote.SdfSplitMain;
import de.uni_potsdam.hpi.asg.delaymatch.setup.EligibleModuleFinder;
import de.uni_potsdam.hpi.asg.delaymatch.setup.MeasureRecordGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.VerilogParser;

public class DelayMatchMain {

    public static final String                  DEF_CONFIG_FILE_NAME         = "delaymatchconfig.xml";
    public static final File                    DEF_CONFIG_FILE              = new File(CommonConstants.DEF_CONFIG_DIR_FILE, DEF_CONFIG_FILE_NAME);

    private static final float                  normalMatchMinStartFactor    = 1.0f;
    private static final float                  normalMatchMinIncreaseFactor = 0.1f;
    private static final float                  normalMatchMaxStartFactor    = 1.1f;
    private static final float                  normalMatchMaxIncreaseFactor = 0.2f;

    private static final float                  sdfMatchMinStartFactor       = 1.0f;
    private static final float                  sdfMatchMinIncreaseFactor    = 0.2f;
    private static final float                  sdfMatchMaxStartFactor       = 1.5f;
    private static final float                  sdfMatchMaxIncreaseFactor    = 0.5f;

    private static final int                    maxIterations                = 20;

    private static Logger                       logger;
    private static DelayMatchCommandlineOptions options;
    public static Config                        config;

    public static float                         matchMinStartFactor;
    public static float                         matchMinIncreaseFactor;
    public static float                         matchMaxStartFactor;
    public static float                         matchMaxIncreaseFactor;

    public static float                         pathMinStartFactor           = 1.0f;
    public static float                         pathMinIncreaseFactor        = 0.1f;
    public static float                         pathMaxStartFactor           = 1.0f;
    public static float                         pathMaxIncreaseFactor        = 0.1f;

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
            e.printStackTrace();
            System.out.println("An error occurred: " + e.getLocalizedMessage());
            return -1;
        }
    }

    /**
     * 
     * @return -1: runtime error,
     *         0: OK (no timing violation(s)),
     *         1: timing violation(s) detected,
     *         2: no statement
     */
    private static int execute() {
        if(options.getSdfInFile() != null) {
            matchMinStartFactor = sdfMatchMinStartFactor;
            matchMinIncreaseFactor = sdfMatchMinIncreaseFactor;
            matchMaxStartFactor = sdfMatchMaxStartFactor;
            matchMaxIncreaseFactor = sdfMatchMaxIncreaseFactor;
        } else {
            matchMinStartFactor = normalMatchMinStartFactor;
            matchMinIncreaseFactor = normalMatchMinIncreaseFactor;
            matchMaxStartFactor = normalMatchMaxStartFactor;
            matchMaxIncreaseFactor = normalMatchMaxIncreaseFactor;
        }

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

        if(options.getValIn() != null) {
            logger.info("Annotate values");
            ValuesXmlAnnotator anot = new ValuesXmlAnnotator(modules);
            if(!anot.annotate(options.getValIn())) {
                return -1;
            }
        }

        Constraints constraints = new Constraints(modules);
        if(options.getSdcInFile() != null) {
            if(!constraints.readPreviousSdc(options.getSdcInFile())) {
                logger.warn("Failed to read sdc in file");
            }
        }
        File sdcInFile = options.getSdcInFile();

        AbstractScript.readTemplateFiles("delaymatch_");

        SdfSplitMain ssdfmain = new SdfSplitMain(name, rinfo, modules, tech, vparser.getRootModule().getModulename());
        MeasureMain memain = new MeasureMain(name, rinfo, modules, tech);
        CheckMain cmain = new CheckMain(modules, rec, constraints);
        MatchMain mamain = new MatchMain(name, rinfo, modules, tech, vparser.getRootModule().getModulename());

        File sdfFile = options.getSdfInFile();

        long remoteTime = 0;
        int turnid = 1;
        boolean verifyOnlyBreak = false;
        boolean allOkBreak = false;
        boolean sdfBreak = false;
        while(turnid <= maxIterations) {
            logger.info("------------------------------");
            logger.info("SDF phase #" + turnid);
            if(!ssdfmain.split(turnid, sdfFile, verilogFile, sdcInFile)) {
                return -1;
            }
            sdfFile = ssdfmain.getLastSdfFile();
            remoteTime += ssdfmain.getLastTime();

            logger.info("Measure phase #" + turnid);
            if(!memain.measure(turnid, verilogFile)) {
                return -1;
            }
            remoteTime += memain.getLastTime();

            logger.info("Check phase #" + turnid);
            if(!cmain.check()) {
                return -1;
            }

            if(options.isVerifyOnly()) {
                verifyOnlyBreak = true;
            }
            if(cmain.isAllOk()) {
                allOkBreak = true;
            }
            if(verifyOnlyBreak || allOkBreak) {
                break;
            }

            logger.info("Match phase #" + turnid);
            if(!mamain.match(turnid, verilogFile, sdcInFile)) {
                return -1;
            }
            remoteTime += memain.getLastTime();

            verilogFile = mamain.getLastVFile();
            sdfFile = mamain.getLastSdfFile();

            if(options.getSdfInFile() != null) {
                sdfBreak = true;
                break;
            }

            turnid++;
        }

        logger.info("------------------------------");
        int retVal = -1;
        if(sdfBreak) {
            logger.info("Aborting iteration, because SDF file was invalidated by setting new delays");
            retVal = 2;
        } else {
            if(verifyOnlyBreak && allOkBreak) {
                logger.info("No timing violations in the design");
                retVal = 0;
            } else if(verifyOnlyBreak && !allOkBreak) {
                logger.warn("There are timing violations in the design");
                retVal = 1;
            } else if(!verifyOnlyBreak && allOkBreak) {
                logger.info("There are no timing violations left in the design");
                retVal = 0;
            } else if(!verifyOnlyBreak && !allOkBreak) { // else
                logger.warn("Max iterations reached, but there are still timing violations in the design");
                retVal = 1;
            }
        }

        if(!generateOutfiles(verilogFile, cmain, sdfFile, remoteTime)) {
            return -1;
        }

        return retVal;
    }

    private static boolean generateOutfiles(File verilogFile, CheckMain cmain, File sdfFile, long remoteTime) {
        if(options.getOutfile() != null) {
            if(!FileHelper.getInstance().copyfile(verilogFile, options.getOutfile())) {
                return false;
            }
        }

        if(options.getSdcOutFile() != null) {
            File sdc = cmain.getSdcFile();
            if(sdc.exists()) {
                if(!FileHelper.getInstance().copyfile(sdc, options.getSdcOutFile())) {
                    return false;
                }
            } else {
                logger.warn("Generating SDC file failed");
            }
        }

        if(options.getValOut() != null) {
            if(!cmain.writeOutVals(options.getValOut())) {
                return false;
            }
        }

        if(options.getSdfOutFile() != null) {
            if(options.getSdfInFile() == null) {
                if(sdfFile.exists()) {
                    if(!FileHelper.getInstance().copyfile(sdfFile, options.getSdfOutFile())) {
                        return false;
                    }
                } else {
                    logger.warn("Generating SDF file failed");
                }
            } else {
                logger.warn("Because there was an SDF infile, the SDF outfile is not generated. Please run the process (e.g. layout) in which the sdf infile was generated again, supplying the new verilog outfile");
            }
        }

        if(options.getStatistics() != null) {
            Statistics stat = new Statistics();
            stat.setRemoteTime(remoteTime);
            if(!StatisticsFile.writeOut(stat, options.getStatistics())) {
                return false;
            }
        }

        return true;
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