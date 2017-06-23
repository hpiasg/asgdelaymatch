package de.uni_potsdam.hpi.asg.delaymatch.runner;

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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters.GeneralBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractRunPanel;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractRunner.TerminalMode;
import de.uni_potsdam.hpi.asg.delaymatch.DelayMatchMain;
import de.uni_potsdam.hpi.asg.delaymatch.runner.DelayMatchParameters.BooleanParam;
import de.uni_potsdam.hpi.asg.delaymatch.runner.DelayMatchParameters.EnumParam;
import de.uni_potsdam.hpi.asg.delaymatch.runner.DelayMatchParameters.TextParam;

public class RunDelayMatchPanel extends AbstractRunPanel {
    private static final long    serialVersionUID = 2663337555026127634L;
    private static final Logger  logger           = LogManager.getLogger();

    private DelayMatchParameters params;
    private Window               parent;
    private boolean              userHitRun;

    private boolean              fixSTGfile;

    public RunDelayMatchPanel(Window parent, final DelayMatchParameters params, boolean isDebug) {
        this(parent, params, isDebug, false, false, false);
    }

    public RunDelayMatchPanel(final Window parent, final DelayMatchParameters params, boolean isDebug, boolean hideGeneral, boolean fixSTGfile, final boolean closeOnRun) {
        super(params);
        final RunDelayMatchPanel thepanel = this;
        this.params = params;
        this.parent = parent;
        this.fixSTGfile = fixSTGfile;

        this.setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        this.add(tabbedPane, BorderLayout.CENTER);

        constructGeneralPanel(tabbedPane);
        constructAdvancedPanel(tabbedPane);
        constructDebugPanel(tabbedPane, isDebug);

        JButton runBtn = new JButton("Run");
        runBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(closeOnRun) {
                    parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
                    thepanel.userHitRun = true;
                    return;
                }
                DelayMatchRunner run = new DelayMatchRunner(params);
                run.run(TerminalMode.frame);
            }
        });
        this.add(runBtn, BorderLayout.PAGE_END);

        if(hideGeneral) {
            tabbedPane.setEnabledAt(0, false);
            tabbedPane.setSelectedIndex(1);
        }
    }

    private void constructGeneralPanel(JTabbedPane tabbedPane) {
        PropertiesPanel panel = new PropertiesPanel(parent);
        tabbedPane.addTab("General", null, panel, null);
        GridBagLayout gbl_generalpanel = new GridBagLayout();
        gbl_generalpanel.columnWidths = new int[]{150, 300, 30, 80, 0};
        gbl_generalpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_generalpanel.rowHeights = new int[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 0};
        gbl_generalpanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_generalpanel);

        panel.addTextEntry(0, TextParam.VerilogFile, "Verilog file", params.getDefVerilogInFileName(), true, JFileChooser.FILES_ONLY, false);
        panel.addTextEntry(1, TextParam.ProfileFile, "Profile file", params.getDefProfileFileName(), true, JFileChooser.FILES_ONLY, true);

        String[] techs = params.getAvailableTechs();
        String defTech = params.getDefTech();
        if(techs.length == 0) {
            logger.error("No technologies installed. Please run ASGtechmngr");
            errorOccured = true;
        }
        panel.addTechnologyChooserWithDefaultEntry(2, "Technology library", techs, defTech, EnumParam.TechLib, BooleanParam.TechLibDef, "Use default");
        addOutSection(panel, 3, params.getDefOutFileName(), params.getDefOutDirName());
        // 4: blank
        addIOSection(panel, 6, DelayMatchMain.DEF_CONFIG_FILE_NAME, params.getDefLogFileName(), params.getDefZipFileName());

        getDataFromPanel(panel);
    }

    private void constructAdvancedPanel(JTabbedPane tabbedPane) {
        PropertiesPanel panel = new PropertiesPanel(parent);
        tabbedPane.addTab("Advanced", null, panel, null);
        GridBagLayout gbl_advpanel = new GridBagLayout();
        gbl_advpanel.columnWidths = new int[]{200, 300, 30, 80, 0};
        gbl_advpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_advpanel.rowHeights = new int[]{15, 15, 15, 0};
        gbl_advpanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_advpanel);

        panel.addCheckboxEntry(0, BooleanParam.future, "Future algorithm", false);
        panel.addCheckboxEntry(1, BooleanParam.past, "Past algorithm", false);

        boolean stgPathButton = !fixSTGfile;
        panel.addTextEntry(2, TextParam.STGFile, "STG file for past algorithm", params.getDefPastStgFileName(), stgPathButton, JFileChooser.FILES_ONLY, false);

        getDataFromPanel(panel);

        if(fixSTGfile) {
            final JTextField field = textfields.get(TextParam.STGFile);
            field.setEnabled(false);
        }
    }

    private void constructDebugPanel(JTabbedPane tabbedPane, boolean isDebug) {
        PropertiesPanel panel = new PropertiesPanel(parent);
        if(isDebug) {
            tabbedPane.addTab("Debug", null, panel, null);
        }
        GridBagLayout gbl_advpanel = new GridBagLayout();
        gbl_advpanel.columnWidths = new int[]{200, 300, 30, 80, 0};
        gbl_advpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_advpanel.rowHeights = new int[]{15, 0};
        gbl_advpanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_advpanel);

        panel.addCheckboxEntry(0, GeneralBooleanParam.debug, "Debug", isDebug);

        getDataFromPanel(panel);
    }

    public boolean isUserHitRun() {
        return userHitRun;
    }
}