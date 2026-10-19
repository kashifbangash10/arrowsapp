"""Author exceptional APEX silhouettes using alternate legal tile heads.

The ordinary generator stores one self-clear Hamiltonian path per direction.
Donut lanes benefit from the other legal boundary head on the same tile: that
connects neighboring dependency regions while keeping every original mask cell.
"""
import argparse
import json
import time
import author_apex_campaign as apex

advanced = apex.advanced
dense = advanced.dense
_HEAD_CACHE = {}


def expanded_choices(tiles, width, height):
    dense.helper.W, dense.helper.H = width, height
    result = []
    for cells in tiles:
        ox = min(x for x, y in cells)
        oy = min(y for x, y in cells)
        normalized = tuple(sorted((x - ox, y - oy) for x, y in cells))
        if normalized not in _HEAD_CACHE:
            local = set(normalized)
            options = {}
            for direction, (dx, dy) in dense.DIRS:
                heads = sorted(p for p in local if (p[0] - dx, p[1] - dy) in local
                               and not any((p[0] + step * dx, p[1] + step * dy) in local
                                           for step in range(1, max(width, height))))
                for index, head in enumerate(heads):
                    path = dense._straighter_find_path(local, (dx, dy), budget=20000,
                                                       preferred_head=head)
                    if path:
                        options[f'{direction}|{index}'] = path
            assert options, ('No legal head', normalized)
            _HEAD_CACHE[normalized] = options
        result.append({name: [(x + ox, y + oy) for x, y in path]
                       for name, path in _HEAD_CACHE[normalized].items()})
    return result


def author(level_id, attempts=96):
    board = apex.source(level_id)
    palettes = ([4, 8, 8, 4, 8, 12, 4, 8, 4], [8, 4, 8, 8, 4, 8, 12, 8, 4],
                [4, 4, 8, 4, 4, 12, 4, 4, 8], [8, 8, 4, 8, 12, 4, 8, 8, 4],
                [4] * 7 + [8, 12], [4] * 7 + [12])
    best = None
    for attempt in range(attempts):
        seed = level_id + attempt * 5101
        tiles = dense.cluster_mask(board['mask'], board['cols'], board['rows'], seed,
                                   targets=palettes[attempt % len(palettes)])
        assert {p for tile in tiles for p in tile} == board['mask']
        choices = expanded_choices(tiles, board['cols'], board['rows'])
        for tie in range(12):
            arrows, solution, metrics = advanced.choose(
                tiles, choices, board['cols'], board['rows'], seed + tie * 7907,
                10000, min_openings=1)
            score = (max(0, len(metrics['openingMoves']) - 5),
                     max(0, apex.MIN_DEPTH - metrics['dependencyRounds']))
            if best is None or score < best[0]:
                best = (score, dict(metrics))
                print(f'BEST {level_id} attempt={attempt} tie={tie} {metrics}', flush=True)
            if len(metrics['openingMoves']) <= 5 and metrics['dependencyRounds'] >= apex.MIN_DEPTH:
                for arrow in arrows:
                    arrow['exit'] = arrow['exit'].split('|')[0]
                return advanced.audit(dict(board, arrows=arrows, solution=solution,
                                           metrics=metrics))
            if len(metrics['openingMoves']) > 5:
                break
        print(f'RETRY {level_id} attempt={attempt} best={best[1]}', flush=True)
    raise ValueError(f'Cannot author outlier {level_id}: {best[1]}')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--ids', default='1103,1353')
    parser.add_argument('--attempts', type=int, default=96)
    args = parser.parse_args()
    signatures = set()
    for level_id in map(int, args.ids.split(',')):
        assert level_id in (1103, 1191, 1353, 1441)
        path = apex.DEST / f'level-{level_id}.txt'
        record = apex.OUT / f'level-{level_id}.json'
        if path.exists() and record.exists():
            _, signature = apex.read_saved(level_id)
        else:
            started = time.time()
            level = author(level_id, args.attempts)
            apex.emit(level)
            saved, signature = apex.read_saved(level_id)
            print(f'SAVED {level_id}: {saved["metrics"]} seconds={time.time()-started:.2f}', flush=True)
        assert signature not in signatures, ('Repeated outlier layout', level_id)
        signatures.add(signature)
    print('PASS: saved outlier resources independently replayed with distinct layouts.', flush=True)


if __name__ == '__main__':
    main()


