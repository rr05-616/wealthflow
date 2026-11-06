import {json,userFromRequest} from '../_lib.js';
export default async function handler(request,response){const user=await userFromRequest(request);return user?json(response,{user}):json(response,{error:'UNAUTHENTICATED'},401);}
