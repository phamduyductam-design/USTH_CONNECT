#!/usr/bin/env python3
"""Run local IRC services and optional Android emulator; stop owned services on exit."""

import argparse
import os
from pathlib import Path
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
SUFFIX = ".exe" if os.name == "nt" else ""


def start_android():
    sdk = Path(os.environ.get("ANDROID_HOME", ROOT / ".tools/android-sdk"))
    adb = sdk / f"platform-tools/adb{SUFFIX}"
    serial = os.environ.get("ANDROID_SERIAL", "emulator-5554")
    devices = subprocess.check_output([str(adb), "devices"], text=True)
    if f"{serial}\tdevice" not in devices:
        if serial != "emulator-5554":
            raise RuntimeError(f"Device {serial} is not connected")
        log_path = ROOT / ".tools/emulator.log"
        log_path.parent.mkdir(exist_ok=True)
        with log_path.open("a", encoding="utf-8") as log:
            subprocess.Popen(
                [str(sdk / f"emulator/emulator{SUFFIX}"), "-avd",
                 os.environ.get("USTH_AVD_NAME", "usth_demo_api35"), "-port", "5554", "-no-snapshot-load"],
                stdout=log, stderr=log,
            )
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            boot = subprocess.run([str(adb), "-s", serial, "shell", "getprop", "sys.boot_completed"],
                                  capture_output=True, text=True)
            if boot.stdout.strip() == "1":
                break
            time.sleep(1)
        else:
            raise RuntimeError("Android emulator boot timed out; see .tools/emulator.log")
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    if not apk.is_file():
        raise RuntimeError("Build the debug APK first with tools/run-android.sh --build-only")
    subprocess.run([str(adb), "-s", serial, "install", "-r", str(apk)], check=True)
    subprocess.run([str(adb), "-s", serial, "shell", "am", "start", "-n",
                    "vn.edu.usth.connect/.ui.ConnectActivity"], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android", action="store_true", help="Also install/open the app on Android")
    parser.add_argument("--no-bot", action="store_true", help="Use two human clients")
    args = parser.parse_args()
    daemon = Path(os.environ.get("USTH_IRCD", ROOT / f".tools/ergo/ergo{SUFFIX}"))
    config = Path(os.environ.get("USTH_IRCD_CONFIG", ROOT / ".tools/ergo/ircd.yaml")).resolve()
    processes = []
    flags = {"creationflags": subprocess.CREATE_NO_WINDOW} if os.name == "nt" else {}
    try:
        try:
            with socket.create_connection(("127.0.0.1", 6667), timeout=1):
                pass
        except OSError:
            if not daemon.is_file() or not config.is_file():
                parser.error("Install Ergo in .tools/ergo or set USTH_IRCD and USTH_IRCD_CONFIG")
            server = subprocess.Popen([str(daemon.resolve()), "run", "--conf", str(config)],
                                      cwd=config.parent, **flags)
            processes.append(server)
            deadline = time.monotonic() + 10
            while True:
                if server.poll() is not None:
                    raise RuntimeError("IRC server exited during startup")
                try:
                    with socket.create_connection(("127.0.0.1", 6667), timeout=1):
                        break
                except OSError:
                    if time.monotonic() > deadline:
                        raise RuntimeError("IRC server startup timed out")
                    time.sleep(0.1)
        if not args.no_bot:
            processes.append(subprocess.Popen([sys.executable, str(ROOT / "tools/demo_bot.py")], **flags))
        if args.android:
            start_android()
        print("Emulator: 10.0.2.2:6667, TLS off, #web-general. Desktop: 127.0.0.1. Ctrl+C stops this demo.", flush=True)
        while all(process.poll() is None for process in processes):
            time.sleep(0.5)
        raise RuntimeError("A demo service exited unexpectedly")
    except KeyboardInterrupt:
        pass
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    main()
