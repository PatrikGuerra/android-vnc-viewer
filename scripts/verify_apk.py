"""Verify final APK metadata, alignment and legacy-compatible signature."""
import argparse
import re
import subprocess


def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--tools", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--code", required=True)
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--expected-cert")
    args = parser.parse_args()
    tools = args.tools.rstrip("/")
    badging = run(tools + "/aapt", "dump", "badging", args.apk)
    for field, expected in (("name", "android.androidVNC"), ("versionName", args.version), ("versionCode", args.code)):
        match = re.search(r"\b" + field + r"='([^']+)'", badging.splitlines()[0])
        if not match or match.group(1) != expected:
            raise SystemExit("Unexpected APK " + field)
    if "sdkVersion:'3'" not in badging or "targetSdkVersion:'17'" not in badging:
        raise SystemExit("Unexpected Android SDK metadata")
    if args.release and "application-debuggable" in badging:
        raise SystemExit("Public release must not be debuggable")
    run(tools + "/zipalign", "-c", "4", args.apk)
    signature = run(tools + "/apksigner", "verify", "--verbose", "--print-certs", "--min-sdk-version", "17", args.apk)
    if "Verified using v1 scheme (JAR signing): true" not in signature:
        raise SystemExit("Android 4.2.2 requires the v1 signature")
    if args.expected_cert and args.expected_cert.lower() not in signature.lower():
        raise SystemExit("Unexpected signing certificate")
    print("PASS: APK metadata, zip alignment and v1 signature" + (", non-debuggable release and persistent certificate" if args.release else ""))


if __name__ == "__main__":
    main()
