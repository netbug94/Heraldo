// src/features/dashboard/routes.ts
import { Router } from 'express';
import { requireAuth } from '../auth/auth.js';
import { getDashboardHtml } from './dashboard.js';

export const dashboardRouter = Router();

dashboardRouter.get('/', requireAuth, (_req, res) => {
    res.send(getDashboardHtml());
});