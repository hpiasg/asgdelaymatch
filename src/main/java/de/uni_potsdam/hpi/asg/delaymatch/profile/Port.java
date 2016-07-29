package de.uni_potsdam.hpi.asg.delaymatch.profile;

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

@XmlAccessorType(XmlAccessType.NONE)
public class Port {

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

    @Override
    public String toString() {
        return name + "_" + id.toString() + type + ((bit != null) ? "[" + bit.toString() + "]" : "");
    }
}
