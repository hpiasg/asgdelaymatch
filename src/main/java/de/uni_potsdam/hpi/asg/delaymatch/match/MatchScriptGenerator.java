package de.uni_potsdam.hpi.asg.delaymatch.match;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.helper.AbstractScriptGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.misc.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class MatchScriptGenerator extends AbstractScriptGenerator {
    private static final Logger              logger              = LogManager.getLogger();

    private static final String              v_file              = ".v";

    // generated by SimFlow
    private static final String              dc_sh_file          = "_match.sh";
    private static final String              dc_tcl_file         = "_match.tcl";

    // generated by scripts of SimFlow
    private static final String              dc_log_file         = "_match.log";
    private static final String              dc_out_file         = "_match.v";

    // template files
    private static final File                dc_sh_templatefile  = FileHelper.getInstance().getBasedirFile("templates/delay_match.sh");
    private static final File                dc_tcl_templatefile = FileHelper.getInstance().getBasedirFile("templates/delay_match.tcl");

    public static final Pattern              module_pattern      = Pattern.compile("module (.*) \\(.*");

    private static Map<String, List<String>> templates;

    private String                           name;
    private File                             localfile;
    private String                           localfolder;
    private String                           root;

    private Map<String, DelayMatchModule>    modules;

    public static MatchScriptGenerator create(File arg_origfile, Map<String, DelayMatchModule> modules) {
        if(templates == null) {
            templates = readTemplateCodeSnippets(dc_tcl_templatefile, new String[]{"setup", "elab", "setdelay", "settouch", "compile", "final"});
            if(templates == null) {
                return null;
            }
        }
        return new MatchScriptGenerator(arg_origfile, modules);
    }

    private MatchScriptGenerator(File arg_origfile, Map<String, DelayMatchModule> modules) {
        this.modules = modules;
        localfolder = WorkingdirGenerator.getInstance().getWorkingdir();
        localfile = new File(localfolder + arg_origfile);
        name = localfile.getName().split("\\.")[0];
        root = getRoot(arg_origfile);
    }

    public boolean generate() {
        String rmdcshfile = name + dc_sh_file;
        String dcshfile = localfolder + rmdcshfile;
        FileHelper.getInstance().copyfile(dc_sh_templatefile, new File(dcshfile));
        replaceInSh(localfolder + name + dc_sh_file);

        String dctclfile = localfolder + name + dc_tcl_file;
        List<String> tclfilecontent = new ArrayList<>();
        tclfilecontent.addAll(generateSetupTcl());

        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                tclfilecontent.addAll(generateElabTcl(mod.getName()));
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        for(int eachid = 0; eachid < num; eachid++) {
                            Float val = computeValue(path, mod);
                            if(val == null) {
                                return false;
                            }
                            tclfilecontent.addAll(generateMatch(mod, path, eachid, val));
                            tclfilecontent.addAll(generateDontTouch(mod, path, eachid));
                        }
                    } else {
                        Float val = computeValue(path, mod);
                        if(val == null) {
                            return false;
                        }
                        tclfilecontent.addAll(generateMatch(mod, path, null, val));
                        tclfilecontent.addAll(generateDontTouch(mod, path, null));
                    }
                }
                tclfilecontent.addAll(generatCompileTcl(mod.getName()));
            }
        }
        tclfilecontent.addAll(generateFinalTcl());

        if(!FileHelper.getInstance().writeFile(new File(dctclfile), tclfilecontent)) {
            return false;
        }

        return true;
    }

    private Float computeValue(MatchPath path, DelayMatchModule mod) {
        Float val = mod.getMeasureValue(path.getMeasure());
        List<Float> negvals = mod.getNegativeMatchValues(path.getMatch());
        if(negvals != null && !negvals.isEmpty()) {
            Float minnegval = Collections.min(negvals);
            return val - minnegval;
        }
        return val;
    }

    private List<String> generateMatch(DelayMatchModule plan, MatchPath path, Integer eachid, Float value) {
        String from = PortHelper.getPortListAsDCString(path.getMatch().getFrom(), eachid, plan.getSignals());
        String to = PortHelper.getPortListAsDCString(path.getMatch().getTo(), eachid, plan.getSignals());
        return generateSetDelayTcl(plan.getName(), from, to, value);
    }

    private List<String> generateDontTouch(DelayMatchModule plan, MatchPath path, Integer eachid) {
        String touch = PortHelper.getPortListAsDCString(path.getMeasure().getFrom(), eachid, plan.getSignals());
        return generatSetTouchTcl(plan.getName(), touch);
    }

    private void replaceInSh(String filename) {
        try {
            File f = new File(filename);
            List<String> out = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            while((line = reader.readLine()) != null) {
                line = line.replace("#*dc_tcl*#", name + dc_tcl_file);
                out.add(line);
            }
            reader.close();
            FileHelper.getInstance().writeFile(new File(filename), out);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> generateSetupTcl() {
        if(!templates.containsKey("setup")) {
            logger.error("Setup template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("setup")) {
            line = line.replace("#*orig*#", name + v_file);
            line = line.replace("#*dc_log*#", name + dc_log_file);
            line = line.replace("#*root*#", root);
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generateElabTcl(String component) {
        if(!templates.containsKey("elab")) {
            logger.error("Elab template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("elab")) {
            line = line.replace("#*dc_sub_log*#", name + "_" + component + dc_log_file);
            line = line.replace("#*root_sub*#", component);
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generateSetDelayTcl(String component, String from, String to, Float time) {
        if(!templates.containsKey("setdelay")) {
            logger.error("Setdelay template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("setdelay")) {
            line = line.replace("#*dc_sub_log*#", name + "_" + component + dc_log_file);
            line = line.replace("#*root_sub*#", component);
            line = line.replace("#*from_sub*#", from);
            line = line.replace("#*to_sub*#", to);
            line = line.replace("#*time_min_sub*#", time.toString());
            line = line.replace("#*time_max_sub*#", Float.toString(time * DelayMatchMain.matchMaxFactor));
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generatSetTouchTcl(String component, String touch) {
        if(!templates.containsKey("settouch")) {
            logger.error("Settouch template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("settouch")) {
            line = line.replace("#*dc_sub_log*#", name + "_" + component + dc_log_file);
            line = line.replace("#*root_sub*#", component);
            line = line.replace("#*touch_sub*#", touch);
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generatCompileTcl(String component) {
        if(!templates.containsKey("compile")) {
            logger.error("Compile template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("compile")) {
            line = line.replace("#*dc_sub_log*#", name + "_" + component + dc_log_file);
            line = line.replace("#*root_sub*#", component);
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generateFinalTcl() {
        if(!templates.containsKey("final")) {
            logger.error("Final template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("final")) {
            line = line.replace("#*dc_log*#", name + dc_log_file);
            line = line.replace("#*outfile*#", name + dc_out_file);
            line = line.replace("#*root*#", root);
            newlines.add(line);
        }
        return newlines;
    }

    private static String getRoot(File f) {
        String root = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            Matcher matcher = null;
            while((line = reader.readLine()) != null) {
                matcher = module_pattern.matcher(line);
                if(matcher.matches()) {
                    root = matcher.group(1);
                }
            }
            reader.close();
        } catch(IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return root;
    }

    public Set<String> getScriptFiles() {
        Set<String> retVal = new HashSet<>();
        retVal.add(localfolder + name + dc_sh_file);
        retVal.add(localfolder + name + dc_tcl_file);
        return retVal;
    }

    public String getExec() {
        return name + dc_sh_file;
    }

    public String getOutfile() {
        return name + dc_out_file;
    }
}
