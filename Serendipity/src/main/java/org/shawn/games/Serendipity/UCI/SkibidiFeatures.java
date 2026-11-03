/*
 * SerenSkibidi additions, 2026-11-02.
 * Licensed under GNU AGPL v3 or later; see LICENSE.
 */
package org.shawn.games.Serendipity.UCI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.shawn.games.Serendipity.Chess.Board;
import org.shawn.games.Serendipity.Chess.move.Move;
import org.shawn.games.Serendipity.Search.AlphaBeta;
import org.shawn.games.Serendipity.Search.Listener.SearchReport;

/** Optional personality at the UCI boundary; no search evaluation changes. */
public final class SkibidiFeatures
{
    private static final Map<String, String> GAMBITS = new HashMap<>();
    private static final Map<String, String> BONGCLOUD = new HashMap<>();
    private static final Set<String> ANALYSIS_LIMITS = Set.of("depth", "nodes", "mate", "infinite", "ponder", "searchmoves");
    private static CheckOption commentary;
    private static CheckOption aura;
    private static ComboOption opening;

    static
    {
        addLine(GAMBITS, "e2e4 e7e5 f2f4 e5f4 g1f3");
        addLine(GAMBITS, "d2d4 d7d5 c2c4 e7e6 b1c3");
        addLine(GAMBITS, "e2e4 c7c5 b2b4 c5b4 a2a3");
        addLine(BONGCLOUD, "e2e4 e7e5 e1e2 e8e7");
    }

    private SkibidiFeatures() {}

    public static void registerOptions()
    {
        commentary = new CheckOption(false, "Skibidi Mode");
        aura = new CheckOption(false, "Aura Meter");
        opening = new ComboOption("Sigma Opening", "Off", "Off", "Gambit", "Bongcloud");
    }

    private static void addLine(Map<String, String> book, String line)
    {
        Board board = new Board();
        for (String uci : line.split(" "))
        {
            Move move = board.legalMoves().stream().filter(m -> m.toString().equals(uci)).findFirst().orElseThrow();
            book.putIfAbsent(board.getFen(false), uci);
            board.doMove(move);
        }
    }

    /** Book is for timed play only. Analysis requests always run the search. */
    public static boolean isTimedPlay(String[] command)
    {
        boolean timed = false;
        for (String token : command)
        {
            if (ANALYSIS_LIMITS.contains(token)) return false;
            if (token.equals("wtime") || token.equals("btime") || token.equals("movetime")) timed = true;
        }
        return timed;
    }

    public static Move bookMove(Board board, String mode)
    {
        // Never revive a drawn game or apply an opening to a late-game FEN.
        if (board.getMoveCounter() > 3 || board.isDraw()) return null;
        Map<String, String> book = switch (mode)
        {
            case "Gambit" -> GAMBITS;
            case "Bongcloud" -> BONGCLOUD;
            default -> null;
        };
        if (book == null) return null;
        String candidate = book.get(board.getFen(false));
        return board.legalMoves().stream().filter(m -> m.toString().equals(candidate)).findFirst().orElse(null);
    }

    public static boolean playOpening(Board board, String[] command)
    {
        if (opening == null || !isTimedPlay(command)) return false;
        Move move = bookMove(board, opening.getString());
        if (move == null) return false;
        System.out.println("info string Sigma Opening " + opening.getString() + " book move " + move);
        if (commentary.get()) System.out.println("info string skibidi: opening rizz activated");
        System.out.println("bestmove " + move);
        return true;
    }

    /** A bounded display score, NOT a win probability or an engine evaluation. */
    public static int auraPoints(int centipawns)
    {
        return (int) Math.round(100.0 * Math.tanh(centipawns / 400.0));
    }

    public static String mood(int cp)
    {
        if (cp >= 200) return "sigma advantage secured";
        if (cp >= 50) return "positive rizz detected";
        if (cp > -50) return "balanced aura; stay locked in";
        if (cp > -200) return "aura debt; time to lock in";
        return "ohio position; keep calculating";
    }

    public static void report(SearchReport report)
    {
        if (commentary == null || (!commentary.get() && !aura.get())) return;
        boolean mate = Math.abs(report.score) >= AlphaBeta.MATE_EVAL - AlphaBeta.MAX_PLY;
        int cp = mate ? (report.score > 0 ? 100000 : -100000) : WDLModel.normalizeEval(report.score, report.board);
        if (aura.get())
            System.out.println("info string aura " + auraPoints(cp) + "/100 side-to-move depth " + report.depth);
        if (commentary.get())
            System.out.println("info string skibidi: " + (mate
                    ? (report.score > 0 ? "sigma mate sequence found" : "incoming mate; aura emergency")
                    : mood(cp)));
    }

    public static void bestMove(Move move)
    {
        if (commentary != null && commentary.get())
            System.out.println("info string skibidi: " + (move == null ? "no legal moves; gg" : "locked in " + move));
    }
}
