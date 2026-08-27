#!/usr/bin/env python3
"""Structural/provenance validation; report files are generated QA artifacts."""
import json
import re
from pathlib import Path
from urllib.parse import urlparse
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[1]
packs = [json.loads((BASE / "app/src/main/assets" / name).read_text(encoding="utf-8"))
         for name in ["guides.json", "taxes.json"]]
guides, sources = [], {}
for pack in packs:
    assert pack["version"] == 1 and pack["reviewed"] and pack["note"]
    assert not (sources.keys() & pack["sources"].keys()), "Duplicate source IDs"
    sources.update(pack["sources"])
    guides.extend(pack["guides"])
assert len(packs[0]["guides"]) == 24, "Original guide pack must be preserved"
assert len(packs[1]["guides"]) == 37, "Expected 37 tax guides"
assert len(guides) == 61, f"Expected 61 guides, got {len(guides)}"
policy = (BASE / "app/src/main/java/ir/cafenet/hamyar/UrlPolicy.java").read_text()
host_block = re.search(r"Arrays.asList\((.*?)\)\);", policy, re.S).group(1)
hosts = set(re.findall(r'"([a-z0-9.-]+)"', host_block))
def approved(url):
    parsed = urlparse(url)
    return parsed.scheme == "https" and parsed.hostname in hosts and parsed.username is None and parsed.port in (None,443)
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
    if identifier.startswith("tax_"):
        assert guide["category"].startswith("مالیات؛")
        assert guide["evidence"] == "route", "Current live tax workflows were not tested"
        assert "فرم" in guide["caution"] and any(s in guide["caution"] for s in ["حسابدار", "مشاور", "متخصص"])
        assert all(x.startswith("tax_") for x in guide["sources"])
        assert not re.search(r"\b(?:TODO|TBD)\b", json.dumps(guide, ensure_ascii=False))
    for step in guide["steps"]:
        assert len(step["title"]) >= 5 and len(step["text"]) >= 70, (identifier, step)
        assert "TODO" not in step["text"]
    for problem in guide["problems"]:
        assert len(problem["problem"]) >= 5 and len(problem["fix"]) >= 60
    if guide["route"]:
        assert approved(guide["route"]), guide["route"]
    count_steps += len(guide["steps"])
    count_problems += len(guide["problems"])
for source in sources.values():
    assert approved(source["url"]), source["url"]
    assert source["title"] and source["scope"]
    assert " " not in source["url"]
manifest = ET.parse(BASE / "app/src/main/AndroidManifest.xml").getroot()
assert not manifest.findall("uses-permission"), "Source manifest must require no device permissions"
assert manifest.find("application").get("{http://schemas.android.com/apk/res/android}allowBackup") == "false"
report = {
    "guides": len(guides),
    "tax_guides": len(packs[1]["guides"]),
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
