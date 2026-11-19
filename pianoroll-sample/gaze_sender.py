"""gaze_sender.py
Tobii Eye Tracker 5 または マウスカーソルから正規化座標 (0.0〜1.0) と
有効フラグ (1 or 0) を取得し、UDP (127.0.0.1:5005) へ送信するスクリプト。

【送信仕様】
- 送信先: 127.0.0.1:5005 (UDP)
- フォーマット: "x,y,valid" (例: "0.5234,0.4120,1")
  * x, y: 0.0〜1.0 の正規化座標 (左上が 0.0, 0.0、右下が 1.0, 1.0)
  * valid: 1 (有効) または 0 (無効/トラッキング喪失)
- 送信レート: 約90Hz
"""

import sys
import os
import time
import socket
import math
import argparse
import shutil
from pathlib import Path
import ctypes
from ctypes import (
    c_void_p,
    c_char_p,
    c_int,
    c_int64,
    c_float,
    POINTER,
    byref,
    CFUNCTYPE,
)

# ================= 設定項目 =================
USE_MOCK_MOUSE = True      # True: Tobii無しでマウス座標を使用 / False: Tobii Eye Tracker 5 を使用
UDP_IP = "127.0.0.1"
UDP_PORT = 5005
TARGET_HZ = 90             # 送信レート (~90Hz)
DLL_NAME = "tobii_stream_engine.dll"
# ============================================

PROJECT_ROOT = Path(__file__).resolve().parent

# DLLの典型的な探索候補パス
DLL_SEARCH_PATHS = [
    Path(r"C:\Program Files\Tobii"),
    Path(r"C:\Program Files (x86)\Tobii"),
    Path(os.environ.get("LOCALAPPDATA", "")) / "Tobii",
    Path(os.environ.get("APPDATA", "")) / "Tobii",
]


# ---------------- マウス座標取得 (Win32 API) ----------------
class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]


def get_mouse_normalized():
    """Windows API を使用してマウス座標を取得し、0.0〜1.0 に正規化して返す"""
    pt = POINT()
    ctypes.windll.user32.GetCursorPos(byref(pt))
    screen_w = ctypes.windll.user32.GetSystemMetrics(0)  # SM_CXSCREEN
    screen_h = ctypes.windll.user32.GetSystemMetrics(1)  # SM_CYSCREEN

    if screen_w <= 1 or screen_h <= 1:
        return 0.5, 0.5, 1

    norm_x = max(0.0, min(1.0, pt.x / float(screen_w - 1)))
    norm_y = max(0.0, min(1.0, pt.y / float(screen_h - 1)))
    return norm_x, norm_y, 1


# ---------------- Tobii Stream Engine DLL 関連 ----------------
def is_64bit_pe(file_path: Path) -> bool:
    """DLL が 64-bit (x64) かどうかをPEヘッダから確認"""
    try:
        with open(file_path, "rb") as f:
            f.seek(0x3C)
            pe_offset = int.from_bytes(f.read(4), "little")
            f.seek(pe_offset + 4)
            machine = int.from_bytes(f.read(2), "little")
            return machine == 0x8664  # IMAGE_FILE_MACHINE_AMD64
    except Exception:
        return False


def find_and_copy_dll() -> Path:
    r"""
    プロジェクトルートに DLL が存在しなければ PC内 (C:\Program Files\Tobii 等) を探索し、
    見つかった場合はプロジェクトルートへコピーする。
    """
    target_path = PROJECT_ROOT / DLL_NAME
    if target_path.exists():
        return target_path

    print(f"[*] {DLL_NAME} がプロジェクトルートに見つかりません。PC内を探索します...")
    for base_dir in DLL_SEARCH_PATHS:
        if not base_dir.exists():
            continue
        try:
            for candidate in base_dir.rglob(DLL_NAME):
                if candidate.is_file() and is_64bit_pe(candidate):
                    print(f"[+] 互換性のある DLL を発見: {candidate}")
                    print(f"[*] プロジェクトルートへコピーします -> {target_path}")
                    shutil.copy2(candidate, target_path)
                    return target_path
        except Exception as e:
            print(f"[!] 探索中にエラー: {e}")

    return target_path


def print_nuget_instructions():
    """NuGet からの tobii_stream_engine.dll 取得手順を出力"""
    print("=" * 70)
    print("【tobii_stream_engine.dll の取得方法 (NuGet)】")
    print("PC内に DLL が見つからない場合、NuGet パッケージから抽出できます。")
    print("\n[方法 1: PowerShell でワンライナーダウンロード & 展開]")
    print(r'  Invoke-WebRequest -Uri "https://api.nuget.org/v3-flatcontainer/tobii.streamengine.native/2.2.2.363/tobii.streamengine.native.2.2.2.363.nupkg" -OutFile "$env:TEMP\tobii.zip"')
    print(r'  tar -xf "$env:TEMP\tobii.zip" -C . "build/native/lib/x64/tobii_stream_engine.dll"')
    print(r'  Move-Item -Path "build\native\lib\x64\tobii_stream_engine.dll" -Destination ".\tobii_stream_engine.dll" -Force')
    print(r'  Remove-Item -Recurse -Force "build"')
    print("\n[方法 2: NuGet CLI / nuget.exe を使用]")
    print(r'  nuget install Tobii.StreamEngine.Native -Version 2.2.2.363')
    print("=" * 70)


# Tobii 構造体定義
class TobiiGazePoint(ctypes.Structure):
    _fields_ = [
        ("timestamp_us", c_int64),
        ("validity", c_int),       # 0: TOBII_VALIDITY_INVALID, 1: TOBII_VALIDITY_VALID
        ("position_xy", c_float * 2),
    ]


# コールバック型定義
GazePointCallback = CFUNCTYPE(None, POINTER(TobiiGazePoint), c_void_p)
DeviceUrlCallback = CFUNCTYPE(None, c_char_p, c_void_p)


def setup_tobii_lib(dll_path: Path):
    """tobii_stream_engine.dll を ctypes で読み込み関数シグネチャを設定"""
    if not dll_path.exists():
        print(f"\n[エラー] DLL が見つかりません: {dll_path}")
        print_nuget_instructions()
        raise FileNotFoundError(f"{dll_path} not found")

    lib = ctypes.CDLL(str(dll_path))

    # tobii_error_message
    lib.tobii_error_message.argtypes = [c_int]
    lib.tobii_error_message.restype = c_char_p

    # tobii_api_create / tobii_api_destroy
    lib.tobii_api_create.argtypes = [POINTER(c_void_p), c_void_p, c_void_p]
    lib.tobii_api_create.restype = c_int

    lib.tobii_api_destroy.argtypes = [c_void_p]
    lib.tobii_api_destroy.restype = c_int

    # tobii_enumerate_local_device_urls
    lib.tobii_enumerate_local_device_urls.argtypes = [c_void_p, DeviceUrlCallback, c_void_p]
    lib.tobii_enumerate_local_device_urls.restype = c_int

    # tobii_device_create / tobii_device_destroy
    lib.tobii_device_create.argtypes = [c_void_p, c_char_p, POINTER(c_void_p)]
    lib.tobii_device_create.restype = c_int

    lib.tobii_device_destroy.argtypes = [c_void_p]
    lib.tobii_device_destroy.restype = c_int

    # tobii_gaze_point_subscribe / tobii_gaze_point_unsubscribe
    lib.tobii_gaze_point_subscribe.argtypes = [c_void_p, GazePointCallback, c_void_p]
    lib.tobii_gaze_point_subscribe.restype = c_int

    lib.tobii_gaze_point_unsubscribe.argtypes = [c_void_p]
    lib.tobii_gaze_point_unsubscribe.restype = c_int

    # tobii_wait_for_callbacks / tobii_device_process_callbacks
    lib.tobii_wait_for_callbacks.argtypes = [c_void_p, c_int, POINTER(c_void_p)]
    lib.tobii_wait_for_callbacks.restype = c_int

    lib.tobii_device_process_callbacks.argtypes = [c_void_p]
    lib.tobii_device_process_callbacks.restype = c_int

    return lib


def check_tobii_error(lib, error_code: int, action_name: str = ""):
    """Tobii API の返り値が 0 (TOBII_ERROR_NO_ERROR) 以外の場合に例外を送出"""
    if error_code != 0:
        msg = lib.tobii_error_message(error_code).decode("utf-8", errors="ignore")
        raise RuntimeError(f"Tobii API エラー ({action_name}): [{error_code}] {msg}")


# ---------------- 実行モード ----------------
def run_mock_mouse(sock: socket.socket, target_ip: str, target_port: int, target_hz: float):
    """マウス座標をテスト送信するモード"""
    print("=" * 60)
    print("【モックマウスモード (テスト送信)】")
    print(f"  送信先アドレス : {target_ip}:{target_port}")
    print(f"  目標周波数     : ~{target_hz:.0f} Hz")
    print("  ※画面上のマウスカーソル位置を (0.0〜1.0) に正規化して送信します。")
    print("  ※終了するには [Ctrl + C] を押してください。")
    print("=" * 60 + "\n")

    interval = 1.0 / target_hz
    next_time = time.perf_counter()
    send_count = 0
    last_log_time = time.time()

    try:
        while True:
            x, y, valid = get_mouse_normalized()
            payload = f"{x:.4f},{y:.4f},{valid}".encode("ascii")
            sock.sendto(payload, (target_ip, target_port))
            send_count += 1

            # 周期制御 (~90Hz)
            next_time += interval
            sleep_sec = next_time - time.perf_counter()
            if sleep_sec > 0:
                time.sleep(sleep_sec)
            else:
                next_time = time.perf_counter()

            now = time.time()
            if now - last_log_time >= 3.0:
                actual_hz = send_count / (now - last_log_time)
                print(f"[Mock] 送信中: {actual_hz:.1f} Hz | 最新値: \"{x:.4f},{y:.4f},{valid}\"")
                send_count = 0
                last_log_time = now

    except KeyboardInterrupt:
        print("\n[*] モックマウスモードを終了しました。")


def run_tobii_tracker(sock: socket.socket, target_ip: str, target_port: int, dll_path: Path):
    """Tobii Eye Tracker 5 からの視線座標を送信するモード"""
    print("=" * 60)
    print("【Tobii Eye Tracker 5 モード】")
    print(f"  DLLパス        : {dll_path}")
    print(f"  送信先アドレス : {target_ip}:{target_port}")
    print("=" * 60)

    lib = setup_tobii_lib(dll_path)

    api = c_void_p()
    err = lib.tobii_api_create(byref(api), None, None)
    check_tobii_error(lib, err, "tobii_api_create")

    # デバイスの検出
    discovered_urls = []

    def on_device_url(url, user_data):
        if url:
            discovered_urls.append(url)

    c_url_cb = DeviceUrlCallback(on_device_url)
    err = lib.tobii_enumerate_local_device_urls(api, c_url_cb, None)
    check_tobii_error(lib, err, "tobii_enumerate_local_device_urls")

    if not discovered_urls:
        lib.tobii_api_destroy(api)
        print("\n[!] 接続されている Tobii デバイスが見つかりませんでした。")
        print("    1. Tobii Eye Tracker 5 が USB ポートにしっかり接続されているか確認してください。")
        print("    2. Windows サービスで 'Tobii Service' が実行中であることを確認してください。")
        print("    3. Tobii なしでテストしたい場合は、スクリプト先頭の USE_MOCK_MOUSE = True にするか、")
        print("       'python gaze_sender.py --mock' を実行してください。\n")
        return

    device_url = discovered_urls[0]
    dev_str = device_url.decode("utf-8", errors="ignore")
    print(f"[+] デバイスを発見しました: {dev_str}")

    device = c_void_p()
    err = lib.tobii_device_create(api, device_url, byref(device))
    check_tobii_error(lib, err, "tobii_device_create")

    send_count = 0
    last_log_time = time.time()
    last_payload = ""

    # 視線コールバック
    def on_gaze_point(gaze_ptr, user_data):
        nonlocal send_count, last_payload
        if not gaze_ptr:
            return
        gaze = gaze_ptr.contents
        valid = 1 if gaze.validity == 1 else 0
        x = gaze.position_xy[0]
        y = gaze.position_xy[1]

        # NaN や Inf を防御
        if math.isnan(x) or math.isinf(x):
            x = 0.0
        if math.isnan(y) or math.isinf(y):
            y = 0.0

        payload_str = f"{x:.4f},{y:.4f},{valid}"
        sock.sendto(payload_str.encode("ascii"), (target_ip, target_port))
        send_count += 1
        last_payload = payload_str

    c_gaze_cb = GazePointCallback(on_gaze_point)
    err = lib.tobii_gaze_point_subscribe(device, c_gaze_cb, None)
    check_tobii_error(lib, err, "tobii_gaze_point_subscribe")

    print(f"[*] 視線ストリーミングを開始しました (~90Hz) -> UDP {target_ip}:{target_port}")
    print("[*] 終了するには [Ctrl + C] を押してください\n")

    try:
        while True:
            # 新しいデータパケットが到着するまで待機 (タイムアウトは 6: TOBII_ERROR_TIMED_OUT)
            err = lib.tobii_wait_for_callbacks(None, 1, byref(device))
            if err == 0 or err == 6:
                # コールバック関数を実行してデータ受信
                lib.tobii_device_process_callbacks(device)
            elif err == 5:  # TOBII_ERROR_CONNECTION_FAILED
                print("[!] Tobii との接続が切断されました。再接続を試みます...")
                time.sleep(1.0)
                if hasattr(lib, "tobii_device_reconnect"):
                    lib.tobii_device_reconnect(device)

            now = time.time()
            if now - last_log_time >= 3.0:
                actual_hz = send_count / (now - last_log_time)
                print(f"[Tobii] 送信中: {actual_hz:.1f} Hz | 最新値: \"{last_payload}\"")
                send_count = 0
                last_log_time = now

    except KeyboardInterrupt:
        print("\n[*] 視線ストリーミングを停止中...")
    finally:
        lib.tobii_gaze_point_unsubscribe(device)
        lib.tobii_device_destroy(device)
        lib.tobii_api_destroy(api)
        print("[*] Tobii リソースを安全に解放しました。")


def main():
    parser = argparse.ArgumentParser(description="Tobii / Mock Mouse Gaze UDP Sender")
    parser.add_argument("--mock", action="store_true", help="モックマウスモードで強制起動")
    parser.add_argument("--tobii", action="store_true", help="Tobiiモードで強制起動")
    parser.add_argument("--ip", type=str, default=UDP_IP, help=f"送信先IPアドレス (デフォルト: {UDP_IP})")
    parser.add_argument("--port", type=int, default=UDP_PORT, help=f"送信先UDPポート (デフォルト: {UDP_PORT})")
    parser.add_argument("--hz", type=float, default=TARGET_HZ, help=f"送信周波数 (デフォルト: {TARGET_HZ}Hz)")
    args = parser.parse_args()

    # モード決定: コマンドライン引数が優先、指定がなければ USE_MOCK_MOUSE 定数に従う
    if args.mock:
        use_mock = True
    elif args.tobii:
        use_mock = False
    else:
        use_mock = USE_MOCK_MOUSE

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    if use_mock:
        run_mock_mouse(sock, args.ip, args.port, args.hz)
    else:
        dll_path = find_and_copy_dll()
        run_tobii_tracker(sock, args.ip, args.port, dll_path)


if __name__ == "__main__":
    main()
