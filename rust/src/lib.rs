use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jstring, jobjectArray, jboolean, jint};
use std::fs;
use std::path::Path;
use std::os::unix::fs::PermissionsExt;
use std::process::Command;
use std::time::UNIX_EPOCH;

macro_rules! make_java_string {
    ($env:expr, $s:expr) => {
        $env.new_string($s)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut())
    };
}

fn run_as_root(cmd: &str) -> (String, bool) {
    let output = Command::new("su")
        .arg("-c")
        .arg(cmd)
        .output();

    match output {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).to_string();
            let stderr = String::from_utf8_lossy(&out.stderr).to_string();
            let result = if stdout.is_empty() { stderr } else { stdout };
            (result, out.status.success())
        }
        Err(e) => (format!("Failed to execute: {}", e), false),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_listDirectory(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jobjectArray {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let entries = if use_root != 0 {
        let cmd = format!("ls -la \"{}\"", path_str);
        let (output, success) = run_as_root(&cmd);
        if !success {
            return std::ptr::null_mut();
        }
        parse_ls_output(&output)
    } else {
        match fs::read_dir(&*path_str) {
            Ok(dir) => {
                let mut result = Vec::new();
                for entry in dir.flatten() {
                    let name = entry.file_name().to_string_lossy().to_string();
                    if name == "." || name == ".." {
                        continue;
                    }
                    let is_dir = entry.path().is_dir();
                    let size = entry.metadata().map(|m| m.len()).unwrap_or(0);
                    let perms = entry.metadata()
                        .map(|m| format!("{:o}", m.permissions().mode() & 0o777))
                        .unwrap_or_else(|_| "644".to_string());
                    let modified = entry.metadata()
                        .ok()
                        .and_then(|m| m.modified().ok())
                        .map(|t| {
                            let secs = t.duration_since(UNIX_EPOCH)
                                .map(|d| d.as_secs())
                                .unwrap_or(0);
                            format!("{}", secs)
                        })
                        .unwrap_or_else(|| "0".to_string());
                    result.push(format!("{}|{}|{}|{}|{}", name, is_dir, size, perms, modified));
                }
                result
            }
            Err(_) => return std::ptr::null_mut(),
        }
    };

    let jentries: Vec<JString> = entries.iter().filter_map(|e| {
        env.new_string(e).ok()
    }).collect();

    let array = match env.new_object_array(
        jentries.len() as jint,
        "java/lang/String",
        JObject::null(),
    ) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    for (i, entry) in jentries.iter().enumerate() {
        let _ = env.set_object_array_element(&array, i as jint, entry);
    }

    array.into_raw()
}

fn parse_ls_output(output: &str) -> Vec<String> {
    let mut result = Vec::new();
    for line in output.lines().skip(1) {
        let parts: Vec<&str> = line.split_whitespace().collect();
        if parts.len() < 9 {
            continue;
        }
        let perms = parts.get(0).unwrap_or(&"");
        let size_str = parts.get(4).unwrap_or(&"0");
        let name = parts[8..].join(" ");
        if name == "." || name == ".." {
            continue;
        }
        let is_dir = perms.starts_with('d');
        let size: u64 = size_str.parse().unwrap_or(0);
        let perm_str = &perms[1..];
        result.push(format!("{}|{}|{}|{}|{}", name, is_dir, size, perm_str, ""));
    }
    result
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_executeScript(
    mut env: JNIEnv,
    _class: JClass,
    script: JString,
    use_root: jboolean,
) -> jstring {
    let script_str: String = match env.get_string(&script) {
        Ok(s) => s.into(),
        Err(_) => return make_java_string!(env, "Error: Invalid script string"),
    };

    let output = if use_root != 0 {
        let cmd = format!("sh -c '{}'", script_str.replace('\'', "'\\''"));
        run_as_root(&cmd)
    } else {
        match Command::new("sh").arg("-c").arg(&*script_str).output() {
            Ok(out) => {
                let stdout = String::from_utf8_lossy(&out.stdout).to_string();
                let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                let result = format!("{}{}", stdout, stderr);
                (result, out.status.success())
            }
            Err(e) => (format!("Execution failed: {}", e), false),
        }
    };

    let result_json = format!(
        r#"{{"output": {:?}, "success": {}}}"#,
        output.0,
        output.1
    );
    make_java_string!(env, result_json)
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_copyFile(
    mut env: JNIEnv,
    _class: JClass,
    src: JString,
    dst: JString,
    use_root: jboolean,
) -> jboolean {
    let src_str: String = match env.get_string(&src) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let dst_str: String = match env.get_string(&dst) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("cp -rp \"{}\" \"{}\"", src_str, dst_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        match fs::copy(&*src_str, &*dst_str) {
            Ok(_) => 1 as jboolean,
            Err(_) => 0 as jboolean,
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_moveFile(
    mut env: JNIEnv,
    _class: JClass,
    src: JString,
    dst: JString,
    use_root: jboolean,
) -> jboolean {
    let src_str: String = match env.get_string(&src) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let dst_str: String = match env.get_string(&dst) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("mv \"{}\" \"{}\"", src_str, dst_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        match fs::rename(&*src_str, &*dst_str) {
            Ok(_) => 1 as jboolean,
            Err(_) => 0 as jboolean,
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_deleteFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jboolean {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("rm -rf \"{}\"", path_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        let path = Path::new(&*path_str);
        if path.is_dir() {
            fs::remove_dir_all(path).is_ok() as jboolean
        } else {
            fs::remove_file(path).is_ok() as jboolean
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_renameFile(
    mut env: JNIEnv,
    _class: JClass,
    old_path: JString,
    new_path: JString,
    use_root: jboolean,
) -> jboolean {
    let old_str: String = match env.get_string(&old_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let new_str: String = match env.get_string(&new_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("mv \"{}\" \"{}\"", old_str, new_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        fs::rename(&*old_str, &*new_str).is_ok() as jboolean
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_createDirectory(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jboolean {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("mkdir -p \"{}\"", path_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        fs::create_dir_all(&*path_str).is_ok() as jboolean
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_createFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jboolean {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("touch \"{}\"", path_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        fs::File::create(&*path_str).is_ok() as jboolean
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_getFilePermissions(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jstring {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return make_java_string!(env, ""),
    };

    if use_root != 0 {
        let cmd = format!("stat -c '%a:%U:%G:%s:%Y' \"{}\"", path_str);
        let (output, success) = run_as_root(&cmd);
        if success {
            make_java_string!(env, output.trim())
        } else {
            make_java_string!(env, "")
        }
    } else {
        match fs::metadata(&*path_str) {
            Ok(meta) => {
                let mode = meta.permissions().mode();
                let perm_str = format!("{:o}", mode & 0o7777);
                make_java_string!(env, perm_str)
            }
            Err(_) => make_java_string!(env, ""),
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_setFilePermissions(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    mode: JString,
    use_root: jboolean,
) -> jboolean {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let mode_str: String = match env.get_string(&mode) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let cmd = format!("chmod {} \"{}\"", mode_str, path_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        let mode_int: u32 = mode_str.parse().unwrap_or(0o644);
        match fs::metadata(&*path_str) {
            Ok(meta) => {
                let mut perms = meta.permissions();
                perms.set_mode(mode_int);
                fs::set_permissions(&*path_str, perms).is_ok() as jboolean
            }
            Err(_) => 0 as jboolean,
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_readTextFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    use_root: jboolean,
) -> jstring {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return make_java_string!(env, ""),
    };

    if use_root != 0 {
        let cmd = format!("cat \"{}\"", path_str);
        let (output, _) = run_as_root(&cmd);
        make_java_string!(env, output)
    } else {
        match fs::read_to_string(&*path_str) {
            Ok(content) => make_java_string!(env, content),
            Err(_) => make_java_string!(env, ""),
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_writeTextFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    content: JString,
    use_root: jboolean,
) -> jboolean {
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let content_str: String = match env.get_string(&content) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    if use_root != 0 {
        let escaped = content_str.replace('\'', "'\\''");
        let cmd = format!("echo -n '{}' > \"{}\"", escaped, path_str);
        let (_, success) = run_as_root(&cmd);
        if success { 1 as jboolean } else { 0 as jboolean }
    } else {
        match fs::write(&*path_str, &*content_str) {
            Ok(_) => 1 as jboolean,
            Err(_) => 0 as jboolean,
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_checkRootAccess(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let output = Command::new("su")
        .arg("-c")
        .arg("id")
        .output();

    match output {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout);
            if stdout.contains("uid=0") { 1 as jboolean } else { 0 as jboolean }
        }
        Err(_) => 0 as jboolean,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_omo_manager_native_RustBridge_executeScriptWithOutput(
    mut env: JNIEnv,
    _class: JClass,
    script_path: JString,
    use_root: jboolean,
) -> jstring {
    let script_path_str: String = match env.get_string(&script_path) {
        Ok(s) => s.into(),
        Err(_) => return make_java_string!(env, r#"{"output": "Invalid path", "success": false}"#),
    };

    let output = if use_root != 0 {
        let cmd = format!("sh \"{}\"", script_path_str);
        run_as_root(&cmd)
    } else {
        match Command::new("sh").arg(&*script_path_str).output() {
            Ok(out) => {
                let stdout = String::from_utf8_lossy(&out.stdout).to_string();
                let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                (format!("{}{}", stdout, stderr), out.status.success())
            }
            Err(e) => (format!("Execution failed: {}", e), false),
        }
    };

    let result_json = format!(
        r#"{{"output": {:?}, "success": {}}}"#,
        output.0,
        output.1
    );
    make_java_string!(env, result_json)
}
