#!/usr/bin/env python3
"""Normalize vendored common maps and remove duplicate destination cells."""
from __future__ import annotations

import argparse
import struct
from pathlib import Path


def normalize(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:4] != b"BGM1":
        raise ValueError("invalid common map: " + str(path))
    board_count, preview_count = struct.unpack_from(">HH", data, 4)
    position = 8
    board = []
    preview = []
    for _ in range(board_count):
        board.append(struct.unpack_from(">BBBB", data, position))
        position += 4
    for _ in range(preview_count):
        preview.append(struct.unpack_from(">BBBB", data, position))
        position += 4

    # Keep the later source entry for duplicate destinations. The only current
    # duplicate case is Stack Challenge's hidden top row, so this crops that
    # row instead of blending it with the first visible row.
    board_by_cell = {}
    for entry in board:
        board_by_cell[(entry[2], entry[3])] = entry
    preview_by_cell = {}
    for entry in preview:
        preview_by_cell[(entry[2], entry[3])] = entry

    normalized_board = [board_by_cell[key] for key in sorted(board_by_cell, key=lambda value: (value[1], value[0]))]
    normalized_preview = [preview_by_cell[key] for key in sorted(preview_by_cell, key=lambda value: (value[1], value[0]))]

    with path.open("wb") as output:
        output.write(b"BGM1")
        output.write(struct.pack(">HH", len(normalized_board), len(normalized_preview)))
        for entry in normalized_board:
            output.write(struct.pack(">BBBB", *entry))
        for entry in normalized_preview:
            output.write(struct.pack(">BBBB", *entry))
    return len(board) - len(normalized_board), len(preview) - len(normalized_preview)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--resources", required=True, type=Path)
    args = parser.parse_args()
    root = args.resources.resolve()
    removed = 0
    for path in sorted(root.glob("*.map")):
        board_removed, preview_removed = normalize(path)
        removed += board_removed + preview_removed
        print("%-10s board-duplicates=%d preview-duplicates=%d" % (
            path.stem, board_removed, preview_removed
        ))
    print("Removed %d duplicate destination mappings" % removed)


if __name__ == "__main__":
    main()
