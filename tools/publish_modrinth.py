#!/usr/bin/env python3
"""Publish the supported Minecraft builds to Modrinth; retries skip existing files."""
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from multiversion import COVERS, ROOT, SKIP, TARGETS, WORK, mod_version  # noqa: E402

API = "https://api.modrinth.com/v3"
PROJECT = os.environ.get("MODRINTH_ID", "nCHc68zN")

# Environment matches the packaged mod and its optional client features.
ENVIRONMENT = "dedicated_server_only"

def request(method, path, token, data=None, headers=None):
    req = urllib.request.Request(API + path, method=method, data=data)
    req.add_header("Authorization", token)
    req.add_header("User-Agent", "CodeW4VE/WorldEaterNotifier (release script)")

    for key, value in (headers or {}).items():
        req.add_header(key, value)

    with urllib.request.urlopen(req) as response:
        body = response.read()

    return json.loads(body) if body else None


def multipart(fields, files):
    """A multipart body, written out by hand so this script needs nothing but the stdlib."""
    boundary = "----worldeaternotifier-release-boundary"
    body = b""

    for name, value in fields.items():
        body += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n"
                 f"Content-Type: application/json\r\n\r\n{value}\r\n").encode("utf-8")

    for name, path in files.items():
        body += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"; "
                 f"filename=\"{path.name}\"\r\n"
                 f"Content-Type: application/java-archive\r\n\r\n").encode("utf-8")
        body += path.read_bytes() + b"\r\n"

    body += f"--{boundary}--\r\n".encode("utf-8")
    return body, f"multipart/form-data; boundary={boundary}"


def main():
    dry_run = "--dry-run" in sys.argv
    token = os.environ.get("MODRINTH_TOKEN", "")

    if not token and not dry_run:
        print("MODRINTH_TOKEN is not set")
        return 1

    version = mod_version()
    changelog = f"https://github.com/CodeW4VE/WorldEaterNotifier/releases/tag/v{version}"

    # By file name as well as by version number: a half finished release leaves some of both
    # behind, and uploading a second copy of a jar is worse than skipping one.
    existing = set()

    if token:
        for known in request("GET", f"/project/{PROJECT}/version", token) or []:
            existing.add(known["version_number"])
            existing.update(file["filename"] for file in known.get("files", []))

    shipped = [name for name in TARGETS if name not in SKIP]

    for minecraft in shipped:
        jar = WORK / f"worldeaternotifier-{version}+{minecraft}.jar"
        number = f"{version}+{minecraft}"

        if not jar.is_file():
            print(f"  {minecraft}: no jar at {jar}, run tools/multiversion.py first")
            return 1

        if number in existing or jar.name in existing:
            print(f"  {minecraft}: {number} is already up, skipped")
            continue

        data = {
            "project_id": PROJECT,
            "name": f"v{version} for {minecraft}",
            "version_number": number,
            "changelog": changelog,
            "game_versions": [minecraft] + COVERS.get(minecraft, []),
            "version_type": "release",
            "loaders": ["fabric"],
            # The newest version is the one to hand someone who lands on the page.
            "featured": minecraft == shipped[-1],
            "environment": ENVIRONMENT,
            "dependencies": [{"project_id": "P7dR8mSH", "dependency_type": "required"}],
            "file_parts": ["file"],
        }

        if dry_run:
            print(f"  {minecraft}: would upload {jar.name} as {number} "
                  f"for {data['game_versions']}, {ENVIRONMENT}")
            continue

        body, content_type = multipart({"data": json.dumps(data)}, {"file": jar})

        try:
            result = request("POST", "/version", token, body, {"Content-Type": content_type})
            print(f"  {minecraft}: {result['version_number']} uploaded")
        except urllib.error.HTTPError as error:
            print(f"  {minecraft}: failed, {error.code} {error.read().decode('utf-8')[:300]}")
            return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
