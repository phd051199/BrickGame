#!/usr/bin/env python3
"""Deprecated compatibility entry point.

Runtime ROMs, common block maps and bitmap fonts are already vendored in the
repository. Use build-j2me.sh; no external graphics conversion is performed.
"""
raise SystemExit("Asset conversion was removed; run sh build-j2me.sh")
