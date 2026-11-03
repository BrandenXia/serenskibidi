/* SerenSkibidi additions, 2026-11-02. GNU AGPL v3 or later; see LICENSE. */
package org.shawn.games.Serendipity;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Before;
import org.junit.Test;
import org.shawn.games.Serendipity.Chess.Board;
import org.shawn.games.Serendipity.Chess.move.Move;
import org.shawn.games.Serendipity.Search.Listener.FinalReport;
import org.shawn.games.Serendipity.Search.Listener.SearchReport;
import org.shawn.games.Serendipity.UCI.*;

public class SkibidiFeaturesTest
{
    @Before public void reset() { SkibidiFeatures.registerOptions(); }

    @Test public void openingsAreLegalAndDoNotMutateBoard()
    {
        String[][] lines = {
            {"Gambit", "e2e4 e7e5 f2f4 e5f4 g1f3"},
            {"Bongcloud", "e2e4 e7e5 e1e2 e8e7"}
        };
        for (String[] line : lines)
        {
            Board board = new Board();
            for (String expected : line[1].split(" "))
            {
                String before = board.getFen();
                Move move = SkibidiFeatures.bookMove(board, line[0]);
                assertEquals(expected, move.toString());
                assertTrue(board.legalMoves().contains(move));
                assertEquals(before, board.getFen());
                board.doMove(move);
            }
            assertNull(SkibidiFeatures.bookMove(board, line[0]));
        }
    }

    @Test public void gambitBranchesAndFallback()
    {
        Board board = new Board();
        assertNull(SkibidiFeatures.bookMove(board, "Off"));
        board.doMove("d2d4"); board.doMove("d7d5");
        assertEquals("c2c4", SkibidiFeatures.bookMove(board, "Gambit").toString());
        assertNull(SkibidiFeatures.bookMove(board, "Bongcloud"));
        board = new Board(); board.doMove("e2e4"); board.doMove("c7c5");
        assertEquals("b2b4", SkibidiFeatures.bookMove(board, "Gambit").toString());
        board = new Board(); board.doMove("a2a3");
        assertNull(SkibidiFeatures.bookMove(board, "Gambit"));
        board.loadFromFen(new Board().getFen(false) + " 0 50");
        assertNull(SkibidiFeatures.bookMove(board, "Gambit"));
        board.loadFromFen(new Board().getFen(false) + " 100 1");
        assertNull(SkibidiFeatures.bookMove(board, "Gambit"));
    }

    @Test public void analysisNeverUsesBook()
    {
        assertTrue(SkibidiFeatures.isTimedPlay("go wtime 1000 btime 1000".split(" ")));
        assertTrue(SkibidiFeatures.isTimedPlay("go movetime 100".split(" ")));
        for (String limit : new String[]{"depth 5", "nodes 100", "mate 2", "infinite", "ponder", "searchmoves e2e4"})
            assertFalse(SkibidiFeatures.isTimedPlay(("go movetime 100 " + limit).split(" ")));
        assertFalse(SkibidiFeatures.isTimedPlay(new String[]{"go"}));
    }

    @Test public void auraIsBoundedSymmetricAndMonotonic()
    {
        assertEquals(0, SkibidiFeatures.auraPoints(0));
        int last = -100;
        for (int cp = -10000; cp <= 10000; cp++)
        {
            int aura = SkibidiFeatures.auraPoints(cp);
            assertTrue(aura >= -100 && aura <= 100 && aura >= last);
            assertEquals(-aura, SkibidiFeatures.auraPoints(-cp));
            last = aura;
        }
    }

    @Test public void malformedOptionsLeavePreviousValue()
    {
        CheckOption check = new CheckOption(false, "Test Check");
        check.set("TRUE");
        try { check.set("maybe"); fail(); } catch (IllegalArgumentException expected) {}
        assertTrue(check.get());
        ComboOption combo = new ComboOption("Test Combo", "Off", "Off", "Gambit");
        combo.set("gambit");
        try { combo.set("typo"); fail(); } catch (IllegalArgumentException expected) {}
        assertEquals("Gambit", combo.getString());
    }

    @Test public void personalityIsOptionalAndProtocolSafe()
    {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(bytes));
            SearchReport report = new SearchReport(4, 4, 1000, 0, 0, 10, new Board(), new Move[0]);
            SkibidiFeatures.report(report);
            assertEquals("", bytes.toString());
            UCI.setOption("skibidi mode", "true");
            UCI.setOption("aura meter", "true");
            SkibidiFeatures.report(report);
            for (String line : bytes.toString().split("\\R")) assertTrue(line.startsWith("info string "));
            assertTrue(bytes.toString().contains("aura 0/100 side-to-move"));
            bytes.reset();
            new UCIListener().notify(new FinalReport(null));
            assertTrue(bytes.toString().contains("bestmove 0000"));
        }
        finally { System.setOut(original); }
    }
}
