import importlib.util
from pathlib import Path
import tempfile
import unittest
from xml.etree import ElementTree as ET


def load_script(name):
    path = Path(__file__).resolve().parents[1] / "scripts" / (name + ".py")
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


version = load_script("release_version")
publisher = load_script("publish_release")


class VersionTests(unittest.TestCase):
    def test_initial_release_ignores_legacy_and_prerelease_tags(self):
        self.assertEqual(version.select_version(["v0_7_2", "v0.9.9", "v1.0.0-rc1"], []), "v1.0.0")

    def test_patch_increment_uses_numeric_sort(self):
        self.assertEqual(version.select_version(["v1.0.9", "v1.0.10", "v0.8.0"], []), "v1.0.11")

    def test_retry_reuses_commit_tag(self):
        self.assertEqual(version.select_version(["v1.0.0", "v1.0.1"], ["v1.0.0"]), "v1.0.0")

    def test_version_code_is_monotonic_and_above_installed_version(self):
        self.assertEqual(version.version_code(1), 1_000_001)
        self.assertGreater(version.version_code(2), version.version_code(1))
        for invalid in (0, -1, 2_100_000_000):
            with self.assertRaises(ValueError):
                version.version_code(invalid)

    def test_manifest_changes_only_version_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "AndroidManifest.xml"
            manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                                'package="android.androidVNC" android:versionCode="14" android:versionName="0.6.0">'
                                '<uses-sdk android:minSdkVersion="3" android:targetSdkVersion="17" /></manifest>')
            version.inject_version(manifest, "1.0.0", 1_000_010)
            root = ET.parse(manifest).getroot()
            self.assertEqual(root.get("package"), "android.androidVNC")
            self.assertEqual(root.get(version.ANDROID + "versionName"), "1.0.0")
            self.assertEqual(root.get(version.ANDROID + "versionCode"), "1000010")
            self.assertEqual(root.find("uses-sdk").get(version.ANDROID + "targetSdkVersion"), "17")


class FakeGitHub:
    def __init__(self, state="new", fail_upload=False):
        self.state = state
        self.fail_upload = fail_upload
        self.calls = []

    def request(self, method, path, data=None, **kwargs):
        self.calls.append((method, path, data))
        if path.startswith("/git/ref/"):
            return None if self.state == "new" else {"object": {"type": "commit", "sha": "wrong" if self.state == "conflict" else "abc"}}
        if path.startswith("/releases/tags/"):
            if self.state in ("new", "conflict"):
                return None
            return {"id": 7, "draft": self.state == "draft", "upload_url": "https://uploads.test/assets{?name}", "html_url": "https://test/release"}
        if path == "/releases/generate-notes":
            return {"body": "Notes"}
        if path == "/releases" and method == "POST":
            return {"id": 7, "draft": True, "upload_url": "https://uploads.test/assets{?name}"}
        if method == "GET" and path.endswith("assets?per_page=100"):
            return [{"id": 5, "name": "SHA256SUMS.txt"}] if self.state == "draft" else []
        if path.startswith("https://uploads.test") and self.fail_upload:
            raise RuntimeError("Upload failed")
        if method == "PATCH":
            return {"html_url": "https://test/release", "draft": False}
        return {}


class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.assets = [Path(self.directory.name) / "androidVNC2026-v1.0.0.apk", Path(self.directory.name) / "SHA256SUMS.txt"]
        for asset in self.assets:
            asset.write_bytes(b"test")

    def test_new_release_publishes_only_after_uploads(self):
        api = FakeGitHub()
        publisher.publish(api, "v1.0.0", "abc", self.assets)
        self.assertEqual(api.calls[-1][0], "PATCH")
        self.assertEqual(sum(path.startswith("https://uploads.test") for _, path, _ in api.calls), 2)
        draft = next(data for method, path, data in api.calls if method == "POST" and path == "/releases")
        self.assertTrue(draft["draft"])

    def test_published_release_is_not_modified_on_retry(self):
        api = FakeGitHub("published")
        publisher.publish(api, "v1.0.0", "abc", self.assets)
        self.assertTrue(all(method == "GET" for method, _, _ in api.calls))

    def test_draft_retry_replaces_assets_and_finishes(self):
        api = FakeGitHub("draft")
        publisher.publish(api, "v1.0.0", "abc", self.assets)
        self.assertTrue(any(method == "DELETE" for method, _, _ in api.calls))
        self.assertEqual(api.calls[-1][0], "PATCH")

    def test_failed_upload_does_not_publish(self):
        api = FakeGitHub(fail_upload=True)
        with self.assertRaises(RuntimeError):
            publisher.publish(api, "v1.0.0", "abc", self.assets)
        self.assertFalse(any(method == "PATCH" for method, _, _ in api.calls))

    def test_conflicting_tag_is_not_overwritten(self):
        api = FakeGitHub("conflict")
        with self.assertRaises(RuntimeError):
            publisher.publish(api, "v1.0.0", "abc", self.assets)
        self.assertTrue(all(method == "GET" for method, _, _ in api.calls))


if __name__ == "__main__":
    unittest.main()
