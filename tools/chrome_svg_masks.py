#!/usr/bin/env python3
"""Removed legacy graphics converter.

The MIDlet now uses vendored RAM-bit common block maps and direct LCDUI block
rendering. This file remains only to make stale local commands fail clearly.
"""
raise SystemExit("Legacy graphics conversion was removed; run sh build-j2me.sh")
