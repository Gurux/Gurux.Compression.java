import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const versionPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-SNAPSHOT)?$/;

export function nextRelease(current, tags) {
  const match = versionPattern.exec(current);
  if (!match) throw new Error(`Expected a major.minor.patch version, got: ${current}`);
  const [, major, minor, patch] = match;
  let highest = BigInt(patch);
  for (const tag of tags) {
    const release = /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/.exec(tag);
    if (release && release[1] === major && release[2] === minor) {
      const value = BigInt(release[3]);
      if (value > highest) highest = value;
    }
  }
  return `${major}.${minor}.${highest + 1n}`;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  // This standalone POM has no parent; its first version is the project version.
  const pom = readFileSync('pom.xml', 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  const current = /<version>\s*([^<]+?)\s*<\/version>/.exec(pom)?.[1];
  if (!current || !versionPattern.test(current)) throw new Error('Invalid project version in pom.xml');
  const tags = execFileSync('git', ['tag', '--list', 'v*'], { encoding: 'utf8' }).trim().split(/\r?\n/);
  console.log(process.argv.includes('--current') ? current : nextRelease(current, tags));
}
