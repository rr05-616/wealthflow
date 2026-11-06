import bcrypt from 'bcryptjs';
import { SignJWT } from 'jose';
import { db,json } from '../_lib.js';
export default async function handler(request,response) {
  if(request.method!=='POST') return json(response,{error:'METHOD_NOT_ALLOWED'},405);
  const {email,password}=request.body||{};
  if(!email||!password) return json(response,{error:'VALIDATION_ERROR'},400);
  const sql=db('AUTH');
  const rows=await sql`SELECT id,email,password_hash,role FROM users WHERE lower(email)=lower(${email}) LIMIT 1`;
  const row=rows[0];
  if(!row||!(await bcrypt.compare(password,row.password_hash))) return json(response,{error:'INVALID_CREDENTIALS'},401);
  const secret=new TextEncoder().encode(process.env.JWT_SECRET);
  const token=await new SignJWT({email:row.email,role:row.role}).setProtectedHeader({alg:'HS256'}).setSubject(String(row.id)).setIssuedAt().setExpirationTime('8h').sign(secret);
  return json(response,{user:{email:row.email,role:row.role}},200,{'Set-Cookie':`wf_session=${token}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=28800`});
}
