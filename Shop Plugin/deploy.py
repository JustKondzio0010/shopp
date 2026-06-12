import os
import sys
import subprocess
import socket
import struct
import urllib.request
import urllib.parse
import json
import uuid

# Helper to load properties from local.properties
def load_properties(filepath):
    props = {}
    if not os.path.exists(filepath):
        print(f"Error: {filepath} not found! Please create it.")
        return props
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, val = line.split('=', 1)
                props[key.strip()] = val.strip()
    return props

# Simple pure-Python RCON client
class RconClient:
    def __init__(self, host, port, password):
        self.host = host
        self.port = port
        self.password = password
        self.sock = None

    def connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(10.0)
        try:
            self.sock.connect((self.host, self.port))
        except Exception as e:
            raise Exception(f"Failed to connect to RCON server at {self.host}:{self.port} - {e}")
        
        # Authenticate (type 3: Login)
        self._send(3, self.password)
        
        # Read response
        req_id, packet_type, payload = self._read()
        if req_id == -1:
            raise Exception("RCON Authentication failed! Check your rcon.password in local.properties.")

    def send_command(self, cmd):
        # type 2: Command execution
        self._send(2, cmd)
        req_id, packet_type, payload = self._read()
        return payload

    def _send(self, packet_type, payload):
        req_id = 1234  # arbitrary request ID
        payload_bytes = payload.encode('utf-8')
        packet_len = 4 + 4 + len(payload_bytes) + 2
        packet = struct.pack(f"<iii{len(payload_bytes)}sBB", packet_len, req_id, packet_type, payload_bytes, 0, 0)
        self.sock.sendall(packet)

    def _read(self):
        length_data = self.sock.recv(4)
        if len(length_data) < 4:
            raise Exception("RCON connection closed prematurely")
        packet_len = struct.unpack("<i", length_data)[0]
        
        data = b""
        while len(data) < packet_len:
            chunk = self.sock.recv(packet_len - len(data))
            if not chunk:
                break
            data += chunk
            
        req_id, packet_type = struct.unpack("<ii", data[:8])
        payload = data[8:-2].decode('utf-8', errors='ignore')
        return req_id, packet_type, payload

    def close(self):
        if self.sock:
            self.sock.close()

# Pterodactyl API Operations
def delete_file_via_api(panel_host, server_id, api_key, file_name, target_dir="plugins"):
    print(f"Deleting existing file {file_name} from {target_dir} via Pterodactyl API...")
    url = f"https://{panel_host}/api/client/servers/{server_id}/files/delete"
    
    payload = {
        "root": target_dir,
        "files": [file_name]
    }
    data = json.dumps(payload).encode('utf-8')
    
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Authorization", f"Bearer {api_key}")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    req.add_header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    
    try:
        with urllib.request.urlopen(req) as response:
            status = response.getcode()
            if status in [200, 204]:
                print(f"[SUCCESS] Deleted {file_name} successfully!")
                return True
            else:
                print(f"[WARNING] Delete returned status code: {status}")
    except Exception as e:
        print(f"[WARNING] Failed to delete file (it might not exist yet): {e}")

def upload_file_via_api(panel_host, server_id, api_key, file_path, target_dir="plugins"):
    print(f"Requesting upload URL from Pterodactyl API (https://{panel_host})...")
    url = f"https://{panel_host}/api/client/servers/{server_id}/files/upload"
    if target_dir:
        url += f"?directory={urllib.parse.quote(target_dir)}"
        
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"Bearer {api_key}")
    req.add_header("Accept", "application/json")
    req.add_header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    
    try:
        with urllib.request.urlopen(req) as response:
            res_data = json.loads(response.read().decode('utf-8'))
            upload_url = res_data["attributes"]["url"]
    except Exception as e:
        raise Exception(f"Failed to get upload URL: {e}")
        
    print(f"Uploading file to Node daemon...")
    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    filename = os.path.basename(file_path)
    
    with open(file_path, 'rb') as f:
        file_content = f.read()
        
    part_header = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="files"; filename="{filename}"\r\n'
        f"Content-Type: application/java-archive\r\n\r\n"
    ).encode('utf-8')
    
    part_footer = f"\r\n--{boundary}--\r\n".encode('utf-8')
    body = part_header + file_content + part_footer
    
    upload_req = urllib.request.Request(upload_url, data=body, method="POST")
    upload_req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    upload_req.add_header("Accept", "application/json")
    upload_req.add_header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    
    try:
        with urllib.request.urlopen(upload_req) as response:
            status = response.getcode()
            if status in [200, 204]:
                return True
            else:
                raise Exception(f"HTTP Status {status}")
    except Exception as e:
        raise Exception(f"Failed to upload file content: {e}")

def main():
    properties_file = "local.properties"
    props = load_properties(properties_file)
    
    panel_host = props.get("panel.host")
    server_id = props.get("panel.server_id")
    api_key = props.get("panel.api_key")
    
    rcon_host = props.get("rcon.host")
    rcon_port = props.get("rcon.port")
    rcon_pass = props.get("rcon.password")
    
    if not all([panel_host, server_id, api_key]):
        print("[ERROR] Please configure API settings (panel.host, panel.server_id, panel.api_key) in local.properties.")
        sys.exit(1)
        
    if not all([rcon_host, rcon_port, rcon_pass]) or rcon_pass == "WPISZ_TUTAJ_HASLO_RCON":
        print("[WARNING] RCON credentials not set. Server reload step will be skipped.")
        rcon_enabled = False
    else:
        rcon_enabled = True
        rcon_port = int(rcon_port)

    # 1. Build project
    print("\n--- [1/3] Building Plugin with Gradle ---")
    gradlew = "gradlew.bat" if os.name == 'nt' else "./gradlew"
    
    env = os.environ.copy()
    jdk_path = r"C:\Users\konra\OneDrive\Pulpit\vipshop\..\..\..\.jdks\temurin-25.0.3-1"
    if not os.path.exists(jdk_path):
        jdk_path = r"C:\Users\konra\.jdks\temurin-25.0.3-1"
        
    if os.path.exists(jdk_path):
        env["JAVA_HOME"] = jdk_path
    
    build_process = subprocess.run([gradlew, "jar"], capture_output=False, env=env)
    if build_process.returncode != 0:
        print("[ERROR] Build failed! Check compiler errors above.")
        sys.exit(1)
    print("[SUCCESS] Build complete!")

    # Find jar file
    jar_name = "Shop-1.0.0.jar"
    jar_path = os.path.join("build", "libs", jar_name)
    if not os.path.exists(jar_path):
        libs_dir = os.path.join("build", "libs")
        if os.path.exists(libs_dir):
            jars = [f for f in os.listdir(libs_dir) if f.endswith(".jar")]
            if jars:
                jar_name = jars[0]
                jar_path = os.path.join(libs_dir, jar_name)
                
    if not os.path.exists(jar_path):
        print(f"[ERROR] Jar file not found at {jar_path}")
        sys.exit(1)

    # 2. Upload via Pterodactyl API
    print("\n--- [2/3] Uploading JAR and Config to IceHost via Web API ---")
    try:
        # Delete old JAR first to handle locked open files on server
        delete_file_via_api(panel_host, server_id, api_key, jar_name, "plugins")
        upload_file_via_api(panel_host, server_id, api_key, jar_path, "plugins")
        print("[SUCCESS] Plugin JAR uploaded successfully to /plugins/ folder!")
        
        # Delete old config first
        delete_file_via_api(panel_host, server_id, api_key, "config.yml", "plugins/Shop")
        config_path = os.path.join("src", "main", "resources", "config.yml")
        if os.path.exists(config_path):
            upload_file_via_api(panel_host, server_id, api_key, config_path, "plugins/Shop")
            print("[SUCCESS] config.yml uploaded successfully to /plugins/Shop/ folder!")
        else:
            print("[WARNING] config.yml not found in src/main/resources/, skipping upload.")
    except Exception as e:
        print(f"[ERROR] Upload failed: {e}")
        sys.exit(1)

    # 3. Reload Minecraft Server via RCON
    if rcon_enabled:
        print("\n--- [3/3] Sending Reload Command via RCON ---")
        client = RconClient(rcon_host, rcon_port, rcon_pass)
        try:
            print("Connecting to RCON...")
            client.connect()
            # We will run restart
            command = "restart"
            print(f"Sending command: {command}")
            response = client.send_command(command)
            print("\nServer response:")
            print(response if response.strip() else "(No response text, check server console)")
            print("\n[SUCCESS] Reload complete!")
        except Exception as e:
            print(f"[ERROR] RCON step failed: {e}")
        finally:
            client.close()
    else:
        print("\n--- [3/3] RCON Skipped ---")

if __name__ == "__main__":
    main()
