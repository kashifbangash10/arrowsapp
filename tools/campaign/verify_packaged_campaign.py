"""Check all 1500 compiled levels, packaged resources and complete JUnit reports.

Run after the full Gradle test/build and compiled CampaignExport. This does not
replace solver tests or device verification and deliberately rejects stale output.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET

from audit_packaged_campaign import (
    OUT, ROOT, TOTAL_LEVELS, audit_package, compiled_levels, read_json, require,
)


def unit_test_evidence():
    source_classes = set()
    source_method_counts = {}
    for path in (ROOT / 'app/src/test').rglob('*.kt'):
        text = path.read_text(encoding='utf-8-sig')
        package = re.search(r'^package\s+([\w.]+)', text, re.M)
        if package:
            names = re.findall(r'^class\s+(\w+Test)\b', text, re.M)
            require(len(names) <= 1, f'Update test discovery for multiple concrete classes in {path.name}')
            for name in names:
                identity = package.group(1) + '.' + name
                source_classes.add(identity)
                source_method_counts[identity] = len(re.findall(r'@Test\b', text))
    require(source_classes, 'No source test classes found')
    reports = {}
    for path in (ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        suite = ET.parse(path).getroot()
        require(suite.tag == 'testsuite', f'Unexpected JUnit XML format: {path.name}')
        require(suite.attrib['name'] not in reports, f'Duplicate JUnit suite: {suite.attrib["name"]}')
        reports[suite.attrib['name']] = {
            key: int(suite.attrib.get(key, 0)) for key in ('tests', 'failures', 'errors', 'skipped')
        }
    require(set(reports) == source_classes,
            f"Full unit suite coverage differs; missing={sorted(source_classes-set(reports))}, "
            f"extra={sorted(set(reports)-source_classes)}")
    for name, count in source_method_counts.items():
        require(reports[name]['tests'] == count,
                f'Filtered or incomplete test methods in {name}: expected {count}, reported {reports[name]["tests"]}')
    totals = {key: sum(row[key] for row in reports.values()) for key in ('tests', 'failures', 'errors', 'skipped')}
    require(totals['failures'] == totals['errors'] == totals['skipped'] == 0,
            f'Full suite is not entirely passing: {totals}')
    return {
        'sourceTestClasses': len(source_classes),
        'sourceTestMethods': sum(source_method_counts.values()),
        'reportedTestClasses': len(reports),
        'unitTests': totals,
    }


def independent_geometry_evidence(by_id):
    audit = read_json(OUT / 'campaign-validation.json')
    require(audit['levelCount'] == TOTAL_LEVELS and
            {row['levelId'] for row in audit['levels']} == set(range(1, TOTAL_LEVELS + 1)),
            'Independent geometry audit does not cover all 1500 levels')
    require(len(audit['levels']) == TOTAL_LEVELS, 'Duplicate independent audit level IDs')
    for row in audit['levels']:
        level = by_id[row['levelId']]
        for audited_key, exported_key in [
            ('arrowCount', 'arrowCount'), ('openings', 'initiallyFree'),
            ('dependencyRounds', 'dependencyRounds'), ('difficulty', 'difficulty'),
            ('movementRule', 'movementRule'),
        ]:
            require(row[audited_key] == level[exported_key], f"Stale audit result on {row['levelId']}")
        count = level['arrowCount']
        require(row['verifiedChoices'] == count * (count + 1) // 2,
                f"Incomplete choice replay on {row['levelId']}")
        require(row['solutionIds'] == level['solutionIds'],
                f"Independent audit solution differs from compiled export on {row['levelId']}")
    baseline = read_json(OUT / 'baseline-1000/campaign-validation.json')
    require(baseline['levelCount'] == 1000, 'Missing complete first-1000 geometry checkpoint')
    current_rows = {row['levelId']: row for row in audit['levels']}
    require(len(baseline['levels']) == 1000, 'Incomplete first-1000 audit checkpoint')
    for previous in baseline['levels']:
        require(current_rows[previous['levelId']] == previous,
                f"Independent audit changed on existing level {previous['levelId']}")
    return sum(row['verifiedChoices'] for row in audit['levels'])


def difficulty_evidence(by_id):
    result = {}
    previous = None
    for name, identities in [
        ('grandmaster', range(251, 501)), ('legend', range(501, 751)), ('extreme', range(751, 1001)),
        ('apex', range(1001, 1501)),
    ]:
        levels = [by_id[identity] for identity in identities]
        require(all(row['difficulty'] == name.upper() for row in levels), f'Incorrect {name} difficulty band')
        metrics = {
            'levels': len(levels),
            'arrowRange': [min(row['arrowCount'] for row in levels), max(row['arrowCount'] for row in levels)],
            'openingRange': [min(row['initiallyFree'] for row in levels), max(row['initiallyFree'] for row in levels)],
            'dependencyRange': [min(row['dependencyRounds'] for row in levels), max(row['dependencyRounds'] for row in levels)],
            'averageArrows': sum(row['arrowCount'] for row in levels) / len(levels),
            'averageDependencyRounds': sum(row['dependencyRounds'] for row in levels) / len(levels),
            'categories': len({row['category'] for row in levels}),
        }
        if previous is not None:
            require(metrics['dependencyRange'][0] > previous['dependencyRange'][1],
                    f'{name} does not exceed every board in the preceding difficulty band')
            require(metrics['averageArrows'] > previous['averageArrows'],
                    f'{name} does not increase the average board size')
        result[name] = metrics
        previous = metrics
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
    args = parser.parse_args()
    catalog = read_json(OUT / 'catalog.json')
    by_id = compiled_levels(catalog)
    packaged = audit_package(args.apk, catalog)
    test_evidence = unit_test_evidence()
    choices = independent_geometry_evidence(by_id)
    tiers = difficulty_evidence(by_id)
    report = {
        'scope': 'Final 1500-level compiled catalog, full source unit test class set, and actual APK resources',
        'levelCount': TOTAL_LEVELS,
        'first1000GeometryUnchanged': True,
        'packagedGrandmasterBoards': packaged['packagedGrandmasterLevels'],
        'packagedLegendBoards': packaged['packagedLegendLevels'],
        'packagedExtremeBoards': packaged['packagedExtremeLevels'],
        'packagedApexBoards': packaged['packagedApexLevels'],
        'packagedResourceFiles': packaged['packagedResourceFiles'],
        'packagedCoordinatesMatchCompiledCatalog': True,
        'packagedSolutionsReplayed': packaged['packagedSolutionsReplayed'],
        **test_evidence,
        'independentChoices': choices,
        'first1000IndependentAuditUnchanged': True,
        'difficultyTiers': tiers,
        'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
        'limitations': ['This checks existing test evidence; it does not execute tests.',
                        'Logical and packaging verification does not establish device responsiveness or touch comfort.',
                        'Visual recognition needs separate rendered-board inspection.'],
    }
    (OUT / 'apk-resource-validation.json').write_text(json.dumps(packaged, indent=2) + '\n', encoding='utf-8')
    (OUT / 'final-build-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()

