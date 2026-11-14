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

    # 5. Native Matrix Core Settings Screen & Developer Socials Footer
    if "MatrixCoreScreen" in content:
        print("[web-patch] MatrixCoreScreen already present in main.js")
    else:
        developer_code = (
            'var DeveloperFooter=()=>A.createElement("div",{'
            'className:"mx-2 mb-1 mt-1 px-3 py-2 flex items-center justify-between border-t border-border/40 text-[11px] text-muted-foreground select-none"'
            '},'
            'A.createElement("div",{className:"flex items-center gap-1.5"},'
            'A.createElement("span",{className:"opacity-70"},"Crafted by"),'
            'A.createElement("span",{className:"font-medium text-foreground/90"},"Pratik Mengal")'
            '),'
            'A.createElement("div",{className:"flex items-center gap-2.5"},'
            'A.createElement("a",{'
            'href:"https://github.com/mengalpratik",'
            'target:"_blank",'
            'rel:"noopener noreferrer",'
            'className:"hover:text-foreground transition-colors cursor-pointer text-muted-foreground no-underline font-medium"'
            '},"GitHub ↗"),'
            'A.createElement("span",{className:"opacity-30"},"•"),'
            'A.createElement("a",{'
            'href:"https://instagram.com/pratikmengal_",'
            'target:"_blank",'
            'rel:"noopener noreferrer",'
            'className:"hover:text-foreground transition-colors cursor-pointer text-muted-foreground no-underline font-medium"'
            '},"Instagram ↗")'
            ')'
            ');'
        )

        matrix_screen_code = (
            developer_code +
            'var MatrixCoreScreen=()=>{'
            'var [rootInfo,setRootInfo]=(0,A.useState)(()=>{'
            'try{var a=!1,p="None",s="/system/bin/su";'
            'if(window.Android&&typeof window.Android.isRootActive==="function")a=window.Android.isRootActive();'
            'if(window.Android&&typeof window.Android.getRootProvider==="function")p=window.Android.getRootProvider()||"None";'
            'if(window.Android&&typeof window.Android.getRootStatusJson==="function"){'
            'var j=JSON.parse(window.Android.getRootStatusJson());if(j.suPath)s=j.suPath;}'
            'return{active:a,provider:p,suPath:s};'
            '}catch(_){return{active:!1,provider:"Unknown",suPath:"/system/bin/su"}'
            '}});'
            'var [verifying,setVerifying]=(0,A.useState)(!1);'
            'var [feedback,setFeedback]=(0,A.useState)("");'
            'var handleVerify=(0,A.useCallback)(()=>{'
            'setVerifying(!0);setFeedback("");'
            'try{if(window.Android&&typeof window.Android.isRootActive==="function"){'
            'var ok=window.Android.isRootActive();var prov="None",su="/system/bin/su";'
            'if(typeof window.Android.getRootProvider==="function")prov=window.Android.getRootProvider()||"None";'
            'if(typeof window.Android.getRootStatusJson==="function"){'
            'var j=JSON.parse(window.Android.getRootStatusJson());if(j.suPath)su=j.suPath;}'
            'setRootInfo({active:ok,provider:prov,suPath:su});'
            'setFeedback(ok?"\\u2713 Root verified active (uid=0 root) via su!":"\\u2717 Root probe failed or permission not granted.");'
            '}else{setFeedback("Notice: Android bridge unavailable in current frame.");}'
            '}catch(e){setFeedback("Error: "+(e.message||e));'
            '}finally{setVerifying(!1);}'
            '},[]);'
            'var prefixPath="/data/user/0/com.agy/files/usr";'
            'try{if(window.location.port==="45158"||(window.__APP_CONFIG__&&window.__APP_CONFIG__.port===45158)){'
            'prefixPath="/data/user/0/com.agy.dev/files/usr";}'
            '}catch(_){}'
            'return A.createElement(G_,{title:"Matrix Core",description:"Native Android tools, elevated root access, and custom plugins hub."},'
            'A.createElement(w3,{title:"Root & System Elevation"},'
            'A.createElement(u3,null,'
            'A.createElement(v3,{label:"Root Privilege (uid=0)",description:rootInfo.active?"Superuser active via KernelSU / Magisk su":"Inactive or not detected",badge:rootInfo.active?"Active":"Inactive",rightElement:A.createElement(tA,{variant:"outline",size:"sm",disabled:verifying,onClick:handleVerify},verifying?"Checking...":"Verify")}),'
            'feedback?A.createElement("div",{className:"px-3 py-2 text-xs font-mono "+(rootInfo.active?"text-green-500":"text-amber-500")},feedback):null,'
            'A.createElement(v3,{label:"Superuser Provider",description:rootInfo.provider}),'
            'A.createElement(v3,{label:"Binary Path",description:rootInfo.suPath}),'
            'A.createElement(v3,{label:"Elevation Channel",description:rootInfo.active?"Direct KernelSU / Magisk su":"None / Shizuku Fallback"})'
            ')),'
            'A.createElement(w3,{title:"AI Background Daemon"},'
            'A.createElement(u3,null,'
            'A.createElement(v3,{label:"Supervisor (agy-daemon)",description:"Foreground Service Partial WakeLock persistence",badge:"Ready"}),'
            'A.createElement(v3,{label:"Resource Footprint",description:"<10MB RAM \\u2022 0.0% CPU Idle"}),'
            'A.createElement(v3,{label:"CLI Management",description:"Run \'agy-daemon task list\' in terminal"})'
            ')),'
            'A.createElement(w3,{title:"Linux Userspace & Environment"},'
            'A.createElement(u3,null,'
            'A.createElement(v3,{label:"Package Manager",description:"Native pkg (Bash & AWK)",badge:"Healthy"}),'
            'A.createElement(v3,{label:"Userspace Prefix ($PREFIX)",description:prefixPath}),'
            'A.createElement(v3,{label:"Workspace Storage Root",description:"/storage/emulated/0/Antigravity"})'
            ')),'
            'A.createElement(w3,{title:"Developer & Socials"},'
            'A.createElement(u3,null,'
            'A.createElement(v3,{label:"Architect & Developer",description:"Pratik Mengal (Neuro)",badge:"Creator"}),'
            'A.createElement(v3,{label:"GitHub Profile",description:"github.com/mengalpratik",rightElement:A.createElement("a",{href:"https://github.com/mengalpratik",target:"_blank",rel:"noopener noreferrer",className:"select-none rounded px-2.5 py-1 text-xs font-medium bg-secondary hover:bg-secondary/80 text-foreground transition-colors no-underline inline-flex items-center gap-1"},"Profile ↗")}),'
            'A.createElement(v3,{label:"Instagram",description:"@pratikmengal_",rightElement:A.createElement("a",{href:"https://instagram.com/pratikmengal_",target:"_blank",rel:"noopener noreferrer",className:"select-none rounded px-2.5 py-1 text-xs font-medium bg-secondary hover:bg-secondary/80 text-foreground transition-colors no-underline inline-flex items-center gap-1"},"Follow ↗")})'
            ')),'
            'A.createElement(w3,{title:"Custom Plugins & Future Tools"},'
            'A.createElement(u3,null,'
            'A.createElement(v3,{label:"Custom Tools Hub",description:"All your upcoming tools, custom scripts, and extensions will appear here."})'
            '))'
            ');'
            '};'
        )

        fn_target = 'function fNb(a){switch(a){'
        if fn_target in content:
            content = content.replace(fn_target, matrix_screen_code + fn_target, 1)

            route_target = 'case "Provide Feedback":return A.createElement(H_,null);'
            route_replace = 'case "Provide Feedback":return A.createElement(H_,null);case "Matrix Core":return A.createElement(MatrixCoreScreen,null);'
            content = content.replace(route_target, route_replace, 1)

            jnb_target = 'b=[...e.filter(q=>q!=="Account"),...f,...(c?[]:["Shortcuts"]),...(b.hideProvideFeedback?[]:["Provide Feedback"])];'
            jnb_replace = 'b=[...e.filter(q=>q!=="Account"),...f,...(c?[]:["Shortcuts"]),...(b.hideProvideFeedback?[]:["Provide Feedback"]),"Matrix Core"];'
            content = content.replace(jnb_target, jnb_replace, 1)

            enb_target = 'h=(0,A.useMemo)(()=>f.filter(k=>k!=="Provide Feedback"),[f]);'
            enb_replace = 'h=(0,A.useMemo)(()=>[...f.filter(k=>k!=="Provide Feedback"),"Matrix Core"],[f]);'
            content = content.replace(enb_target, enb_replace, 1)

            desk_target = 'A.createElement("div",{style:{display:b==="Provide Feedback"?"block":"none"}},A.createElement(H_,null))'
            desk_replace = 'A.createElement("div",{style:{display:b==="Provide Feedback"?"block":"none"}},A.createElement(H_,null)),A.createElement("div",{style:{display:b==="Matrix Core"?"block":"none"}},A.createElement(MatrixCoreScreen,null))'
            content = content.replace(desk_target, desk_replace, 1)

            gtb_target = 'ProvideFeedback:"Provide Feedback"'
            gtb_replace = 'ProvideFeedback:"Provide Feedback",MatrixCore:"Matrix Core"'
            content = content.replace(gtb_target, gtb_replace, 1)

            # Footer with Developer Socials (Mobile & Desktop)
            f_mobile_target = 'footer:e.includes("Account")?A.createElement("div",{className:"flex flex-col border-t border-border p-1"},A.createElement(ZJb,{showChevron:!0,className:"mx-1 px-3 py-2.5",onClick:()=>{a(qX("Account"));k.push("Account")}})):void 0'
            f_mobile_replace = 'footer:e.includes("Account")?A.createElement("div",{className:"flex flex-col border-t border-border p-1"},A.createElement(ZJb,{showChevron:!0,className:"mx-1 px-3 py-2.5",onClick:()=>{a(qX("Account"));k.push("Account")}}),A.createElement(DeveloperFooter,null)):A.createElement(DeveloperFooter,null)'
            content = content.replace(f_mobile_target, f_mobile_replace, 1)

            f_desk_target = 'C&&A.createElement("div",{className:"flex flex-col border-t border-solid border-sidebar-border py-2"},A.createElement(ZJb,{isActive:E&&c==="Account",onClick:()=>{D(qX("Account"))}}))'
            f_desk_replace = 'C&&A.createElement("div",{className:"flex flex-col border-t border-solid border-sidebar-border py-2"},A.createElement(ZJb,{isActive:E&&c==="Account",onClick:()=>{D(qX("Account"))}}),A.createElement(DeveloperFooter,null))'
            content = content.replace(f_desk_target, f_desk_replace, 1)

            modified = True
            print("[web-patch] Successfully patched Native Matrix Core Screen and Developer Socials Footer into main.js")
        else:
            print("[web-patch] Warning: fNb function target not found in main.js", file=sys.stderr)



    if modified:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

    return True

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "main.js")
    if not patch_main(path):
        sys.exit(1)
