package de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerilogModule {
    private static final Logger                         logger = LogManager.getLogger();

    private String                                      modulename;
    private List<VerilogSignal>                         interfaceSignals;
//    private List<String>                                code;
    private Map<String, VerilogSignal>                  signals;
    private Map<String, VerilogSignalGroup>             signalgroups;

    private List<VerilogModuleInstance>                 submodules;
    private Map<VerilogSignal, VerilogModuleConnection> connections;
    private List<VerilogModuleInstance>                 instances;
    private boolean                                     hasContent;

    public VerilogModule(String modulename, List<VerilogSignal> interfaceSignals, List<String> code, Map<String, VerilogSignal> signals, Map<String, VerilogSignalGroup> signalgroups) {
        this.modulename = modulename;
        this.interfaceSignals = interfaceSignals;
//        this.code = code;
        this.signals = signals;
        this.signalgroups = signalgroups;
        this.submodules = new ArrayList<>();
        this.connections = new HashMap<>();
        this.instances = new ArrayList<>();
        this.hasContent = false;
    }

    public boolean addSubmodule(VerilogModuleInstance submodule) {
        return this.submodules.add(submodule);
    }

    public boolean addConnection(VerilogModuleConnection con, VerilogSignal sig) {
        if(!this.signals.values().contains(sig)) {
            logger.error("Signal is not part of module!");
            return false;
        }
        this.connections.put(sig, con);
        return true;
    }

    public VerilogModuleConnection getConnection(VerilogSignal sig) {
        return connections.get(sig);
    }

//    public VerilogModuleConnection getConnectionOrCreate(VerilogSignal sig) {
//        if(!connections.containsKey(sig)) {
//            connections.put(sig, new VerilogModuleConnection(this, sig));
//        }
//        return connections.get(sig);
//    }

    public VerilogModuleInstance getNewInstance(String id) {
        VerilogModuleInstance inst = new VerilogModuleInstance(this, id);
        this.instances.add(inst);
        return inst;
    }

    public VerilogSignal getSignal(String name) {
        return signals.get(name);
    }

    public VerilogSignal getSignal(Integer pos) {
        return interfaceSignals.get(pos);
    }

    public Map<String, VerilogSignal> getSignals() {
        return signals;
    }

    public Map<String, VerilogSignalGroup> getSignalGroups() {
        return signalgroups;
    }

    public String getModulename() {
        return modulename;
    }

    public Map<VerilogSignal, VerilogModuleConnection> getConnections() {
        return connections;
    }

    public List<VerilogModuleInstance> getSubmodules() {
        return submodules;
    }

    public List<VerilogModuleInstance> getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        return "Mod:" + modulename;
    }

    public void setHasContent(boolean hasContent) {
        this.hasContent = hasContent;
    }

    public boolean hasContent() {
        return hasContent;
    }
}
