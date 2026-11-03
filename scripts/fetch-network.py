#!/usr/bin/env python3
"""Fetch the pinned upstream NNUE atomically and verify its SHA-256."""
import hashlib
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEST = ROOT / "Serendipity/src/main/resources/embedded.nnue"
URL = "https://raw.githubusercontent.com/xu-shawn/Serendipity-Networks/bfd3862fa2aa1570018fa71de8f86ab10ead8047/net2.nnue"
SHA256 = "165bb50864a9e872c5a268d7a23471cb45e0fbacdc7bf19f5e9a03d636529f48"


def verified(path):
    return path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() == SHA256


if __name__ == "__main__":
    DEST.parent.mkdir(parents=True, exist_ok=True)
    if verified(DEST):
        print("NNUE checksum OK (cached)")
    else:
        with tempfile.NamedTemporaryFile(dir=DEST.parent, delete=False) as tmp:
            temporary = pathlib.Path(tmp.name)
        try:
            subprocess.run(["curl", "--fail", "--location", "--retry", "3", URL, "--output", str(temporary)], check=True)
            if not verified(temporary):
                raise SystemExit("NNUE checksum mismatch; refusing to use download")
            temporary.replace(DEST)
            print("NNUE checksum OK (downloaded)")
        finally:
            temporary.unlink(missing_ok=True)
