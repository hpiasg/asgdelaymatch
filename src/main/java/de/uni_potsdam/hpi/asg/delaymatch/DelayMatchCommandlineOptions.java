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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import de.uni_potsdam.hpi.asg.common.iohelper.CommandlineOptions;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;

public class DelayMatchCommandlineOptions extends CommandlineOptions {

    public boolean parseCmdLine(String[] args) {
        return super.parseCmdLine(args, "Usage: ASGdelaymatch -p <profilefile> -out <outfile> [options] <verilog file>\nOptions:");
    }

    //@formatter:off
    
    @Option(name = "-o", metaVar = "<level>", usage = "Outputlevel: 0:nothing\n1:errors\n[2:+warnings]\n3:+info")
    private int outputlevel             = 2;
    @Option(name = "-log", metaVar = "<logfile>", usage = "Define output Logfile, default is delaymatch" + CommonConstants.LOG_FILE_EXTENSION)
    private File logfile = new File(System.getProperty("user.dir"), "delaymatch" + CommonConstants.LOG_FILE_EXTENSION);
    @Option(name = "-zip", metaVar = "<zipfile>", usage = "Define the zip file with all temp files, default is delaymatch" + CommonConstants.ZIP_FILE_EXTENSION)
    private File workfile = new File(System.getProperty("user.dir"), "delaymatch" + CommonConstants.ZIP_FILE_EXTENSION);
    
    @Option(name = "-cfg", metaVar = "<configfile>", usage = "Config file, default is " + DelayMatchMain.DEF_CONFIG_FILE_NAME)
    private File configfile = DelayMatchMain.DEF_CONFIG_FILE;
    @Option(name = "-w", metaVar = "<workingdir>", usage = "Working directory. If not given, the value in configfile is used. If there is no entry, 'delaywork*' in the os default tmp dir is used.")
    private File workingdir = null;
    
    @Option(name = "-p", metaVar = "<profile>", usage = "Profile file", required = true)
    private File profilefile = null;
    @Option(name = "-out", metaVar = "<outfile>", usage = "Outfile")
    private File outfile = null;
    @Option(name = "-lib", metaVar = "<technologyfile>", usage = "technology description for implementation")
    private File technology;
    
    @Option(name = "-future", usage = "Use future alorithm (Resyn only!)")
    private boolean future = false;
    @Option(name = "-past", metaVar = "<gfile>", usage = "Use past algorithm (STG file needed; Resyn only!)")
    private File stgfile = null;
    
    @Option(name = "-verifyOnly", usage = "Do not delaymatch. Just check timing conditions. Default off")
    private boolean verifyOnly = false;
    @Option(name = "-sdf", usage = "SDF file for timing verification (only used if -verifyOnly is activated)")
    private File sdfFile = null;
    @Option(name = "-sdc", usage = "SDC output file")
    private File sdcFile = null;
    @Option(name = "-valOut", usage = "Delaymatch values output file")
    private File valOut = null;
    @Option(name = "-valIn", usage = "Delaymatch values input file")
    private File valIn = null;
    
    @Argument(metaVar = "Verilog File", required = true)
    private File vfile;

    @Option(name = "-debug")
    private boolean debug = false;
    
    //@formatter:on

    public int getOutputlevel() {
        return outputlevel;
    }

    public File getLogfile() {
        return logfile;
    }

    public File getConfigfile() {
        return configfile;
    }

    public File getWorkfile() {
        return workfile;
    }

    public boolean isDebug() {
        return debug;
    }

    public File getWorkingdir() {
        return workingdir;
    }

    public File getProfilefile() {
        return profilefile;
    }

    public File getVfile() {
        return vfile;
    }

    public File getOutfile() {
        return outfile;
    }

    public boolean isFuture() {
        return future;
    }

    public File getSTGfile() {
        return stgfile;
    }

    public File getTechnology() {
        return technology;
    }

    public boolean isVerifyOnly() {
        return verifyOnly;
    }

    public File getSdfFile() {
        return sdfFile;
    }

    public File getSdcFile() {
        return sdcFile;
    }

    public File getValOut() {
        return valOut;
    }

    public File getValIn() {
        return valIn;
    }
}
