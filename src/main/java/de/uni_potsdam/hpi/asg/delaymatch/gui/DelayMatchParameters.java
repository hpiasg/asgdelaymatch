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

import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel.AbstractBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel.AbstractEnumParam;
import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel.AbstractTextParam;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.common.technology.TechnologyDirectory;

public class DelayMatchParameters extends AbstractParameters {

    private static final String DEF_VERILOGIN_FILE_NAME = "";
    private static final String DEF_PROFILE_FILE_NAME   = "$BASEDIR/config/resynprofile.xml";
    private static final String DEF_OUT_FILE_NAME       = "delaymatched" + CommonConstants.VERILOG_FILE_EXTENSION;
    private static final String DEF_LOG_FILE_NAME       = "delaymatched" + CommonConstants.LOG_FILE_EXTENSION;
    private static final String DEF_ZIP_FILE_NAME       = "delaymatched" + CommonConstants.ZIP_FILE_EXTENSION;

    //@formatter:off
    public enum TextParam implements AbstractTextParam {
        /*general*/ VerilogFile, ProfileFile, STGFile
    }

    public enum BooleanParam implements AbstractBooleanParam {
        /*general*/ TechLibDef,
        /*adv*/ future, past
        /*debug*/ 
    }
    
    public enum EnumParam implements AbstractEnumParam {
        /*general*/ TechLib,
    }
    //@formatter:on

    private String   defTech;
    private String[] techs;
    private boolean  forceTech;
    private String   defVerilogInFileName;
    private String   defProfileFileName;
    private String   defPastStgFileName;
    private String   defOutDirName;
    private String   defOutFileName;
    private String   defLogFileName;
    private String   defZipFileName;

    public DelayMatchParameters(String defTech, TechnologyDirectory techDir) {
        super(CommonConstants.VERILOG_FILE_EXTENSION);
        this.defTech = defTech;
        this.techs = techDir.getTechNames();
        this.forceTech = false;
        this.defVerilogInFileName = DEF_VERILOGIN_FILE_NAME;
        this.defProfileFileName = DEF_PROFILE_FILE_NAME;
        this.defPastStgFileName = "";
        this.defOutDirName = AbstractParameters.DEF_OUT_DIR;
        this.defOutFileName = DEF_OUT_FILE_NAME;
        this.defLogFileName = DEF_LOG_FILE_NAME;
        this.defZipFileName = DEF_ZIP_FILE_NAME;
    }

    public DelayMatchParameters(String defTech, TechnologyDirectory techDir, boolean forceTech, String verilogInFile, String profileFile, String pastStgFile, String outDir, String outFile, String logFile, String zipFile) {
        super(CommonConstants.VERILOG_FILE_EXTENSION);
        this.defTech = defTech;
        this.techs = techDir.getTechNames();
        this.forceTech = true;
        this.defVerilogInFileName = verilogInFile;
        this.defProfileFileName = profileFile;
        this.defPastStgFileName = pastStgFile;
        this.defOutDirName = outDir;
        this.defOutFileName = outFile;
        this.defLogFileName = logFile;
        this.defZipFileName = zipFile;
    }

    @Override
    public String getEnumValue(AbstractEnumParam param) {
        int index = mainpanel.getEnumValue(param);
        if(param == EnumParam.TechLib) {
            return techs[index];
        } else {
            return null;
        }
    }

    public String getDefTech() {
        return defTech;
    }

    public String[] getAvailableTechs() {
        return techs;
    }

    public String getDefVerilogInFileName() {
        return defVerilogInFileName;
    }

    public String getDefProfileFileName() {
        return defProfileFileName;
    }

    public String getDefPastStgFileName() {
        return defPastStgFileName;
    }

    public String getDefOutFileName() {
        return defOutFileName;
    }

    public String getDefOutDirName() {
        return defOutDirName;
    }

    public boolean isForceTech() {
        return forceTech;
    }

    public String getDefLogFileName() {
        return defLogFileName;
    }

    public String getDefZipFileName() {
        return defZipFileName;
    }
}
