"""Record real production withdrawal callbacks in the memory-only debug preview.

The clip is deliberately separate from full-level autoplay verification. Controls
are located by accessibility labels, so recording works across display densities.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from capture_device import ADB, DEVICE, OUT, adb, launch, screenshot


def button_center(label):
    adb('shell', 'uiautomator', 'dump', '/sdcard/campaign-recording-window.xml')
    root = ET.fromstring(adb('shell', 'cat', '/sdcard/campaign-recording-window.xml'))
    matches = [node for node in root.iter('node') if node.attrib.get('content-desc') == label]
    if len(matches) != 1:
        raise RuntimeError(f'Expected one visible {label!r} control; got {len(matches)}')
    bounds = list(map(int, re.findall(r'\d+', matches[0].attrib['bounds'])))
    if len(bounds) != 4:
        raise RuntimeError('Cannot read the actual control bounds')
    x0, y0, x1, y1 = bounds
    return (x0 + x1) // 2, (y0 + y1) // 2


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--level', type=int, default=251)
    parser.add_argument('--seconds', type=int, default=20)
    parser.add_argument('--name')
    args = parser.parse_args()
    if not 5 <= args.seconds <= 180:
        parser.error('--seconds must be between 5 and 180')
    OUT.mkdir(parents=True, exist_ok=True)
    prefix = args.name or f'level-{args.level:03}-production-animation'
    print(launch(args.level), flush=True)
    time.sleep(3)
    control = button_center('Play solution')
    prior_anr = adb('shell', 'dumpsys', 'activity', 'lastanr')
    adb('logcat', '-b', 'all', '-c')
    remote = f'/sdcard/campaign-motion-{args.level:03}.mp4'
    # Let screenrecord finish its own requested clip, rather than killing all
    # recording processes or confusing a short clip with full solution playback.
    record = subprocess.Popen(
        [ADB, '-s', DEVICE, 'shell', 'screenrecord', '--bit-rate', '3000000',
         '--time-limit', str(args.seconds), remote],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    time.sleep(1)
    started = time.monotonic()
    adb('shell', 'input', 'tap', str(control[0]), str(control[1]))
    stdout, stderr = record.communicate(timeout=args.seconds + 30)
    if record.returncode:
        raise RuntimeError((stderr + stdout).decode(errors='replace'))
    target = OUT / f'{prefix}.mp4'
    print(adb('pull', remote, str(target)), flush=True)
    screenshot(f'{prefix}-after.png')
    log = adb('logcat', '-d', '-b', 'all', '-v', 'threadtime')
    last_anr = adb('shell', 'dumpsys', 'activity', 'lastanr')
    (OUT / f'{prefix}-logcat.txt').write_text(log, encoding='utf-8')
    (OUT / f'{prefix}-prior-last-anr.txt').write_text(prior_anr, encoding='utf-8')
    (OUT / f'{prefix}-last-anr.txt').write_text(last_anr, encoding='utf-8')
    report = {
        'level': args.level, 'requestedClipSeconds': args.seconds,
        'elapsedSeconds': round(time.monotonic() - started, 2),
        'productionRenderer': True, 'savedProgressChanged': False,
        'playControlCoordinates': list(control), 'emulatorOnly': True,
        'fullSolutionVerified': False,
        'recordedAppAnr': 'ANR in com.amowpuzzle.aitricker' in log or (
            'com.amowpuzzle.aitricker' in last_anr and last_anr != prior_anr),
        'limitation': 'This short clip needs visual review; it does not certify full completion or physical-phone frame pacing.',
    }
    (OUT / f'{prefix}.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2), flush=True)


if __name__ == '__main__':
    main()
