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
import java.util.Map.Entry;

public class VerilogModuleConnection {

    private VerilogModuleInstance                     writer;
    private VerilogSignal                             writerSig;
    private Map<VerilogModuleInstance, VerilogSignal> reader;
    private VerilogModule                             host;
    private VerilogSignal                             hostSig;

    public VerilogModuleConnection(VerilogModule host, VerilogSignal hostSig) {
        this.reader = new HashMap<>();
        this.host = host;
        this.hostSig = hostSig;
    }

    public void setWriter(VerilogModuleInstance writer, VerilogSignal writerSig) {
        this.writer = writer;
        this.writerSig = writerSig;
    }

    public void addReader(VerilogModuleInstance writer, VerilogSignal writerSig) {
        this.reader.put(writer, writerSig);
    }

    public boolean isInternal() {
        return(writer == null || reader.isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(writer != null) {
            str.append(writer.getModule().getModulename() + ":" + writerSig.getName());
        } else {
            str.append("XX");
        }
        str.append(" > ");
        str.append("(" + host.getModulename() + ":" + hostSig.getName() + ")");
        str.append(" > ");
        if(!reader.isEmpty()) {
            for(Entry<VerilogModuleInstance, VerilogSignal> entry : reader.entrySet()) {
                str.append(entry.getKey().getModule().getModulename() + ":" + entry.getValue().getName() + " | ");
            }
            str.setLength(str.length() - 3);
        } else {
            str.append("XX");
        }
        return str.toString();
    }
}
