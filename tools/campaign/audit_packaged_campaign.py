"""Validate the APK's fixed campaign resources against the compiled catalog.

The package's actual removal certificates are replayed independently. This is
logical and packaging evidence; it does not establish Android touch performance.
"""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'artifacts/campaign'
TOTAL_LEVELS = 1500
GEOMETRY_FIELDS = ('id', 'cols', 'rows', 'difficulty', 'presentation', 'movementRule', 'arrows')
RESOURCE_GROUPS = (
    ('grandmaster', range(251, 501), 'GRANDMASTER_V1'),
    ('advanced', range(501, 1001), 'ADVANCED_V1'),
    ('apex', range(1001, 1501), 'APEX_V1'),
)
VECTORS = {'UP': (0, -1), 'DOWN': (0, 1), 'LEFT': (-1, 0), 'RIGHT': (1, 0)}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def compiled_levels(catalog):
    require(catalog['levelCount'] == TOTAL_LEVELS, 'Export all 1500 compiled levels before auditing packaging')
    levels = catalog['levels']
    by_id = {level['id']: level for level in levels}
    require(len(levels) == TOTAL_LEVELS and set(by_id) == set(range(1, TOTAL_LEVELS + 1)),
            'Missing or duplicate compiled level IDs')
    return by_id


def verify_baseline(by_id):
    baseline = read_json(OUT / 'baseline-1000/catalog.json')
    require(baseline['levelCount'] == 1000 and len(baseline['levels']) == 1000,
            'Incorrect existing-campaign baseline')
    require({level['id'] for level in baseline['levels']} == set(range(1, 1001)),
            'Incomplete existing-campaign baseline')
    for previous in baseline['levels']:
        current = by_id[previous['id']]
        for key in GEOMETRY_FIELDS:
            require(previous[key] == current[key], f"Existing level {previous['id']} changed {key}")
    return 1000


def expanded_path(row):
    fields = row.split('|')
    require(len(fields) == 3, 'Malformed packaged arrow record')
    arrow_id, direction, encoded = fields
    require(direction in VECTORS, 'Invalid arrow direction')
    xy = [int(value) for value in encoded.split(',')]
    require(len(xy) >= 4 and len(xy) % 2 == 0, 'Invalid corner coordinates')
    corners = list(zip(xy[::2], xy[1::2]))
    cells = [list(corners[0])]
    for first, last in zip(corners, corners[1:]):
        require((first[0] == last[0]) != (first[1] == last[1]), 'Non-orthogonal or zero-length segment')
        dx = (last[0] > first[0]) - (last[0] < first[0])
        dy = (last[1] > first[1]) - (last[1] < first[1])
        x, y = first
        while (x, y) != last:
            x += dx
            y += dy
            cells.append([x, y])
    return {'id': int(arrow_id), 'exit': direction, 'cells': cells}


def replay_certificate(level, solution, arrows):
    level_id = level['id']
    require(len(solution) == len(arrows) and len(set(solution)) == len(arrows) and
            set(solution) == {arrow['id'] for arrow in arrows},
            f'Incomplete or duplicate packaged certificate for {level_id}')
    active = {arrow['id']: arrow for arrow in arrows}
    require(len(active) == len(arrows), f'Duplicate packaged arrow ID for {level_id}')
    occupied = {}
    for arrow in arrows:
        for point in arrow['cells']:
            cell = tuple(point)
            require(0 <= cell[0] < level['cols'] and 0 <= cell[1] < level['rows'],
                    f'Packaged cell out of bounds at level {level_id}')
            require(cell not in occupied, f'Packaged static overlap at level {level_id}')
            occupied[cell] = arrow['id']
        tail, head = arrow['cells'][-2:]
        require((head[0] - tail[0], head[1] - tail[1]) == VECTORS[arrow['exit']],
                f'Packaged head points against its final segment at level {level_id}')
    for arrow_id in solution:
        moving = active[arrow_id]
        dx, dy = VECTORS[moving['exit']]
        hx, hy = moving['cells'][-1]
        x, y = hx + dx, hy + dy
        while 0 <= x < level['cols'] and 0 <= y < level['rows']:
            require((x, y) not in occupied,
                    f'Illegal packaged certificate at level {level_id}, arrow {arrow_id}')
            x += dx
            y += dy
        for point in moving['cells']:
            del occupied[tuple(point)]
        del active[arrow_id]
    require(not active and not occupied, f'Packaged certificate does not clear {level_id}')


def audit_package(apk_path, catalog):
    by_id = compiled_levels(catalog)
    preserved = verify_baseline(by_id)
    resource_hashes = {}
    file_count = 0
    with ZipFile(apk_path) as apk:
        expected_all = set()
        for group, identities, version in RESOURCE_GROUPS:
            prefix = f'levels/{group}/'
            expected = {prefix + 'index.tsv'} | {prefix + f'level-{i}.txt' for i in identities}
            expected_all.update(expected)
            entries = [entry for entry in apk.namelist() if entry.startswith(prefix) and not entry.endswith('/')]
            require(len(entries) == len(set(entries)), f'Duplicate {group} resource paths')
            require(set(entries) == expected, f'Missing or unexpected {group} resource entries')
            file_count += len(expected)
            index_payload = apk.read(prefix + 'index.tsv')
            require(index_payload == (ROOT / 'app/src/main/resources' / prefix / 'index.tsv').read_bytes(),
                    f'Packaged {group} index differs from source')
            metadata = {}
            for row in index_payload.decode('utf-8').splitlines():
                if not row.strip():
                    continue
                level_id, name, category, cols, rows = row.split('\t')
                require(int(level_id) not in metadata, f'Duplicate packaged {group} index identity')
                metadata[int(level_id)] = (name, category, int(cols), int(rows))
            require(set(metadata) == set(identities), f'Incomplete packaged {group} index')
            for level_id in identities:
                level = by_id[level_id]
                expected_difficulty = 'GRANDMASTER' if level_id <= 500 else 'LEGEND' if level_id <= 750 else 'EXTREME' if level_id <= 1000 else 'APEX'
                require(level['difficulty'] == expected_difficulty, f'Incorrect difficulty for {level_id}')
                require(level['movementRule'] == 'HEAD_CORRIDOR', f'Unsupported movement rule for {level_id}')
                require(metadata[level_id] == (level['name'], level['category'], level['cols'], level['rows']),
                        f'Packaged metadata differs from compiled level {level_id}')
                resource = prefix + f'level-{level_id}.txt'
                payload = apk.read(resource)
                require(payload == (ROOT / 'app/src/main/resources' / resource).read_bytes(),
                        f'APK and source differ for {level_id}')
                resource_hashes[str(level_id)] = hashlib.sha256(payload).hexdigest()
                lines = [row for row in payload.decode('utf-8').splitlines() if row.strip()]
                require(len(lines) >= 4 and lines[0] == version and int(lines[1]) == level_id,
                        f'Invalid packaged resource header for {level_id}')
                solution = [int(value) for value in lines[2].split(',')]
                arrows = [expanded_path(row) for row in lines[3:]]
                require(arrows == level['arrows'], f'Packaged geometry differs from compiled export for {level_id}')
                replay_certificate(level, solution, arrows)
        actual_all = [entry for entry in apk.namelist() if entry.startswith('levels/') and not entry.endswith('/')]
        require(len(actual_all) == len(set(actual_all)) and set(actual_all) == expected_all,
                'Unexpected or duplicate campaign resource entries')
    return {
        'levelCount': TOTAL_LEVELS,
        'preservedPreviousLevels': preserved,
        'packagedGrandmasterLevels': 250,
        'packagedLegendLevels': 250,
        'packagedExtremeLevels': 250,
        'packagedApexLevels': 500,
        'packagedResourceFiles': file_count,
        'packagedSolutionsReplayed': len(resource_hashes),
        'apk': str(apk_path.relative_to(ROOT)) if apk_path.is_relative_to(ROOT) else str(apk_path),
        'apkSha256': hashlib.sha256(apk_path.read_bytes()).hexdigest(),
        'resourceSha256': resource_hashes,
        'limitations': ['ZIP/resource verification does not certify an Android launch, touch ergonomics or frame pacing.'],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
    args = parser.parse_args()
    result = audit_package(args.apk, read_json(OUT / 'catalog.json'))
    (OUT / 'apk-resource-validation.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f"APK contains all {result['packagedResourceFiles']} fixed campaign resources; "
          f"{result['packagedSolutionsReplayed']} packaged solutions replayed and first 1000 levels preserved.")


if __name__ == '__main__':
    main()

