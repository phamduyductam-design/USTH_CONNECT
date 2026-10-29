#!/usr/bin/env python3
"""Package committed source and an optional debug APK; omit SDKs, caches and local data."""

import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def package(root=ROOT):
    status = subprocess.run(
        ["git", "status", "--porcelain"],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    if status.stdout.strip():
        raise RuntimeError("Commit project changes first: the source archive is built from HEAD.")

    output = root / "output"
    output.mkdir(exist_ok=True)
    archive = output / "USTH-Connect-source.zip"
    subprocess.run(
        [
            "git", "archive", "--format=zip", "--prefix=USTH_CONNECT/",
            f"--output={archive}", "HEAD",
        ],
        cwd=root,
        check=True,
    )
    artifacts = [archive]
    apk = root / "app/build/outputs/apk/debug/app-debug.apk"
    if apk.is_file():
        packaged_apk = output / "USTH-Connect-debug.apk"
        shutil.copy2(apk, packaged_apk)
        artifacts.append(packaged_apk)

    checksums = "".join(
        f"{hashlib.sha256(artifact.read_bytes()).hexdigest()}  {artifact.name}\n"
        for artifact in artifacts
    )
    (output / "SHA256SUMS.txt").write_text(checksums, encoding="utf-8")
    return output


def main():
    argparse.ArgumentParser(description=__doc__).parse_args()
    try:
        print(f"Source archive and optional debug APK: {package()}")
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error


if __name__ == "__main__":
    main()
