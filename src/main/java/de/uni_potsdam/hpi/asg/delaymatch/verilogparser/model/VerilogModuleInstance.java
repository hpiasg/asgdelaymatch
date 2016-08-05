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

import java.util.HashSet;
import java.util.Set;

public class VerilogModuleInstance {

    private VerilogModule                module;
    private Set<VerilogModuleConnection> connections;

    public VerilogModuleInstance(VerilogModule module) {
        this.module = module;
        this.module.addSubmodule(this);
        this.connections = new HashSet<>();
    }

    public boolean addConnection(VerilogModuleConnection con) {
        return connections.add(con);
    }

    public Set<VerilogModuleConnection> getConnections() {
        return connections;
    }

    public VerilogModule getModule() {
        return module;
    }
}
