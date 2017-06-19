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

import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.remote.RunSHScript.TimedResult;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.helper.AbstractErrorRemoteScript;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModuleInst;

public class SdfSplitScript extends AbstractErrorRemoteScript {

    private Technology tech;

    private long       time;
    private boolean    result;
    private String     resultErrorMsg;

    public SdfSplitScript(RemoteInformation rinfo, String subdir, File outputDir, Technology tech) {
        super(rinfo, subdir, outputDir, true);
        this.tech = tech;
    }

    public boolean generate(File dcShFile, File dcTclFile, File vInFile, File sdcInFile, boolean generateSdf, File sdfInFile, Map<DelayMatchModuleInst, File> subSdfFiles, File logFile, String rootModule) {
        if(!generateDcShFile(dcShFile, dcTclFile, logFile)) {
            return false;
        }
        if(!generateDcTclFile(dcTclFile, vInFile, sdcInFile, generateSdf, sdfInFile, subSdfFiles, rootModule)) {
            return false;
        }

        addUploadFiles(dcShFile, dcTclFile, vInFile);
        if(!generateSdf) {
            addUploadFiles(sdfInFile);
        }
        if(sdcInFile != null) {
            addUploadFiles(sdcInFile);
        }

        addExecFileNames(dcShFile.getName());
        addDownloadIncludeFileNames(logFile.getName());
        for(File f : subSdfFiles.values()) {
            addDownloadIncludeFileNames(f.getName());
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

    private boolean generateDcTclFile(File dcTclFile, File vInFile, File sdcInFile, boolean generateSdf, File sdfInFile, Map<DelayMatchModuleInst, File> subSdfFiles, String rootModule) {
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

        // elaborate
        replacements.put("dc_tcl_module", rootModule);
        setErrorMsg(replacements, "Elaborate failed");
        tmpcode = replaceInTemplate("dc_tcl_elab", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        // generate sdf (if needed)
        if(generateSdf) {
            // read sdc
            if(sdcInFile != null) {
                replacements.put("dc_tcl_sdcfile", sdcInFile.getName());
                setErrorMsg(replacements, "Read Sdc failed");
                tmpcode = replaceInTemplate("dc_tcl_read_sdc", replacements);
                if(tmpcode == null) {
                    return false;
                }
                code.addAll(tmpcode);
            }

            // write sdf
            replacements.put("dc_tcl_sdfout", sdfInFile.getName());
            setErrorMsg(replacements, "Generate Sdf failed");
            tmpcode = replaceInTemplate("dc_tcl_write_sdf", replacements);
            if(tmpcode == null) {
                return false;
            }
            code.addAll(tmpcode);
        }

        // split sdf
        for(Entry<DelayMatchModuleInst, File> entry : subSdfFiles.entrySet()) {
            replacements.put("dc_tcl_sdfinstname", entry.getKey().getInstName());
            replacements.put("dc_tcl_sdfout", entry.getValue().getName());
            setErrorMsg(replacements, "Write Sdf " + entry.getKey().getInstName() + " failed");
            tmpcode = replaceInTemplate("dc_tcl_write_sdf_split", replacements);
            if(tmpcode == null) {
                return false;
            }
            code.addAll(tmpcode);
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
