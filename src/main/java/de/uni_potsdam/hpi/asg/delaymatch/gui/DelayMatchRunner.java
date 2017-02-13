package de.uni_potsdam.hpi.asg.delaymatch.gui;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters.GeneralBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractRunner;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchGuiMain;
import de.uni_potsdam.hpi.asg.delaymatch.gui.DelayMatchParameters.BooleanParam;
import de.uni_potsdam.hpi.asg.delaymatch.gui.DelayMatchParameters.EnumParam;
import de.uni_potsdam.hpi.asg.delaymatch.gui.DelayMatchParameters.TextParam;

public class DelayMatchRunner extends AbstractRunner {
    private static final Logger  logger = LogManager.getLogger();

    private DelayMatchParameters params;

    public DelayMatchRunner(DelayMatchParameters params) {
        super(params);
        this.params = params;
    }

    public void run() {
        if(!checkParams()) {
            return;
        }
        List<String> cmd = buildCmd();
        exec(cmd, "ASGdelaymatch terminal");
    }

    private boolean checkParams() {
        File verilogfile = new File(params.getTextValue(TextParam.VerilogFile));
        if(!verilogfile.exists()) {
            logger.error("Verilog input file not found");
            return false;
        }

        File profilefile = new File(params.getTextValue(TextParam.ProfileFile));
        if(!profilefile.exists()) {
            logger.error("Profile file not found");
            return false;
        }

        if(!params.getBooleanValue(BooleanParam.TechLibDef)) {
            File techfile = getTechFile();
            if(!techfile.exists()) {
                logger.error("Techfile not found");
                return false;
            }
        }

        return true;
    }

    private File getTechFile() {
        String techName = params.getEnumValue(EnumParam.TechLib);
        return new File(CommonConstants.DEF_TECH_DIR_FILE, techName + CommonConstants.XMLTECH_FILE_EXTENSION);
    }

    private List<String> buildCmd() {
        List<String> cmd = new ArrayList<>();
        cmd.add(DelayMatchGuiMain.DELAYMATCH_BIN.getAbsolutePath());

        addGeneralParams(cmd);
        addAdvancedParams(cmd);
        addDebugParams(cmd);

        cmd.add(params.getTextValue(TextParam.VerilogFile));

        return cmd;
    }

    private void addGeneralParams(List<String> cmd) {
        cmd.add("-p");
        cmd.add(params.getTextValue(TextParam.ProfileFile));

        if(!params.getBooleanValue(BooleanParam.TechLibDef)) {
            cmd.add("-lib");
            File techfile = getTechFile();
            cmd.add(techfile.getAbsolutePath());
        }

        addStandardIOParams(cmd, "-out");
    }

    private void addAdvancedParams(List<String> cmd) {
        if(params.getBooleanValue(BooleanParam.future)) {
            cmd.add("-future");
        }

        String stgfilename = params.getTextValue(TextParam.STGFile);
        if(!stgfilename.equals("")) {
            File stgfile = new File(stgfilename);
            if(stgfile.exists()) {
                cmd.add("-past");
                cmd.add(stgfilename);
            } else {
                logger.warn("STG file for past alogrithm does not exists. Omitting");
            }
        }
    }

    private void addDebugParams(List<String> cmd) {
        if(params.getBooleanValue(GeneralBooleanParam.debug)) {
            cmd.add("-debug");
        }
    }
}
