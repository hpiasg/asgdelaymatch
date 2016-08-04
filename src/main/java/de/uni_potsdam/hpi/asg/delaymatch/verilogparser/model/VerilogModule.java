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

import java.util.List;

public class VerilogModule {

    private String              name;
    private List<VerilogSignal> interfaceSignals;

    public VerilogModule(String name, List<VerilogSignal> interfaceSignals) {
        this.name = name;
        this.interfaceSignals = interfaceSignals;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(name + ": ");
        for(VerilogSignal sig : interfaceSignals) {
            str.append(sig.toString() + "; ");
        }
        str.setLength(str.length() - 2);
        return str.toString();
    }

    public String getName() {
        return name;
    }
}
