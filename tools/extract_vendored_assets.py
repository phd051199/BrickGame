#!/usr/bin/env python3
import argparse
import base64
import io
import tarfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--parts", nargs="+", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    encoded = b"".join(part.read_bytes() for part in args.parts)
    archive = base64.b64decode(encoded, validate=True)
    args.output.mkdir(parents=True, exist_ok=True)

    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as stream:
        for member in stream.getmembers():
            path = Path(member.name)
            if path.is_absolute() or ".." in path.parts:
                raise ValueError("Invalid asset path")
            if any(part.startswith("._") for part in path.parts):
                continue
            if not path.parts or path.parts[0] not in ("brickrom", "ui"):
                raise ValueError("Unexpected asset path")
            if member.isdir():
                continue
            source = stream.extractfile(member)
            if source is None:
                raise ValueError("Unreadable asset")
            target = args.output / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read())


if __name__ == "__main__":
    main()
