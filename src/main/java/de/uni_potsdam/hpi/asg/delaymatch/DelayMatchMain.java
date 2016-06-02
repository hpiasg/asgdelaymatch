package de.uni_potsdam.hpi.asg.delaymatch;

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

import java.util.Arrays;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.io.Zipper;
import de.uni_potsdam.hpi.asg.common.io.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.delaymatch.io.Config;
import de.uni_potsdam.hpi.asg.delaymatch.io.RemoteInvocation;
import de.uni_potsdam.hpi.asg.delaymatch.measure.MeasureMain;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;

public class DelayMatchMain {
    private static Logger                       logger;
    private static DelayMatchCommandlineOptions options;
    public static Config                        config;

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
                config = Config.readIn(options.getConfigfile());
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

        MeasureMain mmain = new MeasureMain(comps, rinfo);
        if(!mmain.measure(options.getVfile())) {
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