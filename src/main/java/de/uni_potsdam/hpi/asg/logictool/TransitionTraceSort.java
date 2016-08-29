package de.uni_potsdam.hpi.asg.logictool;

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

import java.util.Comparator;

import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;

public class TransitionTraceSort implements Comparator<Transition> {

    @Override
    public int compare(Transition o1, Transition o2) {
        if(o1 == o2) {
            return 0;
        }
        if(findPost(o1, o2)) {
            return -1;
        }
        if(findPre(o1, o2)) {
            return 1;
        }
        System.out.println(o1 + " vs. " + o2);
        return 0;
    }

    private boolean findPost(Transition o1, Transition find) {
        if(o1.getPostset().size() == 1) {
            if(o1.getPostset().get(0).getPostset().size() == 1) {
                if(o1.getPostset().get(0).getPostset().get(0) == find) {
                    return true;
                } else {
                    return findPost(o1.getPostset().get(0).getPostset().get(0), find);
                }
            }
        }
        return false;
    }

    private boolean findPre(Transition o1, Transition find) {
        if(o1.getPreset().size() == 1) {
            if(o1.getPreset().get(0).getPreset().size() == 1) {
                if(o1.getPreset().get(0).getPreset().get(0) == find) {
                    return true;
                } else {
                    return findPre(o1.getPreset().get(0).getPreset().get(0), find);
                }
            }
        }
        return false;
    }

}
