import bcrypt from 'bcryptjs';
import {neon} from '@neondatabase/serverless';
const url=process.env.AUTH_DATABASE_URL||process.env.DATABASE_URL;if(!url)throw new Error('Set AUTH_DATABASE_URL');
const sql=neon(url);for(const [email,password,role] of [[process.env.DEMO_LAB_EMAIL,process.env.DEMO_LAB_PASSWORD,'labUser'],[process.env.DEMO_OFFICER_EMAIL,process.env.DEMO_OFFICER_PASSWORD,'billingOfficer']]){if(!email||!password)throw new Error('Set DEMO_LAB_EMAIL/PASSWORD and DEMO_OFFICER_EMAIL/PASSWORD');const hash=await bcrypt.hash(password,12);await sql`INSERT INTO users(email,password_hash,role) VALUES(${email},${hash},${role}) ON CONFLICT(email) DO UPDATE SET password_hash=EXCLUDED.password_hash,role=EXCLUDED.role`;console.log(`Seeded ${role}: ${email}`);}
