package org.qualet.irl.patcher;

import java.util.ArrayList;
import java.util.List;

/** Outcome of applying a patch, with a human-readable log. */
public final class PatchResult
{
    public boolean ok = true;
    public String summary = "";
    public final List<String> log = new ArrayList<>();

    public void info(String message)
    {
        this.log.add(message);
    }

    public void fail(String message)
    {
        this.ok = false;
        this.summary = message;
        this.log.add("ERROR: " + message);
    }
}
