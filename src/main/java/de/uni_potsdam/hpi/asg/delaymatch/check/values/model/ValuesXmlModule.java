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
public class ValuesXmlModule {

    //@formatter:off
    @XmlAttribute(name = "name", required = true)
    private String name;
    
    @XmlElement(name = "path", required = true)
    private List<ValuesXmlPath> paths;
    //@formatter:on

    protected ValuesXmlModule() {
    }

    public ValuesXmlModule(String name) {
        this.name = name;
        this.paths = new ArrayList<>();
    }

    public void addPath(ValuesXmlPath path) {
        paths.add(path);
    }

    public String getName() {
        return name;
    }

    public List<ValuesXmlPath> getPaths() {
        return paths;
    }
}
