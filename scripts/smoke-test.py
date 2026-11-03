#!/usr/bin/env python3
"""Exercise the packaged engine over a real UCI subprocess."""
import os
import pathlib
import queue
import subprocess
import sys
import threading
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
jar = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "SerenSkibidi.jar"
java = str(pathlib.Path(os.environ["JAVA_HOME"]) / "bin/java") if "JAVA_HOME" in os.environ else "java"
proc = subprocess.Popen([java, "--add-modules=jdk.incubator.vector", "-jar", str(jar)],
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
lines = queue.Queue()
errors = []
threading.Thread(target=lambda: [lines.put(line.strip()) for line in proc.stdout], daemon=True).start()
threading.Thread(target=lambda: errors.extend(proc.stderr.readlines()), daemon=True).start()


def send(text):
    proc.stdin.write(text + "\n")
    proc.stdin.flush()


def until(prefix, timeout=30):
    transcript = []
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=0.1)
        except queue.Empty:
            if proc.poll() is not None:
                raise AssertionError(f"Engine exited: {errors}")
            continue
        transcript.append(line)
        if line.startswith(prefix):
            return transcript
    raise AssertionError(f"Timed out waiting for {prefix}: {transcript}; {errors}")


try:
    send("uci")
    handshake = "\n".join(until("uciok"))
    assert "id name SerenSkibidi 1.0" in handshake
    assert "option name Skibidi Mode type check default false" in handshake
    assert "option name Sigma Opening type combo default Off" in handshake
    send("isready"); until("readyok")
    send("position startpos"); send("go depth 4")
    normal = until("bestmove ")
    assert any(s.startswith("info depth 4 ") for s in normal), normal
    assert not any("skibidi:" in s or "info string aura" in s for s in normal)
    normal_move = normal[-1]

    send("ucinewgame")
    send("setoption   name   skibidi mode   value   true")
    send("setoption name Aura Meter value true")
    send("position startpos"); send("go depth 4")
    themed = until("bestmove ")
    assert themed[-1] == normal_move, (normal_move, themed[-1])
    assert any("info string aura " in s for s in themed)
    assert any("info string skibidi: " in s for s in themed)
    for name, value in [("Skibidi Mode", "banana"), ("Sigma Opening", "typo")]:
        send(f"setoption name {name} value {value}")
        until("info string Invalid value")
        send("isready"); until("readyok")

    send("setoption name Sigma Opening value Bongcloud")
    send("position startpos moves e2e4 e7e5"); send("go movetime 100")
    assert until("bestmove ")[-1] == "bestmove e1e2"
    send("position startpos moves e2e4 e7e5 e1e2"); send("go movetime 100")
    assert until("bestmove ")[-1] == "bestmove e8e7"
    send("setoption name Sigma Opening value Gambit")
    send("position startpos moves e2e4 e7e5"); send("go movetime 100")
    assert until("bestmove ")[-1] == "bestmove f2f4"

    send("position startpos"); send("go depth 3")
    analysis = until("bestmove ")
    assert any(s.startswith("info depth 3 ") for s in analysis)
    assert not any("book move" in s for s in analysis)
    send("position startpos moves a2a3"); send("go movetime 100")
    fallback = until("bestmove ")
    assert any(s.startswith("info depth ") for s in fallback)
    assert not any("book move" in s for s in fallback)

    send("setoption name Sigma Opening value Off")
    send("setoption name Threads value 2")
    send("position startpos"); send("go depth 3"); until("bestmove ")
    send("position startpos"); send("go infinite"); until("info depth 2 ")
    send("stop"); until("bestmove ")
    send("setoption name Threads value 1")

    # Checkmate and stalemate both use UCI's null move.
    for fen in ["7k/6Q1/5K2/8/8/8/8/8 b - - 0 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"]:
        send("ucinewgame"); send("position fen " + fen); send("go depth 2")
        assert until("bestmove ")[-1] == "bestmove 0000"

    send("position startpos"); send("perft 3"); send("isready")
    assert any("Nodes searched:" in s and s.endswith("8902") for s in until("readyok"))
    send("quit"); proc.wait(timeout=10)
    assert proc.returncode == 0
    assert not any("Exception" in e for e in errors), errors
    print("PASS: identity, options, search parity, aura, legal openings, fallback, analysis, 2 threads, stop, terminal positions, perft=8902, quit")
finally:
    if proc.poll() is None:
        proc.kill(); proc.wait()
