package de.uni_potsdam.hpi.asg.logictool;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition.Edge;
import de.uni_potsdam.hpi.asg.logictool.trace.TraceFinder;

public class STGTestMain {
    private static Logger logger;

    public static void main(String[] args) {
        LoggerHelper.initLogger(0, null, true);
        logger = LogManager.getLogger();
        long start = System.currentTimeMillis();

        String filename = "/home/norman/share/testdir/gcd_fordeco.g";

//        String filename = "/home/norman/share/testdir/parallel.g";
//        String startSigName = "a";
//        Edge startEdge = Edge.rising;
//        String endSigName = "i";
//        Edge endEdge = Edge.rising;

        TraceFinder tfind = new TraceFinder(new File(filename));
        System.out.println(tfind.find("r1", Edge.rising, "rD_25", Edge.rising));
        System.out.println(tfind.find("aD_2", Edge.falling, "rD_25", Edge.rising));
        System.out.println(tfind.find("aD_0", Edge.falling, "rD_25", Edge.rising));

        long end = System.currentTimeMillis();
        System.out.println(LoggerHelper.formatRuntime(end - start, true));
    }
}
