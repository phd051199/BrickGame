#!/usr/bin/env python3
"""Removed external font importer.

Regular7 and Bold8 bitmap-font resources are vendored in the repository and
are extracted by build-j2me.sh.
"""
raise SystemExit("Bitmap fonts are already vendored; run sh build-j2me.sh")
