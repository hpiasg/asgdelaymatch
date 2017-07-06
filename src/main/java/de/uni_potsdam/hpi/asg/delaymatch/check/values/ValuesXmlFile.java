package de.uni_potsdam.hpi.asg.delaymatch.check.values;

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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.uni_potsdam.hpi.asg.delaymatch.check.values.model.ValuesXml;

public class ValuesXmlFile {
    protected static final Logger logger         = LogManager.getLogger();
    protected static final String schemafilename = "/values_xml.xsd";

    public static ValuesXml readIn(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ValuesXml.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new StreamSource(ValuesXml.class.getResourceAsStream(schemafilename)));

            jaxbUnmarshaller.setSchema(schema);

            if(file.exists()) {
                return (ValuesXml)jaxbUnmarshaller.unmarshal(file);
            } else {
                logger.error("File " + file.getAbsolutePath() + " not found");
                return null;
            }
        } catch(JAXBException e) {
            if(e.getLinkedException() instanceof SAXParseException) {
                SAXParseException e2 = (SAXParseException)e.getLinkedException();
                logger.error("File: " + file.getAbsolutePath() + ", Line: " + e2.getLineNumber() + ", Col: " + e2.getColumnNumber());
                logger.error(e2.getLocalizedMessage());
                return null;
            }
            logger.error(e.getLocalizedMessage());
            return null;
        } catch(SAXException e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public static boolean writeOut(ValuesXml valxml, File file) {
        try {
            Writer fw = new FileWriter(file);
            JAXBContext context = JAXBContext.newInstance(ValuesXml.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(valxml, fw);
            return true;
        } catch(JAXBException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        } catch(IOException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }
}
