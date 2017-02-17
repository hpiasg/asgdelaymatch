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
    //@formatter:off
    public enum TextParam implements AbstractTextParam {
        /*general*/ VerilogFile, ProfileFile, STGFile
    }

    public enum BooleanParam implements AbstractBooleanParam {
        /*general*/ TechLibDef,
        /*adv*/ future
        /*debug*/ 
    }
    
    public enum EnumParam implements AbstractEnumParam {
        /*general*/ TechLib,
    }
    //@formatter:on

    private String   defTech;
    private String[] techs;

    public DelayMatchParameters(String defTech, TechnologyDirectory techDir) {
        super(CommonConstants.VERILOG_FILE_EXTENSION);
        this.defTech = defTech;
        this.techs = techDir.getTechNames();
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
}
