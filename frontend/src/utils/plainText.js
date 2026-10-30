/**
 * 将问答内容转换为可直接展示的纯文本，避免模型或用户输入中的 Markdown 标记原样出现。
 */
export function toPlainText(value) {
  if (value == null) return ''
  let text = String(value).replace(/\r\n?/g, '\n')

  // 移除代码围栏，保留其中文字。
  text = text.replace(/^\s*(?:```|~~~)[\w-]*\s*$/gm, '')
  // 移除标题、引用和无序列表标记。
  text = text.replace(/^\s{0,3}#{1,6}\s*/gm, '')
  text = text.replace(/^\s{0,3}>\s?/gm, '')
  text = text.replace(/^\s*[-*+]\s+/gm, '• ')
  text = text.replace(/^\s*([-*_])\1{2,}\s*$/gm, '')
  // 表格分隔行不展示，表格行改为用空格分隔的普通文本。
  text = text.replace(/^\s*\|?(?:\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$/gm, '')
  text = text.replace(/^\s*\|(.+)\|\s*$/gm, (_, row) => row.split('|').map((cell) => cell.trim()).join('  '))
  // 链接保留可读文字和地址，不保留下标语法。
  text = text.replace(/!\[([^\]]*)]\(([^)]+)\)/g, '$1（$2）')
  text = text.replace(/\[([^\]]+)]\(([^)]+)\)/g, '$1（$2）')
  // 移除行内代码、加粗、斜体、删除线等标记。
  text = text.replace(/`([^`\n]+)`/g, '$1')
  text = text.replace(/\*\*([^*\n]+)\*\*/g, '$1')
  text = text.replace(/__([^_\n]+)__/g, '$1')
  text = text.replace(/~~([^~\n]+)~~/g, '$1')
  text = text.replace(/\*([^*\n]+)\*/g, '$1')
  text = text.replace(/_([^_\n]+)_/g, '$1')
  text = text.replace(/\\([*_#])/g, '$1')

  return text.replace(/\n{3,}/g, '\n\n').trim()
}
