package de.uni_potsdam.hpi.asg.delaymatch.check.values.model;

/*
 * Copyright (C) 2017 Norman Kluge
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
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ValuesXmlInstance {

    //@formatter:off
    @XmlAttribute(name = "name", required = true)
    private String name;
    
    @XmlElement(name = "value", required = true)
    private float value;
    @XmlElement(name = "check", required = true)
    private Float check;
    @XmlElement(name = "past", required = false)
    private Float past;
    @XmlElement(name = "future", required = false)
    private Float future;
    //@formatter:on

    public ValuesXmlInstance(String name, float value, Float check, Float past, Float future) {
        this.name = name;
        this.value = value;
        this.check = check;
        this.past = past;
        this.future = future;
    }

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }

    public float getCheck() {
        return check;
    }

    public Float getPast() {
        return past;
    }

    public Float getFuture() {
        return future;
    }
}
