package de.uni_potsdam.hpi.asg.delaymatch.helper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.uni_potsdam.hpi.asg.common.remote.AbstractScript;
import de.uni_potsdam.hpi.asg.common.remote.RemoteInformation;

public abstract class AbstractErrorRemoteScript extends AbstractScript {

    private int                    nextErrorId;
    protected Map<Integer, String> errorMsgMap;

    public AbstractErrorRemoteScript(RemoteInformation rinfo, String subdir, File outputDir, boolean removeRemoteDir) {
        super(rinfo, subdir, outputDir, removeRemoteDir);
        this.nextErrorId = 1;
        this.errorMsgMap = new HashMap<>();
    }

    protected void setErrorMsg(Map<String, String> replacements, String msg) {
        replacements.put("sim_sh_exitcode", Integer.toString(nextErrorId));
        errorMsgMap.put(nextErrorId++, msg);
    }
}
