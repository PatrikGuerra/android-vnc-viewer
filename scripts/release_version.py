"""Select a stable version and inject Android metadata in the CI workspace."""
import argparse
import os
from pathlib import Path
import re
import subprocess
from xml.etree import ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
TAG_PATTERN = re.compile(r"^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


def stable_tags(tags):
    result = {}
    for tag in tags:
        match = TAG_PATTERN.fullmatch(tag)
        if match:
            version = tuple(map(int, match.groups()))
            if version >= (1, 0, 0):
                result[tag] = version
    return result


def select_version(tags, head_tags):
    existing = stable_tags(head_tags)
    if existing:
        return max(existing, key=existing.get)
    versions = stable_tags(tags)
    if not versions:
        return "v1.0.0"
    major, minor, patch = max(versions.values())
    return "v%d.%d.%d" % (major, minor, patch + 1)


def version_code(run_number):
    number = int(run_number)
    code = 1_000_000 + number
    if number < 1 or code > 2_100_000_000:
        raise ValueError("Workflow run number exceeds the Android versionCode range")
    return code


def inject_version(manifest, name, code):
    ET.register_namespace("android", ANDROID[1:-1])
    tree = ET.parse(manifest)
    tree.getroot().set(ANDROID + "versionName", name)
    tree.getroot().set(ANDROID + "versionCode", str(code))
    tree.write(manifest, encoding="utf-8", xml_declaration=True)


def git_lines(*args):
    return subprocess.check_output(["git", *args], text=True).splitlines()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--run-number", required=True, type=int)
    parser.add_argument("--manifest", default="androidVNC/AndroidManifest.xml")
    parser.add_argument("--output", default=os.environ.get("GITHUB_OUTPUT"))
    args = parser.parse_args()
    tag = select_version(git_lines("tag", "--list"), git_lines("tag", "--points-at", "HEAD"))
    name = tag[1:] if args.release else tag[1:] + "-dev." + str(args.run_number)
    code = version_code(args.run_number)
    inject_version(args.manifest, name, code)
    if args.output:
        with Path(args.output).open("a", encoding="utf-8") as output:
            output.write("tag=%s\nversion=%s\nversion_code=%s\n" % (tag, name, code))
    print("Version: %s, Android versionCode: %s" % (name, code))


if __name__ == "__main__":
    main()
