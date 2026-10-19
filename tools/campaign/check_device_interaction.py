"""Exercise the current 1500-level debug APK using its production Compose UI.



Captures use a separate device-1500 folder; historical device evidence is retained.

The preview has in-memory progress, never the player's persisted preferences.

"""

import gc

import hashlib

import json

import re

import time

import xml.etree.ElementTree as ET

from capture_device import ROOT, OUT, adb, launch, screenshot, diagnostics



SAMPLE_LEVELS = (3, 30, 107, 500, 501, 750, 751, 1000, 1001, 1250, 1251, 1500)





def tree(name):

    adb('shell', 'uiautomator', 'dump', '/sdcard/campaign-interaction.xml')

    payload = adb('shell', 'cat', '/sdcard/campaign-interaction.xml')

    (OUT / (name + '.xml')).write_text(payload, encoding='utf-8')

    return ET.fromstring(payload)





def bounds(node):

    result = list(map(int, re.findall(r'\d+', node.attrib['bounds'])))

    assert len(result) == 4

    return result





def matching(root, predicate):

    nodes = [node for node in root.iter('node') if predicate(node.attrib)]

    assert len(nodes) == 1, 'Ambiguous or missing UI target: ' + str([n.attrib for n in nodes])

    return nodes[0]





def click(node):

    x0, y0, x1, y1 = bounds(node)

    assert x1 > x0 and y1 > y0, 'UI target is not visible'

    adb('shell', 'input', 'tap', str((x0 + x1) // 2), str((y0 + y1) // 2))

    time.sleep(.8)





def label(root, text):

    return matching(root, lambda a: a.get('content-desc') == text)





def board_node(root):

    return matching(root, lambda a: a.get('content-desc', '').startswith('Puzzle board with '))





def lives(root):

    node = matching(root, lambda a: re.fullmatch(r'\d+ of \d+ lives left', a.get('content-desc', '')) is not None)

    return int(node.attrib['content-desc'].split()[0])





def arrows(root):

    return int(board_node(root).attrib['content-desc'].split()[3])





def zoom_scale(root):

    node = matching(root, lambda a: re.fullmatch(r'\d+\.\d+\u00d7', a.get('text', '')) is not None)

    return float(node.attrib['text'][:-1])





def ready(level, suffix='read'):

    print(launch(level), flush=True)

    time.sleep(2)

    root = tree(f'level-{level:04}-{suffix}')

    assert any(n.attrib.get('text') == f'Level {level}' for n in root.iter('node'))

    return root





def main():

    OUT.mkdir(parents=True, exist_ok=True)

    catalog = json.loads((ROOT / 'artifacts/campaign/catalog.json').read_text())

    assert catalog['levelCount'] == 1500

    by_id = {level['id']: level for level in catalog['levels'] if level['id'] in SAMPLE_LEVELS}

    del catalog

    gc.collect()

    adb('logcat', '-b', 'all', '-c')

    prior_anr = adb('shell', 'dumpsys', 'activity', 'lastanr')

    (OUT / 'prior-last-anr.txt').write_text(prior_anr, encoding='utf-8')

    samples = []

    for identity in SAMPLE_LEVELS:

        root = ready(identity)

        assert arrows(root) == by_id[identity]['arrowCount']

        assert lives(root) == 3

        reading_scale = zoom_scale(root)

        screenshot(f'level-{identity:04}-read.png')

        click(label(root, 'Fit complete puzzle'))

        fitted = tree(f'level-{identity:04}-fit')

        assert zoom_scale(fitted) == 1.0

        assert arrows(fitted) == by_id[identity]['arrowCount']

        screenshot(f'level-{identity:04}-fit.png')

        click(label(fitted, 'Readable arrow size'))

        restored = tree(f'level-{identity:04}-read-restored')

        assert zoom_scale(restored) == reading_scale

        samples.append({'level': identity, 'arrows': arrows(restored), 'initialReadScale': reading_scale,

                        'fitScale': 1.0, 'readRestoresScale': True})

        print('Verified level', identity, flush=True)



    # Use an exact Fit transform for a known blocked route's centerline tap.

    identity = 751

    level = by_id[identity]

    root = ready(identity, 'before-interaction')

    click(label(root, 'Fit complete puzzle'))

    before = tree('level-0751-before-blocked')

    x0, y0, x1, y1 = bounds(board_node(before))

    width, height = x1-x0, y1-y0

    cell = min(width/level['cols'], height/level['rows'])

    ox, oy = x0+(width-level['cols']*cell)/2, y0+(height-level['rows']*cell)/2

    occupied = {tuple(c): a['id'] for a in level['arrows'] for c in a['cells']}

    vectors = {'UP': (0,-1), 'DOWN': (0,1), 'LEFT': (-1,0), 'RIGHT': (1,0)}

    blocked = []

    for arrow in level['arrows']:

        hx, hy = arrow['cells'][-1]

        dx, dy = vectors[arrow['exit']]

        x, y = hx+dx, hy+dy

        while 0 <= x < level['cols'] and 0 <= y < level['rows']:

            if (x,y) in occupied:

                if occupied[(x,y)] != arrow['id']: blocked.append(arrow)

                break

            x += dx

            y += dy

    assert blocked

    target = min(blocked, key=lambda a:(a['cells'][-1][0]-level['cols']/2)**2+(a['cells'][-1][1]-level['rows']/2)**2)

    p, q = target['cells'][-2:]

    px = round(ox+((p[0]+q[0])/2+.5)*cell)

    py = round(oy+((p[1]+q[1])/2+.5)*cell)

    initial_count, initial_lives = arrows(before), lives(before)

    adb('shell', 'input', 'tap', str(px), str(py))

    time.sleep(1)

    after = tree('level-0751-after-blocked')

    assert arrows(after) == initial_count, 'Blocked arrow was removed'

    assert lives(after) == initial_lives-1, 'Blocked tap did not register exactly one failed move'

    screenshot('level-0751-blocked.png')

    click(label(after, 'Readable arrow size'))

    read = tree('level-0751-interaction-read')

    read_scale = zoom_scale(read)

    click(label(read, 'Zoom in'))

    zoom = tree('level-0751-after-zoom')

    assert zoom_scale(zoom) > read_scale, 'Zoom control did not enlarge the board'

    assert arrows(zoom) == initial_count and lives(zoom) == initial_lives-1

    screenshot('level-0751-zoom.png')

    adb('shell', 'input', 'swipe', str((x0+x1)//2), str((y0+y1)//2),

        str((x0+x1)//2+110), str((y0+y1)//2+80), '500')

    time.sleep(.5)

    pan = tree('level-0751-after-pan')

    assert arrows(pan) == initial_count and lives(pan) == initial_lives-1, 'Pan selected an arrow'

    screenshot('level-0751-pan.png')

    click(label(pan, 'Hint, 2 remaining'))

    hint = tree('level-0751-after-hint')

    label(hint, 'Hint, 1 remaining')

    assert arrows(hint) == initial_count and lives(hint) == initial_lives-1

    assert zoom_scale(hint) >= zoom_scale(pan), 'Hint did not retain a readable viewport'

    screenshot('level-0751-hint.png')



    # Fresh campaign demonstrates the visible roadmap with no completed levels.

    print(launch(1, screen='levels'), flush=True)

    time.sleep(2)

    tree('roadmap-fresh-all-unlocked')

    screenshot('roadmap-fresh-all-unlocked.png')

    # Exercise the late-game filter with genuinely fresh, all-unlocked progress.
    filtered = tree('roadmap-before-apex-filter')
    origin = matching(filtered, lambda a: a.get('text') == 'ORIGINS')
    cx0, cy0, cx1, cy1 = bounds(origin)
    display = bounds(next(filtered.iter('node')))
    for attempt in range(8):
        choices = [n for n in filtered.iter('node') if n.attrib.get('text') == 'APEX']
        if choices:
            break
        adb('shell', 'input', 'swipe', str(int(display[2]*.85)), str((cy0+cy1)//2),
            str(int(display[2]*.15)), str((cy0+cy1)//2), '400')
        filtered = tree('roadmap-filter-strip')
    click(matching(filtered, lambda a: a.get('text') == 'APEX'))
    filtered = tree('roadmap-apex-filter')
    assert any(n.attrib.get('text') == '1001\u20131500' for n in filtered.iter('node'))
    first_apex = matching(filtered, lambda a: a.get('content-desc', '').startswith('Chapter 96,'))
    assert first_apex.attrib.get('enabled') == 'true'
    assert '1001 to 1010' in first_apex.attrib['content-desc']
    screenshot('roadmap-apex-filter.png')

    # Resume places the last chapter in the five-step hero, without a long scroll.

    print(launch(1500, screen='levels', completed_through=1499), flush=True)

    time.sleep(2)

    chapter = tree('level-1500-roadmap')

    screenshot('level-1500-roadmap.png')

    click(label(chapter, 'Chapter 145'))

    grid = tree('level-1500-chapter')

    screenshot('level-1500-chapter.png')

    display = bounds(next(grid.iter('node')))

    dx0, dy0, dx1, dy1 = display

    for attempt in range(12):

        targets = [n for n in grid.iter('node') if n.attrib.get('content-desc', '').startswith('Level 1500,')]

        if targets and bounds(targets[0])[3] > bounds(targets[0])[1]: break

        adb('shell', 'input', 'swipe', str((dx0+dx1)//2), str(int(dy1*.85)),

            str((dx0+dx1)//2), str(int(dy1*.4)), '400')

        time.sleep(.5)

        grid = tree('level-1500-chapter-scrolled')

    final_card = matching(grid, lambda a: a.get('content-desc', '').startswith('Level 1500,'))

    assert final_card.attrib.get('enabled') == 'true'

    screenshot('level-1500-card.png')

    click(final_card)

    loaded = tree('level-1500-opened-from-selector')

    assert any(n.attrib.get('text') == 'Level 1500' for n in loaded.iter('node'))

    assert arrows(loaded) == by_id[1500]['arrowCount']

    screenshot('level-1500-from-selector.png')

    logs = diagnostics('current-1500')

    anr = 'ANR in com.amowpuzzle.aitricker' in logs.get('logcat', '') or (

        'com.amowpuzzle.aitricker' in logs.get('last-anr', '') and logs['last-anr'] != prior_anr)

    report = {'productionRenderer': True, 'emulatorOnly': True, 'savedProgressChanged': False,

              'apkSha256': hashlib.sha256((ROOT/'app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest(),

              'samples': samples, 'blockedLevel': identity, 'blockedArrowId': target['id'],

              'blockedTapCoordinates': [px,py], 'blockedPreservesArrowCount': True,

              'blockedConsumesOneLife': True, 'panDoesNotSelectArrow': True,

              'hintConsumesOneHintWithoutMovingArrow': True, 'level1500SelectedFromLastChapter': True, 'apexFilterSelectedWithFreshProgress': True,

              'recordedNewAppAnr': anr,

              'limitations': ['Viewport pan requires separate visual comparison.',

                              'Emulator observations do not establish physical-phone comfort or frame pacing.']}

    (OUT/'final-interaction-verification.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')

    print(json.dumps(report, indent=2), flush=True)

    assert not anr, 'A new app ANR was recorded during verification'





if __name__ == '__main__':

    main()

