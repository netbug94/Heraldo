import { Router } from 'express';
import { handleLoginGet, handleLoginPost, handleLogout } from './auth.js';

export const authRouter = Router();

authRouter.get('/login', handleLoginGet);
authRouter.post('/login', handleLoginPost);
authRouter.get('/logout', handleLogout);