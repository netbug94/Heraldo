// logger.ts
import winston from 'winston';

export const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp({format: 'YYYY-MM-DD HH:mm:ss'}),
        winston.format.printf(({timestamp, level, message}) => `[${timestamp}] ${level.toUpperCase()}: ${message}`)
    ),
    transports: [
        // Standard Output (captured by Docker logs)
        new winston.transports.Console({
            format: winston.format.colorize({all: true})
        }),
        // Persistent log file
        new winston.transports.File({
            filename: 'heraldo-mensajero.log'
        })
    ],
});