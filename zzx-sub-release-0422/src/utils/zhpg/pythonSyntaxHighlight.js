const LANGUAGE_CONFIGS = {
  python: {
    keywords: [
      'False', 'None', 'True', 'and', 'as', 'assert', 'async', 'await', 'break', 'class', 'continue',
      'def', 'del', 'elif', 'else', 'except', 'finally', 'for', 'from', 'global', 'if', 'import', 'in',
      'is', 'lambda', 'nonlocal', 'not', 'or', 'pass', 'raise', 'return', 'try', 'while', 'with', 'yield',
      'self', 'cls', 'match', 'case'
    ],
    builtins: [
      'print', 'len', 'range', 'int', 'float', 'str', 'list', 'dict', 'set', 'tuple', 'bool', 'bytes',
      'abs', 'min', 'max', 'sum', 'map', 'filter', 'zip', 'enumerate', 'sorted', 'reversed', 'open',
      'isinstance', 'type', 'super', 'hasattr', 'getattr', 'setattr', 'property', 'staticmethod',
      'classmethod', 'Exception', 'ValueError', 'TypeError', 'KeyError', 'IndexError'
    ],
    lineComment: '#',
    decorators: true,
    functionKeyword: 'def',
    classKeyword: 'class'
  },
  cpp: {
    keywords: [
      'alignas', 'alignof', 'auto', 'bool', 'break', 'case', 'catch', 'char', 'class', 'const', 'constexpr',
      'continue', 'decltype', 'default', 'delete', 'do', 'double', 'else', 'enum', 'explicit', 'extern',
      'false', 'float', 'for', 'friend', 'if', 'inline', 'int', 'long', 'namespace', 'new', 'noexcept',
      'nullptr', 'operator', 'private', 'protected', 'public', 'return', 'short', 'signed', 'sizeof',
      'static', 'struct', 'switch', 'template', 'this', 'throw', 'true', 'try', 'typedef', 'typename',
      'using', 'virtual', 'void', 'volatile', 'while'
    ],
    builtins: ['std', 'string', 'vector', 'map', 'set', 'unordered_map', 'cout', 'cin', 'endl', 'size_t'],
    lineComment: '//',
    blockComments: true,
    preprocessors: true,
    classKeyword: 'class',
    extraClassKeywords: ['struct']
  },
  java: {
    keywords: [
      'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
      'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
      'for', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
      'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super',
      'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile',
      'while', 'true', 'false', 'null'
    ],
    builtins: ['String', 'System', 'Integer', 'Double', 'Float', 'Long', 'Boolean', 'Math', 'List', 'Map', 'Set'],
    lineComment: '//',
    blockComments: true,
    decorators: true,
    classKeyword: 'class',
    extraClassKeywords: ['interface', 'enum']
  },
  r: {
    keywords: [
      'if', 'else', 'repeat', 'while', 'function', 'for', 'in', 'next', 'break', 'TRUE', 'FALSE',
      'NULL', 'Inf', 'NaN', 'NA', 'return'
    ],
    builtins: [
      'library', 'require', 'mean', 'median', 'sd', 'sum', 'min', 'max', 'length', 'data.frame',
      'c', 'list', 'matrix', 'print', 'paste', 'read.csv', 'write.csv', 'lm', 'glm', 'apply', 'lapply', 'sapply'
    ],
    lineComment: '#',
    functionKeyword: 'function'
  },
  matlab: {
    keywords: [
      'break', 'case', 'catch', 'classdef', 'continue', 'else', 'elseif', 'end', 'for', 'function',
      'global', 'if', 'otherwise', 'parfor', 'persistent', 'return', 'switch', 'try', 'while', 'true', 'false'
    ],
    builtins: [
      'mean', 'median', 'std', 'sum', 'min', 'max', 'length', 'size', 'zeros', 'ones', 'disp',
      'plot', 'figure', 'table', 'arrayfun', 'cellfun', 'linspace', 'rand', 'sqrt', 'abs'
    ],
    lineComment: '%',
    functionKeyword: 'function',
    classKeyword: 'classdef'
  }
}

const HINT_MAP = new Map([
  ['py', 'python'],
  ['python', 'python'],
  ['cpp', 'cpp'],
  ['c++', 'cpp'],
  ['cxx', 'cpp'],
  ['cc', 'cpp'],
  ['hpp', 'cpp'],
  ['java', 'java'],
  ['r', 'r'],
  ['matlab', 'matlab'],
  ['m', 'matlab']
])

function asSet(items) {
  return new Set(items || [])
}

for (const config of Object.values(LANGUAGE_CONFIGS)) {
  config.keywordSet = asSet(config.keywords)
  config.builtinSet = asSet(config.builtins)
  config.extraClassKeywordSet = asSet(config.extraClassKeywords)
}

export function escapeHtml(text) {
  if (text == null) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function wrapToken(className, text) {
  return `<span class="${className}">${escapeHtml(text)}</span>`
}

function normalizeLanguageHint(languageHint = '') {
  const value = String(languageHint).trim().toLowerCase()
  if (!value) return ''

  const extension = value.match(/\.([a-z0-9+]+)(?:\?|#|$)/)?.[1]
  return HINT_MAP.get(extension || value) || ''
}

function scorePattern(code, patterns) {
  return patterns.reduce((score, [pattern, weight]) => score + (pattern.test(code) ? weight : 0), 0)
}

export function detectCodeLanguage(code, languageHint = '') {
  const hint = normalizeLanguageHint(languageHint)
  if (hint) return hint

  const source = String(code || '')
  const scores = {
    cpp: scorePattern(source, [
      [/^\s*#\s*include\b/m, 6],
      [/\bstd::\w+/, 4],
      [/\busing\s+namespace\s+std\b/, 4],
      [/\btemplate\s*</, 3],
      [/\b(int|void)\s+main\s*\(/, 3],
      [/\b(cout|cin)\s*<</, 2],
      [/\w+::\w+/, 2]
    ]),
    java: scorePattern(source, [
      [/\bpublic\s+class\s+\w+/, 6],
      [/\b(class|interface|enum)\s+\w+\s*(extends|implements|\{)/, 4],
      [/\bSystem\.out\./, 4],
      [/^\s*package\s+[\w.]+;/m, 3],
      [/^\s*import\s+java\./m, 3],
      [/@Override\b/, 2],
      [/\bpublic\s+static\s+void\s+main\s*\(/, 4]
    ]),
    r: scorePattern(source, [
      [/<-/, 5],
      [/\bfunction\s*\([^)]*\)\s*\{/, 4],
      [/\blibrary\s*\(/, 3],
      [/\bdata\.frame\s*\(/, 3],
      [/%>%/, 3],
      [/\breturn\s*\(/, 1]
    ]),
    matlab: scorePattern(source, [
      [/^\s*function\b.*=/m, 6],
      [/^\s*function\s+\w+\s*\(/m, 4],
      [/^\s*%[^\n]*/m, 2],
      [/\bend\s*$/m, 2],
      [/\b(disp|zeros|ones|linspace)\s*\(/, 2],
      [/\bclassdef\s+\w+/, 4]
    ]),
    python: scorePattern(source, [
      [/^\s*def\s+\w+\s*\(/m, 6],
      [/^\s*class\s+\w+[:(]/m, 4],
      [/^\s*from\s+\w+(?:\.\w+)*\s+import\s+/m, 3],
      [/^\s*import\s+\w+/m, 2],
      [/\bself\b/, 1],
      [/("""|''')/, 2]
    ])
  }

  const [language, score] = Object.entries(scores).sort((a, b) => b[1] - a[1])[0]
  return score > 0 ? language : 'python'
}

function readQuotedString(source, start) {
  const quote = source[start]
  if ((quote === '"' || quote === "'") && source.slice(start, start + 3) === quote.repeat(3)) {
    const end = source.indexOf(quote.repeat(3), start + 3)
    return end === -1 ? source.length : end + 3
  }

  let index = start + 1
  while (index < source.length) {
    if (source[index] === '\\') {
      index += 2
      continue
    }
    if (source[index] === quote) return index + 1
    if (source[index] === '\n') return index
    index += 1
  }
  return source.length
}

function splitSourceSegments(code, language) {
  const source = String(code || '')
  const config = LANGUAGE_CONFIGS[language] || LANGUAGE_CONFIGS.python
  const segments = []
  let index = 0
  let codeStart = 0

  const pushCode = (end) => {
    if (end > codeStart) segments.push({ type: 'code', text: source.slice(codeStart, end) })
  }

  const consumeLine = () => {
    const nextLine = source.indexOf('\n', index)
    return nextLine === -1 ? source.length : nextLine
  }

  while (index < source.length) {
    const atLineStart = index === 0 || source[index - 1] === '\n'
    const remainingLine = source.slice(index, consumeLine())
    const trimmedLine = remainingLine.trimStart()

    if (config.preprocessors && atLineStart && trimmedLine.startsWith('#')) {
      const lineStartOffset = remainingLine.length - trimmedLine.length
      const start = index + lineStartOffset
      const end = consumeLine()
      pushCode(start)
      segments.push({ type: 'preprocessor', text: source.slice(start, end) })
      index = end
      codeStart = end
      continue
    }

    if (config.blockComments && source.startsWith('/*', index)) {
      const end = source.indexOf('*/', index + 2)
      pushCode(index)
      const commentEnd = end === -1 ? source.length : end + 2
      segments.push({ type: 'comment', text: source.slice(index, commentEnd) })
      index = commentEnd
      codeStart = commentEnd
      continue
    }

    if (config.lineComment && source.startsWith(config.lineComment, index)) {
      pushCode(index)
      const end = consumeLine()
      segments.push({ type: 'comment', text: source.slice(index, end) })
      index = end
      codeStart = end
      continue
    }

    if (source[index] === '"' || source[index] === "'") {
      pushCode(index)
      const end = readQuotedString(source, index)
      segments.push({ type: 'string', text: source.slice(index, end) })
      index = end
      codeStart = end
      continue
    }

    index += 1
  }

  pushCode(source.length)
  return segments
}

function highlightCodeSegment(text, language) {
  const config = LANGUAGE_CONFIGS[language] || LANGUAGE_CONFIGS.python
  const tokenRe = /@[A-Za-z_][\w.]*|\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\b[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)?\b/g
  let output = ''
  let last = 0
  let expectName = ''
  let match

  while ((match = tokenRe.exec(text)) !== null) {
    const token = match[0]
    output += escapeHtml(text.slice(last, match.index))

    if (config.decorators && token.startsWith('@')) {
      output += wrapToken('tk-decorator', token)
    } else if (/^\d/.test(token)) {
      output += wrapToken('tk-number', token)
    } else if (expectName === 'function') {
      output += wrapToken('tk-function', token)
      expectName = ''
    } else if (expectName === 'class') {
      output += wrapToken('tk-class', token)
      expectName = ''
    } else if (config.keywordSet.has(token)) {
      output += wrapToken('tk-keyword', token)
      if (token === config.functionKeyword) expectName = 'function'
      else if (token === config.classKeyword || config.extraClassKeywordSet.has(token)) expectName = 'class'
      else expectName = ''
    } else if (config.builtinSet.has(token)) {
      output += wrapToken('tk-builtin', token)
    } else {
      output += escapeHtml(token)
    }

    last = match.index + token.length
  }

  output += escapeHtml(text.slice(last))
  return output
}

export function highlightSourceCode(code, languageHint = '') {
  if (code == null || code === '') return ''

  const language = detectCodeLanguage(code, languageHint)
  return splitSourceSegments(code, language)
    .map((segment) => {
      if (segment.type === 'comment') return wrapToken('tk-comment', segment.text)
      if (segment.type === 'string') return wrapToken('tk-string', segment.text)
      if (segment.type === 'preprocessor') return wrapToken('tk-preprocessor', segment.text)
      return highlightCodeSegment(segment.text, language)
    })
    .join('')
}

export function highlightPython(code) {
  return highlightSourceCode(code, 'python')
}
