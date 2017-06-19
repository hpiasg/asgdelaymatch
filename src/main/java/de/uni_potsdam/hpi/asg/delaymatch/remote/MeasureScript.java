package de.uni_potsdam.hpi.asg.delaymatch.remote;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.remote.RunSHScript.TimedResult;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.helper.AbstractErrorRemoteScript;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord;

public class MeasureScript extends AbstractErrorRemoteScript {

    private Technology                    tech;
    private Map<String, DelayMatchModule> modules;

    private long                          time;
    private boolean                       result;
    private String                        resultErrorMsg;

    public MeasureScript(RemoteInformation rinfo, String subdir, File outputDir, Technology tech, Map<String, DelayMatchModule> modules) {
        super(rinfo, subdir, outputDir, true);
        this.tech = tech;
        this.modules = modules;
    }

    public boolean generate(File dcShFile, File dcTclFile, File vInFile, File logFile) {
        if(!generateDcShFile(dcShFile, dcTclFile, logFile)) {
            return false;
        }
        if(!generateDcTclFile(dcTclFile, vInFile)) {
            return false;
        }

        addUploadFiles(dcShFile, dcTclFile, vInFile);
        addExecFileNames(dcShFile.getName());
        addDownloadIncludeFileNames(logFile.getName());

        for(DelayMatchModule mod : modules.values()) {
            if(!mod.getMeasureRecords().isEmpty()) {
                addUploadFiles(mod.getSdfFile());
                addDownloadIncludeFileNames(mod.getMeasureOutputFile().getName());
            }
        }

        return true;
    }

    @Override
    protected boolean executeCallBack(String script, TimedResult res) {
        switch(res.getCode()) {
            case 0:
                result = true;
                break;
            default:
                result = false;
                if(errorMsgMap.containsKey(res.getCode())) {
                    resultErrorMsg = errorMsgMap.get(res.getCode());
                } else {
                    resultErrorMsg = "Unkown error code: " + res.getCode();
                }
                break;
        }
        time = res.getCPUTime();
        return true;
    }

    private boolean generateDcShFile(File dcShFile, File dcTclFile, File logFile) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("dc_tcl_file", dcTclFile.getName());
        replacements.put("dc_log_file", logFile.getName());

        return replaceInTemplateAndWriteOut("dc_sh", replacements, dcShFile);
    }

    private boolean generateDcTclFile(File dcTclFile, File vInFile) {
        List<String> code = new ArrayList<>();
        List<String> tmpcode;
        Map<String, String> replacements = new HashMap<>();

        // setup
        replacements.put("dc_tcl_search_path", tech.getSynctool().getSearchPaths());
        replacements.put("dc_tcl_libraries", tech.getSynctool().getLibraries());
        tmpcode = replaceInTemplate("dc_tcl_setup", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        // analyze
        replacements.put("dc_tcl_vin", vInFile.getName());
        setErrorMsg(replacements, "Anaylze failed");
        tmpcode = replaceInTemplate("dc_tcl_analyze", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        for(DelayMatchModule mod : modules.values()) {
            if(!mod.getMeasureRecords().isEmpty()) {
                replacements.put("dc_tcl_sub_log", mod.getMeasureOutputFile().getName());

                // elab
                replacements.put("dc_tcl_sub_module", mod.getModuleName());
                setErrorMsg(replacements, "Elab " + mod.getModuleName() + " failed");
                tmpcode = replaceInTemplate("dc_tcl_elab_sub", replacements);
                if(tmpcode == null) {
                    return false;
                }
                code.addAll(tmpcode);

                // read sdf
                replacements.put("dc_tcl_sub_sdffile", mod.getSdfFile().getName());
                setErrorMsg(replacements, "Read Sdf " + mod.getModuleName() + " failed");
                tmpcode = replaceInTemplate("dc_tcl_read_sdf_sub", replacements);
                if(tmpcode == null) {
                    return false;
                }
                code.addAll(tmpcode);

                for(MeasureRecord rec : mod.getMeasureRecords().values()) {
                    // measure
                    String template = getMeasureTemplateName(rec);
                    String from = rec.getFromSignals();
                    String to = rec.getToSignals();
                    replacements.put("dc_tcl_sub_from", from);
                    replacements.put("dc_tcl_sub_to", to);
                    replacements.put("dc_tcl_sub_id", rec.getId());
                    setErrorMsg(replacements, "Report timing of module " + mod.getModuleName() + ": " + template + ", from: {" + from + "}, to: {" + to + "} failed");
                    tmpcode = replaceInTemplates(new String[]{"dc_tcl_echo_sub", template}, replacements);
                    if(tmpcode == null) {
                        return false;
                    }
                    code.addAll(tmpcode);
                }
            }
        }

        // final
        tmpcode = replaceInTemplate("dc_tcl_finish", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        if(!FileHelper.getInstance().writeFile(dcTclFile, code)) {
            return false;
        }

        return true;
    }

    private String getMeasureTemplateName(MeasureRecord rec) {
        StringBuilder templateName = new StringBuilder();
        templateName.append("dc_tcl_measure_");
        switch(rec.getType()) {
            case max:
                templateName.append("max_");
                break;
            case min:
                templateName.append("min_");
                break;
        }
        switch(rec.getFromEdge()) {
            case both:
                templateName.append("both_");
                break;
            case falling:
                templateName.append("fall_");
                break;
            case rising:
                templateName.append("rise_");
                break;
        }
        switch(rec.getToEdge()) {
            case both:
                templateName.append("both");
                break;
            case falling:
                templateName.append("fall");
                break;
            case rising:
                templateName.append("rise");
                break;
        }

        return templateName.toString();
    }

    public long getTime() {
        return time;
    }

    public boolean getResult() {
        return result;
    }

    public String getResultErrorMsg() {
        return resultErrorMsg;
    }
}
