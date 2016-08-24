package de.uni_potsdam.hpi.asg.delaymatch.verilogparser;

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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModule;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleConnection;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogModuleInstance;
import de.uni_potsdam.hpi.asg.delaymatch.verilogparser.model.VerilogSignal;

public class VerilogGraph extends JFrame {
    private static final Logger logger           = LogManager.getLogger();
    private static final long   serialVersionUID = 801986534712786851L;

    public VerilogGraph(VerilogModule rootModule, boolean wait, File exp) {
        super("VerilogModule graph - " + rootModule.getModulename());

        final mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        mxIGraphModel model = graph.getModel();

        final mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.getGraphControl().addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if(cell != null && cell instanceof mxCell) {
                    mxCell mxcell = (mxCell)cell;
                    graph.getModel().beginUpdate();
                    mxcell.setStyle("textOpacity=100");
                    graph.getModel().endUpdate();
                    new mxHierarchicalLayout(graph).execute(graph.getDefaultParent());
                    new mxParallelEdgeLayout(graph).execute(graph.getDefaultParent());

                    //System.out.println(.toString());
                }
            }
        });

        graph.setAutoSizeCells(true);
        graph.setCellsResizable(false);

        graph.setCellsEditable(false);
        graph.setAllowDanglingEdges(false);
        graph.setAllowLoops(false);
        graph.setCellsDeletable(false);
        graph.setCellsCloneable(false);
        graph.setCellsDisconnectable(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        graph.setCellsBendable(false);

        Map<VerilogModuleInstance, Object> map = new HashMap<>();
        graph.getModel().beginUpdate();

//        StringBuilder legendstr = new StringBuilder();
//        for(Signal sig : stategraph.getAllSignals()) {
//            legendstr.append(sig.getName() + " ");
//        }
//        Object legend = graph.insertVertex(parent, null, legendstr.toString(), 0, 0, 0, 0, "");
//        graph.updateCellSize(legend);
        for(VerilogModuleInstance inst : rootModule.getSubmodules()) {
            String style = "shape=square";

            Object v = graph.insertVertex(parent, null, inst.getModule().getModulename(), 0, 0, 0, 0, style);
            graph.updateCellSize(v);
//            mxGeometry geo = graph.getCellGeometry(v);
//            mxGeometry geo2 = new mxGeometry(0, 0, geo.getWidth() * 1.5, geo.getHeight() * 1.5);
//            model.setGeometry(v, geo2);

            map.put(inst, v);
        }

        Object environment = graph.insertVertex(parent, null, "Environment", 0, 0, 0, 0, "shape=square");

        for(VerilogModuleInstance inst : rootModule.getSubmodules()) {

            for(VerilogModuleConnection con : inst.getConnections().values()) {//rootModule.getConnections().values()) {
                if(con.getHostSig().getName().equals("_reset")) {
                    continue;
                }
                if(con.getReader().isEmpty()) {
                    graph.insertEdge(parent, null, con.toString(), map.get(con.getWriter()), environment, "textOpacity=0");
                    continue;
                }

                for(Entry<VerilogModuleInstance, VerilogSignal> entry : con.getReader().entrySet()) {
                    Object from = null;
                    if(con.getWriter() == null) {
                        from = environment;
                    } else {
                        from = map.get(con.getWriter());
                    }
                    graph.insertEdge(parent, null, con.toString(), from, map.get(entry.getKey()), "textOpacity=0");
                }
            }
        }

//        for(State s : states2) {
//            for(Entry<Transition, State> entry : s.getNextStates().entrySet()) {
//                String str = entry.getKey().toString();
//                State s2 = null;
//                for(State s3 : entry.getValue().getPrevStates()) {
//                    if(s3 == s) {
//                        s2 = s3;
//                    }
//                }
//                if(s2 == null) {
//                    logger.error("next yes, prev no");
//                    str += " / XXX";
//                }
//                String style = "";
//                graph.insertEdge(parent, null, str, map.get(s), map.get(entry.getValue()), style);
//            }
//            for(State s2 : s.getPrevStates()) {
//                State s3 = null;
//                for(Entry<Transition, State> entry : s2.getNextStates().entrySet()) {
//                    if(entry.getValue() == s) {
//                        s3 = entry.getValue();
//                    }
//                }
//                if(s3 == null) {
//                    logger.error("next no, prev yes");
//                    graph.insertEdge(parent, null, "", map.get(s2), map.get(s), "strokeColor=red");
//                }
//            }
//        }

        graph.getModel().endUpdate();

        mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
        layout.setForceConstant(150);
        layout.execute(graph.getDefaultParent());

        graphComponent.setConnectable(false);

        getContentPane().add(graphComponent);

//        if(exp != null) {
//            ExportAction a = new ExportAction();
//            a.exportPng(exp, graph, graphComponent);
//        }

        setSize(1024, 768);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

//        graphComponent.getInputMap().put(KeyStroke.getKeyStroke('s'), "save");
//        graphComponent.getActionMap().put("save", new ExportAction());

        //http://stackoverflow.com/questions/5603306/jgraphx-auto-organise-of-cells-and-bidirectional-edges
        new mxHierarchicalLayout(graph).execute(graph.getDefaultParent());
        new mxParallelEdgeLayout(graph).execute(graph.getDefaultParent());

        if(wait) {
            while(isVisible()) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
