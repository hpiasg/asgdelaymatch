package de.uni_potsdam.hpi.asg.delaymatch.helper;

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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.profile.Port;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;

public class PortHelper {
    private static final Logger logger = LogManager.getLogger();

    public static String getPortListAsString(List<Port> ports, Integer eachid, Map<String, VerilogSignal> vars, String instance) {
        StringBuilder str = new StringBuilder();
        for(Port p : ports) {
            str.append(getPortAsString(p, eachid, vars, instance) + " ");
        }
        str.setLength(str.length() - 1);
        return str.toString();
    }

    public static String getPortListAsString(List<Port> ports, Integer eachid, Map<String, VerilogSignal> vars) {
        return getPortListAsString(ports, eachid, vars, null);
    }

    private static String getPortAsString(Port p, Integer eachid, Map<String, VerilogSignal> vars, String instance) {
        int id = (p.getId().isEach()) ? eachid : p.getId().getId();
        String type = null;
        switch(p.getType()) {
            case ack:
                type = "a";
                break;
            case data:
                VerilogSignal var = vars.get(p.getName());
                if(var == null) {
                    logger.error("Variable not found");
                    return null;
                }
                if(var.getWidth() == 1) {
                    type = "d";
                } else if(var.getWidth() > 1) {
                    if(p.getBit().isALL()) {
                        type = "d[*]";
                    } else {
                        type = "d[" + p.getBit().getId() + "]";
                    }
                }
                if(type == null) {
                    System.out.println();
                }
                break;
            case req:
                type = "r";
                break;
        }

        return ((instance == null) ? "" : instance + "/") + p.getName() + "_" + id + type;
    }
}
