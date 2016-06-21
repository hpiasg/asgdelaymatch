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

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

public class ProfileSchemaGenerator {

    public static void main(String[] args) {
        final File baseDir = new File("./src/main/resources/schema");
        final String filename = "profile_config.xsd";

        class MySchemaOutputResolver extends SchemaOutputResolver {
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                return new StreamResult(new File(baseDir, filename));
            }
        }

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(ProfileComponents.class);
            context.generateSchema(new MySchemaOutputResolver());
        } catch(JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("done");
    }
}
