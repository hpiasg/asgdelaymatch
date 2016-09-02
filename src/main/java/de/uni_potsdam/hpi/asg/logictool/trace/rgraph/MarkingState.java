package de.uni_potsdam.hpi.asg.logictool.trace.rgraph;

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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractState;

public class MarkingState extends AbstractState<MarkingState> {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void setSignalState(Signal sig, Value val) {
        logger.error("Not implemented");
    }

    @Override
    public Map<Signal, Value> getStateValues() {
        logger.error("Not implemented");
        return null;
    }

    @Override
    public boolean isSignalSet(Signal sig) {
        logger.error("Not implemented");
        return false;
    }
}
