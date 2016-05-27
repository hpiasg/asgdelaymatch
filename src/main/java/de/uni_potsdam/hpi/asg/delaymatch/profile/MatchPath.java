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
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class MatchPath {

    //@formatter:off
    @XmlElement(name = "measure", required = true)
    private Path measure;
    @XmlElement(name = "match", required = true)
    private Path match;
    @XmlAttribute(name = "foreach", required = false)
    private String foreach;
    //@formatter:on

    public Path getMatch() {
        return match;
    }

    public Path getMeasure() {
        return measure;
    }

    public String getForeach() {
        return foreach;
    }
}
