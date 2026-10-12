const fs = require('fs');
const files = [
  'src/main/resources/assets/infinitycraft/lang/zh_cn.json',
  'src/main/resources/assets/infinitycraft/lang/en_us.json'
];
for (const f of files) {
  try { JSON.parse(fs.readFileSync(f, 'utf8')); console.log('OK ' + f); }
  catch (e) { console.log('BAD ' + f + ' ' + e.message); }
}
