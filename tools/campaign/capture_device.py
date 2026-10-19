"""Capture production UI from the debug memory-only preview on emulator-5554.

No real GamePreferences instance is created by this debug Activity. Diagnostics
are observations of an emulator run, not a physical-device performance rating.
"""
from pathlib import Path
import argparse
import json
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'artifacts/campaign/device-1500'
ADB = 'C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
DEVICE = 'emulator-5554'
PACKAGE = 'com.amowpuzzle.aitricker'
ACTIVITY = PACKAGE + '/.redesign.CampaignPreviewActivity'


def adb(*args, binary=False, timeout=60):
    result = subprocess.run([ADB, '-s', DEVICE, *args], capture_output=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace') + result.stdout.decode(errors='replace'))
    return result.stdout if binary else result.stdout.decode(errors='replace')


def launch(level, autoplay=False, screen='game', completed_through=0, unlocked_through=None):
    extras = [
        '--ei', 'level', str(level), '--ez', 'dark', 'true',
        '--ez', 'grid', 'false', '--ez', 'autoplay', str(autoplay).lower(),
        '--es', 'screen', screen, '--ei', 'completedThrough', str(completed_through),
    ]
    if unlocked_through is not None:
        extras += ['--ei', 'unlockedThrough', str(unlocked_through)]
    return adb('shell', 'am', 'start', '-S', '-W', '-n', ACTIVITY, *extras, timeout=90)


def screenshot(name):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    path.write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    print(path, flush=True)
    return path


def hierarchy(name):
    OUT.mkdir(parents=True, exist_ok=True)
    adb('shell', 'uiautomator', 'dump', '/sdcard/campaign-window.xml')
    (OUT / name).write_text(adb('shell', 'cat', '/sdcard/campaign-window.xml'), encoding='utf-8')


def diagnostics(prefix):
    OUT.mkdir(parents=True, exist_ok=True)
    commands = {
        'logcat': ('logcat', '-d', '-b', 'all', '-v', 'threadtime'),
        'last-anr': ('shell', 'dumpsys', 'activity', 'lastanr'),
        'exit-info': ('shell', 'dumpsys', 'activity', 'exit-info', PACKAGE),
        'frame-stats': ('shell', 'dumpsys', 'gfxinfo', PACKAGE, 'framestats'),
        'memory': ('shell', 'dumpsys', 'meminfo', PACKAGE),
    }
    output = {}
    for key, command in commands.items():
        try:
            output[key] = adb(*command, timeout=60)
        except (RuntimeError, subprocess.TimeoutExpired) as error:
            output[key] = 'CAPTURE ERROR: ' + str(error)
        (OUT / f'{prefix}-{key}.txt').write_text(output[key], encoding='utf-8')
    return output


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--level', type=int, default=6)
    parser.add_argument('--autoplay', action='store_true')
    parser.add_argument('--wait', type=float, default=4)
    parser.add_argument('--name')
    parser.add_argument('--screen', choices=['game', 'levels'], default='game')
    parser.add_argument('--completed-through', type=int, default=0)
    parser.add_argument('--unlocked-through', type=int)
    parser.add_argument('--diagnostics', action='store_true')
    parser.add_argument('--until-victory', action='store_true')
    parser.add_argument('--timeout', type=float, default=900)
    args = parser.parse_args()
    if args.until_victory and (not args.autoplay or args.screen != 'game'):
        parser.error('--until-victory requires --autoplay and --screen game')
    prefix = Path(args.name).stem if args.name else f'level-{args.level:03}-device'
    OUT.mkdir(parents=True, exist_ok=True)
    if args.diagnostics or args.until_victory:
        adb('logcat', '-b', 'all', '-c')
    # lastanr survives logcat clearing and can describe an older APK session.
    baseline_anr = ''
    if args.diagnostics:
        baseline_anr = adb('shell', 'dumpsys', 'activity', 'lastanr')
        (OUT / f'{prefix}-prior-last-anr.txt').write_text(baseline_anr, encoding='utf-8')
    started = time.monotonic()
    try:
        launch_output = launch(args.level, args.autoplay, args.screen, args.completed_through, args.unlocked_through)
    except (RuntimeError, subprocess.TimeoutExpired) as error:
        (OUT / f'{prefix}-launch.txt').write_text('LAUNCH ERROR: ' + str(error), encoding='utf-8')
        if args.diagnostics:
            diagnostics(prefix)
        raise
    (OUT / f'{prefix}-launch.txt').write_text(launch_output, encoding='utf-8')
    print(launch_output, flush=True)
    time.sleep(args.wait)
    victory = False
    preview_log = ''
    if args.until_victory:
        next_report = 0
        while time.monotonic() - started < args.timeout:
            preview_log = adb('logcat', '-d', '-s', 'CampaignPreview:I', '*:S')
            victory = f'VICTORY level={args.level} ' in preview_log
            if victory:
                time.sleep(2)
                break
            elapsed = time.monotonic() - started
            if elapsed >= next_report:
                print(f'Waiting for Level {args.level} victory: {elapsed:.1f}s', flush=True)
                next_report = elapsed + 30
            time.sleep(5)
    screenshot(args.name or f'level-{args.level:03}-device.png')
    logs = diagnostics(prefix) if args.diagnostics else {}
    if args.until_victory:
        (OUT / f'{prefix}-autoplay.log').write_text(preview_log, encoding='utf-8')
        report = {
            'level': args.level,
            'victory': victory,
            'elapsedSeconds': round(time.monotonic() - started, 2),
            'activity': 'CampaignPreviewActivity',
            'productionRenderer': True,
            'savedProgressChanged': False,
            'emulatorOnly': True,
            'recordedAppAnr': f'ANR in {PACKAGE}' in logs.get('logcat', '') or (
                PACKAGE in logs.get('last-anr', '') and logs.get('last-anr', '') != baseline_anr),
            'preexistingAppAnr': PACKAGE in baseline_anr,
        }
        (OUT / f'{prefix}-autoplay.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
        print(json.dumps(report), flush=True)
        if not victory:
            raise SystemExit('Autoplay did not reach victory within the capture timeout.')
