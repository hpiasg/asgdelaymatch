package de.uni_potsdam.hpi.asg.logictool.trace.helper;

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

public class TraceCmp implements Comparator<TempTrace> {

    @Override
    public int compare(TempTrace arg0, TempTrace arg1) {
        int i = 0;
        while(true) {
            if(arg0.getTrace().size() == i && arg1.getTrace().size() == i) {
                return 0;
            }
            if(arg0.getTrace().size() == i) {
                return -1;
            }
            if(arg1.getTrace().size() == i) {
                return 1;
            }
            int cmpT = arg0.getTrace().get(i).compareTo(arg1.getTrace().get(i));
            if(cmpT != 0) {
                return cmpT;
            }
            i++;
        }
    }
}
