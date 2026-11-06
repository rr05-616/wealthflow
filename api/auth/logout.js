import {json} from '../_lib.js';
export default function handler(request,response){return json(response,{ok:true},200,{'Set-Cookie':'wf_session=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0'});}
