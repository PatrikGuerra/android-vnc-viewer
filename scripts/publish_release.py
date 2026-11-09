"""Publish verified assets; retries complete drafts without rewriting releases."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import urllib.error
import urllib.parse
import urllib.request


class GitHub:
    def __init__(self, repository, token):
        self.base = "https://api.github.com/repos/" + repository
        self.token = token

    def request(self, method, path, data=None, missing_ok=False, content_type="application/json"):
        url = path if path.startswith("https://") else self.base + path
        body = data if isinstance(data, bytes) else (json.dumps(data).encode() if data is not None else None)
        request = urllib.request.Request(url, data=body, method=method, headers={
            "Authorization": "Bearer " + self.token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": content_type,
        })
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            if missing_ok and error.code == 404:
                return None
            raise RuntimeError("GitHub request failed: HTTP %s %s" % (error.code, url)) from None


def publish(api, tag, commit, assets):
    ref = api.request("GET", "/git/ref/tags/" + urllib.parse.quote(tag, safe=""), missing_ok=True)
    if ref:
        target = ref["object"]
        while target["type"] == "tag":
            target = api.request("GET", "/git/tags/" + target["sha"])["object"]
        if target["type"] != "commit" or target["sha"] != commit:
            raise RuntimeError("Refusing to publish: tag belongs to a different commit")
    release = api.request("GET", "/releases/tags/" + urllib.parse.quote(tag, safe=""), missing_ok=True)
    if release and not ref:
        raise RuntimeError("Release exists without its tag; refusing to change its origin")
    if release and not release["draft"]:
        print("Release already published; leaving assets unchanged: " + release["html_url"])
        return release
    if not release:
        if not ref:
            api.request("POST", "/git/refs", {"ref": "refs/tags/" + tag, "sha": commit})
        notes = api.request("POST", "/releases/generate-notes", {"tag_name": tag, "target_commitish": commit})
        intro = ("APK release assinado para Android antigo, com foco no Android 4.2.2.\n\n"
                 "Commit: `%s`\n\nConfira o APK usando `SHA256SUMS.txt`.\n\n" % commit)
        release = api.request("POST", "/releases", {
            "tag_name": tag, "target_commitish": commit, "name": "androidVNC2026 " + tag,
            "body": intro + notes["body"], "draft": True, "prerelease": False,
        })
    existing = api.request("GET", "/releases/%s/assets?per_page=100" % release["id"])
    names = {asset.name for asset in assets}
    for old in existing:
        if old["name"] in names:
            api.request("DELETE", "/releases/assets/" + str(old["id"]))
    upload = release["upload_url"].split("{")[0]
    for asset in assets:
        api.request("POST", upload + "?name=" + urllib.parse.quote(asset.name), asset.read_bytes(),
                    content_type="application/octet-stream")
    release = api.request("PATCH", "/releases/" + str(release["id"]), {"draft": False, "make_latest": "true"})
    print("Published: " + release["html_url"])
    return release


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--dist", default="dist")
    args = parser.parse_args()
    expected = [Path(args.dist) / ("androidVNC2026-" + args.tag + ".apk"), Path(args.dist) / "SHA256SUMS.txt"]
    if any(not asset.is_file() for asset in expected):
        raise SystemExit("Verified APK and checksum file are required")
    if subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip() != args.commit:
        raise SystemExit("Checkout does not match the built commit")
    publish(GitHub(os.environ["GITHUB_REPOSITORY"], os.environ["GH_TOKEN"]), args.tag, args.commit, expected)


if __name__ == "__main__":
    main()
