package de.uni_potsdam.hpi.asg.delaymatch.misc;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;

public class DelayMatchModuleInst {
    private VerilogModuleInstance               inst;
    private DelayMatchModule                    dmmodule;
    private Map<MatchPath, MeasureRecord>       measureAdditions;
    private Map<MatchPath, List<MeasureRecord>> futureSubtractions;
    private Map<MatchPath, List<Trace>>         pastSubtrationTraces;

    public DelayMatchModuleInst(VerilogModuleInstance inst, DelayMatchModule dmmodule) {
        this.inst = inst;
        this.dmmodule = dmmodule;
        this.measureAdditions = new HashMap<>();
        this.futureSubtractions = new HashMap<>();
        this.pastSubtrationTraces = new HashMap<>();
    }

    public void addMeasureAddition(MatchPath p, MeasureRecord rec) {
        measureAdditions.put(p, rec);
    }

    public void addFutureSubtraction(MatchPath p, MeasureRecord rec) {
        if(!futureSubtractions.containsKey(p)) {
            futureSubtractions.put(p, new ArrayList<MeasureRecord>());
        }
        futureSubtractions.get(p).add(rec);
    }

    public void addPastSubtractionTraces(MatchPath p, List<Trace> t) {
        if(!pastSubtrationTraces.containsKey(p)) {
            pastSubtrationTraces.put(p, new ArrayList<Trace>());
        }
        pastSubtrationTraces.get(p).addAll(t);
    }

    public List<MeasureRecord> getFutureSubtractions(MatchPath path) {
        return futureSubtractions.get(path);
    }

    public MeasureRecord getMeasureAddition(MatchPath path) {
        return measureAdditions.get(path);
    }

    public List<Trace> getPastSubtrationTraces(MatchPath path) {
        return pastSubtrationTraces.get(path);
    }

    public DelayMatchModule getDMmodule() {
        return dmmodule;
    }

    public String getInstName() {
        return inst.getInstName();
    }

    public VerilogModuleInstance getVerilogModuleInst() {
        return inst;
    }
}
