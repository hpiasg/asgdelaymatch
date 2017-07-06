package de.uni_potsdam.hpi.asg.delaymatch.verilogparser;

/*
 * Copyright (C) 2016 - 2017 Norman Kluge
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleConnection;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceConnectionTemp;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceTemp;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;

public class VerilogParser {
    private static final Logger        logger           = LogManager.getLogger();

    private static final Pattern       modulepattern    = Pattern.compile("^\\s*module (.*) \\((.*)\\);\\s*$");
    private static final Pattern       endmodulepattern = Pattern.compile("^\\s*endmodule\\s*$");
    private static final Pattern       linepattern      = Pattern.compile("^.*;\\s*$");

    private Map<String, VerilogModule> modules;
    private VerilogModule              rootModule;

    public VerilogParser() {
        rootModule = null;
    }

    public boolean parseVerilogStructure(File vfile) {
        List<String> lines = FileHelper.getInstance().readFile(vfile);
        if(lines == null) {
            return false;
        }
        Matcher m = null;
        Queue<String> linequeue = new LinkedList<>(lines);
        String line = null;
        String currModule = null;

        Map<String, VerilogModuleContentParser> parserMap = new HashMap<>();
        Map<String, List<String>> linesMap = new HashMap<>();

        VerilogModuleContentParser currContentParser = null;
        String rootModuleName = null;

        while((line = linequeue.poll()) != null) {
            do {
                if(line.startsWith("`")) {
                    line = "";
                }

                if(line.contains("//")) {
                    line = line.substring(0, line.indexOf("//"));
                }
                if(line.contains("/*")) {
                    String linebegin = line.substring(0, line.indexOf("/*"));
                    String comment = line.substring(line.indexOf("/*"));
                    while(!comment.contains("*/")) {
                        comment = comment + linequeue.poll();
                    }
                    String lineend = comment.substring(comment.indexOf("*/") + 2);
                    line = linebegin + lineend;
                }

                m = linepattern.matcher(line);
                if(m.matches()) {
                    break;
                }
                // after endmodule ";" is not required (strangely)
                m = endmodulepattern.matcher(line);
                if(m.matches()) {
                    break;
                }
                if(line.equals("")) {
                    break;
                }

                String tmp = linequeue.poll();
                if(tmp == null) {
                    logger.error("no ; but null: #" + line + "#");
                    return false;
                }
                line = line + tmp;
            } while(true);

            m = modulepattern.matcher(line);
            if(m.matches()) {
                String modulename = m.group(1);
                String[] split = m.group(2).split(",");
                List<String> interfaceSignals = new ArrayList<>();
                for(String signal : split) {
                    interfaceSignals.add(signal.trim());
                }
                currContentParser = new VerilogModuleContentParser(interfaceSignals);
                parserMap.put(modulename, currContentParser);
                rootModuleName = modulename;
                currModule = modulename;
                linesMap.put(currModule, new ArrayList<String>());
                linesMap.get(currModule).add(line);
                continue;
            }

            m = endmodulepattern.matcher(line);
            if(m.matches()) {
                if(currModule == null) {
                    logger.error("endmode before module");
                    return false;
                }
                linesMap.get(currModule).add(line);
                currModule = null;
                currContentParser = null;
                continue;
            }

            if(currContentParser != null) {
                currContentParser.addLine(line);
            }
            if(currModule != null) {
                linesMap.get(currModule).add(line);
            }
        }

//        Map<String, Boolean> foundmap = new HashMap<>();
//        Map<String, Integer> nummap = new HashMap<>();
//        for(Entry<String, VerilogModuleContentParser> entry : parserMap.entrySet()) {
//            for(VerilogModuleInstanceAbstract inst : entry.getValue().getInstances()) {
//                foundmap.put(inst.getModuleName(), parserMap.containsKey(inst.getModuleName()));
//                if(!nummap.containsKey(inst.getModuleName())) {
//                    nummap.put(inst.getModuleName(), 0);
//                }
//                nummap.put(inst.getModuleName(), nummap.get(inst.getModuleName()) + 1);
//            }
//        }
//        for(Entry<String, Boolean> entry : foundmap.entrySet()) {
//            System.out.println(entry.toString() + ", " + nummap.get(entry.getKey()));
//            if(nummap.get(entry.getKey()) > 1 && entry.getValue()) {
//                System.out.println(linesMap.get(entry.getKey()));
//            }
//        }

        modules = new HashMap<>();
        for(Entry<String, VerilogModuleContentParser> entry : parserMap.entrySet()) {
            String modulename = entry.getKey();
            VerilogModuleContentParser parser = entry.getValue();
            VerilogModule mod = new VerilogModule(modulename, parser.getInterface(), linesMap.get(modulename), parser.getSignals(), parser.getSignalGroups());
            modules.put(modulename, mod);
            if(modulename.equals(rootModuleName)) {
                rootModule = mod;
            }
        }

        for(VerilogModule module : modules.values()) {
            VerilogModuleContentParser moduleparser = parserMap.get(module.getModulename());
            module.setHasContent(moduleparser.hasSubInst());
            for(VerilogModuleInstanceTemp tinst : moduleparser.getInstances()) {
                if(parserMap.containsKey(tinst.getModuleName())) {
                    VerilogModule submodule = modules.get(tinst.getModuleName());
                    VerilogModuleInstance submoduleinst = submodule.getNewInstance(tinst.getInstanceName());
                    module.addSubmodule(submoduleinst);
                    for(VerilogModuleInstanceConnectionTemp tcon : tinst.getInterfaceSignals()) {
                        VerilogSignal moduleSignal = tcon.getLocalSig();
                        VerilogSignal submoduleSignal = getSubModuleSignal(submodule, tcon);
                        if(submoduleSignal == null) {
                            return false;
                        }

                        VerilogModuleConnection con = null;
                        if((con = module.getConnection(moduleSignal)) == null) {
                            con = new VerilogModuleConnection(module, moduleSignal);
                        }
                        switch(moduleSignal.getDirection()) {
                            case input:
                                switch(submoduleSignal.getDirection()) {
                                    case input:
                                        con.addReader(submoduleinst, submoduleSignal);
                                        break;
                                    case output:
                                        logger.error("fail inp out");
                                        return false;
                                    case wire:
                                        logger.error("fail sub wire");
                                        return false;
                                }
                                break;
                            case output:
                                switch(submoduleSignal.getDirection()) {
                                    case input:
                                        logger.warn("output signal '" + moduleSignal.getName() + "' of component '" + module.getModulename() + "' used as input for subcomponent '" + submodule.getModulename() + "'");
                                        con.addReader(submoduleinst, submoduleSignal);
                                        break; //return false;
                                    case output:
                                        con.setWriter(submoduleinst, submoduleSignal);
                                        break;
                                    case wire:
                                        logger.error("fail sub wire");
                                        return false;
                                }
                                break;
                            case wire:
                                switch(submoduleSignal.getDirection()) {
                                    case input:
                                        con.addReader(submoduleinst, submoduleSignal);
                                        break;
                                    case output:
                                        con.setWriter(submoduleinst, submoduleSignal);
                                        break;
                                    case wire:
                                        logger.error("fail sub wire");
                                        return false;
                                }
                                break;
                        }
                    }
                }
            }
        }
        return true;
    }

    private VerilogSignal getSubModuleSignal(VerilogModule submodule, VerilogModuleInstanceConnectionTemp tcon) {
        if(tcon.getModuleSigName() != null) {
            return submodule.getSignal(tcon.getModuleSigName());
        } else if(tcon.getModuleSigPos() != null) {
//            System.out.println(tcon.getModuleSigPos() + " (" + submodule + ") = " + submodule.getSignal(tcon.getModuleSigPos()));
            return submodule.getSignal(tcon.getModuleSigPos());
        } else {
            logger.error("Position and name are null");
            return null;
        }
    }

    public Map<String, VerilogModule> getModules() {
        return modules;
    }

    public VerilogModule getRootModule() {
        return rootModule;
    }
}
