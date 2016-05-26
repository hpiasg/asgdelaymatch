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

import java.io.File;

import org.kohsuke.args4j.Option;

import de.uni_potsdam.hpi.asg.common.io.CommandlineOptions;

public class DelayMatchCommandlineOptions extends CommandlineOptions {

    public boolean parseCmdLine(String[] args) {
        return super.parseCmdLine(args, "Usage: ASGresyn -lib <technologyfile> [options] <breeze file>\nOptions:");
    }

    //@formatter:off
    
    @Option(name = "-o", metaVar = "<level>", usage = "Outputlevel: 0:nothing\n1:errors\n[2:+warnings]\n3:+info")
    private int outputlevel             = 2;
    @Option(name = "-log", metaVar = "<logfile>", usage = "Define output Logfile, default is resyn.log")
    private File logfile = new File("resyn.log");
    @Option(name = "-zip", metaVar = "<zipfile>", usage = "Define the zip file with all temp files, default is resyn.zip")
    private File workfile = new File(System.getProperty("user.dir") + File.separator + "resyn.zip");
    
    @Option(name = "-cfg", metaVar = "<configfile>", usage = "Config file, default is delaymatchconfig.xml")
    private File configfile = new File("resynconfig.xml");
    @Option(name = "-w", metaVar = "<workingdir>", usage = "Working directory. If not given, the value in configfile is used. If there is no entry, 'delaywork*' in the os default tmp dir is used.")
    private File workingdir = null;

//    @Argument(metaVar = "breeze File", required = true)
//    private File hsfile;

    @Option(name = "-debug")
    private boolean debug = false;
    
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
}
