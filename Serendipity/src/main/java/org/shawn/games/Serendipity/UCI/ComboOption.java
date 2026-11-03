/*
 * SerenSkibidi additions, 2026-11-02.
 * Licensed under GNU AGPL v3 or later; see LICENSE.
 */
package org.shawn.games.Serendipity.UCI;

import java.util.List;

public final class ComboOption implements UCIOption
{
    private volatile String value;
    private final String name;
    private final String defaultValue;
    private final List<String> choices;

    public ComboOption(String name, String defaultValue, String... choices)
    {
        this.name = name;
        this.choices = List.of(choices);
        if (!this.choices.contains(defaultValue)) throw new IllegalArgumentException("invalid default");
        this.value = this.defaultValue = defaultValue;
        UCI.addOption(name, this);
    }
    public String getString() { return value; }
    public void set(String value)
    {
        for (String choice : choices)
            if (choice.equalsIgnoreCase(value)) { this.value = choice; return; }
        throw new IllegalArgumentException("expected one of " + choices);
    }
    public String toString()
    {
        return "option name " + name + " type combo default " + defaultValue
                + " var " + String.join(" var ", choices);
    }
}
