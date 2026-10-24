#!/usr/bin/env python3
"""Verify the Java client's TLS trust and hostname checks using temporary loopback certificates."""

import os
from pathlib import Path
import shutil
import socket
import ssl
import subprocess
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]


def executable(name):
    suffix = ".exe" if os.name == "nt" else ""
    jdk = os.environ.get("JAVA_HOME")
    return str(Path(jdk) / "bin" / (name + suffix)) if jdk else name


def run():
    openssl = shutil.which("openssl")
    if not openssl and os.name == "nt":
        candidate = Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Git/usr/bin/openssl.exe"
        if candidate.is_file():
            openssl = str(candidate)
    if not openssl:
        raise RuntimeError("OpenSSL is required for the local certificate fixture")
    tools = ROOT / ".tools"
    tools.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="tls-smoke-", dir=tools) as temp:
        folder = Path(temp)
        cert, key, trust = folder / "server.pem", folder / "key.pem", folder / "trust.p12"
        subprocess.run([openssl, "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                        "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost",
                        "-keyout", str(key), "-out", str(cert)], check=True, capture_output=True)
        subprocess.run([executable("keytool"), "-importcert", "-noprompt", "-alias", "local-smoke",
                        "-file", str(cert), "-keystore", str(trust), "-storetype", "PKCS12",
                        "-storepass", "local-test-only"], check=True, capture_output=True)
        classes = folder / "classes"
        classes.mkdir()
        source = ROOT / "app/src/main/java/vn/edu/usth/connect"
        sources = [str(p) for group in ("model", "irc") for p in (source / group).glob("*.java")]
        subprocess.run([executable("javac"), "-encoding", "UTF-8", "-d", str(classes),
                        *sources, str(ROOT / "tools/TlsSmoke.java")], check=True)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(cert, key)
        for host, trusted, accepted, label in (
            ("localhost", True, True, "valid certificate"),
            ("localhost", False, False, "untrusted certificate"),
            ("127.0.0.1", True, False, "wrong hostname"),
        ):
            with socket.socket() as listener:
                listener.bind(("127.0.0.1", 0))
                listener.listen()
                listener.settimeout(20)
                errors = []

                def serve():
                    try:
                        raw, _ = listener.accept()
                        raw.settimeout(20)
                        with raw, context.wrap_socket(raw, server_side=True) as peer:
                            with peer.makefile("rb") as lines:
                                for line in lines:
                                    if line.startswith(b"USER "):
                                        peer.sendall(b":s 001 tls_smoke :Welcome\r\n")
                                        return
                    except ssl.SSLError:
                        if accepted:
                            errors.append("Valid TLS handshake was rejected")
                    except OSError as error:
                        errors.append(str(error))

                worker = threading.Thread(target=serve, daemon=True)
                worker.start()
                command = [executable("java"), "-Djava.net.preferIPv4Stack=true"]
                if trusted:
                    command += [f"-Djavax.net.ssl.trustStore={trust}",
                                "-Djavax.net.ssl.trustStorePassword=local-test-only"]
                try:
                    subprocess.run([*command, "-cp", str(classes), "TlsSmoke", host,
                                    str(listener.getsockname()[1]), str(accepted).lower()],
                                   check=True, timeout=25)
                finally:
                    worker.join(timeout=21)
                if errors:
                    raise RuntimeError("; ".join(errors))
                print(f"PASS: {label}", flush=True)


if __name__ == "__main__":
    run()
