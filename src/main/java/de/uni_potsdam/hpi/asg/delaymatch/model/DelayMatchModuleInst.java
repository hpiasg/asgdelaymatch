package de.uni_potsdam.hpi.asg.delaymatch.model;

/*
 * Copyright (C) 2016 - 2017 Norman Kluge
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;

public class DelayMatchModuleInst {
    private VerilogModuleInstance                          inst;
    private DelayMatchModule                               dmmodule;
    private Table<MatchPath, Integer, MeasureRecord>       measureAdditions;
    private Table<MatchPath, Integer, List<MeasureRecord>> futureSubtractions;
    private Table<MatchPath, Integer, List<Trace>>         pastSubtrationTraces;
    private Table<MatchPath, Integer, List<MeasureRecord>> checkSubtractions;

    public DelayMatchModuleInst(VerilogModuleInstance inst, DelayMatchModule dmmodule) {
        this.inst = inst;
        this.dmmodule = dmmodule;
        this.measureAdditions = HashBasedTable.create();
        this.futureSubtractions = HashBasedTable.create();
        this.pastSubtrationTraces = HashBasedTable.create();
        this.checkSubtractions = HashBasedTable.create();
    }

    public void addMeasureAddition(MatchPath p, Integer eachid, MeasureRecord rec) {
        measureAdditions.put(p, convertEachId(eachid), rec);
    }

    public void addFutureSubtraction(MatchPath p, Integer eachid, MeasureRecord rec) {
        if(!futureSubtractions.contains(p, convertEachId(eachid))) {
            futureSubtractions.put(p, convertEachId(eachid), new ArrayList<MeasureRecord>());
        }
        futureSubtractions.get(p, convertEachId(eachid)).add(rec);
    }

    public void addCheckSubtraction(MatchPath p, Integer eachid, MeasureRecord rec) {
        if(!checkSubtractions.contains(p, convertEachId(eachid))) {
            checkSubtractions.put(p, convertEachId(eachid), new ArrayList<MeasureRecord>());
        }
        checkSubtractions.get(p, convertEachId(eachid)).add(rec);
    }

    public void addPastSubtractionTraces(MatchPath p, Integer eachid, List<Trace> t) {
        if(!pastSubtrationTraces.contains(p, convertEachId(eachid))) {
            pastSubtrationTraces.put(p, convertEachId(eachid), new ArrayList<Trace>());
        }
        pastSubtrationTraces.get(p, convertEachId(eachid)).addAll(t);
    }

    public List<MeasureRecord> getFutureSubtractions(MatchPath path, Integer eachid) {
        return futureSubtractions.get(path, convertEachId(eachid));
    }

    public List<MeasureRecord> getCheckSubtractions(MatchPath path, Integer eachid) {
        return checkSubtractions.get(path, convertEachId(eachid));
    }

    public MeasureRecord getMeasureAddition(MatchPath path, Integer eachid) {
        return measureAdditions.get(path, convertEachId(eachid));
    }

    public List<Trace> getPastSubtrationTraces(MatchPath path, Integer eachid) {
        return pastSubtrationTraces.get(path, convertEachId(eachid));
    }

    private int convertEachId(Integer eachid) {
        return eachid == null ? -1 : eachid;
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
