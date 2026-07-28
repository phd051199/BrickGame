#!/usr/bin/env python3
import argparse
import base64
import io
import tarfile
from pathlib import Path

FILES = (
    "e23/program.bin",
    "e23/display.map",
    "e23/regular.bmf",
    "e23/bold.bmf",
)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--parts", nargs="+", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    encoded = b"".join(part.read_bytes() for part in args.parts)
    archive = base64.b64decode(encoded, validate=True)
    args.output.mkdir(parents=True, exist_ok=True)

    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as stream:
        members = {member.name: member for member in stream.getmembers()}
        for name in FILES:
            member = members.get(name)
            if member is None:
                raise ValueError("Missing asset: " + name)
            source = stream.extractfile(member)
            if source is None:
                raise ValueError("Unreadable asset: " + name)
            target = args.output / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read())


if __name__ == "__main__":
    main()
