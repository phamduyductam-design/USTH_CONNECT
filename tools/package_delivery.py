"""Package sources and verification artifacts without machine-specific SDK/cache/preferences."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import shutil, hashlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output'; OUT.mkdir(exist_ok=True)
def copy(source,name):
    target=OUT/name;target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(ROOT/source,target)
copy('app/build/outputs/apk/debug/app-debug.apk','USTH-Connect-debug.apk')
copy('app/build/reports/lint-results-debug.html','verification/lint-results-debug.html')
copy('app/build/reports/lint-results-debug.xml','verification/lint-results-debug.xml')
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('*.xml'):
    copy(p.relative_to(ROOT),f'verification/unit-tests/{p.name}')
for name in ['build.log','network-local.log','network-public.log','emulator-final-attempt.log','apk-signature.log']:
    copy(Path('.tools')/name, 'verification/'+name)
with ZipFile(OUT/'USTH-Connect-source.zip','w',ZIP_DEFLATED) as archive:
    for folder in ['app/src','docs','tools','gradle','.vscode']:
        for p in (ROOT/folder).rglob('*'):
            if p.is_file() and 'sources' not in p.relative_to(ROOT).parts and '__pycache__' not in p.parts:
                archive.write(p,Path('USTHConnect')/p.relative_to(ROOT))
    for name in ['settings.gradle','build.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore','.gitattributes','README.md','app/build.gradle']:
        archive.write(ROOT/name,Path('USTHConnect')/name)
lines=[]
for p in sorted(OUT.glob('*')):
    if p.is_file() and p.name!='SHA256SUMS.txt':
        lines.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name)
(OUT/'SHA256SUMS.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('\n'.join(lines))
