#!/usr/bin/env python3
"""
Simple webhook server for testing Grafana alerting.

Start this server, then configure a Grafana webhook contact point
with URL: http://host.docker.internal:9999/webhook

When alerts fire, you'll see the payload logged to the console.

Usage:
    python3 test-webhook-server.py

The server listens on port 9999 by default. Use Ctrl+C to stop.
"""

import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime


class WebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)

        print("\n" + "=" * 60)
        print(f"ALERT RECEIVED at {datetime.now().isoformat()}")
        print("=" * 60)
        print(f"Path: {self.path}")
        print(f"Headers:")
        for header, value in self.headers.items():
            print(f"  {header}: {value}")
        print()

        try:
            payload = json.loads(body)
            print("Payload (formatted):")
            print(json.dumps(payload, indent=2))
        except json.JSONDecodeError:
            print("Raw body:")
            print(body.decode('utf-8', errors='replace'))

        print("=" * 60 + "\n")

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(b'{"status": "ok"}')

    def log_message(self, format, *args):
        # Suppress default logging
        pass


def main():
    port = 9999
    host = '127.0.0.1'  # Bind to localhost only for security
    server = HTTPServer((host, port), WebhookHandler)
    print(f"Webhook test server listening on port {port}")
    print(f"Configure Grafana contact point with: http://host.docker.internal:{port}/webhook")
    print("Press Ctrl+C to stop\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down...")
        server.shutdown()


if __name__ == '__main__':
    main()
