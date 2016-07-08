package de.uni_potsdam.hpi.asg.delaymatch.helper;

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;

public abstract class AbstractScriptGenerator {
    private static final Logger                logger = LogManager.getLogger();
    protected static Map<String, List<String>> templates;

    protected static boolean readTemplateCodeSnippets(File templatefile, String[] templatenames) {
        if(templates == null) {
            templates = new HashMap<String, List<String>>();
            List<String> current = null;
            List<String> lines = FileHelper.getInstance().readFile(templatefile);
            boolean templatefound = false;
            for(String line : lines) {
                templatefound = false;
                for(String str : templatenames) {
                    if(line.equals("#+" + str + "_begin+#")) {
                        if(current != null) {
                            logger.error("Found begin before end: " + str);
                            return false;
                        }
                        if(templates.containsKey(str)) {
                            logger.error("Templatename already registered: " + str);
                        }
                        current = new ArrayList<>();
                        templates.put(str, current);
                        templatefound = true;
                        break;
                    } else if(line.equals("#+" + str + "_end+#")) {
                        current = null;
                        templatefound = true;
                        break;
                    }
                }
                if(!templatefound) {
                    if(current != null) {
                        current.add(line);
                    }
                }
            }
        }
        return true;
    }
}
