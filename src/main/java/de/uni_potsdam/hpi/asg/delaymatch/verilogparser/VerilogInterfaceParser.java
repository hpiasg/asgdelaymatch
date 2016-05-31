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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerilogInterfaceParser {

    private static final Pattern  linebuspattern  = Pattern.compile("\\s*(input|output)\\s*\\[\\s*(\\d+):(\\d+)\\]\\s*(.*);");
    private static final Pattern  linepattern     = Pattern.compile("\\s*(input|output)\\s*(.*);");

    private static final Pattern  hssignalpattern = Pattern.compile("(.*)\\_(\\d+)(r|a|d)");

    private Map<String, Variable> variables;

    public VerilogInterfaceParser() {
        this.variables = new HashMap<>();
    }

    public void addLine(String line) {
        Matcher m = linebuspattern.matcher(line);
        String signals = null;
        Integer datawidth = null;
        if(m.matches()) {
            int left = Integer.parseInt(m.group(2));
            int right = Integer.parseInt(m.group(3));
            datawidth = Math.abs(left - right) + 1;
            signals = m.group(4);
        } else {
            m = linepattern.matcher(line);
            if(m.matches()) {
                signals = m.group(2);
            } else {
                return; // line is neither input nor output defintion line
            }
        }
        String[] signalsplit = signals.split(",");
        for(String str : signalsplit) {
            str = str.trim();
            m = hssignalpattern.matcher(str);
            if(m.matches()) {
                String name = m.group(1);
                int id = Integer.parseInt(m.group(2));
                if(!this.variables.containsKey(name)) {
                    this.variables.put(name, new Variable(name));
                }
                Variable var = this.variables.get(name);
                if(m.group(3).equals("d")) {
                    if(datawidth != null) {
                        var.setDatawidth(datawidth);
                    } else {
                        var.setDatawidth(1);
                    }
                }
                var.setCountWithId(id);
            }
        }
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }
}
