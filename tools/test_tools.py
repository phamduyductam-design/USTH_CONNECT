#!/usr/bin/env python3
"""Regression tests for Python demo framing, byte limits and source packaging."""

import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import unittest
from zipfile import ZipFile

import demo_bot
import package_delivery


class ToolsTest(unittest.TestCase):
    def test_unicode_reply_fits_irc_byte_budget_without_split_characters(self):
        for target, text in (("#c", "😀" * 200), ("#" + "x" * 400, "Tiếng Việt 😀" * 80)):
            line = demo_bot.reply_line(target, text)
            self.assertLessEqual(len(line), 512)
            self.assertTrue(line.endswith(b"\r\n"))
            self.assertNotIn("�", line.decode("utf-8"))
        for line in ("PRIVMSG #c :hello\r\nQUIT", "NICK a\0", "x" * 511):
            with self.assertRaises(ValueError):
                demo_bot.checked(line)

    def test_bot_handles_fragmented_unicode_ping_and_ignores_ctcp(self):
        errors = []
        with socket.socket() as server:
            server.bind(("127.0.0.1", 0))
            server.listen()
            server.settimeout(5)

            def bot():
                try:
                    demo_bot.run("127.0.0.1", server.getsockname()[1], False, "#c")
                except Exception as error:
                    errors.append(error)

            worker = threading.Thread(target=bot, daemon=True)
            worker.start()
            peer, _ = server.accept()
            with peer:
                peer.settimeout(5)
                with peer.makefile("rb") as lines:
                    self.assertTrue(lines.readline().startswith(b"NICK "))
                    self.assertTrue(lines.readline().startswith(b"USER "))
                    peer.sendall(b":s 001 Demo_Bot :Welcome\r\n")
                    self.assertEqual(b"JOIN #c\r\n", lines.readline())
                    peer.sendall(b"PING :token\r\n:an!u@h PRIVMSG #c :\x01VERSION\x01\r\n")
                    self.assertEqual(b"PONG :token\r\n", lines.readline())
                    incoming = "@time=now :an!u@h PRIVMSG #c :Tiếng Việt 😀\r\n".encode("utf-8")
                    for byte in incoming:
                        peer.sendall(bytes([byte]))
                    self.assertEqual(demo_bot.reply_line("#c", "Tiếng Việt 😀"), lines.readline())
                    peer.shutdown(socket.SHUT_WR)
            worker.join(timeout=5)
            self.assertFalse(worker.is_alive())
            self.assertEqual([], errors)

    def test_archive_uses_committed_files_and_optional_apk_without_local_secrets(self):
        with tempfile.TemporaryDirectory(prefix="usth-package-") as temp:
            root = Path(temp).resolve()

            def git(*args):
                return subprocess.run(["git", *args], cwd=root, check=True,
                                      capture_output=True, env={**os.environ, "GIT_CONFIG_NOSYSTEM": "1"})

            git("init")
            (root / ".gitignore").write_text(".tools/\noutput/\napp/build/\n", encoding="utf-8")
            (root / "source.txt").write_text("committed source", encoding="utf-8")
            git("add", ".")
            git("-c", "user.name=Local Test", "-c", "user.email=test@example.invalid", "commit", "-m", "fixture")
            (root / ".tools").mkdir()
            (root / ".tools/private-key.pem").write_text("local secret", encoding="utf-8")
            output = package_delivery.package(root)
            with ZipFile(output / "USTH-Connect-source.zip") as archive:
                self.assertIn("USTH_CONNECT/source.txt", archive.namelist())
                self.assertFalse(any("private-key" in name for name in archive.namelist()))
            self.assertFalse((output / "USTH-Connect-debug.apk").exists())
            apk = root / "app/build/outputs/apk/debug/app-debug.apk"
            apk.parent.mkdir(parents=True)
            apk.write_bytes(b"fixture apk")
            package_delivery.package(root)
            self.assertEqual(b"fixture apk", (output / "USTH-Connect-debug.apk").read_bytes())
            self.assertEqual(2, len((output / "SHA256SUMS.txt").read_text().splitlines()))
            (root / "source.txt").write_text("uncommitted change", encoding="utf-8")
            with self.assertRaisesRegex(RuntimeError, "Commit project changes first"):
                package_delivery.package(root)


if __name__ == "__main__":
    unittest.main()
