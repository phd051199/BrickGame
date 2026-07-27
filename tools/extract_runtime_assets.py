#!/usr/bin/env python3
import argparse
import pathlib
import zipfile

ALLOWED_SUFFIXES = (".bin", ".map", ".lcd", ".bmf")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=pathlib.Path)
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.archive) as source:
        for name in source.namelist():
            if not name.startswith(("brickrom/", "ui/")):
                continue
            if not name.endswith(ALLOWED_SUFFIXES):
                continue
            target = args.output / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read(name))


if __name__ == "__main__":
    main()
