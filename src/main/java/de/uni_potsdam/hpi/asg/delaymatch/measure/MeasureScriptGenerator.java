package de.uni_potsdam.hpi.asg.delaymatch.measure;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchPlan;
import de.uni_potsdam.hpi.asg.delaymatch.helper.AbstractScriptGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.helper.PortHelper;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;

public class MeasureScriptGenerator extends AbstractScriptGenerator {
    private static final Logger              logger              = LogManager.getLogger();

    private static final String              v_file              = ".v";

    // generated by SimFlow
    private static final String              dc_sh_file          = "_measure.sh";
    private static final String              dc_tcl_file         = "_measure.tcl";

    // generated by scripts of SimFlow
    private static final String              dc_log_file         = "_measure.log";

    // template files
    private static final File                dc_sh_templatefile  = FileHelper.getInstance().getBasedirFile("templates/delay_measure.sh");
    private static final File                dc_tcl_templatefile = FileHelper.getInstance().getBasedirFile("templates/delay_measure.tcl");

    private static Map<String, List<String>> templates;

    private String                           name;
    private File                             localfile;
    private String                           localfolder;

    private Set<DelayMatchPlan>              modules;

    public static MeasureScriptGenerator create(File arg_origfile, Set<DelayMatchPlan> modules) {
        if(templates == null) {
            templates = readTemplateCodeSnippets(dc_tcl_templatefile, new String[]{"setup", "elab", "measure", "final"});
            if(templates == null) {
                return null;
            }
        }
        return new MeasureScriptGenerator(arg_origfile, modules);
    }

    private MeasureScriptGenerator(File arg_origfile, Set<DelayMatchPlan> modules) {
        this.modules = modules;
        localfolder = WorkingdirGenerator.getInstance().getWorkingdir();
        localfile = new File(localfolder + arg_origfile);
        name = localfile.getName().split("\\.")[0];
    }

    public boolean generate() {
        String rmdcshfile = name + dc_sh_file;
        String dcshfile = localfolder + rmdcshfile;
        FileHelper.getInstance().copyfile(dc_sh_templatefile, new File(dcshfile));
        replaceInSh(localfolder + name + dc_sh_file);

        String dctclfile = localfolder + name + dc_tcl_file;
        List<String> tclfilecontent = new ArrayList<>();
        tclfilecontent.addAll(generateSetupTcl());
        for(DelayMatchPlan plan : modules) {
            tclfilecontent.addAll(generateElabTcl(plan.getName()));
            for(MatchPath path : plan.getProfilecomp().getMatchpaths()) {
                if(path.getForeach() != null) {
                    int num = plan.getVariables().get(path.getForeach()).getCount();
                    for(int eachid = 0; eachid < num; eachid++) {
                        addMeasure(tclfilecontent, plan, path, eachid);
                    }
                } else {
                    addMeasure(tclfilecontent, plan, path, null);
                }
            }
            plan.setMeasureOutputfile(name + "_" + plan.getName() + dc_log_file);
        }
        tclfilecontent.addAll(generateFinalTcl());

        if(!FileHelper.getInstance().writeFile(new File(dctclfile), tclfilecontent)) {
            return false;
        }

        return true;
    }

    private void addMeasure(List<String> tclfilecontent, DelayMatchPlan plan, MatchPath path, Integer eachid) {
        String from = PortHelper.getPortListAsString(path.getMeasure().getFrom(), eachid, plan.getVariables());
        String to = PortHelper.getPortListAsString(path.getMeasure().getTo(), eachid, plan.getVariables());
        tclfilecontent.addAll(generateMeasureTcl(plan.getName(), from, to));
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

    private List<String> generateMeasureTcl(String component, String from, String to) {
        if(!templates.containsKey("measure")) {
            logger.error("Measure template code not found");
            return null;
        }
        List<String> newlines = new ArrayList<>();
        for(String line : templates.get("measure")) {
            line = line.replace("#*dc_sub_log*#", name + "_" + component + dc_log_file);
            line = line.replace("#*root_sub*#", component);
            line = line.replace("#*from_sub*#", from);
            line = line.replace("#*to_sub*#", to);
            newlines.add(line);
        }
        return newlines;
    }

    private List<String> generateFinalTcl() {
        if(!templates.containsKey("final")) {
            logger.error("Final template code not found");
            return null;
        }
        return templates.get("final");
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
}
