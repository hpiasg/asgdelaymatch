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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.common.io.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.io.Zipper;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponent;
import de.uni_potsdam.hpi.asg.delaymatch.profile.ProfileComponents;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.VerilogInterfaceParser;

public class DelayMatchMain {
    private static Logger                       logger;
    private static DelayMatchCommandlineOptions options;

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
                WorkingdirGenerator.getInstance().create(options.getWorkingdir(), null, "delaywork", null);
                status = execute();
                zipWorkfile();
                //WorkingdirGenerator.getInstance().delete();
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

        Set<DelayMatchPlan> modules = new HashSet<>();
        Pattern p = Pattern.compile("module (.*) \\(.*");
        Matcher m;
        List<String> lines = FileHelper.getInstance().readFile(options.getVfile());
        VerilogInterfaceParser parser = null;
        for(String str : lines) {
            m = p.matcher(str);
            if(m.matches()) {
                String modulename = m.group(1);
                ProfileComponent pc = comps.getComponentByRegex(modulename);
                parser = null;
                if(pc != null) {
                    parser = new VerilogInterfaceParser();
                    modules.add(new DelayMatchPlan(modulename, pc, parser));
                }
            }
            if(parser != null) {
                parser.addLine(str);
            }
        }

        MeasureScriptGenerator gen = MeasureScriptGenerator.create(options.getVfile(), modules);
        gen.generate();

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