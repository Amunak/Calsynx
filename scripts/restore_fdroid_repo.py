import json
import os
import urllib.request


def main() -> int:
    repo_url = os.environ["FDROID_REPO_URL"].rstrip("/")
    dest_dir = os.path.join("fdroid", "repo")
    index_path = "/tmp/index-v1.json"

    with open(index_path, "r", encoding="utf-8") as fh:
        data = json.load(fh)

    apk_names: set[str] = set()
    for app in data.get("apps", []):
        for pkg in app.get("packages", []):
            name = pkg.get("apkName")
            if name:
                apk_names.add(name)

    for pkg_list in data.get("packages", {}).values():
        for pkg in pkg_list:
            name = pkg.get("apkName")
            if name:
                apk_names.add(name)

    os.makedirs(dest_dir, exist_ok=True)
    for name in sorted(apk_names):
        url = f"{repo_url}/{name}"
        out_path = os.path.join(dest_dir, name)
        if os.path.exists(out_path):
            continue
        try:
            urllib.request.urlretrieve(url, out_path)
        except Exception:
            pass

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
