#!/usr/bin/env python3
"""Build each supported Minecraft version in an isolated copy of the source tree."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / 'build/multiversion'
TARGETS = {'1.21.11': '0.141.4+1.21.11', '26.3': '0.161.0+26.3'}
COVERS = {}
SKIP = set()
MOD_ID = 'worldeaternotifier'

def mod_version():
    return re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1].strip()

def rewrite_java(text):
    text = text.replace('ResourceLocation', 'Identifier').replace('.dimension().location()', '.dimension().identifier()')
    text = text.replace('.requires(s -> s.hasPermission(2))', '.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))')
    text = re.sub(r'\.getList\(("[^"]+"),\s*(?:Tag\.)?TAG_COMPOUND\)', r'.getListOrEmpty(\1)', text)
    text = re.sub(r'\.getCompound\(([^()]*)\)', r'.getCompoundOrEmpty(\1)', text)
    text = re.sub(r'\.getString\(("[^"]*")\)', r'.getStringOr(\1, "")', text)
    text = re.sub(r'\.getInt\(("[^"]*")\)', r'.getIntOr(\1, 0)', text)
    text = re.sub(r'\.getLong\(("[^"]*")\)', r'.getLongOr(\1, 0L)', text)
    text = re.sub(r'\.getLongArray\(("[^"]*")\)', r'.getLongArray(\1).orElse(new long[0])', text)
    text = re.sub(r'\.contains\(("[^"]*"),\s*(?:Tag\.)?TAG_\w+\)', r'.contains(\1)', text)
    return text

def build(version, errors_only=False):
    target = WORK / version
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    for name in ['src', 'gradle', 'gradlew', 'build.gradle', 'settings.gradle', 'gradle.properties', 'LICENSE']:
        source, destination = ROOT / name, target / name
        if source.is_dir():
            shutil.copytree(source, destination)
        elif source.is_file():
            shutil.copy2(source, destination)
    props = target / 'gradle.properties'
    text = props.read_text()
    text = re.sub(r'^minecraft_version=.*$', 'minecraft_version=' + version, text, flags=re.M)
    text = re.sub(r'^fabric_version=.*$', 'fabric_version=' + TARGETS[version], text, flags=re.M)
    text = re.sub(r'^fabric_api_version=.*$', 'fabric_api_version=' + TARGETS[version], text, flags=re.M)
    manifest = target / 'src/main/resources/fabric.mod.json'
    metadata = json.loads(manifest.read_text())
    metadata['depends']['minecraft'] = version
    if version == '26.3':
        text = re.sub(r'^loader_version=.*$', 'loader_version=0.19.5', text, flags=re.M)
        text = re.sub(r'^modmenu_version=.*$', 'modmenu_version=21.0.0', text, flags=re.M)
        metadata['depends'].update(java='>=25', fabricloader='>=0.19.5')
        gradle = target / 'build.gradle'
        content = gradle.read_text().replace("id 'fabric-loom' version '1.14.10'", "id 'net.fabricmc.fabric-loom' version '1.17.20'")
        content = re.sub(r'^\s*mappings .*\n', '\n', content, flags=re.M)
        content = content.replace('modImplementation ', 'implementation ').replace('modCompileOnly ', 'compileOnly ').replace('modImplementation(', 'implementation(')
        content = content.replace('def targetJavaVersion = 21', 'def targetJavaVersion = 25')
        content = content.replace('remapJar', 'jar')
        content = content.replace('JavaVersion.VERSION_21', 'JavaVersion.VERSION_25')
        content = content.replace('JavaLanguageVersion.of(21)', 'JavaLanguageVersion.of(25)')
        gradle.write_text(content)
        wrapper = target / 'gradle/wrapper/gradle-wrapper.properties'
        wrapper.write_text(re.sub(r'gradle-[\d.]+-bin.zip', 'gradle-9.6.0-bin.zip', wrapper.read_text()))
        for java in (target / 'src').rglob('*.java'):
            java.write_text(rewrite_java(java.read_text()))
        variant = ROOT / 'variants' / version
        if variant.exists():
            shutil.copytree(variant, target, dirs_exist_ok=True)
    props.write_text(text)
    manifest.write_text(json.dumps(metadata, indent=2) + '\n')
    java_home = Path(os.environ.get('JAVA_HOME_25_X64', str(Path.home() / '.local/opt/jdk-25'))) if version == '26.3' else Path(os.environ.get('JAVA_HOME', str(Path.home() / '.local/opt/jdk-21')))
    if not java_home.is_dir():
        raise RuntimeError('JDK not found: ' + str(java_home))
    task = 'classes' if errors_only else 'build'
    print(f'{version}: building {MOD_ID}', flush=True)
    result = subprocess.run(['./gradlew', task, '--console=plain', '-q'], cwd=target,
                            env={**os.environ, 'JAVA_HOME': str(java_home)}, text=True, capture_output=True)
    log = target / 'build.log'
    log.write_text(result.stdout + result.stderr)
    if result.returncode:
        print(log.read_text())
        print('FAILED; full output:', log)
        return False
    if not errors_only:
        jars = [jar for jar in (target / 'build/libs').glob('*.jar') if 'sources' not in jar.name and 'dev' not in jar.name]
        if len(jars) != 1:
            raise RuntimeError('Expected one output jar, found: ' + str(jars))
        output = WORK / f'{MOD_ID}-{mod_version()}+{version}.jar'
        shutil.copy2(jars[0], output)
        print('OK:', output.name, flush=True)
    return True

def main():
    wanted = [arg for arg in sys.argv[1:] if not arg.startswith('--')] or list(TARGETS)
    success = True
    for version in wanted:
        if version not in TARGETS:
            raise ValueError('Unknown Minecraft version: ' + version)
        success = build(version, '--errors' in sys.argv) and success
    return 0 if success else 1

if __name__ == '__main__':
    raise SystemExit(main())
