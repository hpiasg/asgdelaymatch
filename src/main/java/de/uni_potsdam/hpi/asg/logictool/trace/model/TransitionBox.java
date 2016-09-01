package de.uni_potsdam.hpi.asg.logictool.trace.model;

import de.uni_potsdam.hpi.asg.common.stg.model.Transition;

public class TransitionBox extends Box implements PTBox {

    private Transition trans;

    public TransitionBox(Box superBox, Transition trans) {
        super(superBox);
        this.trans = trans;
    }

    public Transition getTransition() {
        return trans;
    }

    @Override
    public String toString() {
        return "[T " + trans.toString() + "]";
    }
}
