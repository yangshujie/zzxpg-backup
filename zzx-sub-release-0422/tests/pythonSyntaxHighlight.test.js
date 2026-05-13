import test from 'node:test'
import assert from 'node:assert/strict'

import { detectCodeLanguage, highlightPython, highlightSourceCode } from '../src/utils/zhpg/pythonSyntaxHighlight.js'

test('highlightPython escapes html and marks python tokens', () => {
  const html = highlightPython('def run(x):\n    # guard\n    return "<tag>"')

  assert.match(html, /<span class="tk-keyword">def<\/span>/)
  assert.match(html, /<span class="tk-function">run<\/span>/)
  assert.match(html, /<span class="tk-comment"># guard<\/span>/)
  assert.match(html, /<span class="tk-keyword">return<\/span>/)
  assert.match(html, /<span class="tk-string">&quot;&lt;tag&gt;&quot;<\/span>/)
  assert.doesNotMatch(html, /"<tag>"/)
})

test('highlightSourceCode auto-detects c++ and highlights preprocessor, comments, and keywords', () => {
  const html = highlightSourceCode('#include <vector>\nint main() { return 0; } // ok')

  assert.equal(detectCodeLanguage('#include <vector>\nint main() { return 0; }'), 'cpp')
  assert.match(html, /<span class="tk-preprocessor">#include &lt;vector&gt;<\/span>/)
  assert.match(html, /<span class="tk-keyword">int<\/span>/)
  assert.match(html, /<span class="tk-keyword">return<\/span>/)
  assert.match(html, /<span class="tk-comment">\/\/ ok<\/span>/)
})

test('highlightSourceCode auto-detects java', () => {
  const html = highlightSourceCode('public class Demo { public static void main(String[] args) { return; } }')

  assert.equal(detectCodeLanguage('public class Demo { public static void main(String[] args) { return; } }'), 'java')
  assert.match(html, /<span class="tk-keyword">public<\/span>/)
  assert.match(html, /<span class="tk-class">Demo<\/span>/)
  assert.match(html, /<span class="tk-keyword">static<\/span>/)
})

test('highlightSourceCode auto-detects R', () => {
  const html = highlightSourceCode('score <- function(x) {\n  return(mean(x))\n}')

  assert.equal(detectCodeLanguage('score <- function(x) {\n  return(mean(x))\n}'), 'r')
  assert.match(html, /<span class="tk-keyword">function<\/span>/)
  assert.match(html, /<span class="tk-keyword">return<\/span>/)
  assert.match(html, /<span class="tk-builtin">mean<\/span>/)
})

test('highlightSourceCode auto-detects matlab', () => {
  const html = highlightSourceCode('function y = score(x)\n% normalize\ny = mean(x);\nend')

  assert.equal(detectCodeLanguage('function y = score(x)\n% normalize\ny = mean(x);\nend'), 'matlab')
  assert.match(html, /<span class="tk-keyword">function<\/span>/)
  assert.match(html, /<span class="tk-comment">% normalize<\/span>/)
  assert.match(html, /<span class="tk-builtin">mean<\/span>/)
  assert.match(html, /<span class="tk-keyword">end<\/span>/)
})
