import assert from 'node:assert/strict';
import test from 'node:test';
import { nextRelease } from './next-release.mjs';

test('increments the patch version, including snapshots', () => {
  assert.equal(nextRelease('1.0.1', []), '1.0.2');
  assert.equal(nextRelease('1.0.1-SNAPSHOT', []), '1.0.2');
});

test('advances past existing tags in the same release line', () => {
  assert.equal(nextRelease('1.0.1-SNAPSHOT', ['v1.0.2', 'v1.0.10']), '1.0.11');
});

test('ignores other release lines and prereleases', () => {
  assert.equal(nextRelease('1.1.0', ['v1.0.99', 'v2.0.0', 'v1.1.9-rc1']), '1.1.1');
});

test('does not wrap the patch at nine', () => {
  assert.equal(nextRelease('1.0.9', []), '1.0.10');
});

test('rejects unsupported or ambiguous version formats', () => {
  for (const version of ['1.0', '1.0.1-rc1', '01.0.1', '', '${revision}']) {
    assert.throws(() => nextRelease(version, []));
  }
});
