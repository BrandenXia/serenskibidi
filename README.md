# SerenSkibidi

**Serious chess engine. Optional unserious aura.**

A Java UCI chess-engine fork of [Shawn Xu's Serendipity](https://github.com/xu-shawn/Serendipity), based on commit `74d96da65320febe33d0840da3284a252f5a6257`. This independent fork retains the upstream search and NNUE evaluation, adds opt-in Skibidi Sigma features, and makes no Elo improvement claim.

| UCI option | Default | Feature |
| --- | --- | --- |
| `Skibidi Mode` | `false` | Meme commentary such as “positive rizz detected” and “sigma mate sequence found,” emitted as valid `info string` lines. |
| `Aura Meter` | `false` | A −100 to +100 display from the root side-to-move's perspective: `round(100 × tanh(normalized centipawns / 400))`. A playful display, not a win probability. |
| `Sigma Opening` | `Off` | Choose `Off`, `Gambit`, or `Bongcloud`. A tiny deterministic opening book for timed games, followed by ordinary search. |

Gambit mode offers the King's Gambit after `1. e4 e5`, Queen's Gambit after `1. d4 d5`, and Wing Gambit after `1. e4 c5`. Bongcloud follows `1. e4 e5 2. Ke2 Ke7` when possible. It deliberately gives up castling rights and can weaken play. Both books can supply moves for either color; all book moves are checked for legality.

The book applies only to timed commands (`movetime`, `wtime`, or `btime`), and returns a move immediately. Commands containing `depth`, `nodes`, `mate`, `infinite`, `ponder`, or `searchmoves` bypass the book. This bypass does not add support for UCI commands or limits absent from upstream. Positions outside the book, late-game FENs, and drawn positions receive no book move.

All features default off. Commentary and aura do not change evaluations or search heuristics, though extra output may affect timed-game throughput.

## Build and run

Requires JDK 17 or newer with `jdk.incubator.vector`, Maven, Make, Python 3.8+, and curl. JDK 17 is the baseline build target.

```sh
git clone https://github.com/BrandenXia/serenskibidi.git
cd serenskibidi
make
make test
java --add-modules=jdk.incubator.vector -jar SerenSkibidi.jar
```

`make` runs Java unit tests and produces `SerenSkibidi.jar` (cross-platform) and `SerenSkibidi` (macOS/Linux launcher). On macOS/Linux you can run `./SerenSkibidi` directly. This is a UCI engine, not a graphical chessboard; connect it to a UCI-compatible chess GUI or enter commands directly.

The first build downloads Maven dependencies and a pinned upstream NNUE network. Later builds reuse the checksum-verified network. Override Maven with `MAVEN_EXE=/path/to/mvn`, pass Maven options with `MAVEN_ARGS='...'`, or provide a compatible custom network with `EVALFILE=/path/to/network.nnue`.

The network is [net2.nnue at bfd3862](https://github.com/xu-shawn/Serendipity-Networks/tree/bfd3862fa2aa1570018fa71de8f86ab10ead8047). Its SHA-256 is `165bb50864a9e872c5a268d7a23471cb45e0fbacdc7bf19f5e9a03d636529f48`. Downloads are atomic and verified before use.

## Activate maximum aura

```text
uci
setoption name Skibidi Mode value true
setoption name Aura Meter value true
setoption name Sigma Opening value Bongcloud
isready
ucinewgame
position startpos moves e2e4 e7e5
go movetime 1000
```

This returns `bestmove e1e2`. Use `setoption name Sigma Opening value Off` for ordinary move selection. For commentary and aura from a real search, enter `position startpos`, then `go depth 6`, and wait for `bestmove` before sending `quit`.

## Validation and attribution

`make test` runs nine Java tests and a packaged-engine UCI integration test covering identity, options, fixed-depth move parity with commentary enabled, aura, legal openings, fallback, analysis, two threads, stop, checkmate/stalemate, perft (depth 3 = 8,902), and quit.

Fork changes dated 2026-11-02 include branding, three themed options, multiword and case-insensitive option parsing, rejection of invalid option values, `bestmove 0000` for no legal move, shutdown on stdin EOF, quoted launcher paths, pinned network downloads, and build/test CI. Java package names remain unchanged to keep upstream merges straightforward.

Original project history and acknowledgements are preserved in [UPSTREAM.md](UPSTREAM.md). Existing copyright notices and the [AGPL-3.0-or-later license](LICENSE) remain intact. This is not an official Serendipity release.
