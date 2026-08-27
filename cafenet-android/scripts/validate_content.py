#!/usr/bin/env python3
"""Structural/provenance validation; report files are generated QA artifacts."""
import json
import re
from pathlib import Path
from urllib.parse import urlparse
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[1]
data = json.loads((BASE / "app/src/main/assets/guides.json").read_text(encoding="utf-8"))
assert data["version"] == 1
assert data["reviewed"] and data["note"]
guides = data["guides"]
sources = data["sources"]
assert len(guides) == 24, f"Expected 24 guides, got {len(guides)}"
ids = set()
count_steps = 0
count_problems = 0
for guide in guides:
    identifier = guide["id"]
    assert re.fullmatch(r"[a-z_]+", identifier) and identifier not in ids, identifier
    ids.add(identifier)
    for key in ["title", "category", "summary", "mode", "caution", "outcome"]:
        assert isinstance(guide[key], str) and len(guide[key]) >= 5, (identifier, key)
    assert guide["evidence"] in ("route", "official")
    assert len(guide["ready"]) >= 3
    assert len(guide["steps"]) >= 8
    assert len(guide["problems"]) >= 3
    assert len(guide["finish"]) >= 2
    assert len(guide["aliases"]) >= 3
    assert guide["sources"] and all(x in sources for x in guide["sources"])
    for step in guide["steps"]:
        assert len(step["title"]) >= 5 and len(step["text"]) >= 70, (identifier, step)
        assert "TODO" not in step["text"]
    for problem in guide["problems"]:
        assert len(problem["problem"]) >= 5 and len(problem["fix"]) >= 60
    if guide["route"]:
        assert urlparse(guide["route"]).scheme == "https"
    count_steps += len(guide["steps"])
    count_problems += len(guide["problems"])
for source in sources.values():
    assert urlparse(source["url"]).scheme == "https"
    assert source["title"] and source["scope"]
    assert " " not in source["url"]
manifest = ET.parse(BASE / "app/src/main/AndroidManifest.xml").getroot()
assert not manifest.findall("uses-permission"), "Source manifest must require no device permissions"
assert manifest.find("application").get("{http://schemas.android.com/apk/res/android}allowBackup") == "false"
report = {
    "guides": len(guides),
    "steps": count_steps,
    "troubleshooting_cases": count_problems,
    "sources": len(sources),
    "categories": sorted({g["category"] for g in guides}),
    "live_customer_transactions_tested": False,
    "permissions_in_source_manifest": [],
    "result": "passed"
}
out = BASE / "app/build/reports/content"
out.mkdir(parents=True, exist_ok=True)
(out / "validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
