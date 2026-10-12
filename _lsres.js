const fs = require('fs');
const path = require('path');
const root = '资源文件/模型资源';
function walk(d, base) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const full = path.join(d, e.name);
    if (e.isDirectory()) { console.log('[DIR] ' + full.substring(base.length + 1)); walk(full, base); }
    else {
      const rel = full.substring(base.length + 1);
      let info = '';
      const lower = e.name.toLowerCase();
      if (lower.endsWith('.png')) {
        const buf = fs.readFileSync(full);
        const isPng = buf.slice(0, 8).toString('hex') === '89504e470d0a1a0a';
        let w = -1, h = -1;
        if (isPng && buf.slice(12, 16).toString('ascii') === 'IHDR') { w = buf.readUInt32BE(16); h = buf.readUInt32BE(20); }
        info = '  [' + w + 'x' + h + ' png=' + isPng + ']';
      } else if (lower.endsWith('.json')) {
        let head = '';
        try {
          const m = JSON.parse(fs.readFileSync(full, 'utf8'));
          const keys = Object.keys(m);
          head = '  {keys:' + keys.slice(0, 8).join(',') + (keys.length > 8 ? ',...' : '') + '}';
          if (m.texture_size) head += ' texSize=' + JSON.stringify(m.texture_size);
          if (m.elements) head += ' elements=' + m.elements.length;
          if (m['format_version']) head += ' fmt=' + m['format_version'];
        } catch (err) { head = '  (json parse fail: ' + err.message + ')'; }
        info = head;
      } else if (lower.endsWith('.mcmeta')) {
        info = '  <' + fs.readFileSync(full, 'utf8').replace(/\s+/g, ' ') + '>';
      }
      console.log(rel + '  (' + fs.statSync(full).size + ' bytes)' + info);
    }
  }
}
walk(root, root);
