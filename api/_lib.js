import { neon } from '@neondatabase/serverless';
import { jwtVerify } from 'jose';

export function db(name='APP') {
  const url = process.env[`${name}_DATABASE_URL`] || process.env.DATABASE_URL;
  if (!url) throw new Error(`Missing ${name}_DATABASE_URL`);
  return neon(url);
}
export function json(res,body,status=200,headers={}) { for(const [key,value] of Object.entries(headers))res.setHeader(key,value);return res.status(status).json(body); }
export async function userFromRequest(request) {
  const cookie=request.headers.cookie?.split(';').map(x=>x.trim()).find(x=>x.startsWith('wf_session='))?.slice(11);
  const token=cookie || request.headers.authorization?.replace(/^Bearer\s+/i,'');
  if(!token) return null;
  try { const {payload}=await jwtVerify(token,new TextEncoder().encode(process.env.JWT_SECRET)); return {id:payload.sub,email:payload.email,role:payload.role}; } catch { return null; }
}
export async function requireUser(request,roles=[]) {
  const user=await userFromRequest(request);
  if(!user) return {error:{error:'UNAUTHENTICATED',status:401}};
  if(roles.length&&!roles.includes(user.role)) return {error:{error:'FORBIDDEN',status:403}};
  return {user};
}
export const corsHeaders={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'Content-Type, Authorization','Access-Control-Allow-Methods':'GET, POST, OPTIONS'};
