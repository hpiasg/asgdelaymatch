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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceTemp;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceConnectionTemp;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal.Direction;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroupSignal;

public class VerilogModuleContentParser {
    private static final Logger             logger                = LogManager.getLogger();

//    private static final String             commentBehindLine     = "\\s*(\\/\\/.*)?";
    private static final Pattern            linebuspattern        = Pattern.compile("\\s*(input|output|wire)\\s*\\[\\s*(\\d+):(\\d+)\\]\\s*(.*);\\s*");
    private static final Pattern            linepattern           = Pattern.compile("\\s*(input|output|wire)\\s*(.*);\\s*");
    private static final Pattern            instancePattern       = Pattern.compile("\\s*(.*)\\s+([A-Za-z0-9_]+)\\s+\\((.*)\\);\\s*");
    private static final Pattern            mappedPositionPattern = Pattern.compile("\\.(.*)\\((.*)\\)");

    private static final Pattern            hssignalpattern       = Pattern.compile("(.*)\\_(\\d+)(r|a|d)");

    private Map<String, VerilogSignal>      signals;
    private Map<String, VerilogSignalGroup> signalgroups;
    private List<VerilogModuleInstanceTemp> instances;

    private List<String>                    interfaceSignalNames;

    public VerilogModuleContentParser(List<String> interfaceSignalNames) {
        this.signals = new HashMap<>();
        this.signalgroups = new HashMap<>();
        this.instances = new ArrayList<>();
        this.interfaceSignalNames = interfaceSignalNames;
    }

    public boolean addLine(String line) {
        if(!addLineInterface(line)) {
            return false;
        }
        if(!addLineInstance(line)) {
            return false;
        }
        return true;
    }

    private boolean addLineInstance(String line) {
        Matcher m = instancePattern.matcher(line);
        Matcher m2 = null;
        if(m.matches()) {
            String modulename = m.group(1);
            String instancename = m.group(2);
            List<VerilogModuleInstanceConnectionTemp> interfaceSignals = new ArrayList<>();
            String[] splitsig = m.group(3).split(",");
            int id = 0;
            for(String str : splitsig) {
                m2 = mappedPositionPattern.matcher(str.trim());
                if(m2.matches()) {
                    // mapped
                    String moduleSigName = m2.group(1);
                    String localSigName = m2.group(2).trim();
                    localSigName = localSigName.replaceAll("\\[.*\\]", ""); //TODO: ??
                    if(!signals.containsKey(localSigName)) {
//                        logger.error("Signal " + localSigName + " not found");
//                        return false;
                        signals.put(localSigName, new VerilogSignal(localSigName, Direction.wire));
                    }
                    interfaceSignals.add(new VerilogModuleInstanceConnectionTemp(signals.get(localSigName), moduleSigName));
                } else {
                    // positional
                    String localSigName = str.trim();
                    localSigName = localSigName.replaceAll("\\[.*\\]", ""); //TODO: ??
                    if(!signals.containsKey(localSigName)) {
//                        logger.error("Signal " + localSigName + " not found");
//                        return false;
                        signals.put(localSigName, new VerilogSignal(localSigName, Direction.wire));
                    }
                    interfaceSignals.add(new VerilogModuleInstanceConnectionTemp(signals.get(localSigName), id));
                }
                id++;
            }
            this.instances.add(new VerilogModuleInstanceTemp(modulename, instancename, interfaceSignals));
        }

        return true;
    }

    private boolean addLineInterface(String line) {
        Matcher m = linebuspattern.matcher(line);
        String signalnames = null;
        Integer datawidth = null;
        Direction dir = null;
        if(m.matches()) {
            dir = getDirection(m.group(1));
            int left = Integer.parseInt(m.group(2));
            int right = Integer.parseInt(m.group(3));
            datawidth = Math.abs(left - right) + 1;
            signalnames = m.group(4);
        } else {
            m = linepattern.matcher(line);
            if(m.matches()) {
                dir = getDirection(m.group(1));
                signalnames = m.group(2);
            } else {
                return true; // line is neither input nor output defintion line
            }
        }

        if(dir == null) {
            logger.error("Direction unkown");
            return false;
        }

        String[] signalsplit = signalnames.split(",");
        for(String str : signalsplit) {
            str = str.trim();
            if(this.signals.containsKey(str)) {
                VerilogSignal sig = signals.get(str);
                if(sig.getDirection() != Direction.wire) {
                    continue;
                } else {
                    signals.remove(str);
                }
            }

            m = hssignalpattern.matcher(str);
            VerilogSignal sig = null;
            if(m.matches()) {
                String name = m.group(1);
                int id = Integer.parseInt(m.group(2));

                if(!this.signalgroups.containsKey(name)) {
                    this.signalgroups.put(name, new VerilogSignalGroup(name, dir));
                }
                VerilogSignalGroup group = this.signalgroups.get(name);
                if(m.group(3).equals("d")) {
                    if(datawidth != null) {
                        group.setDatawidth(datawidth);
                    } else {
                        group.setDatawidth(1);
                    }
                }
                group.setCountWithId(id);
                sig = new VerilogSignalGroupSignal(str, dir, group, id);
            } else {
                sig = new VerilogSignal(str, dir);
            }

            if(this.signals.containsKey(str)) {
                logger.error("Name already registered");
                return false;
            }
            if(datawidth != null) {
                sig.setWidth(datawidth);
            } else {
                sig.setWidth(1);
            }
            this.signals.put(str, sig);
        }

        return true;
    }

    private Direction getDirection(String str) {
        switch(str) {
            case "input":
                return Direction.input;
            case "output":
                return Direction.output;
            case "wire":
                return Direction.wire;
        }
        return null;
    }

    public List<VerilogSignal> getInterfaceSignals() {
        List<VerilogSignal> interfaceSignals = new ArrayList<>();
        for(String name : interfaceSignalNames) {
            VerilogSignal sig = signals.get(name);
            if(sig == null) {
                logger.error("Signal " + name + " not found");
                return null;
            }
            interfaceSignals.add(sig);
        }
        return interfaceSignals;
    }

    public List<VerilogModuleInstanceTemp> getInstances() {
        return instances;
    }

    public Map<String, VerilogSignal> getSignals() {
        return signals;
    }

    public Map<String, VerilogSignalGroup> getSignalGroups() {
        return signalgroups;
    }

    public List<VerilogSignal> getInterface() {
        List<VerilogSignal> retVal = new ArrayList<>();
        for(String str : interfaceSignalNames) {
            if(!signals.containsKey(str)) {
                logger.error("Signal " + str + " not found");
                return null;
            }
            retVal.add(signals.get(str));
        }
        return retVal;
    }
}
