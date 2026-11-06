import {readFile} from 'node:fs/promises';
import {neon} from '@neondatabase/serverless';
const ddl=(await readFile(new URL('../db/schema.sql',import.meta.url),'utf8')).split(';').map(x=>x.trim()).filter(Boolean);
for(const name of ['AUTH','USAGE','RATE','CHARGEBACK','WORKFLOW']){const url=process.env[`${name}_DATABASE_URL`]||process.env.DATABASE_URL;if(!url)throw new Error(`Set ${name}_DATABASE_URL (or DATABASE_URL for local setup)`);const sql=neon(url);for(const statement of ddl)await sql.query(statement);console.log(`${name} schema is ready`);}
