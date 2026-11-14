#!/usr/bin/env python3
"""
web/patch_main.py - Dynamic patcher for web/main.js.

Locates the Appearance settings block and injects the Desktop Mode toggle
if not already present. Safe, idempotent, and resilient to upstream changes.
"""

import sys
import os
import re

def patch_main(file_path):
    if not os.path.isfile(file_path):
        print(f"[web-patch] file not found: {file_path}", file=sys.stderr)
        return False

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    modified = False

    # 1 & 2. Appearance settings: inject Desktop Mode toggle if not present
    if "Desktop Mode" in content or "ag_desktop_mode" in content:
        print("[web-patch] Desktop Mode toggle already present in main.js")
    else:
        hook_pattern = re.compile(r'(var\s+[A-Za-z0-9_$]+=[A-Za-z0-9_$]+!==3,\s*[A-Za-z0-9_$]+=[A-Za-z0-9_$]+!==2,\s*[A-Za-z0-9_$]+=[A-Za-z0-9_$]+=>\s*[A-Za-z0-9_$]+=>\{.*?\};)')
        hook_match = hook_pattern.search(content)

        theme_idx = content.find('{label:"Theme"')
        theme_match = None
        react_id = "A"
        if theme_idx != -1:
            window_start = max(0, theme_idx - 100)
            window_end = min(len(content), theme_idx + 500)
            window_text = content[window_start:window_end]
            theme_pattern = re.compile(
                r'([A-Za-z0-9_$]+)\.createElement\(([A-Za-z0-9_$]+),\{label:"Theme",.*?onSelect:[A-Za-z0-9_$]+\}\)'
            )
            theme_match = theme_pattern.search(window_text)
            if theme_match:
                react_id = theme_match.group(1)

        if not hook_match or not theme_match:
            print(f"[web-patch] patterns not matched: hook={bool(hook_match)}, theme={bool(theme_match)}", file=sys.stderr)
            return False

        orig_hook = hook_match.group(1)
        injected_hook = (
            f"var [isDesktop,setIsDesktop]=(0,{react_id}.useState)(()=>{{"
            "try{if(window.Android&&typeof window.Android.isDesktopMode==='function')return window.Android.isDesktopMode();"
            "var s=localStorage.getItem('ag_desktop_mode');return s===null?!1:s==='true'}catch(_){return !1}});"
            f"var onDesktopToggle=(0,{react_id}.useCallback)(v=>{{"
            "setIsDesktop(v);try{localStorage.setItem('ag_desktop_mode',String(v));"
            "if(window.Android&&typeof window.Android.setDesktopMode==='function')window.Android.setDesktopMode(v)}catch(_){}},[]);"
            + orig_hook
        )

        item_comp = "v3"
        switch_comp = "dX"
        re_idx = content.find('rightElement:')
        if re_idx != -1:
            re_window = content[max(0, re_idx - 150):min(len(content), re_idx + 150)]
            item_m = re.search(r'([A-Za-z0-9_$]+)\.createElement\(([A-Za-z0-9_$]+),\{label:[^,]+,description:[^,]+,rightElement:', re_window)
            if item_m:
                item_comp = item_m.group(2)
            sw_m = re.search(r'rightElement:[A-Za-z0-9_$]+\.createElement\(([A-Za-z0-9_$]+),\{checked:', re_window)
            if sw_m:
                switch_comp = sw_m.group(1)

        orig_theme = theme_match.group(0)
        injected_theme = (
            orig_theme
            + f',{react_id}.createElement({item_comp},{{label:"Desktop Mode",description:"Use desktop viewport and user agent for the workspace",'
            + f'rightElement:{react_id}.createElement({switch_comp},{{checked:isDesktop,onCheckedChange:onDesktopToggle}})}})'
        )

        content = content.replace(orig_hook, injected_hook, 1)
        content = content.replace(orig_theme, injected_theme, 1)
        modified = True
        print("[web-patch] Successfully patched Desktop Mode toggle into main.js (mobile by default, English)")

    # 3. Interactive Folder Picker default path: default to /storage/emulated/0/Antigravity/
    picker_target = 'initialPath:b?b.fsPath:ch?"C:/":"/"'
    if picker_target in content:
        content = content.replace(
            picker_target,
            'initialPath:(window.__APP_CONFIG__&&window.__APP_CONFIG__.workspaceRoot?window.__APP_CONFIG__.workspaceRoot:"/storage/emulated/0/Antigravity/")',
            1
        )
        modified = True
        print("[web-patch] Successfully patched folder picker initialPath to default to /storage/emulated/0/Antigravity/")
    elif 'window.__APP_CONFIG__.workspaceRoot' in content:
        print("[web-patch] Folder picker initialPath already patched")
    else:
        print("[web-patch] Note: picker_target string not found in file")

    # 4. Default security preset: default to unrestricted_unix (Turbo) on Linux/Android
    preset_target = 'linux:{presets:["default_unix","full_machine_unix","unrestricted_unix"],defaultPreset:"default_unix"}'
    preset_replace = 'linux:{presets:["default_unix","full_machine_unix","unrestricted_unix"],defaultPreset:"unrestricted_unix"}'
    if preset_target in content:
        content = content.replace(preset_target, preset_replace, 1)
        modified = True
        print("[web-patch] Successfully patched Linux/Android default preset to unrestricted_unix (Turbo)")
    elif preset_replace in content:
        print("[web-patch] Linux default preset already patched to unrestricted_unix")
    else:
        print("[web-patch] Note: preset_target string not found in file")

    if modified:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

    return True

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "main.js")
    if not patch_main(path):
        sys.exit(1)
