package de.uni_potsdam.hpi.asg.delaymatch.profile;

import java.util.HashSet;
import java.util.Set;

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

@XmlAccessorType(XmlAccessType.NONE)
public class Port {
    private static final Logger logger = LogManager.getLogger();

    public enum SignalType {
        req, ack, data
    }

    //@formatter:off
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "id", required = true)
    private ID id;
    @XmlAttribute(name = "type", required = true)
    private SignalType type;
    @XmlAttribute(name = "bit", required = false)
    private Bit bit;
    //@formatter:on

    public String getName() {
        return name;
    }

    public ID getId() {
        return id;
    }

    public SignalType getType() {
        return type;
    }

    public Bit getBit() {
        return bit;
    }

    public Set<VerilogSignal> getCorrespondingSignals(VerilogModule mod) {
        Set<VerilogSignal> retVal = new HashSet<>();
        VerilogSignalGroup group = mod.getSignalGroups().get(name);
        if(group == null) {
            logger.warn("Group " + name + " in module " + mod.getModulename() + " not found. Maybe a wrong profile file was used?");
            return null;
        }

        if(id.isEach()) {
            for(int i = 0; i < group.getCount(); i++) {
                String sigName = name + "_" + i + typeString();
                VerilogSignal sig = mod.getSignal(sigName);
                if(sig == null) {
                    logger.warn("Signal " + sigName + " in module " + mod.getModulename() + " not found. Maybe a wrong profile file was used?");
                    return null;
                }
                retVal.add(sig);
            }
        } else {
            String sigName = name + "_" + id.getId() + typeString();
            VerilogSignal sig = mod.getSignal(sigName);
            if(sig == null) {
                logger.warn("Signal " + sigName + " in module " + mod.getModulename() + " not found. Maybe a wrong profile file was used?");
                return null;
            }
            retVal.add(sig);
        }

        return retVal;
    }

    private String typeString() {
        switch(type) {
            case ack:
                return "a";
            case data:
                return "d";
            case req:
                return "r";
        }
        return "";
    }

    @Override
    public String toString() {
        return name + "_" + id.toString() + type + ((bit != null) ? "[" + bit.toString() + "]" : "");
    }
}
