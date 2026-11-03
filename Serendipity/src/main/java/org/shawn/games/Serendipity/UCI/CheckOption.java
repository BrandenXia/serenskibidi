/*
 * SerenSkibidi additions, 2026-11-02.
 * Licensed under GNU AGPL v3 or later; see LICENSE.
 */
package org.shawn.games.Serendipity.UCI;

/** Strict UCI boolean with visibility across the command and search threads. */
public final class CheckOption implements UCIOption
{
    private volatile boolean value;
    private final boolean defaultValue;
    private final String name;

    public CheckOption(boolean value, String name)
    {
        this.value = this.defaultValue = value;
        this.name = name;
        UCI.addOption(name, this);
    }

    public boolean get() { return value; }
    public void set(String value)
    {
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("expected true or false");
        this.value = Boolean.parseBoolean(value);
    }
    public String getString() { return Boolean.toString(value); }
    public String toString()
    {
        return "option name " + name + " type check default " + defaultValue;
    }
}
