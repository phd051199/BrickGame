#!/usr/bin/env python3
"""Convert legacy per-segment LCD metadata into compact common block maps.

This is intentionally geometry-only. Segment masks are never copied to the
MIDlet and no SVG engine is involved. Each ROM receives a RAM-bit -> canonical
10x20 board mapping, plus an optional 4x4 next-piece mapping.
"""
from __future__ import annotations

import argparse
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple


@dataclass(frozen=True)
class Spec:
    columns: int
    rows: int
    min_width: int
    max_width: int
    min_height: int
    max_height: int


SPECS = {
    "e23": Spec(10, 20, 19, 22, 10, 13),
    "e88": Spec(10, 20, 19, 22, 10, 13),
    "e33": Spec(10, 20, 19, 22, 10, 13),
    "apollo126": Spec(10, 20, 19, 22, 10, 13),
    "apollo18": Spec(10, 16, 20, 23, 11, 14),
    "stack": Spec(9, 21, 19, 22, 11, 14),
    "ga888": Spec(8, 12, 35, 41, 15, 18),
    "key55": Spec(8, 12, 35, 41, 15, 18),
    "ga878": Spec(8, 11, 35, 42, 17, 21),
    "micon": Spec(8, 14, 35, 41, 14, 17),
}


@dataclass
class Segment:
    ram: int
    bit: int
    x: int
    y: int
    width: int
    height: int

    @property
    def cx(self) -> int:
        return self.x * 2 + self.width

    @property
    def cy(self) -> int:
        return self.y * 2 + self.height


def read_layout(path: Path) -> List[Segment]:
    data = path.read_bytes()
    if data[:4] != b"BGL1":
        raise ValueError("invalid legacy LCD layout: " + str(path))
    _, _, count = struct.unpack_from(">HHH", data, 4)
    position = 10
    segments = []
    for _ in range(count):
        ram, bit, x, y, width, height, length = struct.unpack_from(
            ">BBHHHHH", data, position
        )
        position += 12 + length
        segments.append(Segment(ram, bit, x, y, width, height))
    return segments


def cluster(values: Iterable[int], tolerance: int = 6) -> List[int]:
    ordered = sorted(values)
    groups: List[List[int]] = []
    for value in ordered:
        if not groups or value - groups[-1][-1] > tolerance:
            groups.append([value])
        else:
            groups[-1].append(value)
    return [sum(group) // len(group) for group in groups]


def select_axis(values: Sequence[int], expected: int, counts: dict) -> List[int]:
    if len(values) < expected:
        raise ValueError("not enough grid coordinates")
    if len(values) == expected:
        return list(values)
    # Prefer dense coordinates, then choose a spatially coherent run. This
    # discards score/preview blocks that happen to share playfield dimensions.
    best = None
    for start in range(0, len(values) - expected + 1):
        run = values[start : start + expected]
        density = sum(counts.get(value, 0) for value in run)
        gaps = sum(abs((run[i + 1] - run[i]) - (run[-1] - run[0]) // max(1, expected - 1))
                   for i in range(expected - 1))
        score = density * 1000 - gaps
        if best is None or score > best[0]:
            best = (score, run)
    return list(best[1])


def nearest(value: int, centers: Sequence[int]) -> Tuple[int, int]:
    best_index = 0
    best_distance = abs(value - centers[0])
    for index in range(1, len(centers)):
        distance = abs(value - centers[index])
        if distance < best_distance:
            best_index = index
            best_distance = distance
    return best_index, best_distance


def map_row(source_row: int, source_rows: int) -> int:
    if source_rows <= 20:
        return source_row + (20 - source_rows) // 2
    # Stack Challenge exposes one extra hidden/top row. Crop that row instead
    # of OR-ing two independent source rows into one visible destination row.
    return source_row - 1


def build_map(machine: str, segments: Sequence[Segment]) -> Tuple[list, list]:
    spec = SPECS[machine]
    candidates = [
        segment
        for segment in segments
        if spec.min_width <= segment.width <= spec.max_width
        and spec.min_height <= segment.height <= spec.max_height
    ]
    if len(candidates) < spec.columns * spec.rows:
        raise ValueError(
            "%s: found only %d block candidates, expected %d"
            % (machine, len(candidates), spec.columns * spec.rows)
        )

    raw_x = cluster(segment.cx for segment in candidates)
    raw_y = cluster(segment.cy for segment in candidates)
    x_counts = {center: 0 for center in raw_x}
    y_counts = {center: 0 for center in raw_y}
    for segment in candidates:
        xi, _ = nearest(segment.cx, raw_x)
        yi, _ = nearest(segment.cy, raw_y)
        x_counts[raw_x[xi]] += 1
        y_counts[raw_y[yi]] += 1

    xs = select_axis(raw_x, spec.columns, x_counts)
    ys = select_axis(raw_y, spec.rows, y_counts)
    x_spacing = max(8, (xs[-1] - xs[0]) // max(1, len(xs) - 1))
    y_spacing = max(8, (ys[-1] - ys[0]) // max(1, len(ys) - 1))

    cells = {}
    leftovers = []
    for segment in candidates:
        column, dx = nearest(segment.cx, xs)
        row, dy = nearest(segment.cy, ys)
        if dx <= x_spacing // 2 and dy <= y_spacing // 2:
            key = (column, row)
            distance = dx + dy
            previous = cells.get(key)
            if previous is None or distance < previous[0]:
                if previous is not None:
                    leftovers.append(previous[1])
                cells[key] = (distance, segment)
            else:
                leftovers.append(segment)
        else:
            leftovers.append(segment)

    board = []
    x_offset = (10 - spec.columns) // 2
    for (column, row), (_, segment) in sorted(cells.items(), key=lambda item: (item[0][1], item[0][0])):
        if spec.rows > 20 and row == 0:
            continue
        board.append((segment.ram, segment.bit, column + x_offset, map_row(row, spec.rows)))

    # Only use a real 4x4 regular block cluster to populate NEXT. Arbitrary
    # symbols and digit segments are intentionally ignored.
    preview = []
    outside = [segment for segment in leftovers if segment.cx > xs[-1] + x_spacing]
    px = cluster(segment.cx for segment in outside)
    py = cluster(segment.cy for segment in outside)
    if len(px) >= 4 and len(py) >= 4:
        px = px[:4]
        py = py[:4]
        for segment in outside:
            column, dx = nearest(segment.cx, px)
            row, dy = nearest(segment.cy, py)
            if column < 4 and row < 4 and dx <= x_spacing and dy <= y_spacing:
                preview.append((segment.ram, segment.bit, column, row))

    expected = spec.columns * spec.rows
    allowed_missing = max(12, expected // 10)
    if len(board) < expected - allowed_missing:
        raise ValueError("%s: mapped only %d/%d board cells" % (machine, len(board), expected))
    return board, preview


def write_map(path: Path, board: Sequence[tuple], preview: Sequence[tuple]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as output:
        output.write(b"BGM1")
        output.write(struct.pack(">HH", len(board), len(preview)))
        for entry in board:
            output.write(struct.pack(">BBBB", *entry))
        for entry in preview:
            output.write(struct.pack(">BBBB", *entry))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--resources", required=True, type=Path)
    args = parser.parse_args()
    root = args.resources.resolve()
    for machine in SPECS:
        source = root / (machine + ".lcd")
        target = root / (machine + ".map")
        board, preview = build_map(machine, read_layout(source))
        write_map(target, board, preview)
        print("%-10s board=%3d preview=%2d" % (machine, len(board), len(preview)))


if __name__ == "__main__":
    main()
