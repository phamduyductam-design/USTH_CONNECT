#!/usr/bin/env python3
"""Format Java and Android view XML consistently, or verify them with --check.

The formatter JAR stays in ignored .tools/. Its pinned SHA-256 is verified before execution.
XML formatting changes whitespace only; canonical XML is compared before writing.
"""

import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
from xml.dom import Node, minidom
from xml.etree.ElementTree import canonicalize
from xml.sax.saxutils import quoteattr

PROJECT_DIR = Path(__file__).resolve().parent.parent
FORMATTER_VERSION = "1.22.0"
FORMATTER_SHA256 = "16b2a1ee938686c8b1d88abf19eb83dfd0d623cee9de6fc6d09980214f816d3f"
FORMATTER_NAME = f"google-java-format-{FORMATTER_VERSION}.jar"
FORMATTER_URL = (
    "https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/"
    f"{FORMATTER_VERSION}/google-java-format-{FORMATTER_VERSION}-all-deps.jar"
)


def formatter_path():
    jar = PROJECT_DIR / ".tools" / FORMATTER_NAME
    if not jar.exists():
        jar.parent.mkdir(exist_ok=True)
        print(f"Downloading google-java-format {FORMATTER_VERSION}...", flush=True)
        with urllib.request.urlopen(FORMATTER_URL, timeout=60) as response:
            jar.write_bytes(response.read())
    if hashlib.sha256(jar.read_bytes()).hexdigest() != FORMATTER_SHA256:
        raise RuntimeError(f"Formatter checksum mismatch; delete {jar} and retry.")
    return jar


def format_view_xml(source):
    """One attribute per line for Android layouts, menus, icons and view backgrounds."""
    document = minidom.parseString(source)

    def render(node, depth=0):
        indent = "    " * depth
        if node.nodeType == Node.COMMENT_NODE:
            return [indent + node.toxml()]
        if node.nodeType != Node.ELEMENT_NODE:
            if node.nodeType == Node.TEXT_NODE and not node.data.strip():
                return []
            return [indent + node.toxml()]
        children = [n for n in node.childNodes if not (
            n.nodeType == Node.TEXT_NODE and not n.data.strip()
        )]
        attributes = list(node.attributes.items())
        opening = [indent + "<" + node.tagName]
        for name, value in attributes:
            opening.append(indent + "    " + name + "=" + quoteattr(value))
        opening[-1] += ">" if children else " />"
        for child in children:
            opening.extend(render(child, depth + 1))
        if children:
            opening.append(indent + "</" + node.tagName + ">")
        return opening

    lines = ['<?xml version="1.0" encoding="utf-8"?>']
    for child in document.childNodes:
        lines.extend(render(child))
    result = "\n".join(lines) + "\n"
    if canonicalize(source, strip_text=True) != canonicalize(result, strip_text=True):
        raise ValueError("XML formatting changed the document's meaning")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Report differences without editing source")
    args = parser.parse_args()
    java_files = sorted((PROJECT_DIR / "app/src").rglob("*.java"))
    java_files += sorted((PROJECT_DIR / "tools").glob("*.java"))
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin/java") if java_home else "java"
    flags = ["--dry-run", "--set-exit-if-changed"] if args.check else ["--replace"]
    java_result = subprocess.run([
        java, "-jar", str(formatter_path()), *flags, *(str(p) for p in java_files)
    ])
    res = PROJECT_DIR / "app/src/main/res"
    xml_files = sorted(p for folder in ("layout", "drawable", "menu", "color")
                       for p in (res / folder).glob("*.xml"))
    changed = []
    for path in xml_files:
        source = path.read_text(encoding="utf-8")
        result = format_view_xml(source)
        if source != result:
            changed.append(path)
            if args.check:
                print(path.relative_to(PROJECT_DIR))
            else:
                path.write_text(result, encoding="utf-8")
    print(f"Java: {len(java_files)} files; XML: {len(xml_files)} files; XML differences: {len(changed)}")
    return 1 if java_result.returncode or (args.check and changed) else 0


if __name__ == "__main__":
    sys.exit(main())
