package de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model;

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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerilogModuleInstance {
    private static final Logger                         logger = LogManager.getLogger();

    private VerilogModule                               module;
    private String                                      instName;
    private Map<VerilogSignal, VerilogModuleConnection> connections;

    public VerilogModuleInstance(VerilogModule module, String instName) {
        this.module = module;
        this.instName = instName;
        this.connections = new HashMap<>();
    }

    public boolean addConnection(VerilogModuleConnection con, VerilogSignal sig) {
        if(!this.module.getSignals().values().contains(sig)) {
            logger.error("Signal is not part of module!");
            return false;
        }
        this.connections.put(sig, con);
        return true;
    }

    public VerilogModuleConnection getConnection(VerilogSignal sig) {
        return connections.get(sig);
    }

    public VerilogModule getModule() {
        return module;
    }

    public Map<VerilogSignal, VerilogModuleConnection> getConnections() {
        return connections;
    }

    public String getInstName() {
        return instName;
    }

    @Override
    public String toString() {
        return "ModInst:" + module.getModulename() + "(" + instName + ")";
    }
}
