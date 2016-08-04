package de.uni_potsdam.hpi.asg.delaymatch.verilogparser;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceAbstract;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstanceMapping;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal.Direction;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroupSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalNormal;

public class VerilogModuleContentParser {
    private static final Logger                 logger                = LogManager.getLogger();

    private static final Pattern                linebuspattern        = Pattern.compile("\\s*(input|output|wire)\\s*\\[\\s*(\\d+):(\\d+)\\]\\s*(.*);");
    private static final Pattern                linepattern           = Pattern.compile("\\s*(input|output|wire)\\s*(.*);");
    private static final Pattern                instancePattern       = Pattern.compile("\\s*(.*)\\s+([A-Za-z0-9]+)\\s+\\((.*)\\);\\s*");
    private static final Pattern                mappedPositionPattern = Pattern.compile("\\.(.*)\\((.*)\\)");

    private static final Pattern                hssignalpattern       = Pattern.compile("(.*)\\_(\\d+)(r|a|d)");

    private Map<String, VerilogSignal>          signals;
    private Map<String, VerilogSignalGroup>     signalgroups;
    private List<VerilogModuleInstanceAbstract> instances;

    private List<String>                        interfaceSignalNames;

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
            List<VerilogModuleInstanceMapping> interfaceSignals = new ArrayList<>();
            String[] splitsig = m.group(3).split(",");
            for(String str : splitsig) {
                m2 = mappedPositionPattern.matcher(str.trim());
                if(m2.matches()) {
                    String moduleSigName = m2.group(1);
                    String localSigName = m2.group(2).trim();
                    localSigName = localSigName.replaceAll("\\[.*\\]", ""); //TODO: ??
                    if(!signals.containsKey(localSigName)) {
                        logger.error("Signal " + localSigName + " not found");
                        return false;
                    }
                    interfaceSignals.add(new VerilogModuleInstanceMapping(signals.get(localSigName), moduleSigName));
                } else {
                    logger.warn("Positional mapping not yet implemented");
                    return false;
                }
            }
            this.instances.add(new VerilogModuleInstanceAbstract(modulename, instancename, interfaceSignals));
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
                if(this.signals.containsKey(str)) {
                    logger.error("Name already registered");
                    return false;
                }
                this.signals.put(str, new VerilogSignalGroupSignal(group, str, id));
            } else {
                if(this.signals.containsKey(str)) {
                    logger.error("Name already registered");
                    return false;
                }
                this.signals.put(str, new VerilogSignalNormal(str, dir));
                VerilogSignal var = this.signals.get(str);
                if(datawidth != null) {
                    var.setDatawidth(datawidth);
                } else {
                    var.setDatawidth(1);
                }
            }
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

    public List<VerilogModuleInstanceAbstract> getInstances() {
        return instances;
    }
}
