from pathlib import Path
import os, socket, subprocess, time, xml.etree.ElementTree as ET, re, sys
ROOT = Path(__file__).resolve().parent.parent
TOOLS = ROOT / '.tools'
SDK = Path(os.environ.get('ANDROID_HOME', str(TOOLS/'android-sdk')))
TOOLS.mkdir(exist_ok=True)
ADB = SDK / 'platform-tools/adb'
SERIAL = 'emulator-5554'
PACKAGE = 'vn.edu.usth.connect'
env = os.environ.copy()
env.update(ANDROID_HOME=str(SDK), ANDROID_AVD_HOME=str(TOOLS/'avd'), ANDROID_USER_HOME=str(TOOLS/'android-user'))
def adb(*args, **kwargs):
    return subprocess.run([str(ADB), '-s', SERIAL, *args], check=True, **kwargs)
def spawn(args, name, cwd=ROOT):
    log = open(TOOLS / (name+'.log'), 'a')
    process = subprocess.Popen([str(a) for a in args], cwd=cwd, env=env, stdin=subprocess.DEVNULL, stdout=log, stderr=log, start_new_session=True)
    (TOOLS/(name+'.pid')).write_text(str(process.pid))
def alive(name):
    try:
        os.kill(int((TOOLS/(name+'.pid')).read_text()), 0)
        return True
    except (OSError, ValueError):
        return False
try:
    with socket.create_connection(('127.0.0.1', 6667), timeout=1):
        pass
except OSError:
    spawn([TOOLS/'ergo/ergo', 'run', '--conf', 'ircd.yaml'], 'ergo', TOOLS/'ergo')
    time.sleep(1)
if not alive('demo-bot'):
    spawn([sys.executable, ROOT/'tools/demo-bot.py'], 'demo-bot')
devices = subprocess.check_output([str(ADB),'devices'], text=True)
if SERIAL not in devices:
    spawn([SDK/'emulator/emulator', '-avd', os.environ.get('USTH_AVD_NAME', 'usth_demo_api35'), '-port', '5554', '-no-snapshot', '-no-boot-anim', '-no-audio', '-gpu', 'auto'], 'emulator')
print('Dang cho may ao Android khoi dong...', flush=True)
for _ in range(180):
    result = subprocess.run([str(ADB), '-s', SERIAL, 'shell', 'getprop', 'sys.boot_completed'], capture_output=True, text=True)
    if result.stdout.strip() == '1':
        break
    time.sleep(1)
else:
    raise SystemExit('May ao chua khoi dong xong. Xem .tools/emulator.log')
apk = ROOT/'app/build/outputs/apk/debug/app-debug.apk'
if not apk.exists():
    subprocess.run(['bash', str(ROOT/'gradlew'), 'assembleDebug', '--console=plain'], cwd=ROOT, env=env, check=True)
adb('install','-r',str(apk))
adb('shell','am','force-stop', PACKAGE)
adb('shell','run-as',PACKAGE,'mkdir','-p','shared_prefs')
# Keep drafts and the chosen nickname when restarting the local demo.
existing = subprocess.run([str(ADB), '-s', SERIAL, 'exec-out', 'run-as', PACKAGE,
    'cat', 'shared_prefs/settings.xml'], capture_output=True, text=True)
try:
    prefs = ET.fromstring(existing.stdout)
except ET.ParseError:
    prefs = ET.Element('map')
def preference(tag, name, value):
    node = next((n for n in prefs if n.get('name') == name), None)
    if node is None: node = ET.SubElement(prefs, tag, name=name)
    if tag == 'string': node.text = value
    else: node.set('value', value)
preference('string', 'host', '10.0.2.2')
preference('int', 'port', '6667')
preference('boolean', 'tls', 'false')
for name, value in [('nick','mathew_demo'), ('username','usth'), ('realName','USTH Local Demo')]:
    if not any(n.get('name') == name for n in prefs): preference('string', name, value)
adb('shell','run-as',PACKAGE,'sh','-c','"cat > shared_prefs/settings.xml"',
    input=ET.tostring(prefs, encoding='unicode'), text=True)
adb('shell','input','keyevent','KEYCODE_WAKEUP')
adb('shell','wm','dismiss-keyguard')
adb('shell','am','start','-n',PACKAGE+'/.ui.ConnectActivity')
for _ in range(12):
    time.sleep(1)
    adb('shell','uiautomator','dump','/sdcard/usth-demo-ui.xml', stdout=subprocess.DEVNULL)
    result = adb('exec-out','cat','/sdcard/usth-demo-ui.xml', capture_output=True, text=True)
    nodes = ET.fromstring(result.stdout).iter('node')
    target = next((n for n in nodes if n.get('resource-id') == PACKAGE+':id/connect'), None)
    if target is not None:
        x1,y1,x2,y2 = map(int,re.findall(r'\d+',target.get('bounds')))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
        break
print('Demo da mo. Bam Tham gia kenh > Web Development > web-general > Tham gia de chat voi Demo_Bot.', flush=True)
