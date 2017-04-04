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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ValuesXmlPath {

    //@formatter:off
    @XmlAttribute(name = "id", required = true)
    private String id;
    
    @XmlElement(name = "each", required = true)
    private List<ValuesXmlEach> eachs;
    //@formatter:on

    protected ValuesXmlPath() {
    }

    public ValuesXmlPath(String id) {
        this.id = id;
        this.eachs = new ArrayList<>();
    }

    public void addEach(ValuesXmlEach each) {
        eachs.add(each);
    }

    public String getId() {
        return id;
    }

    public List<ValuesXmlEach> getEachs() {
        return eachs;
    }

    public ValuesXmlEach getEach(String eachid) {
        for(ValuesXmlEach valeach : eachs) {
            if(valeach.getId().equals(eachid)) {
                return valeach;
            }
        }
        return null;
    }
}
