package de.uni_potsdam.hpi.asg.delaymatch.check;

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.ValuesXmlFile;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXml;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlEach;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlInstance;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlModule;
import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXmlPath;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModule;
import de.uni_potsdam.hpi.asg.delaymatch.model.DelayMatchModuleInst;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureEntry;
import de.uni_potsdam.hpi.asg.delaymatch.model.MeasureRecord;
import de.uni_potsdam.hpi.asg.delaymatch.profile.MatchPath;
import de.uni_potsdam.hpi.asg.delaymatch.setup.MeasureRecordGenerator;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.PTBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.ParallelBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.SequenceBox;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.Trace;
import de.uni_potsdam.hpi.asg.delaymatch.trace.model.TransitionBox;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignalGroup;

public class CheckMain {
    private static final Logger                         logger      = LogManager.getLogger();

    private static final Pattern                        arrivalTime = Pattern.compile("\\s+data arrival time\\s+([0-9.]+)");
    private static final Pattern                        pathSpec    = Pattern.compile("ASGdm;(.*);");

    private Map<String, DelayMatchModule>               modules;
    private Table<Transition, Transition, MeasureEntry> transtable;
    private boolean                                     allok;
    private Constraints                                 constr;
    private File                                        sdcfile;
    private Map<DelayMatchModule, ValuesXmlModule>      valmods;

    public CheckMain(Map<String, DelayMatchModule> modules, MeasureRecordGenerator rec) {
        this.modules = modules;
        this.transtable = rec.getTransTable();
        this.allok = false;
        this.constr = new Constraints(modules);
        this.sdcfile = new File(WorkingdirGenerator.getInstance().getWorkingdir(), "sdc.sdc");
        this.valmods = new HashMap<>();
    }

    public boolean check() {
        allok = false;
        if(!parseValues()) {
            return false;
        }

        if(!checkTiming()) {
            return false;
        }

        if(!constr.generate(sdcfile)) {
            return false;
        }

        allok = modules.isEmpty();
        return true;
    }

    public boolean writeOutVals(File file) {
        List<ValuesXmlModule> valmodlist = new ArrayList<>(valmods.values());
        ValuesXml valxml = new ValuesXml(valmodlist);
        return ValuesXmlFile.writeOut(valxml, file);
    }

    private boolean parseValues() {
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getMeasureOutputfile() != null) {
                List<String> lines = FileHelper.getInstance().readFile(mod.getMeasureOutputfile());
                if(lines == null) {
                    return false;
                }
                Matcher m = null;
                String currSpec = null;
                for(String line : lines) {
                    m = pathSpec.matcher(line);
                    if(m.matches()) {
                        currSpec = m.group(1);
                        continue;
                    }
                    m = arrivalTime.matcher(line);
                    if(m.matches()) {
                        if(currSpec == null) {
                            logger.error("No spec?");
                            return false;
                        }
                        mod.addValue(currSpec, Float.parseFloat(m.group(1)));
                        currSpec = null;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkTiming() {
        Set<DelayMatchModule> rmmodules = new HashSet<>();
        for(DelayMatchModule mod : modules.values()) {
            if(mod.getProfilecomp() != null) {
                ValuesXmlModule valmod = new ValuesXmlModule(mod.getModuleName());
                valmods.put(mod, valmod);
                boolean moduleHasMatch = false;
                for(MatchPath path : mod.getProfilecomp().getMatchpaths()) {
                    ValuesXmlPath valpath = new ValuesXmlPath(path.getId());
                    valmod.addPath(valpath);
                    if(path.getForeach() != null) {
                        VerilogSignalGroup group = mod.getSignalGroups().get(path.getForeach());
                        if(group == null) {
                            logger.error("Signal must be group signal!");
                            return false;
                        }
                        int num = group.getCount();
                        int numfails = 0;
                        for(int eachid = 0; eachid < num; eachid++) {
                            ValuesXmlEach valeach = new ValuesXmlEach(Integer.toString(eachid));
                            valpath.addEach(valeach);
                            Float[] vals = computeValue(path, eachid, mod, valeach);
                            if(vals[0] == null) {
                                return false;
                            }
                            if(vals[0] < 0f) { // No delay wooho
                                setValuesXmlEach(mod, path, eachid, valeach);
                                continue;
                            }
                            mod.setMatchVal(path, eachid, vals[1]);
                            setValuesXmlEach(mod, path, eachid, valeach);
                            moduleHasMatch = true;
                            numfails++;
                        }
                        if(numfails == 0) { // all eachs are matched
                        } else if(numfails == num) { // all eachs are unmachted
                        } else { // some are matched, others not
                            mod.increasePathFactors(path);
                        }
                    } else {
                        ValuesXmlEach valeach = new ValuesXmlEach(ValuesXmlEach.NOEACHID);
                        valpath.addEach(valeach);
                        Float[] vals = computeValue(path, null, mod, valeach);
                        if(vals[0] == null) {
                            return false;
                        }
                        if(vals[0] < 0f) { // No delay wooho
                            setValuesXmlEach(mod, path, valeach);
                            continue;
                        }
                        mod.setMatchVal(path, vals[1]);
                        setValuesXmlEach(mod, path, valeach);
                        moduleHasMatch = true;
                    }
                    setPathFactorXml(mod, path, valpath);
                }
                if(!moduleHasMatch) {
                    rmmodules.add(mod);
                }
            }
        }

        Set<DelayMatchModule> newrmmodules = new HashSet<>();
        while(!rmmodules.isEmpty()) {
            for(DelayMatchModule mod : rmmodules) {
                modules.remove(mod.getModuleName());
            }

            for(DelayMatchModule mod : modules.values()) {
                for(DelayMatchModule rmmod : rmmodules) {
                    mod.removeMeasureRecordsFromRequester(rmmod);
                }
                if(mod.getMeasureRecords().isEmpty()) {
                    newrmmodules.add(mod);
                }
            }

            rmmodules.clear();
            rmmodules.addAll(newrmmodules);
            newrmmodules.clear();
        }

        return true;
    }

    private void setValuesXmlEach(DelayMatchModule mod, MatchPath path, ValuesXmlEach valeach) {
        Float[] factors = mod.getValFactors(path);
        valeach.setMinValueFactor(factors[0]);
        valeach.setMaxValueFactor(factors[1]);
    }

    private void setPathFactorXml(DelayMatchModule mod, MatchPath path, ValuesXmlPath valpath) {
        Float[] factors = mod.getPathFactors(path);
        valpath.setMinValueFactor(factors[0]);
        valpath.setMaxValueFactor(factors[1]);
    }

    private void setValuesXmlEach(DelayMatchModule mod, MatchPath path, Integer eachid, ValuesXmlEach valeach) {
        Float[] factors = mod.getValFactors(path, eachid);
        valeach.setMinValueFactor(factors[0]);
        valeach.setMaxValueFactor(factors[1]);
    }

    private Float[] computeValue(MatchPath path, Integer eachid, DelayMatchModule mod, ValuesXmlEach valeach) {
        List<Float> values = new ArrayList<>();
        List<Float> setValues = new ArrayList<>();
        logger.info("Module " + mod.getModuleName());
        for(DelayMatchModuleInst inst : mod.getInstances()) {
            MeasureRecord rec = inst.getMeasureAddition(path, eachid);
            if(rec == null) {
                logger.error("No record for path found");
                return null;
            }
            float val = rec.getValue();
            logger.info("\tInstance " + inst.getInstName());
            logger.info("\tMeasure path: " + rec.getId().replace("max_", "").replaceAll("both_", ""));
            logger.info("\t\tValue:  " + String.format("%+2.5f", rec.getValue()));

            Float futureSubtraction = computeFutureSubtraction(path, eachid, inst);
            if(futureSubtraction != null && futureSubtraction != 0f) {
                logger.info("\t\tFuture: " + String.format("%+2.5f", (0f - futureSubtraction)));
                val -= futureSubtraction;
            }

            Float pastSubtraction = computePastSubtraction(path, eachid, inst);
            if(pastSubtraction != null && pastSubtraction != 0f) {
                logger.info("\t\tPast:   " + String.format("%+2.5f", (0f - pastSubtraction)));
                val -= pastSubtraction;
            }

            float setVal = val;
            Float checkSubtraction = computeCheckSubtraction(path, eachid, inst);
            if(checkSubtraction != null && checkSubtraction != 0f) {
                logger.info("\t\tCheck:  " + String.format("%+2.5f", (0f - checkSubtraction)));
                val -= checkSubtraction;
            }

            logger.info("\t\tFinal:  " + String.format("%+2.5f", val));
            values.add(val);
            setValues.add(setVal);

            ValuesXmlInstance valinst = new ValuesXmlInstance(inst.getInstName(), rec.getValue(), checkSubtraction, pastSubtraction, futureSubtraction);
            valeach.addInstance(valinst);
        }

        int maxIndex = 0;
        float maxVal = Float.NEGATIVE_INFINITY;
        for(int i = 0; i < values.size(); i++) {
            float f = values.get(i);
            if(f > maxVal) {
                maxVal = f;
                maxIndex = i;
            }
        }
//        Float maxVal = Collections.max(values);
        logger.info("\tFinal module:   " + String.format("%+2.5f", maxVal) + " (" + ((maxVal < 0f) ? "No violation" : "Violation") + ")");
        return new Float[]{values.get(maxIndex), setValues.get(maxIndex)};
    }

    private Float computePastSubtraction(MatchPath path, Integer eachid, DelayMatchModuleInst inst) {
        Float pastSubtraction = null;
        if(inst.getPastSubtrationTraces(path, eachid) == null) {
            return null;
        }
        for(Trace trace : inst.getPastSubtrationTraces(path, eachid)) {
            Float tval = computeTraceValue(trace);
            if(tval == null) {
                continue;
            }
            if(pastSubtraction == null) {
                pastSubtraction = tval;
            }
            if(pastSubtraction > tval) {
                pastSubtraction = tval;
            }
        }
        return pastSubtraction;
    }

    private Float computeTraceValue(Trace trace) {
        return computeSBoxValue(trace.getTrace());
    }

    private Float computeSBoxValue(SequenceBox box) {
        Float seqVal = 0f;
        for(PTBox inner : box.getContent()) {
            if(inner instanceof TransitionBox) {
                TransitionBox tinner = (TransitionBox)inner;
                for(TransitionBox prev : tinner.getPrevs()) {
                    if(!transtable.contains(prev.getTransition(), tinner.getTransition())) {
                        logger.error("Transition combination not found: " + prev.getTransition() + ", " + tinner.getTransition());
                        return null;
                    }
                    MeasureEntry entry = transtable.get(prev.getTransition(), tinner.getTransition());
                    switch(entry.getType()) {
                        case unknown:
                        case datapathDelay:
                        case externalDelay:
                            break;
                        case recordDelay:
                            if(entry.getRecord().getValue() == null) {
                                logger.error("No value for " + prev.getTransition() + ", " + tinner.getTransition());
                                return null;
                            }
                            seqVal += entry.getRecord().getValue();
                            break;
                    }

                }
            } else if(inner instanceof ParallelBox) {
                ParallelBox pinner = (ParallelBox)inner;
                Float parMax = 0f;
                for(SequenceBox sb : pinner.getParallelLines()) {
                    Float par = computeSBoxValue(sb);
                    if(parMax < par) {
                        parMax = par;
                    }
                }
                seqVal += parMax;
            }
        }
        return seqVal;
    }

    private Float computeFutureSubtraction(MatchPath path, Integer eachid, DelayMatchModuleInst inst) {
        Float futureSubtraction = null;
        if(inst.getFutureSubtractions(path, eachid) == null) {
            return null;
        }
        for(MeasureRecord recF : inst.getFutureSubtractions(path, eachid)) {
            if(futureSubtraction == null) {
                futureSubtraction = recF.getValue();
            }
            if(futureSubtraction > recF.getValue()) {
                futureSubtraction = recF.getValue();
            }
        }
        return futureSubtraction;
    }

    private Float computeCheckSubtraction(MatchPath path, Integer eachid, DelayMatchModuleInst inst) {
        Float checkSubtraction = null;
        if(inst.getCheckSubtractions(path, eachid) == null) {
            return null;
        }
        for(MeasureRecord recF : inst.getCheckSubtractions(path, eachid)) {
            if(checkSubtraction == null) {
                checkSubtraction = recF.getValue();
            }
            if(checkSubtraction > recF.getValue()) {
                checkSubtraction = recF.getValue();
            }
        }
        return checkSubtraction;
    }

    public boolean isAllOk() {
        return allok;
    }

    public File getSdcFile() {
        return sdcfile;
    }
}
