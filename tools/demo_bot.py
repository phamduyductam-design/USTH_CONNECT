#!/usr/bin/env python3
"""Optional IRC echo bot with verified TLS, CTCP filtering and UTF-8 byte limits."""

import argparse
import socket
import ssl
import time


def checked(line):
    if any(c in line for c in "\r\n\0") or len(line.encode("utf-8")) + 2 > 512:
        raise ValueError("IRC command must be a single line of at most 512 UTF-8 bytes.")
    return (line + "\r\n").encode("utf-8")


def reply_line(target, text):
    prefix = f"PRIVMSG {target} :Demo đã nhận: "
    budget = 510 - len(prefix.encode("utf-8"))
    if budget < 0:
        raise ValueError("Target is too long")
    body = text[:80].encode("utf-8")[:budget].decode("utf-8", errors="ignore")
    return checked(prefix + body)


def run(host, port, tls, room):
    nick = "Demo_Bot"
    with socket.create_connection((host, port), timeout=15) as raw:
        connection = ssl.create_default_context().wrap_socket(raw, server_hostname=host) if tls else raw
        with connection as sock:
            sock.settimeout(180)
            sock.sendall(checked(f"NICK {nick}"))
            sock.sendall(checked("USER demo 0 * :Local demo bot"))
            with sock.makefile("r", encoding="utf-8", errors="replace") as lines:
                for raw_line in lines:
                    line = raw_line.rstrip("\r\n")
                    if line.startswith("@"):
                        line = line.partition(" ")[2]
                    source = ""
                    if line.startswith(":"):
                        source, _, line = line[1:].partition(" ")
                    head, _, text = line.partition(" :")
                    parts = head.split()
                    if not parts:
                        continue
                    command = parts[0].upper()
                    sender = source.split("!", 1)[0]
                    if command == "PING":
                        sock.sendall(checked("PONG " + line.partition(" ")[2]))
                    elif command == "001":
                        sock.sendall(checked("JOIN " + room))
                    elif command == "PRIVMSG" and len(parts) > 1 and sender.casefold() != nick.casefold():
                        if text.startswith("\x01"):
                            continue
                        target = parts[1] if parts[1].startswith(("#", "&", "+", "!")) else sender
                        sock.sendall(reply_line(target, text))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("host", nargs="?", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=6667)
    parser.add_argument("--tls", action="store_true")
    parser.add_argument("--room", default="#web-general")
    args = parser.parse_args()
    if not args.room.startswith(("#", "&", "+", "!")) or any(c.isspace() or c in ",\0" for c in args.room):
        parser.error("room must be a single IRC channel")
    checked("JOIN " + args.room)
    try:
        while True:
            try:
                run(args.host, args.port, args.tls, args.room)
            except ssl.SSLError as error:
                raise SystemExit(f"TLS verification/connection failed: {error}") from error
            except OSError:
                time.sleep(3)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
