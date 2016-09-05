package de.uni_potsdam.hpi.asg.delaymatch;

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

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;

public class ScriptTemplateGenerator {

    public static void main(String[] args) {
        String[] types = {"min", "max"};
        String[] edges = {"rise_", "fall_", ""};

        String newline = FileHelper.getNewline();
        StringBuilder str = new StringBuilder();
        for(String t : types) {
            for(String from : edges) {
                for(String to : edges) {
                    String fromStr = from.equals("") ? "both" : from.replace("_", "");
                    String toStr = to.equals("") ? "both" : to.replace("_", "");
                    str.append("#+measure_" + t + "_" + fromStr + "_" + toStr + "_begin+#" + newline);
                    str.append("redirect -append #*dc_sub_log*# {" + newline);
                    str.append("    set rvs [report_timing " + "-" + from + "from { #*from_sub*# } " + "-" + to + "to { #*to_sub*# } " + "-path full -delay " + t + " -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]" + newline);
                    str.append("}" + newline);
                    str.append("if {$rvs == 0} {" + newline);
                    str.append("    echo \"report_timing_fail #*root_sub*# " + t + " " + fromStr + "_from: { #*from_sub*# } " + toStr + "_to: { #*to_sub*# }\"" + newline);
                    str.append("}" + newline);
                    str.append("#+measure_" + t + "_" + fromStr + "_" + toStr + "_end+#" + newline);
                    str.append(newline);
                }
            }
        }
        System.out.println(str.toString());
    }
}
