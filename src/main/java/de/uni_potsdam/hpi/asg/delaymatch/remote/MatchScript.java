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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;
import de.uni_potsdam.hpi.asg.common.remote.RunSHScript.TimedResult;
import de.uni_potsdam.hpi.asg.common.technology.Technology;
import de.uni_potsdam.hpi.asg.delaymatch.helper.AbstractErrorRemoteScript;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class MatchScript extends AbstractErrorRemoteScript {
    private static final Logger           logger = LogManager.getLogger();

    private Technology                    tech;
    private Map<String, DelayMatchModule> modules;

    private long                          time;
    private boolean                       result;
    private String                        resultErrorMsg;

    public MatchScript(RemoteInformation rinfo, String subdir, File outputDir, Technology tech, Map<String, DelayMatchModule> modules) {
        super(rinfo, subdir, outputDir, true);
        this.tech = tech;
        this.modules = modules;
    }

    public boolean generate(File dcShFile, File dcTclFile, File vInFile, File vOutFile, File sdfOutFile, File logFile, Map<DelayMatchModule, File> subLogFiles, String rootModule) {
        if(!generateDcShFile(dcShFile, dcTclFile, logFile)) {
            return false;
        }
        if(!generateDcTclFile(dcTclFile, vInFile, vOutFile, sdfOutFile, subLogFiles, rootModule)) {
            return false;
        }

        addUploadFiles(dcShFile, dcTclFile, vInFile);
        addExecFileNames(dcShFile.getName());
        addDownloadIncludeFileNames(vOutFile.getName(), sdfOutFile.getName(), logFile.getName());
        for(File f : subLogFiles.values()) {
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

    private boolean generateDcTclFile(File dcTclFile, File vInFile, File vOutFile, File sdfOutFile, Map<DelayMatchModule, File> subLogFiles, String rootModule) {
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
            if(mod.getProfilecomp() != null) {
                replacements.put("dc_tcl_sub_log", subLogFiles.get(mod).getName());

                // elab
                replacements.put("dc_tcl_sub_module", mod.getModuleName());
                setErrorMsg(replacements, "Elab " + mod.getModuleName() + " failed");
                tmpcode = replaceInTemplate("dc_tcl_elab_sub", replacements);
                if(tmpcode == null) {
                    return false;
                }
                code.addAll(tmpcode);

                // match
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        for(int eachid = 0; eachid < num; eachid++) {
                            Float min = mod.getControlMinVal(path, eachid);
                            Float max = mod.getControlMaxVal(path, eachid);
                            if(!generateMatchCode(code, replacements, mod, path, eachid, min, max)) {
                                return false;
                            }
                        }
                    } else {
                        Float min = mod.getControlMinVal(path);
                        Float max = mod.getControlMaxVal(path);
                        if(!generateMatchCode(code, replacements, mod, path, null, min, max)) {
                            return false;
                        }
                    }
                }

                setErrorMsg(replacements, "Compile " + mod.getModuleName() + " failed");
                tmpcode = replaceInTemplate("dc_tcl_compile_sub", replacements);
                if(tmpcode == null) {
                    return false;
                }
                code.addAll(tmpcode);
            }
        }

        // final
        replacements.put("dc_tcl_module", rootModule);
        setErrorMsg(replacements, "Elaborate " + rootModule + " failed");
        tmpcode = replaceInTemplate("dc_tcl_elab", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        replacements.put("dc_tcl_sdfout", sdfOutFile.getName());
        setErrorMsg(replacements, "Write Sdf for module " + rootModule + " failed");
        tmpcode = replaceInTemplate("dc_tcl_write_sdf", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

        replacements.put("dc_tcl_vout", vOutFile.getName());
        setErrorMsg(replacements, "Write verilog for module " + rootModule + " failed");
        tmpcode = replaceInTemplate("dc_tcl_write_verilog", replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);

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

    private boolean generateMatchCode(List<String> code, Map<String, String> replacements, DelayMatchModule mod, MatchPath path, Integer eachid, Float min, Float max) {
        List<String> tmpcode = null;
        if(min != null && max != null) {
            tmpcode = generateSetDelayCode(mod, path, eachid, min, max, replacements);
            if(tmpcode == null) {
                return false;
            }
            code.addAll(tmpcode);
        }
        tmpcode = generateDontTouchCode(mod, path, eachid, replacements);
        if(tmpcode == null) {
            return false;
        }
        code.addAll(tmpcode);
        return true;
    }

    private List<String> generateSetDelayCode(DelayMatchModule mod, MatchPath path, Integer eachid, Float min, Float max, Map<String, String> replacements) {
        String from = PortHelper.getPortListAsDCString(path.getMatch().getFrom(), eachid, mod.getSignals());
        String to = PortHelper.getPortListAsDCString(path.getMatch().getTo(), eachid, mod.getSignals());
        logger.info("Setting " + mod.getModuleName() + " " + from + "->" + to + ": min=" + min + ", max=" + max);
        return generateSetDelayCode(replacements, mod.getModuleName(), from, to, min, max);
    }

    private List<String> generateSetDelayCode(Map<String, String> replacements, String compName, String from, String to, Float min, Float max) {
        List<String> code = new ArrayList<>();
        List<String> tmpCode = null;
        replacements.put("dc_tcl_sub_from", from);
        replacements.put("dc_tcl_sub_to", to);
        replacements.put("dc_tcl_sub_time_min", min.toString());
        replacements.put("dc_tcl_sub_time_max", max.toString());

        setErrorMsg(replacements, "Set min delay of " + compName + " from: {" + from + "}, to: {" + to + "}, value: " + min + " failed");
        tmpCode = replaceInTemplate("dc_tcl_setdelay_min_sub", replacements);
        if(tmpCode == null) {
            return null;
        }
        code.addAll(tmpCode);
        setErrorMsg(replacements, "Set max delay of " + compName + " from: {" + from + "}, to: {" + to + "}, value: " + min + " failed");
        tmpCode = replaceInTemplate("dc_tcl_setdelay_max_sub", replacements);
        if(tmpCode == null) {
            return null;
        }
        code.addAll(tmpCode);

        return code;
    }

    private List<String> generateDontTouchCode(DelayMatchModule mod, MatchPath path, Integer eachid, Map<String, String> replacements) {
        String donttouch = PortHelper.getPortListAsDCString(path.getMeasure().getFrom(), eachid, mod.getSignals());
        replacements.put("dc_tcl_sub_donttouch", donttouch);
        setErrorMsg(replacements, "Dont touch of " + mod.getModuleName() + " of: {" + donttouch + "} failed");
        return replaceInTemplate("dc_tcl_donttouch_sub", replacements);
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
